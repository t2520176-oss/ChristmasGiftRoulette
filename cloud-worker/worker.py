"""FigureCraft cloud worker: a small FastAPI server around a replaceable ModelEngine.

Protocol (used by the desktop app's GenericHttpProvider):
  GET  /health                 -> status, engine, capabilities
  POST /v1/jobs   (multipart)  -> {"job_id"}   fields: params (JSON string), image (optional PNG/JPG/WEBP)
  GET  /v1/jobs/{id}           -> state/stage/progress/gpu_seconds/error
  GET  /v1/jobs/{id}/result    -> GLB file
  POST /v1/jobs/{id}/cancel
  POST /v1/shutdown            -> stop the GPU/pod (if allowed)
All routes require `Authorization: Bearer <WORKER_API_KEY>`.
"""
from __future__ import annotations

import hmac
import io
import json
import logging
import os
import queue
import shutil
import signal
import threading
import time
import uuid
from pathlib import Path

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.responses import FileResponse
from PIL import Image

import config as config_mod
from engines.registry import create_engine

log = logging.getLogger("worker")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

cfg = config_mod.load()
cfg.validate()
engine = create_engine(cfg.engine, cfg)
app = FastAPI(title="FigureCraft worker", version="0.1.0")

JOBS: dict[str, dict] = {}
Q: "queue.Queue[str]" = queue.Queue(maxsize=cfg.max_queue)
LAST_ACTIVITY = time.time()
LOCK = threading.Lock()


def auth(authorization: str | None = Header(default=None)) -> None:
    if cfg.allow_no_auth and not cfg.api_key:
        return
    token = (authorization or "").removeprefix("Bearer ").strip()
    if not hmac.compare_digest(token.encode(), cfg.api_key.encode()):
        raise HTTPException(status_code=401, detail="invalid api key")


def _touch():
    global LAST_ACTIVITY
    LAST_ACTIVITY = time.time()


def _clean_old():
    cutoff = time.time() - cfg.keep_results_min * 60
    for jid, j in list(JOBS.items()):
        if j["state"] in ("succeeded", "failed", "cancelled") and j["finished"] and j["finished"] < cutoff:
            shutil.rmtree(cfg.work_dir / jid, ignore_errors=True)
            JOBS.pop(jid, None)


def _run_job(jid: str) -> None:
    j = JOBS[jid]
    if j["cancel"].is_set():
        j.update(state="cancelled", finished=time.time())
        return
    j.update(state="running", stage="starting", started=time.time())
    d = cfg.work_dir / jid
    try:
        def progress(stage: str, frac):
            j["stage"] = stage
            j["progress"] = frac
            if j["cancel"].is_set():
                raise RuntimeError("cancelled")
        params = j["params"]
        img = Image.open(d / "input.png") if (d / "input.png").exists() else None
        prompt = params.get("prompt", "")
        engine.load()
        if img is not None and prompt and engine.supports_text_image:
            out = engine.generate_from_text_and_image(prompt, img, params, d, progress)
        elif img is not None:
            out = engine.generate_from_image(img, params, d, progress)
        else:
            out = engine.generate_from_text(prompt, params, d, progress)
        j.update(state="succeeded", stage="done", result=str(out))
    except Exception as exc:  # noqa: BLE001
        if j["cancel"].is_set():
            j.update(state="cancelled")
        else:
            log.exception("job %s failed", jid)
            j.update(state="failed", error=str(exc)[:500])
    finally:
        j["finished"] = time.time()
        j["gpu_seconds"] = round(j["finished"] - (j.get("started") or j["finished"]), 1)
        _touch()


def _worker_loop():
    while True:
        jid = Q.get()
        try:
            _run_job(jid)
        finally:
            Q.task_done()


def stop_gpu() -> str:
    """Stop the paid resource. RunPod pod: REST stop. Otherwise terminate this process (a supervisor/orchestrator
    should then release the instance)."""
    if cfg.runpod_pod_id and cfg.runpod_api_key:
        import requests
        r = requests.post(f"https://rest.runpod.io/v1/pods/{cfg.runpod_pod_id}/stop",
                          headers={"Authorization": f"Bearer {cfg.runpod_api_key}"}, timeout=30)
        log.info("RunPod stop requested: HTTP %s", r.status_code)
        return f"runpod stop HTTP {r.status_code}"
    log.info("shutting down worker process")
    threading.Timer(1.0, lambda: os.kill(os.getpid(), signal.SIGTERM)).start()
    return "process exit scheduled"


