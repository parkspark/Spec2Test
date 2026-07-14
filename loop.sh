#!/usr/bin/env bash
# 사용법: ./loop.sh [최대 회차 수]  (기본 30회)
set -uo pipefail

MAX_ITER="${1:-30}"
FAIL_STREAK=0          # 연속 실패 서킷 브레이커
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RETRY_MAX=3            # codex exec 자체 재시도 횟수 (capacity 에러 등 일시 실패 대비)
RETRY_WAIT=60          # 재시도 사이 대기(초)

for i in $(seq 1 "$MAX_ITER"); do
  echo "===== Ralph Loop #$i / $MAX_ITER ====="

  # 완료 판정: 백로그에 미완료 항목이 없으면 종료
  if ! grep -q "^- \[ \]" plan/BACKLOG.md; then
    echo "✅ 백로그 전체 완료. 루프 종료."
    break
  fi

  # codex exec 자체 재시도 (모델 capacity 등 일시적 실패 대비)
  ATTEMPT=1
  EXIT_CODE=1
  while [ "$ATTEMPT" -le "$RETRY_MAX" ] && [ "$EXIT_CODE" -ne 0 ]; do
    if [ "$ATTEMPT" -gt 1 ]; then
      echo "🔁 codex exec 재시도 ${ATTEMPT}/${RETRY_MAX} (${RETRY_WAIT}초 대기 후)"
      sleep "$RETRY_WAIT"
    fi

    # 다른 루트에서 loop.sh 실행할 때 -C로 작업 디렉토리 고정
    codex exec \
      --sandbox workspace-write \
      --dangerously-bypass-hook-trust \
      -c model_reasoning_effort="medium" \
      -c approval_policy="never" \
      -C "$REPO_DIR" \
      "$(cat PROMPT.md)"
    EXIT_CODE=$?

    ATTEMPT=$((ATTEMPT + 1))
  done

  if [ "$EXIT_CODE" -ne 0 ]; then
    echo "❌ ${RETRY_MAX}회 재시도에도 실패 (exit $EXIT_CODE). 이번 회차는 실패로 처리."
  fi

  # 검증: 커밋이 실제로 생겼는지 확인
  if git log --oneline -1 --since="10 minutes ago" | grep -q .; then
    FAIL_STREAK=0
  else
    FAIL_STREAK=$((FAIL_STREAK + 1))
    echo "⚠️ 이번 회차에 커밋 없음 (연속 $FAIL_STREAK회)"
  fi

  # 서킷 브레이커: 3회 연속 진전 없으면 사람 개입 요청
  if [ "$FAIL_STREAK" -ge 3 ]; then
    echo "🛑 서킷 브레이커 작동. PROGRESS.md의 BLOCKED 항목을 확인하고 사람이 개입할 것."
    break
  fi

  sleep 5
done