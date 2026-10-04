"""Generic HTTP provider: talks to the FigureCraft worker protocol (see cloud-worker/worker.py).

  GET  /health                 -> {"status": "ok", "engine": "...", "capabilities": {...}}
  POST /v1/jobs   (multipart)  -> {"job_id": "..."}          fields: params (JSON), image (optional file)
  GET  /v1/jobs/{id}           -> {"state","stage","progress","message","gpu_seconds","error"}
  GET  /v1/jobs/{id}/result    -> GLB bytes
  POST /v1/jobs/{id}/cancel
  POST /v1/shutdown            -> optional: server stops itself / its GPU
"""
from __future__ import annotations

import json
import time
from pathlib import Path

import requests

from ..core.models import ConnectionInfo, GenerationRequest, JobStatus, ProviderCapabilities
from ..errors import ProviderError
from ..logging_setup import get_logger
from .base import GLB_MAGIC, GenerationProvider, ProgressCB, prepare_reference_image, validate_endpoint

log = get_logger("provider.http")


def map_request_error(exc: Exception) -> ProviderError:
    if isinstance(exc, requests.exceptions.Timeout):
        return ProviderError("timeout", str(exc))
    if isinstance(exc, requests.exceptions.SSLError):
        return ProviderError("server_unavailable", "TLS error: " + str(exc))
    if isinstance(exc, requests.exceptions.ConnectionError):
        text = str(exc).lower()
        if "name or service not known" in text or "getaddrinfo" in text or "nodename" in text:
            return ProviderError("no_internet", str(exc))
        return ProviderError("server_unavailable", str(exc))
    return ProviderError("server_unavailable", str(exc))


def check_status(resp: requests.Response) -> None:
    if resp.status_code in (401, 403):
        raise ProviderError("invalid_api_key", f"HTTP {resp.status_code}")
    if resp.status_code == 404:
        raise ProviderError("bad_response", "HTTP 404 (check the endpoint URL)")
    if resp.status_code >= 400:
        raise ProviderError("server_unavailable", f"HTTP {resp.status_code}: {resp.text[:200]}")


def request_params(req: GenerationRequest) -> dict:
    from ..core.models import STYLE_PROMPT
    p = req.print
    hints = []
    if p.flat_bottom:
        hints.append("flat bottom")
    if p.support_friendly:
        hints.append("minimal overhangs, support-friendly pose, limbs attached to the body")
    if p.min_thickness_protection or p.strengthen_thin_parts:
        hints.append(f"no thin parts below {p.min_feature_mm} mm, sturdy shapes")
    if p.remove_floating:
        hints.append("no floating disconnected parts")
    return {
        "prompt": req.full_prompt(),
        "style": req.style,
        "style_prompt": STYLE_PROMPT.get(req.style, ""),
        "mode": req.mode,
        "engine": req.engine,
        "seed": req.seed,
        "height_mm": p.height_mm,
        "max_colors": req.max_colors,
        "printability_hints": ", ".join(hints),
        "revision": {"instruction": req.revision_instruction, "previous_job_id": req.previous_job_id}
        if req.revision_instruction else None,
    }