def _idle_watchdog():
    while cfg.idle_shutdown_min > 0:
        time.sleep(30)
        busy = any(j["state"] in ("queued", "running") for j in JOBS.values())
        if not busy and time.time() - LAST_ACTIVITY > cfg.idle_shutdown_min * 60:
            log.warning("idle for %d min -> stopping to avoid billing", cfg.idle_shutdown_min)
            stop_gpu()
            return


@app.on_event("startup")
def _startup():
    threading.Thread(target=_worker_loop, daemon=True).start()
    if cfg.idle_shutdown_min > 0:
        threading.Thread(target=_idle_watchdog, daemon=True).start()
    log.info("worker started: engine=%s idle_shutdown=%s min", cfg.engine, cfg.idle_shutdown_min)


@app.get("/health", dependencies=[Depends(auth)])
def health():
    _touch()
    return {"status": "ok", "engine": engine.name, "engines": [engine.name], "capabilities": engine.capabilities(),
            "queue": Q.qsize()}


@app.post("/v1/jobs", dependencies=[Depends(auth)])
async def create_job(params: str = Form(...), image: UploadFile | None = File(default=None)):
    _touch()
    _clean_old()
    try:
        p = json.loads(params)
        assert isinstance(p, dict)
    except Exception:
        raise HTTPException(400, "params must be a JSON object")
    if len(str(p.get("prompt", ""))) > 4000:
        raise HTTPException(400, "prompt too long")
    jid = uuid.uuid4().hex
    d = cfg.work_dir / jid
    d.mkdir(parents=True, exist_ok=True)
    if image is not None:
        raw = await image.read(cfg.max_upload_mb * 1024 * 1024 + 1)
        if len(raw) > cfg.max_upload_mb * 1024 * 1024:
            shutil.rmtree(d, ignore_errors=True)
            raise HTTPException(413, "image too large")
        try:
            im = Image.open(io.BytesIO(raw))
            if im.format not in ("PNG", "JPEG", "WEBP"):
                raise ValueError("format")
            im.load()
            im.convert("RGBA").save(d / "input.png")
        except Exception:
            shutil.rmtree(d, ignore_errors=True)
            raise HTTPException(400, "invalid image")
        if not (engine.supports_image or engine.supports_text_image):
            shutil.rmtree(d, ignore_errors=True)
            raise HTTPException(400, "engine does not support image input")
    JOBS[jid] = {"state": "queued", "stage": "queued", "progress": None, "params": p, "cancel": threading.Event(),
                 "created": time.time(), "started": None, "finished": None, "error": None, "result": None}
    try:
        Q.put_nowait(jid)
    except queue.Full:
        JOBS.pop(jid, None)
        shutil.rmtree(d, ignore_errors=True)
        raise HTTPException(429, "queue is full")
    return {"job_id": jid}


def _job(jid: str) -> dict:
    j = JOBS.get(jid)
    if not j:
        raise HTTPException(404, "unknown job")
    return j


@app.get("/v1/jobs/{jid}", dependencies=[Depends(auth)])
def job_status(jid: str):
    _touch()
    j = _job(jid)
    live = round(time.time() - j["started"], 1) if j["started"] and not j["finished"] else None
    return {"state": j["state"], "stage": j["stage"], "progress": j["progress"], "message": "",
            "gpu_seconds": j.get("gpu_seconds", live), "error": j["error"]}


@app.get("/v1/jobs/{jid}/result", dependencies=[Depends(auth)])
def job_result(jid: str):
    _touch()
    j = _job(jid)
    if j["state"] != "succeeded" or not j["result"]:
        raise HTTPException(409, "result not ready")
    return FileResponse(j["result"], media_type="model/gltf-binary", filename="figure.glb")


@app.post("/v1/jobs/{jid}/cancel", dependencies=[Depends(auth)])
def job_cancel(jid: str):
    j = _job(jid)
    j["cancel"].set()
    if j["state"] == "queued":
        j.update(state="cancelled", finished=time.time())
    return {"ok": True}


@app.post("/v1/shutdown", dependencies=[Depends(auth)])
def shutdown():
    if not cfg.allow_shutdown:
        raise HTTPException(403, "shutdown not allowed (ALLOW_SHUTDOWN=0)")
    return {"ok": True, "detail": stop_gpu()}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=cfg.host, port=cfg.port, log_level="info")
