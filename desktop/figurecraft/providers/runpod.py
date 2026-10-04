"""RunPod provider.

Serverless mode (default): endpoint = RunPod endpoint id (or full https://api.runpod.ai/v2/<id> URL).
    Workers scale to zero by themselves, so "stopping the GPU" needs no call (idle workers shut down).
Pod mode: set the Pod ID in Settings and put the pod's HTTP proxy URL (https://<podid>-8000.proxy.runpod.net)
    in the endpoint field; the app then speaks the generic worker protocol (API key = the worker's WORKER_API_KEY)
    and can start/stop the pod through RunPod's REST API (needs the separate RunPod account key).

NOTE: the RunPod REST calls could not be exercised against a real account during development - they follow
RunPod's public API documentation. See README_KR.md.
"""
from __future__ import annotations

import base64
import time
from pathlib import Path

import requests

from ..core.models import ConnectionInfo, GenerationRequest, JobStatus, ProviderCapabilities
from ..errors import ProviderError
from ..logging_setup import get_logger
from .base import GLB_MAGIC, ProgressCB, prepare_reference_image
from .http_generic import GenericHttpProvider, check_status, map_request_error, request_params

log = get_logger("provider.runpod")

API = "https://api.runpod.ai/v2"
REST = "https://rest.runpod.io/v1"
STATE_MAP = {"IN_QUEUE": "queued", "IN_PROGRESS": "running", "COMPLETED": "succeeded", "FAILED": "failed",
             "CANCELLED": "cancelled", "TIMED_OUT": "failed"}


