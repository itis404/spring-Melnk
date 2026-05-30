#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BRIDGE_LOG="$ROOT_DIR/target/telegram-bridge.log"

cd "$ROOT_DIR"
mkdir -p target

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker не найден. Установите Docker Desktop и запустите его." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon не запущен или недоступен." >&2
  echo "Откройте Docker Desktop, дождитесь статуса Running, затем повторите:" >&2
  echo "./scripts/run_docker_with_telegram.sh" >&2
  exit 1
fi

python3 scripts/telegram_api_bridge.py --host 127.0.0.1 --port 9091 > "$BRIDGE_LOG" 2>&1 &
BRIDGE_PID=$!

cleanup() {
  kill "$BRIDGE_PID" 2>/dev/null || true
}

trap cleanup EXIT INT TERM

echo "Telegram API bridge started on 127.0.0.1:9091"
echo "Bridge log: $BRIDGE_LOG"
echo "Waiting for Telegram API bridge..."

READY=0
for _ in 1 2 3 4 5; do
  if python3 -c 'import json, urllib.request; data=json.loads(urllib.request.urlopen("http://127.0.0.1:9091/health", timeout=2).read().decode()); raise SystemExit(0 if data.get("version") == "1.1-chunked" else 1)' >/dev/null 2>&1; then
    READY=1
    break
  fi
  sleep 1
done

if [ "$READY" -ne 1 ]; then
  echo "Telegram API bridge не поднялся. Лог:" >&2
  tail -40 "$BRIDGE_LOG" >&2 || true
  exit 1
fi

echo "Starting Docker Compose..."

docker compose up --build
