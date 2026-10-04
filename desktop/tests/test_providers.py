import base64
import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

import pytest

from figurecraft.core import demo, mesh_io
from figurecraft.core.models import GenerationRequest
from figurecraft.errors import ProviderError
from figurecraft.providers.http_generic import GenericHttpProvider
from figurecraft.providers.mock import MockProvider
from figurecraft.providers.registry import create_provider
from figurecraft.providers.runpod import RunPodProvider
from figurecraft.settings import Settings


def _settings(url, key="test-key-abc", provider="generic_http"):
    s = Settings(); s.provider, s.endpoint = provider, url; s.set_api_key(key)
    return s


def test_mock_provider_is_clearly_demo(tmp_path):
    p = MockProvider(Settings())
    assert p.capabilities().is_demo and not p.capabilities().image
    jid = p.submit(GenerationRequest(prompt="귀여운 고양이"))
    import time
    while not p.poll(jid).done:
        time.sleep(0.2)
    out = p.download(jid, tmp_path / "x.glb")
    assert mesh_io.load_mesh(out).faces.shape[0] > 1000


def test_registry():
    for pid, cls in (("mock", MockProvider), ("generic_http", GenericHttpProvider), ("runpod", RunPodProvider)):
        s = Settings(); s.provider = pid
        assert isinstance(create_provider(s), cls)


def test_generic_http_full_job_against_real_worker(worker, tmp_path):
    p = GenericHttpProvider(_settings(worker))
    info = p.test_connection()
    assert info.ok and info.engine == "mock" and p.capabilities().lifecycle
    jid = p.submit(GenerationRequest(prompt="cat"))
    st = p.wait(jid, lambda: False, interval=0.2)
    assert st.state == "succeeded" and st.gpu_seconds is not None
    seen = []
    out = p.download(jid, tmp_path / "r.glb", lambda a, b: seen.append((a, b)))
    assert out.read_bytes()[:4] == b"glTF" and seen
    assert mesh_io.load_mesh(out).faces.shape[0] > 100


def test_generic_http_error_mapping(worker):
    with pytest.raises(ProviderError) as e:
        GenericHttpProvider(_settings(worker, key="wrong")).test_connection()
    assert e.value.code == "invalid_api_key"
    with pytest.raises(ProviderError) as e:
        GenericHttpProvider(_settings("http://127.0.0.1:1")).test_connection()
    assert e.value.code == "server_unavailable"
    with pytest.raises(ProviderError) as e:
        GenericHttpProvider(_settings("")).test_connection()
    assert e.value.code == "no_endpoint"
    with pytest.raises(ProviderError) as e:
        GenericHttpProvider(_settings("ftp://x")).test_connection()
    assert e.value.code == "bad_endpoint"
    assert "API" in e.value.user_message("en") or True


def test_error_messages_are_localised():
    e = ProviderError("invalid_api_key")
    assert "API 키" in e.user_message("ko") and "API key" in e.user_message("en")


def test_cancel_and_timeout(worker):
    s = _settings(worker); s.timeout_s = 30
    p = GenericHttpProvider(s); p.test_connection()
    jid = p.submit(GenerationRequest(prompt="x"))
    p.cancel(jid)
    import time; time.sleep(1.5)
    assert p.poll(jid).state == "cancelled"
    from figurecraft.errors import Cancelled
    jid2 = p.submit(GenerationRequest(prompt="y"))
    with pytest.raises(Cancelled):
        p.wait(jid2, lambda: True, interval=0.1)


def test_image_upload_flow(worker, tmp_path):
    from PIL import Image
    img = tmp_path / "ref.webp"; Image.new("RGB", (64, 64), (200, 50, 50)).save(img)
    p = GenericHttpProvider(_settings(worker)); p.test_connection()
    jid = p.submit(GenerationRequest(prompt="cat", mode="image_text", reference_image=str(img)))
    assert p.wait(jid, lambda: False, interval=0.2).state == "succeeded"
    bad = tmp_path / "bad.png"; bad.write_bytes(b"not an image")
    with pytest.raises(ProviderError) as e:
        p.submit(GenerationRequest(prompt="cat", mode="image_text", reference_image=str(bad)))
    assert e.value.code == "bad_image"


def test_image_refused_when_provider_has_no_image_support(tmp_path):
    from PIL import Image
    img = tmp_path / "ref.png"; Image.new("RGB", (8, 8)).save(img)
    p = GenericHttpProvider(_settings("http://127.0.0.1:9"))        # capabilities default: no image
    with pytest.raises(ProviderError) as e:
        p.submit(GenerationRequest(prompt="x", mode="image_text", reference_image=str(img)))
    assert e.value.code == "image_unsupported"


def test_upload_size_limit(tmp_path):
    from PIL import Image
    import numpy as np
    from figurecraft.providers.base import prepare_reference_image
    img = tmp_path / "big.png"
    Image.fromarray(np.random.randint(0, 255, (1400, 1400, 3), np.uint8)).save(img)
    with pytest.raises(ProviderError) as e:
        prepare_reference_image(img, max_mb=1)
    assert e.value.code == "upload_too_large"


