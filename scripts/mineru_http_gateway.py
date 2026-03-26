#!/usr/bin/env python3
"""Minimal HTTP gateway for MinerU CLI, matching HttpMinerUClient."""
from __future__ import annotations

import os
import subprocess
import tempfile
from pathlib import Path

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

app = FastAPI(title="MinerU local gateway")

MINERU_BIN = os.environ.get("MINERU_BIN", "mineru")
_extra = os.environ.get("MINERU_EXTRA_ARGS", "").strip()
MINERU_EXTRA_ARGS = _extra.split() if _extra else []
MINERU_TIMEOUT = int(os.environ.get("MINERU_TIMEOUT", "600"))


@app.post("/v1/parse")
async def parse_v1(file: UploadFile = File(...)):
    if not file.filename:
        return JSONResponse({"error": "missing filename"}, status_code=400)

    raw = await file.read()
    if not raw:
        return JSONResponse({"error": "empty file"}, status_code=400)

    suffix = Path(file.filename).suffix.lower()
    if suffix not in (".pdf", ".png", ".jpg", ".jpeg"):
        suffix = ".pdf"

    with tempfile.TemporaryDirectory(prefix="mineru-gw-") as tmp:
        tmp_path = Path(tmp)
        in_path = tmp_path / f"input{suffix}"
        in_path.write_bytes(raw)
        out_dir = tmp_path / "out"
        out_dir.mkdir()

        cmd = [MINERU_BIN, "-p", str(in_path), "-o", str(out_dir), "-b", "pipeline"]
        cmd.extend(MINERU_EXTRA_ARGS)

        try:
            proc = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=MINERU_TIMEOUT,
                env=os.environ.copy(),
            )
        except subprocess.TimeoutExpired:
            return JSONResponse({"error": "mineru timeout"}, status_code=504)

        if proc.returncode != 0:
            err = (proc.stderr or proc.stdout or "mineru failed").strip()
            return JSONResponse({"error": err[:8000]}, status_code=502)

        md_files = list(out_dir.rglob("*.md"))
        if not md_files:
            return JSONResponse({"error": "no markdown produced"}, status_code=502)

        md_files.sort(key=lambda p: p.stat().st_size, reverse=True)
        markdown = md_files[0].read_text(encoding="utf-8", errors="replace")
        return {"markdown": markdown}
