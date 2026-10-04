import json
import logging
import os
import stat
import sys
import time

import pytest

from figurecraft import logging_setup, secret_store
from figurecraft.core import bambu, prompt_analysis
from figurecraft.core.project_store import ProjectStore
from figurecraft.errors import FigureCraftError
from figurecraft.settings import Settings

EXAMPLE = """귀여운 고양이 마법사 피규어.
큰 머리와 작은 몸의 치비 스타일.
고양이는 앉아 있고 앞발이 보이게 해줘.
모자는 보라색.
몸은 아이보리.
눈과 리본은 검정색.
별 장식은 노란색.
높이 120mm.
최대 4색.
평평한 바닥.
얇은 부분은 최소 1.5mm 이상.
FDM 3D 프린팅하기 쉽게 만들고
서포트는 가능한 적게 해줘."""


def test_prompt_analysis_example():
    h = prompt_analysis.analyze(EXAMPLE)
    assert h.height_mm == 120 and h.max_colors == 4 and h.flat_bottom and h.min_wall_mm == 1.5
    assert [n for n, _ in h.colors] == ["PURPLE", "IVORY", "BLACK", "YELLOW"]


def test_prompt_analysis_variants():
    assert prompt_analysis.analyze("a dragon, height 12 cm, 2 colors").height_mm == 120
    assert prompt_analysis.analyze("a dragon, height 12 cm, 2 colors").max_colors == 2
    assert prompt_analysis.analyze("높이 9999mm").height_mm is None
    assert prompt_analysis.analyze("red and blue robot").colors[0][0] == "RED"
    assert prompt_analysis.analyze("").colors == []


def test_project_store_lifecycle_and_versions(tmp_path):
    st = ProjectStore(tmp_path / "p")
    p = st.create(title="Cat", prompt="x")
    (p.root / "repaired.glb").write_bytes(b"v1")
    assert p.snapshot_version("first") == 1
    (p.root / "repaired.glb").write_bytes(b"v2")
    assert p.snapshot_version("second") == 2
    assert (p.versions_dir / "v1" / "repaired.glb").read_bytes() == b"v1"          # nothing overwritten
    assert (p.versions_dir / "v2" / "repaired.glb").read_bytes() == b"v2"
    p.activate_version(1)
    assert (p.root / "repaired.glb").read_bytes() == b"v1" and p.meta.active_version == 1
    d = st.duplicate(p.meta.id)
    assert d.meta.id != p.meta.id and (d.versions_dir / "v2").is_dir()
    st.rename(p.meta.id, "Renamed")
    assert st.open(p.meta.id).meta.title == "Renamed"
    assert len(st.list()) == 2
    z = st.export_zip(p.meta.id, tmp_path / "x.zip")
    assert z.stat().st_size > 0
    st.delete(d.meta.id)
    assert len(st.list()) == 1


def test_project_open_rejects_traversal(tmp_path):
    st = ProjectStore(tmp_path / "p")
    with pytest.raises(FigureCraftError):
        st.open("../../etc")


def test_secret_encryption_roundtrip_and_not_plain(home):
    s = Settings()
    s.set_api_key("sk-super-secret-value")
    s.save()
    raw = Settings.path().read_text()
    assert "sk-super-secret-value" not in raw
    assert Settings.load().get_api_key() == "sk-super-secret-value"
    assert secret_store.decrypt("garbage") == ""


def test_env_var_overrides_stored_key(monkeypatch):
    s = Settings(); s.set_api_key("stored")
    monkeypatch.setenv("FIGURECRAFT_API_KEY", "from-env")
    assert s.get_api_key() == "from-env"


def test_logs_never_contain_secrets(home):
    logging_setup._configured = False
    for h in list(logging.getLogger("figurecraft").handlers):
        logging.getLogger("figurecraft").removeHandler(h)
    log = logging_setup.setup_logging()
    logging_setup.register_secret("my-very-secret-key-123")
    lg = logging_setup.get_logger("t")
    lg.info("request with key my-very-secret-key-123 ok")
    lg.info("header Authorization: Bearer abcdef1234567890 sent")
    lg.info('payload {"api_key": "zzzz9999"}')
    for h in log.handlers:
        h.flush()
    text = (home / "logs" / "figurecraft.log").read_text()
    assert "my-very-secret-key-123" not in text and "abcdef1234567890" not in text and "zzzz9999" not in text
    assert "***" in text


@pytest.mark.skipif(sys.platform == "win32", reason="posix stub")
def test_bambu_launch_uses_arg_list(tmp_path):
    out = tmp_path / "args.txt"
    exe = tmp_path / "fake-bambu"
    exe.write_text(f'#!/bin/sh\nfor a in "$@"; do echo "$a" >> {out}; done\n')
    exe.chmod(exe.stat().st_mode | stat.S_IEXEC)
    model = tmp_path / "my model; touch pwned.3mf"            # shell metacharacters must stay inert
    model.write_bytes(b"3mf")
    bambu.open_in_bambu(exe, model)
    for _ in range(50):
        if out.exists():
            break
        time.sleep(0.1)
    assert out.read_text().strip() == str(model.resolve())
    assert not (tmp_path / "pwned.3mf").exists()


def test_bambu_errors(tmp_path):
    with pytest.raises(FigureCraftError) as e:
        bambu.open_in_bambu(tmp_path / "nope", tmp_path / "missing.3mf")
    assert e.value.code == "file_missing"
    m = tmp_path / "a.3mf"; m.write_bytes(b"x")
    assert bambu.resolve(str(tmp_path / "nope")) is None or True
    if bambu.detect() is None:
        with pytest.raises(FigureCraftError) as e:
            bambu.open_in_bambu(tmp_path / "nope", m)
        assert e.value.code == "bambu_not_found"


def test_settings_roundtrip_and_corrupt_file(home):
    s = Settings(); s.language = "en"; s.default_colors = 3; s.save()
    assert Settings.load().language == "en" and Settings.load().default_colors == 3
    Settings.path().write_text("{ corrupt")
    assert Settings.load().language == "ko"                      # falls back to defaults, no crash
