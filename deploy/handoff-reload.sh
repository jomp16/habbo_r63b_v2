#!/usr/bin/env bash
set -euo pipefail

CURRENT_DIR="/opt/habbo/current"
SHARED_DIR="/opt/habbo/shared"
HANDOFF_SOCK="$SHARED_DIR/run/handoff.sock"
OLD_PID="${MAINPID:-}"

if [[ ! -S "$HANDOFF_SOCK" ]]; then
  echo "handoff socket not found at $HANDOFF_SOCK" >&2
  exit 1
fi

cd "$CURRENT_DIR"

PID_FILE="$(mktemp)"
trap 'rm -f "$PID_FILE"' EXIT

(
  /usr/lib/jvm/java-25-openjdk/bin/java \
    -Xms256m -Xmx768m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=20 \
    -XX:G1ReservePercent=15 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:+ParallelRefProcEnabled \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Dkotlinx.coroutines.debug=off \
    -Dlog4j2.configurationFile=log4j2-prod.xml \
    -Dhabbo.handoff.socket="$HANDOFF_SOCK" \
    -jar launcher-0.1.0-SNAPSHOT.jar \
    </dev/null >> "$SHARED_DIR/logs/handoff-reload.log" 2>&1 &
  echo $! > "$PID_FILE"
)

NEW_PID="$(cat "$PID_FILE")"

for _ in $(seq 1 90); do
  if ! kill -0 "$NEW_PID" 2>/dev/null; then
    echo "New emulator (PID $NEW_PID) exited prematurely during handoff" >&2
    exit 1
  fi
  if [[ -n "$OLD_PID" ]] && ! kill -0 "$OLD_PID" 2>/dev/null; then
    exit 0
  fi
  sleep 0.5
done

echo "Timed out waiting for old emulator (PID $OLD_PID) to exit after handoff" >&2
exit 1
