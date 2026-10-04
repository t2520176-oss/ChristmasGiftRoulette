"""Demo provider: returns bundled procedural figures. It never claims to be AI output."""
from __future__ import annotations

import tempfile
import time
from pathlib import Path

from ..core import demo, mesh_io
from ..core.models import ConnectionInfo, GenerationRequest, JobStatus, ProviderCapabilities, new_id
from ..errors import ProviderError
from .base import GenerationProvider, ProgressCB


class MockProvider(GenerationProvider):
    id = "mock"
    display_name = "Demo (offline)"
    SIM_SECONDS = 1.2

    def __init__(self, settings):
        super().__init__(settings)
        self._jobs: dict[str, dict] = {}

    def capabilities(self) -> ProviderCapabilities:
        return ProviderCapabilities(text=True, image=False, text_image=False, lifecycle=False,
                                    reports_progress=False, is_demo=True, engines=["demo"])

    def test_connection(self) -> ConnectionInfo:
        return ConnectionInfo(ok=True, message="DEMO", engine="demo", capabilities=self.capabilities(), latency_ms=0.0)

    def manual_instructions(self, lang: str = "ko") -> str:
        return ("데모 모드에는 GPU가 없으므로 종료할 것이 없습니다." if lang == "ko"
                else "Demo Mode uses no GPU, so there is nothing to stop.")

    def submit(self, request: GenerationRequest) -> str:
        jid = new_id()
        self._jobs[jid] = {"t0": time.monotonic(), "figure": demo.pick_demo(request.full_prompt()),
                           "height": request.print.height_mm}
        return jid

    def poll(self, job_id: str) -> JobStatus:
        job = self._jobs.get(job_id)
        if job is None:
            raise ProviderError("bad_response", "unknown demo job")
        if time.monotonic() - job["t0"] < self.SIM_SECONDS:
            return JobStatus(state="running", stage="demo", progress=None, message="DEMO")
        return JobStatus(state="succeeded", stage="demo", progress=None, message="DEMO", gpu_seconds=0.0)

    def download(self, job_id: str, dest: Path, progress: ProgressCB | None = None) -> Path:
        job = self._jobs[job_id]
        mesh = demo.build_demo(job["figure"], job["height"])
        dest = Path(dest)
        dest.parent.mkdir(parents=True, exist_ok=True)
        mesh_io.save_glb(mesh, dest)
        self.last_figure = job["figure"]
        if progress:
            progress(dest.stat().st_size, dest.stat().st_size)
        return dest
