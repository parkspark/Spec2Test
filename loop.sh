#!/usr/bin/env bash
# 사용법: ./loop.sh [최대 회차 수]  (기본 30회)
set -uo pipefail

MAX_ITER="${1:-30}"
FAIL_STREAK=0          # 연속 실패 서킷 브레이커
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for i in $(seq 1 "$MAX_ITER"); do
  echo "===== Ralph Loop #$i / $MAX_ITER ====="

  # 완료 판정: 백로그에 미완료 항목이 없으면 종료
  if ! grep -q "^- \[ \]" plan/BACKLOG.md; then
    echo "✅ 백로그 전체 완료. 루프 종료."
    break
  fi

  codex exec \
    --sandbox workspace-write \
    --dangerously-bypass-hook-trust \
    # 다른 루트에서 loop.sh 실행할 때 사용
    -C "$REPO_DIR" \
    "$(cat PROMPT.md)"
  EXIT_CODE=$?

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