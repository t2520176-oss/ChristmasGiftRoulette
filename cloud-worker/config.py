"""Worker configuration. Everything comes from environment variables (see .env.example). No secrets in code."""
from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path


def _bool(name: str, default: bool) -> bool:
    v = os.environ.get(name)
    return default if v is None else v.strip().lower() in ("1", "true", "yes", "on")


@dataclass
class Config:
    api_key: str = field(default_factory=lambda: os.environ.get("WORKER_API_KEY", ""))
    allow_no_auth: bool = field(default_factory=lambda: _bool("ALLOW_NO_AUTH", False))
    engine: str = field(default_factory=lambda: os.environ.get("ENGINE", "trellis").lower())
    host: str = field(default_factory=lambda: os.environ.get("HOST", "0.0.0.0"))
    port: int = field(default_factory=lambda: int(os.environ.get("PORT", "8000")))
    work_dir: Path = field(default_factory=lambda: Path(os.environ.get("WORK_DIR", "/tmp/figurecraft-worker")))
    max_upload_mb: int = field(default_factory=lambda: int(os.environ.get("MAX_UPLOAD_MB", "20")))
    max_queue: int = field(default_factory=lambda: int(os.environ.get("MAX_QUEUE", "4")))
    keep_results_min: int = field(default_factory=lambda: int(os.environ.get("KEEP_RESULTS_MIN", "60")))
    # cost saving
    allow_shutdown: bool = field(default_factory=lambda: _bool("ALLOW_SHUTDOWN", True))
    idle_shutdown_min: int = field(default_factory=lambda: int(os.environ.get("IDLE_SHUTDOWN_MIN", "0")))
    runpod_pod_id: str = field(default_factory=lambda: os.environ.get("RUNPOD_POD_ID", ""))
    runpod_api_key: str = field(default_factory=lambda: os.environ.get("RUNPOD_API_KEY", ""))
    # models
    t2i_model: str = field(default_factory=lambda: os.environ.get("T2I_MODEL", "stabilityai/stable-diffusion-xl-base-1.0"))
    t2i_steps: int = field(default_factory=lambda: int(os.environ.get("T2I_STEPS", "30")))
    trellis_model: str = field(default_factory=lambda: os.environ.get("TRELLIS_MODEL", "JeffreyXiang/TRELLIS-image-large"))
    hunyuan_model: str = field(default_factory=lambda: os.environ.get("HUNYUAN_MODEL", "tencent/Hunyuan3D-2"))
    hunyuan_texture: bool = field(default_factory=lambda: _bool("HUNYUAN_TEXTURE", True))
    texture_size: int = field(default_factory=lambda: int(os.environ.get("TEXTURE_SIZE", "1024")))
    mesh_simplify: float = field(default_factory=lambda: float(os.environ.get("MESH_SIMPLIFY", "0.95")))
    hf_token: str = field(default_factory=lambda: os.environ.get("HF_TOKEN", ""))
    # optional S3-compatible result upload for the RunPod serverless handler (large GLBs)
    s3_bucket: str = field(default_factory=lambda: os.environ.get("S3_BUCKET", ""))
    s3_endpoint: str = field(default_factory=lambda: os.environ.get("S3_ENDPOINT_URL", ""))

    def validate(self) -> None:
        if not self.api_key and not self.allow_no_auth:
            raise SystemExit("WORKER_API_KEY is not set. Set it (recommended) or ALLOW_NO_AUTH=1 for a local test.")
        self.work_dir.mkdir(parents=True, exist_ok=True)


def load() -> Config:
    return Config()
