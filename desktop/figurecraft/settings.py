"""User settings stored as JSON in the app-data folder. API keys are never stored in plain text."""
from __future__ import annotations

import json
import os
from dataclasses import asdict, dataclass, field, fields
from pathlib import Path

from . import logging_setup, paths, secret_store

DEFAULT_AMS = [
    {"hex": "#F2EDE0", "name": "IVORY"},
    {"hex": "#7A3FC4", "name": "PURPLE"},
    {"hex": "#111111", "name": "BLACK"},
    {"hex": "#FFD21F", "name": "YELLOW"},
]

PROVIDERS = ["mock", "generic_http", "runpod"]
ENGINES = ["trellis", "hunyuan3d", "mock"]


@dataclass
class Settings:
    language: str = "ko"
    theme: str = "dark"
    project_dir: str = ""
    bambu_path: str = ""
    # cloud
    provider: str = "mock"
    endpoint: str = ""
    engine: str = "trellis"
    timeout_s: int = 900
    auto_stop_gpu: bool = True
    runpod_pod_id: str = ""
    api_key_enc: str = ""
    runpod_api_key_enc: str = ""      # RunPod account key (Pod start/stop). Separate from the worker's own key.
    max_upload_mb: int = 20
    max_download_mb: int = 400
    # printing
    nozzle_mm: float = 0.4
    min_feature_mm: float = 1.2
    wall_mm: float = 1.5
    default_height_mm: float = 100.0
    default_colors: int = 4
    # bambu / export
    ams_presets: dict = field(default_factory=lambda: {"Classic": list(DEFAULT_AMS)})
    ams_active_preset: str = "Classic"
    default_3mf_mode: str = "multipart"   # multipart | color

    # ---- API key helpers (env var wins; never logged) ----
    def get_api_key(self) -> str:
        env = os.environ.get("FIGURECRAFT_API_KEY", "")
        key = env or secret_store.decrypt(self.api_key_enc)
        logging_setup.register_secret(key)
        return key

    def set_api_key(self, key: str) -> None:
        self.api_key_enc = secret_store.encrypt(key.strip()) if key else ""
        logging_setup.register_secret(key)

    def get_runpod_api_key(self) -> str:
        key = os.environ.get("RUNPOD_API_KEY", "") or secret_store.decrypt(self.runpod_api_key_enc)
        logging_setup.register_secret(key)
        return key

    def set_runpod_api_key(self, key: str) -> None:
        self.runpod_api_key_enc = secret_store.encrypt(key.strip()) if key else ""
        logging_setup.register_secret(key)

    def projects_path(self) -> Path:
        if self.project_dir:
            p = Path(self.project_dir)
            p.mkdir(parents=True, exist_ok=True)
            return p
        return paths.default_projects_dir()

    # ---- persistence ----
    @classmethod
    def path(cls) -> Path:
        return paths.app_data_dir() / "settings.json"

    @classmethod
    def load(cls) -> "Settings":
        s = cls()
        try:
            data = json.loads(cls.path().read_text(encoding="utf-8"))
            names = {f.name for f in fields(cls)}
            for k, v in data.items():
                if k in names:
                    setattr(s, k, v)
        except FileNotFoundError:
            pass
        except Exception as exc:  # corrupt settings must not block startup
            logging_setup.get_logger("settings").warning("settings unreadable, using defaults: %s", exc)
        s.get_api_key()
        s.get_runpod_api_key()
        return s

    def save(self) -> None:
        tmp = self.path().with_suffix(".tmp")
        tmp.write_text(json.dumps(asdict(self), indent=2, ensure_ascii=False), encoding="utf-8")
        tmp.replace(self.path())
