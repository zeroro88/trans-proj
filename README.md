# trans-proj — 本地 PDF 翻译（预览型）

Spring Boot 编排 **MinerU 解析** 与 **vLLM（OpenAI 兼容 API）翻译**，React 前端：上传 PDF → 轮询任务 → 双栏预览 → 导出 Markdown / DOCX。

详细设计见 Cursor 计划文件 `本地_pdf_翻译平台_*.plan.md`（通常在 `.cursor/plans/`）。

## 仓库结构

| 目录 | 说明 |
|------|------|
| `backend/` | Spring Boot 3.4，Java 17，H2 文件库，异步任务 |
| `frontend/` | Vite 5 + React 18 + TypeScript + Ant Design |
| `docker/` | 可选 `docker-compose` 片段（PostgreSQL profile 等） |

## 快速启动（Mock 管线，无需 MinerU / vLLM）

默认 `app.mock-pipeline=true`，创建任务后立即返回示例对照文本，便于前端与 API 联调。

```bash
cd backend && ./mvnw spring-boot:run
# 另开终端
cd frontend && npm install && npm run dev
```

浏览器打开 <http://localhost:5173>，上传任意 PDF 即可看到 Mock 结果。



## Apple Silicon 本机 vLLM（方案 A：源码 + CPU）

官方 **无** Apple 预编译 wheel，需在 **macOS Sonoma+**、**Xcode CLT** 就绪后从源码安装（见 [vLLM CPU / Apple silicon](https://docs.vllm.ai/en/latest/getting_started/installation/cpu.html)）。

在**仓库根目录**执行（已用 `uv`；与 MinerU 习惯一致）：

```bash
mkdir -p .vllm-build && cd .vllm-build
uv venv --python 3.11 --seed
source .venv/bin/activate
git clone --depth 1 https://github.com/vllm-project/vllm.git
cd vllm
uv pip install -r requirements/cpu.txt --index-strategy unsafe-best-match
uv pip install -e .
```

启动 OpenAI 兼容 API（**Apple Silicon CPU 上请用小模型**，例如 0.5B；`app.llm.model` 须与 `--model` 一致）：

```bash
cd /path/to/trans-proj
./scripts/start-vllm-macos.sh
# 或自定义：VLLM_MODEL=Qwen/Qwen2.5-1.5B-Instruct ./scripts/start-vllm-macos.sh
```

后端使用：`SPRING_PROFILES_ACTIVE=local-mac ./mvnw spring-boot:run`（`local-mac` 下默认模型已为 `Qwen/Qwen2.5-0.5B-Instruct`）。

构建目录 `.vllm-build/` 已加入 `.gitignore`，勿提交。

## 接入真实 MinerU / vLLM

1. 在 `backend/src/main/resources/application.yml` 将 **`app.mock-pipeline`** 设为 **`false`**（或使用 profile `local-mac` / `local-nvidia`，其中已设为 `false`）。
2. 启动 **vLLM ≥ 0.6.0**（或兼容 OpenAI 的本地网关），确保 `POST {app.llm.base-url}/chat/completions` 可用。
3. 部署 **MinerU HTTP 网关**，实现占位契约：
   - `POST {app.mineru.base-url}/v1/parse`
   - `multipart/form-data` 字段名 **`file`**（PDF）
   - 响应 JSON 至少包含其一：`markdown`、`text`、或 `blocks[].text`
4. 根据实际网关调整 `HttpMinerUClient` 的 URL 与 JSON 解析逻辑。

## 配置摘要（`application.yml`）

- `app.storage.base-dir`：上传与 H2 数据目录（默认 `./data`）
- `app.mineru.base-url`：默认 `http://127.0.0.1:8001`
- `app.llm.base-url`：默认 `http://127.0.0.1:8000/v1`
- `app.llm.model`：与 vLLM 加载的模型名一致

## API

- `POST /api/jobs` — `multipart/form-data`：`file`, `sourceLang`, `targetLang`
- `GET /api/jobs/{id}` — 状态、进度、`segments`（完成后）
- `GET /api/jobs/{id}/export?format=md|docx`

## 验证

```bash
cd backend && ./mvnw test
cd frontend && npm run build
```

`backend/` 下已包含 **Maven Wrapper**（`mvnw`），未全局安装 Maven 也可构建。

## 前端与 Node 版本

当前 `package.json` 使用 **react-router-dom v6**，以便在 **Node 18** 上安装；若使用 Node 20+，可自行升级到 v7。
