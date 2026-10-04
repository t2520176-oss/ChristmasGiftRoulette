"""Provider interface. The desktop app talks only to this; cloud vendors live behind it."""
from __future__ import annotations

import abc
import io
import time
from pathlib import Path
from typing import Callable
from urllib.parse import urlparse

from PIL import Image

from ..core.models import ConnectionInfo, GenerationRequest, JobStatus, ProviderCapabilities
from ..errors import ProviderError
from ..settings import Settings

ProgressCB = Callable[[int, int | None], None]     # (bytes_done, bytes_total)
GLB_MAGIC = b"glTF"


def validate_endpoint(url: str) -> str:
    url = (url or "").strip().rstrip("/")
    if not url:
        raise ProviderError("no_endpoint")
    p = urlparse(url)
    if p.scheme not in ("http", "https") or not p.netloc:
        raise ProviderError("bad_endpoint", url)
    return url


def prepare_reference_image(path: str | Path, max_mb: int, max_side: int = 1536) -> bytes:
    """Validate (PNG/JPG/WEBP), downscale and re-encode as PNG. Strips EXIF/metadata before upload."""
    try:
        with Image.open(path) as im:
            if im.format not in ("PNG", "JPEG", "WEBP"):
                raise ProviderError("bad_image", f"format {im.format}")
            im = im.convert("RGBA") if im.mode in ("RGBA", "LA", "P") else im.convert("RGB")
            im.thumbnail((max_side, max_side))
            buf = io.BytesIO()
            im.save(buf, "PNG", optimize=True)
    except ProviderError:
        raise
    except Exception as exc:
        raise ProviderError("bad_image", str(exc)) from exc
    data = buf.getvalue()
    if len(data) > max_mb * 1024 * 1024:
        raise ProviderError("upload_too_large", f"{len(data)} bytes", mb=max_mb)
    return data


class GenerationProvider(abc.ABC):
    id = "base"
    display_name = "Base"

    def __init__(self, settings: Settings):
        self.settings = settings

    # ---- capability / connection
    @abc.abstractmethod
    def capabilities(self) -> ProviderCapabilities: ...

    @abc.abstractmethod
    def test_connection(self) -> ConnectionInfo: ...

    # ---- optional GPU lifecycle (cost saving). Default: not supported -> manual instructions.
    @property
    def supports_lifecycle(self) -> bool:
        return self.capabilities().lifecycle

    def start_gpu(self, progress: Callable[[str], None] | None = None) -> None:
        raise ProviderError("lifecycle_unsupported")

    def stop_gpu(self) -> None:
        raise ProviderError("lifecycle_unsupported")

    def manual_instructions(self, lang: str = "ko") -> str:
        return ""

    # ---- jobs
    @abc.abstractmethod
    def submit(self, request: GenerationRequest) -> str: ...

    @abc.abstractmethod
    def poll(self, job_id: str) -> JobStatus: ...

    @abc.abstractmethod
    def download(self, job_id: str, dest: Path, progress: ProgressCB | None = None) -> Path: ...

    def cancel(self, job_id: str) -> None:
        pass

    # ---- helpers
    def wait(self, job_id: str, cancel_check: Callable[[], bool], on_status: Callable[[JobStatus], None] | None = None,
             interval: float = 2.0) -> JobStatus:
        deadline = time.monotonic() + self.settings.timeout_s
        last = None
        while True:
            if cancel_check():
                self.cancel(job_id)
                from ..errors import Cancelled
                raise Cancelled()
            st = self.poll(job_id)
            if on_status and (last is None or (st.state, st.stage, st.progress) != last):
                on_status(st)
            last = (st.state, st.stage, st.progress)
            if st.done:
                return st
            if time.monotonic() > deadline:
                self.cancel(job_id)
                raise ProviderError("timeout", "job wait deadline")
            time.sleep(interval)
