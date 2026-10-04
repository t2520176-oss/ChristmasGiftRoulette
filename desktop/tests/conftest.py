import os
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "desktop"))
os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")


@pytest.fixture(autouse=True)
def home(tmp_path, monkeypatch):
    """Isolated app-data folder per test (settings, logs, projects)."""
    monkeypatch.setenv("FIGURECRAFT_HOME", str(tmp_path / "home"))
    monkeypatch.delenv("FIGURECRAFT_API_KEY", raising=False)
    from figurecraft import i18n
    i18n.set_language("ko")
    return tmp_path / "home"


@pytest.fixture(scope="session")
def demo_mesh():
    from figurecraft.core import demo
    return demo.build_demo("cat_wizard")


def _free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


@pytest.fixture()
def worker(tmp_path):
    """Real cloud-worker process using the mock engine."""
    port = _free_port()
    env = dict(os.environ, WORKER_API_KEY="test-key-abc", ENGINE="mock", PORT=str(port), HOST="127.0.0.1",
               WORK_DIR=str(tmp_path / "wk"), IDLE_SHUTDOWN_MIN="0")
    proc = subprocess.Popen([sys.executable, "worker.py"], cwd=ROOT / "cloud-worker", env=env,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    import requests
    url = f"http://127.0.0.1:{port}"
    for _ in range(60):
        try:
            if requests.get(url + "/health", headers={"Authorization": "Bearer test-key-abc"}, timeout=1).ok:
                break
        except Exception:
            time.sleep(0.25)
    else:
        proc.kill()
        pytest.skip("worker did not start: " + (proc.stdout.read().decode(errors="replace") if proc.stdout else ""))
    yield url
    proc.terminate()
    try:
        proc.wait(5)
    except Exception:
        proc.kill()
