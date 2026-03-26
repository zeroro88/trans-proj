#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/scripts"
export MINERU_PORT="${MINERU_PORT:-8001}"
exec uvicorn mineru_http_gateway:app --host 127.0.0.1 --port "$MINERU_PORT"