class GenericHttpProvider(GenerationProvider):
    id = "generic_http"
    display_name = "Generic HTTP GPU server"

    def __init__(self, settings):
        super().__init__(settings)
        self._caps = ProviderCapabilities(text=True, image=False, text_image=False, lifecycle=False)
        self._session = requests.Session()

    # -- plumbing
    def _base(self) -> str:
        return validate_endpoint(self.settings.endpoint)

    def _headers(self) -> dict:
        key = self.settings.get_api_key()
        return {"Authorization": f"Bearer {key}"} if key else {}

    def _req(self, method: str, path: str, **kw) -> requests.Response:
        url = self._base() + path
        kw.setdefault("timeout", (10, 60))
        try:
            resp = self._session.request(method, url, headers=self._headers(), **kw)
        except requests.RequestException as exc:
            raise map_request_error(exc) from exc
        check_status(resp)
        return resp

    # -- interface
    def capabilities(self) -> ProviderCapabilities:
        return self._caps

    def test_connection(self) -> ConnectionInfo:
        t0 = time.monotonic()
        try:
            data = self._req("GET", "/health", timeout=(8, 15)).json()
        except ProviderError:
            raise
        except ValueError as exc:
            raise ProviderError("bad_response", "health is not JSON") from exc
        c = data.get("capabilities", {}) or {}
        self._caps = ProviderCapabilities(
            text=bool(c.get("text", True)), image=bool(c.get("image", False)),
            text_image=bool(c.get("text_image", False)), lifecycle=bool(c.get("shutdown", False)),
            reports_progress=bool(c.get("progress", False)), engines=list(data.get("engines", [])))
        lat = (time.monotonic() - t0) * 1000
        log.info("connection OK engine=%s latency=%.0fms caps=%s", data.get("engine"), lat, c)
        return ConnectionInfo(ok=True, message=str(data.get("status", "ok")), engine=str(data.get("engine", "")),
                              capabilities=self._caps, latency_ms=lat)

    def start_gpu(self, progress=None) -> None:
        # A plain HTTP server is assumed to be running already (you started it yourself).
        self.test_connection()

    def stop_gpu(self) -> None:
        if not self._caps.lifecycle:
            raise ProviderError("lifecycle_unsupported")
        self._req("POST", "/v1/shutdown", timeout=(8, 20))

    def manual_instructions(self, lang: str = "ko") -> str:
        if lang == "ko":
            return ("이 서버는 원격 종료를 지원하지 않습니다. 사용을 마쳤다면 GPU 서버(클라우드 콘솔)에서 "
                    "인스턴스를 직접 중지/삭제하세요. 켜 둔 GPU는 계속 과금됩니다.")
        return ("This server cannot be stopped remotely. When you are done, stop/delete the instance in your "
                "cloud console yourself - a running GPU keeps billing.")

    def submit(self, request: GenerationRequest) -> str:
        params = request_params(request)
        files = {}
        if request.mode == "image_text" and request.reference_image:
            if not self._caps.image and not self._caps.text_image:
                raise ProviderError("image_unsupported")
            data = prepare_reference_image(request.reference_image, self.settings.max_upload_mb)
            files["image"] = ("reference.png", data, "image/png")
        resp = self._req("POST", "/v1/jobs", data={"params": json.dumps(params)}, files=files or None,
                         timeout=(10, 120))
        try:
            jid = resp.json()["job_id"]
        except Exception as exc:
            raise ProviderError("bad_response", "no job_id") from exc
        log.info("job submitted: %s (engine=%s, mode=%s)", jid, request.engine, request.mode)
        return str(jid)

    def poll(self, job_id: str) -> JobStatus:
        d = self._req("GET", f"/v1/jobs/{job_id}", timeout=(8, 20)).json()
        prog = d.get("progress")
        return JobStatus(state=str(d.get("state", "running")), stage=str(d.get("stage", "")),
                         progress=float(prog) if isinstance(prog, (int, float)) else None,
                         message=str(d.get("message", "")),
                         gpu_seconds=d.get("gpu_seconds"), error=d.get("error"))

    def cancel(self, job_id: str) -> None:
        try:
            self._req("POST", f"/v1/jobs/{job_id}/cancel", timeout=(5, 10))
        except ProviderError as exc:
            log.warning("cancel failed: %s", exc)

    def download(self, job_id: str, dest: Path, progress: ProgressCB | None = None) -> Path:
        return self._download_url(f"{self._base()}/v1/jobs/{job_id}/result", dest, progress, self._headers())

    def _download_url(self, url: str, dest: Path, progress: ProgressCB | None, headers: dict) -> Path:
        dest = Path(dest)
        dest.parent.mkdir(parents=True, exist_ok=True)
        part = dest.with_suffix(dest.suffix + ".part")
        limit = self.settings.max_download_mb * 1024 * 1024
        done = 0
        total = None
        for attempt in range(3):                              # resume an interrupted download up to 2 times
            h = dict(headers)
            if done:
                h["Range"] = f"bytes={done}-"
            try:
                with self._session.get(url, headers=h, stream=True, timeout=(10, 60)) as r:
                    check_status(r)
                    if done and r.status_code != 206:           # server ignored Range: start over
                        done = 0
                        part.unlink(missing_ok=True)
                    clen = r.headers.get("Content-Length")
                    if clen is not None:
                        total = done + int(clen)
                        if total > limit:
                            raise ProviderError("download_too_large", f"{total} bytes", mb=self.settings.max_download_mb)
                    with open(part, "ab" if done else "wb") as fh:
                        for chunk in r.iter_content(1 << 16):
                            if not chunk:
                                continue
                            done += len(chunk)
                            if done > limit:
                                raise ProviderError("download_too_large", f">{limit} bytes", mb=self.settings.max_download_mb)
                            fh.write(chunk)
                            if progress:
                                progress(done, total)
                if total is not None and done != total:
                    raise requests.exceptions.ChunkedEncodingError("short read")
                break
            except ProviderError:
                part.unlink(missing_ok=True)
                raise
            except (requests.exceptions.ChunkedEncodingError, requests.exceptions.ConnectionError,
                    requests.exceptions.Timeout) as exc:
                log.warning("download interrupted at %d bytes (attempt %d): %s", done, attempt + 1, exc)
                if attempt == 2:
                    part.unlink(missing_ok=True)
                    raise ProviderError("download_interrupted", str(exc)) from exc
        with open(part, "rb") as fh:
            if fh.read(4) != GLB_MAGIC:
                part.unlink(missing_ok=True)
                raise ProviderError("corrupt_glb", "downloaded file is not a GLB")
        part.replace(dest)
        log.info("downloaded %s (%d bytes)", dest.name, done)
        return dest