class RunPodProvider(GenericHttpProvider):
    id = "runpod"
    display_name = "RunPod"

    def __init__(self, settings):
        super().__init__(settings)
        self._caps = ProviderCapabilities(text=True, image=True, text_image=True, lifecycle=True)
        self._outputs: dict[str, dict] = {}

    @property
    def pod_mode(self) -> bool:
        return bool(self.settings.runpod_pod_id.strip())

    def _sbase(self) -> str:
        ep = (self.settings.endpoint or "").strip().rstrip("/")
        if not ep:
            raise ProviderError("no_endpoint")
        return ep if ep.startswith("http") else f"{API}/{ep}"

    def _sreq(self, method: str, path: str, **kw):
        kw.setdefault("timeout", (10, 60))
        try:
            r = self._session.request(method, self._sbase() + path, headers=self._headers(), **kw)
        except requests.RequestException as exc:
            raise map_request_error(exc) from exc
        check_status(r)
        return r

    def _rest_headers(self) -> dict:
        """Pod start/stop use the RunPod *account* key - never the worker key."""
        key = self.settings.get_runpod_api_key()
        if not key:
            raise ProviderError("lifecycle_failed", "RunPod API key is not set")
        return {"Authorization": f"Bearer {key}"}

    # ---------------------------------------------------------- interface
    def capabilities(self) -> ProviderCapabilities:
        return self._caps

    def test_connection(self) -> ConnectionInfo:
        if self.pod_mode:
            info = super().test_connection()
            info.capabilities.lifecycle = True
            self._caps = info.capabilities
            return info
        t0 = time.monotonic()
        d = self._sreq("GET", "/health", timeout=(8, 15)).json()
        workers = d.get("workers", {})
        lat = (time.monotonic() - t0) * 1000
        msg = "workers: " + ", ".join(f"{k}={v}" for k, v in workers.items())
        return ConnectionInfo(ok=True, message=msg, engine="runpod-serverless", capabilities=self._caps, latency_ms=lat)

    def start_gpu(self, progress=None) -> None:
        if not self.pod_mode:
            return                                   # serverless: workers start on demand
        pid = self.settings.runpod_pod_id.strip()
        try:
            r = self._session.post(f"{REST}/pods/{pid}/start", headers=self._rest_headers(), timeout=(10, 60))
        except requests.RequestException as exc:
            raise map_request_error(exc) from exc
        if r.status_code not in (200, 202, 204, 409):  # 409 = already running
            check_status(r)
        deadline = time.monotonic() + min(self.settings.timeout_s, 600)
        while time.monotonic() < deadline:                   # wait until the worker answers /health
            if progress:
                progress("starting pod")
            try:
                GenericHttpProvider.test_connection(self)
                return
            except ProviderError:
                time.sleep(5)
        raise ProviderError("lifecycle_failed", "pod did not become ready in time")

    def stop_gpu(self) -> None:
        if not self.pod_mode:
            return                                   # serverless workers scale to zero on their own
        pid = self.settings.runpod_pod_id.strip()
        try:
            r = self._session.post(f"{REST}/pods/{pid}/stop", headers=self._rest_headers(), timeout=(10, 60))
        except requests.RequestException as exc:
            raise map_request_error(exc) from exc
        if r.status_code >= 400:
            raise ProviderError("lifecycle_failed", f"HTTP {r.status_code}")
        log.info("RunPod pod stop requested")

    def manual_instructions(self, lang: str = "ko") -> str:
        if lang == "ko":
            return ("RunPod 콘솔(runpod.io) → Pods에서 해당 Pod의 'Stop'(또는 Terminate)을 누르세요. "
                    "Serverless 엔드포인트는 작업이 없으면 자동으로 0대로 줄어듭니다(Active workers가 0인지 확인).")
        return ("Open the RunPod console (runpod.io) > Pods and press Stop (or Terminate). Serverless endpoints scale "
                "to zero automatically when idle (make sure Active workers = 0).")

    def submit(self, request: GenerationRequest) -> str:
        if self.pod_mode:
            return super().submit(request)
        params = request_params(request)
        if request.mode == "image_text" and request.reference_image:
            data = prepare_reference_image(request.reference_image, min(self.settings.max_upload_mb, 6))
            params["image_base64"] = base64.b64encode(data).decode("ascii")
        d = self._sreq("POST", "/run", json={"input": params}, timeout=(10, 120)).json()
        jid = d.get("id")
        if not jid:
            raise ProviderError("bad_response", "RunPod returned no job id")
        log.info("RunPod job submitted: %s", jid)
        return str(jid)

    def poll(self, job_id: str) -> JobStatus:
        if self.pod_mode:
            return super().poll(job_id)
        d = self._sreq("GET", f"/status/{job_id}", timeout=(8, 20)).json()
        state = STATE_MAP.get(str(d.get("status", "")).upper(), "running")
        out = d.get("output") or {}
        if state == "succeeded":
            if isinstance(out, dict) and out.get("error"):
                return JobStatus(state="failed", error=str(out["error"]))
            self._outputs[job_id] = out
        exec_ms = d.get("executionTime")
        return JobStatus(state=state, stage=str(d.get("status", "")), progress=None,
                         gpu_seconds=(exec_ms / 1000.0) if isinstance(exec_ms, (int, float)) else None,
                         error=str(d.get("error")) if d.get("error") else None)

    def cancel(self, job_id: str) -> None:
        if self.pod_mode:
            return super().cancel(job_id)
        try:
            self._sreq("POST", f"/cancel/{job_id}", timeout=(5, 10))
        except ProviderError as exc:
            log.warning("RunPod cancel failed: %s", exc)

    def download(self, job_id: str, dest: Path, progress: ProgressCB | None = None) -> Path:
        if self.pod_mode:
            return super().download(job_id, dest, progress)
        out = self._outputs.get(job_id)
        if out is None:
            raise ProviderError("bad_response", "no output recorded for this job")
        dest = Path(dest)
        dest.parent.mkdir(parents=True, exist_ok=True)
        if out.get("glb_base64"):
            try:
                raw = base64.b64decode(out["glb_base64"], validate=True)
            except Exception as exc:
                raise ProviderError("corrupt_glb", "bad base64") from exc
            if len(raw) > self.settings.max_download_mb * 1024 * 1024:
                raise ProviderError("download_too_large", "", mb=self.settings.max_download_mb)
            if raw[:4] != GLB_MAGIC:
                raise ProviderError("corrupt_glb", "not a GLB")
            dest.write_bytes(raw)
            if progress:
                progress(len(raw), len(raw))
            return dest
        if out.get("glb_url"):
            url = str(out["glb_url"])
            if not url.startswith("https://"):
                raise ProviderError("bad_response", "result URL must be https")
            return self._download_url(url, dest, progress, headers={})      # presigned URL: no API key sent
        raise ProviderError("bad_response", "output has neither glb_base64 nor glb_url")
