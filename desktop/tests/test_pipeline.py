import numpy as np
import pytest

from figurecraft.core import pipeline
from figurecraft.core.models import CloudStatus, GenerationRequest, Stage, StageState
from figurecraft.core.project_store import ProjectStore
from figurecraft.core.workspace import Workspace
from figurecraft.errors import Cancelled, ProviderError
from figurecraft.providers.http_generic import GenericHttpProvider
from figurecraft.providers.mock import MockProvider
from figurecraft.settings import Settings
from test_core_misc import EXAMPLE


def _run(prompt=EXAMPLE, provider=None, settings=None, project=None, **kw):
    s = settings or Settings()
    store = ProjectStore(s.projects_path())
    events, clouds = [], []
    hooks = pipeline.Hooks(stage=lambda st, state, d: events.append((st, state, d)), cloud=clouds.append, **kw)
    req = GenerationRequest(prompt=prompt)
    res = pipeline.run_generation(req, provider or MockProvider(s), s, store, hooks, project)
    return res, events, clouds, s, store


def test_example_prompt_end_to_end_in_demo_mode():
    res, events, clouds, s, store = _run()
    m = res.project.meta
    assert m.status == "ready" and m.is_demo and m.title.startswith("DEMO")
    assert [p["name"] for p in m.palette] == ["IVORY", "PURPLE", "BLACK", "YELLOW"]
    assert abs(m.size_mm[2] - 120.0) < 0.2
    # all 11 stages completed in order
    done = [st for st, state, _ in events if state == StageState.DONE]
    assert done == list(Stage)
    root = res.project.root
    for f in ("original.glb", "repaired.glb", "color_preview.glb", "model.3mf", "model.stl", "preview.png", "prompt.txt",
              "project.json"):
        assert (root / f).is_file() and (root / f).stat().st_size > 0, f
    assert (root / "versions" / "v1" / "model.3mf").is_file()
    from figurecraft.core import threemf
    assert threemf.validate_3mf(root / "model.3mf").ok
    assert res.workspace.report.status in ("GOOD", "CHECK")
    assert res.gpu_seconds_est == 0.0 and res.gpu_stop == "n/a"


def test_stage_progress_has_no_fake_percentages():
    _, events, *_ = _run()
    assert all("%" not in d for _, _, d in events)


def test_revision_creates_new_version_without_overwriting_old():
    res, *_ = _run()
    first = (res.project.root / "versions" / "v1" / "original.glb").read_bytes()
    s = Settings(); store = ProjectStore(s.projects_path())
    req = GenerationRequest.from_dict(res.project.meta.request)
    req.revision_instruction = "귀를 더 크게 만들어줘"
    res2 = pipeline.run_generation(req, MockProvider(s), s, store, pipeline.Hooks(), res.project)
    assert [v["n"] for v in res2.project.meta.versions] == [1, 2]
    assert (res.project.root / "versions" / "v1" / "original.glb").read_bytes() == first
    assert "귀를 더 크게" in (res.project.root / "versions" / "v2" / "prompt.txt").read_text(encoding="utf-8")


def test_prompt_height_and_color_count_are_honoured():
    res, *_ = _run("용 드래곤 피규어, 높이 80mm, 최대 2색")
    assert abs(res.project.meta.size_mm[2] - 80) < 0.2 and res.project.meta.colors <= 2


def test_empty_prompt_fails_cleanly():
    with pytest.raises(Exception) as e:
        _run("   ")
    assert "empty prompt" in str(e.value) and "empty prompt" in e.value.user_message("en")


def test_cancel_before_generation():
    with pytest.raises(Cancelled):
        _run(cancelled=lambda: True)


def test_workspace_edit_flow_and_reopen():
    res, *_ = _run()
    ws = res.workspace
    ws.reduce(2, "auto")
    assert len(ws.palette) == 2
    ws.reduce(4, "auto")
    # reassign the biggest region to another colour and make sure it sticks through save/open
    parts = ws.parts_summary()
    ws.set_region_color(parts[0]["region"], (parts[0]["label"] + 1) % len(ws.palette))
    ws.rename_color(0, "Warm White")
    ws.save(rebuild_exports=True, mode="multipart")
    ws2 = Workspace.open(ws.project, Settings())
    assert (ws2.labels == ws.labels).all() and ws2.palette[0].name == "WARM_WHITE"
    assert len(ws2.mesh.vertices) < 0.6 * 3 * len(ws2.mesh.faces), "reopened mesh must be welded (shared vertices)"
    assert ws2.mesh.is_watertight and abs(len(ws2.mesh.faces) - len(ws.mesh.faces)) < 30
    from figurecraft.core import threemf
    v = threemf.validate_3mf(ws.project.file("model.3mf"))
    assert v.ok and any("AMS_1_WARM_WHITE" in c for c in v.colors)


def test_workspace_revert_to_original_restores_unrepaired_geometry():
    res, *_ = _run()
    ws = res.workspace
    ws.add_base("round")
    h_with_base = ws.mesh.extents[2]
    ws.revert_to_original()
    assert ws.mesh.extents[2] < h_with_base and abs(ws.mesh.extents[2] - 120) < 0.2


def test_failure_marks_project_failed_and_stops_gpu_on_http_error(worker):
    """If anything fails after the GPU was started, the pipeline must still try to stop it."""
    s = Settings(); s.provider, s.endpoint = "generic_http", worker; s.set_api_key("test-key-abc"); s.auto_stop_gpu = True
    p = GenericHttpProvider(s)
    calls = []
    orig = p.stop_gpu
    p.stop_gpu = lambda: (calls.append(1), orig())[1]
    p.download = lambda *a, **k: (_ for _ in ()).throw(ProviderError("download_interrupted"))
    with pytest.raises(ProviderError):
        _run("cat", provider=p, settings=s)
    assert calls, "GPU stop must be attempted after a failure"


def test_http_pipeline_marks_mock_engine_output_as_demo_and_auto_stops(worker):
    s = Settings(); s.provider, s.endpoint = "generic_http", worker; s.set_api_key("test-key-abc"); s.auto_stop_gpu = True
    res, events, clouds, *_ = _run("cat wizard", provider=GenericHttpProvider(s), settings=s)
    assert res.project.meta.is_demo                      # server-side mock engine is never passed off as AI
    assert res.gpu_stop == "ok" and CloudStatus.STOPPING in clouds
    assert res.project.meta.timing["gpu_seconds_source"] == "reported"


def test_auto_stop_off_warns_instead_of_stopping(worker):
    s = Settings(); s.provider, s.endpoint = "generic_http", worker; s.set_api_key("test-key-abc"); s.auto_stop_gpu = False
    warnings = []
    res, *_ = _run("cat", provider=GenericHttpProvider(s), settings=s, gpu_warning=warnings.append)
    assert res.gpu_stop == "manual" and warnings
