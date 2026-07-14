#!/usr/bin/env bash
# 사용법: ./loop.sh [최대 회차 수]  (기본 30회)
set -uo pipefail

MAX_ITER="${1:-30}"
FAIL_STREAK=0
RETRY_MAX=3
RETRY_WAIT=60

CURRENT_BRANCH="$(git branch --show-current)"
if [ "$CURRENT_BRANCH" = "main" ]; then
  echo "🛑 main 브랜치에서 실행 시도됨. feat/ralf-loop-* 브랜치로 체크아웃 후 재실행하세요."
  exit 1
fi

for i in $(seq 1 "$MAX_ITER"); do
  echo "===== Ralph Loop #$i / $MAX_ITER (branch: $CURRENT_BRANCH) ====="

  if ! grep -q "^- \[ \]" plan/BACKLOG.md; then
    echo "✅ 백로그 전체 완료. 루프 종료."
    break
  fi

  ATTEMPT=1
  EXIT_CODE=1
  while [ "$ATTEMPT" -le "$RETRY_MAX" ] && [ "$EXIT_CODE" -ne 0 ]; do
    if [ "$ATTEMPT" -gt 1 ]; then
      echo "🔁 codex exec 재시도 ${ATTEMPT}/${RETRY_MAX} (${RETRY_WAIT}초 대기 후)"
      sleep "$RETRY_WAIT"
    fi

    codex exec \
      --sandbox workspace-write \
      --dangerously-bypass-hook-trust \
      -c model_reasoning_effort="medium" \
      -c approval_policy="never" \
      -c sandbox_workspace_write.network_access=true \
      "$(cat PROMPT.md)"
    EXIT_CODE=$?

    ATTEMPT=$((ATTEMPT + 1))
  done

  if [ "$EXIT_CODE" -ne 0 ]; then
    echo "❌ ${RETRY_MAX}회 재시도에도 실패 (exit $EXIT_CODE). 이번 회차는 실패로 처리."
  fi

  # --- ACL DENY 재발 대응: 리셋 후 커밋 안 된 변경사항 자동 커밋 ---
  MSYS_NO_PATHCONV=1 icacls .git /reset /T /C >/dev/null 2>&1 || true

  if [ -n "$(git status --porcelain)" ]; then
    echo "⚠️ 커밋되지 않은 변경 감지 (ACL 문제로 추정) — 리셋 후 자동 커밋 시도"
    git add -A
    git commit -m "[auto] ACL 권한 문제로 지연된 자동 커밋 (회차 #$i, 라벨 확인 필요)"
  fi

  # 검증: 커밋이 실제로 생겼는지 확인
  if git log --oneline -1 --since="10 minutes ago" | grep -q .; then
    FAIL_STREAK=0
  else
    FAIL_STREAK=$((FAIL_STREAK + 1))
    echo "⚠️ 이번 회차에 커밋 없음 (연속 $FAIL_STREAK회)"
  fi

  if [ "$FAIL_STREAK" -ge 3 ]; then
    echo "🛑 서킷 브레이커 작동. PROGRESS.md의 BLOCKED 항목을 확인하고 사람이 개입할 것."
    break
  fi

  sleep 5
done