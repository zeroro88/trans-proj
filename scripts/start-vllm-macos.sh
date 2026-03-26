#!/usr/bin/env bash
# 本机已通过源码安装的 vLLM（见 README「Apple Silicon 本机 vLLM」）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENV="$ROOT/.vllm-build/.venv"
SRC="$ROOT/.vllm-build/vllm"
if [[ ! -f "$VENV/bin/python" || ! -d "$SRC" ]]; then
  echo "未找到 $ROOT/.vllm-build；请先在仓库根目录按 README 完成 vLLM 源码安装。" >&2
  exit 1
fi
# shellcheck source=/dev/null
source "$VENV/bin/activate"
cd "$SRC"
export VLLM_MODEL="${VLLM_MODEL:-Qwen/Qwen2.5-0.5B-Instruct}"
export VLLM_PORT="${VLLM_PORT:-8000}"
export VLLM_MAX_LEN="${VLLM_MAX_LEN:-2048}"
exec python -m vllm.entrypoints.openai.api_server \
  --model "$VLLM_MODEL" \
  --port "$VLLM_PORT" \
  --max-model-len "$VLLM_MAX_LEN" \
  "$@"
