"""RunPod *serverless* entry point (alternative to worker.py).

Input  (job["input"]): the same params the HTTP worker takes, plus optional "image_base64" (PNG/JPG/WEBP).
Output: {"glb_base64": "..."} (<= ~15 MB) or {"glb_url": "https://..."} when S3_BUCKET is configured,
        plus "gpu_seconds". Serverless workers scale to zero on their own when idle -> no extra stop call is needed.
"""
from __future__ import annotations

import base64
import io
import tempfile
import time
import uuid
from pathlib import Path

import runpod  # type: ignore
from PIL import Image

import config as config_mod
from engines.registry import create_engine

cfg = config_mod.load()
cfg.work_dir.mkdir(parents=True, exist_ok=True)
engine = create_engine(cfg.engine, cfg)
INLINE_LIMIT = 15 * 1024 * 1024


def _upload(path: Path) -> str:
    import boto3  # type: ignore
    key = f"figurecraft/{uuid.uuid4().hex}.glb"
    s3 = boto3.client("s3", endpoint_url=cfg.s3_endpoint or None)
    s3.upload_file(str(path), cfg.s3_bucket, key, ExtraArgs={"ContentType": "model/gltf-binary"})
    return s3.generate_presigned_url("get_object", Params={"Bucket": cfg.s3_bucket, "Key": key}, ExpiresIn=3600)


def handler(job: dict) -> dict:
    t0 = time.time()
    p = dict(job.get("input") or {})
    b64 = p.pop("image_base64", None)
    img = None
    if b64:
        raw = base64.b64decode(b64, validate=True)
        if len(raw) > cfg.max_upload_mb * 1024 * 1024:
            return {"error": "image too large"}
        img = Image.open(io.BytesIO(raw))
        if img.format not in ("PNG", "JPEG", "WEBP"):
            return {"error": "invalid image"}
        img = img.convert("RGBA")
    d = Path(tempfile.mkdtemp(dir=cfg.work_dir))
    noop = lambda stage, frac: None  # noqa: E731
    try:
        engine.load()
        prompt = p.get("prompt", "")
        if img is not None and prompt and engine.supports_text_image:
            out = engine.generate_from_text_and_image(prompt, img, p, d, noop)
        elif img is not None:
            out = engine.generate_from_image(img, p, d, noop)
        else:
            out = engine.generate_from_text(prompt, p, d, noop)
        size = out.stat().st_size
        res: dict = {"gpu_seconds": round(time.time() - t0, 1)}
        if size <= INLINE_LIMIT:
            res["glb_base64"] = base64.b64encode(out.read_bytes()).decode("ascii")
        elif cfg.s3_bucket:
            res["glb_url"] = _upload(out)
        else:
            return {"error": f"result is {size / 1e6:.0f} MB; set S3_BUCKET (+ AWS credentials) to return large files"}
        return res
    except Exception as exc:  # noqa: BLE001
        return {"error": str(exc)[:500]}


if __name__ == "__main__":
    runpod.serverless.start({"handler": handler})