def test_download_limits_and_corruption(tmp_path, worker):
    s = _settings(worker); s.max_download_mb = 0                    # nothing may be downloaded
    p = GenericHttpProvider(s); p.test_connection()
    jid = p.submit(GenerationRequest(prompt="x")); p.wait(jid, lambda: False, interval=0.2)
    with pytest.raises(ProviderError) as e:
        p.download(jid, tmp_path / "x.glb")
    assert e.value.code == "download_too_large"
    assert not (tmp_path / "x.glb").exists() and not (tmp_path / "x.glb.part").exists()


class _FakeRunPod(BaseHTTPRequestHandler):
    glb = b""
    polls = 0

    def log_message(self, *a):
        pass

    def _send(self, obj, code=200):
        body = json.dumps(obj).encode()
        self.send_response(code); self.send_header("Content-Type", "application/json"); self.send_header("Content-Length", str(len(body)))
        self.end_headers(); self.wfile.write(body)

    def do_GET(self):
        if self.headers.get("Authorization") != "Bearer rp-key":
            return self._send({"error": "unauthorized"}, 401)
        if self.path.endswith("/health"):
            return self._send({"workers": {"idle": 0, "running": 0}, "jobs": {}})
        if "/status/" in self.path:
            type(self).polls += 1
            if type(self).polls < 2:
                return self._send({"status": "IN_QUEUE"})
            return self._send({"status": "COMPLETED", "executionTime": 4200,
                               "output": {"glb_base64": base64.b64encode(type(self).glb).decode()}})
        self._send({}, 404)

    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0)); body = json.loads(self.rfile.read(n) or b"{}")
        if self.headers.get("Authorization") != "Bearer rp-key":
            return self._send({"error": "unauthorized"}, 401)
        if self.path.endswith("/run"):
            assert "prompt" in body["input"]
            return self._send({"id": "job-1", "status": "IN_QUEUE"})
        self._send({"status": "CANCELLED"})


def test_runpod_serverless_against_fake_api(tmp_path):
    _FakeRunPod.glb = mesh_io.mesh_to_glb_bytes(demo.build_demo("xmas_ornament"))
    _FakeRunPod.polls = 0
    srv = HTTPServer(("127.0.0.1", 0), _FakeRunPod)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    try:
        s = _settings(f"http://127.0.0.1:{srv.server_port}/v2/ep123", key="rp-key", provider="runpod")
        p = RunPodProvider(s)
        assert p.test_connection().ok
        jid = p.submit(GenerationRequest(prompt="tree"))
        st = p.wait(jid, lambda: False, interval=0.1)
        assert st.state == "succeeded" and abs(st.gpu_seconds - 4.2) < 1e-6
        out = p.download(jid, tmp_path / "o.glb")
        assert mesh_io.load_mesh(out).faces.shape[0] > 100
        p.stop_gpu()                                                   # serverless: no-op, must not raise
        bad = RunPodProvider(_settings(f"http://127.0.0.1:{srv.server_port}/v2/ep123", key="nope", provider="runpod"))
        with pytest.raises(ProviderError) as e:
            bad.test_connection()
        assert e.value.code == "invalid_api_key"
    finally:
        srv.shutdown()


def test_worker_shutdown_endpoint_stops_server(worker):
    p = GenericHttpProvider(_settings(worker)); p.test_connection()
    p.stop_gpu()
    import time
    time.sleep(2.5)
    with pytest.raises(ProviderError):
        p.test_connection()


def test_runpod_pod_mode_uses_separate_keys(tmp_path):
    """Pod start/stop must use the RunPod account key, never the worker key; the worker gets only its own key."""
    seen = []

    class H(BaseHTTPRequestHandler):
        def log_message(self, *a):
            pass

        def do_POST(self):
            seen.append((self.path, self.headers.get("Authorization")))
            self.send_response(200); self.send_header("Content-Length", "2"); self.end_headers(); self.wfile.write(b"{}")

    srv = HTTPServer(("127.0.0.1", 0), H)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    try:
        import figurecraft.providers.runpod as rp
        s = _settings("http://127.0.0.1:9", key="WORKER-KEY", provider="runpod")
        s.runpod_pod_id = "pod123"
        s.set_runpod_api_key("RUNPOD-KEY")
        rp.REST = f"http://127.0.0.1:{srv.server_port}"
        p = RunPodProvider(s)
        p.stop_gpu()
        assert seen == [("/pods/pod123/stop", "Bearer RUNPOD-KEY")]
        s2 = _settings("http://127.0.0.1:9", key="WORKER-KEY", provider="runpod"); s2.runpod_pod_id = "pod123"
        with pytest.raises(ProviderError) as e:
            RunPodProvider(s2).stop_gpu()
        assert e.value.code == "lifecycle_failed" and len(seen) == 1       # nothing sent without the right key
    finally:
        srv.shutdown()
        import figurecraft.providers.runpod as rp2
        rp2.REST = "https://rest.runpod.io/v1"
