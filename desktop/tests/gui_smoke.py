"""Scripted GUI walkthrough (not collected by pytest). Needs a display (xvfb on Linux) and QtWebEngine.

  FIGURECRAFT_WEB_FLAGS="--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader --disable-gpu-compositing --in-process-gpu" \
  xvfb-run -a python desktop/tests/gui_smoke.py  [outdir]
Exit code 0 = every step worked and no Python exception was raised inside the UI.
"""
import os
import stat
import sys
import tempfile
import time
import traceback
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
out = Path(sys.argv[1] if len(sys.argv) > 1 else tempfile.mkdtemp(prefix="fc-gui-"))
out.mkdir(parents=True, exist_ok=True)
home = Path(tempfile.mkdtemp(prefix="fc-home-"))
os.environ["FIGURECRAFT_HOME"] = str(home)

from figurecraft import app as fcapp  # noqa: E402

fcapp._configure_webengine()
from PySide6.QtWidgets import QApplication, QMessageBox  # noqa: E402

from figurecraft import i18n, logging_setup  # noqa: E402
from figurecraft.settings import Settings  # noqa: E402

errors: list[str] = []


def hook(t, v, tb):
    errors.append("".join(traceback.format_exception(t, v, tb)))
    traceback.print_exception(t, v, tb)


sys.excepthook = hook
logging_setup.setup_logging()

# never block on modal dialogs
QMessageBox.information = staticmethod(lambda *a, **k: QMessageBox.StandardButton.Ok)
QMessageBox.warning = staticmethod(lambda *a, **k: QMessageBox.StandardButton.Ok)
QMessageBox.question = staticmethod(lambda *a, **k: QMessageBox.StandardButton.Yes)
from figurecraft.ui import dialogs  # noqa: E402

shown_errors = []
dialogs.ErrorDialog.exec = lambda self: shown_errors.append(self.windowTitle()) or 0

# stub Bambu Studio that records its argument
stub = home / "fake-bambu"
rec = home / "bambu-args.txt"
stub.write_text(f'#!/bin/sh\necho "$1" > "{rec}"\n')
stub.chmod(stub.stat().st_mode | stat.S_IEXEC)

s = Settings.load()
s.language = "ko"
s.bambu_path = str(stub)
s.save()
i18n.set_language("ko")

from figurecraft.core.models import Stage, StageState  # noqa: E402
from figurecraft.ui.context import AppContext  # noqa: E402
from figurecraft.ui.main_window import MainWindow  # noqa: E402

qapp = QApplication(sys.argv)
ctx = AppContext(s)
win = MainWindow(ctx)
win.show()
g = win.gen
steps_ok: list[str] = []


def pump(sec=0.05):
    end = time.time() + sec
    while time.time() < end:
        qapp.processEvents()
        time.sleep(0.01)


def wait(cond, timeout=120, what=""):
    t = time.time()
    while time.time() - t < timeout:
        qapp.processEvents()
        if cond():
            return
        time.sleep(0.05)
    raise TimeoutError(what or "timeout")


def idle():
    return not g._busy and not g._generating and not g._tasks_pending() if hasattr(g, "_tasks_pending") else (not g._busy and not g._generating)


def shot(name):
    pump(0.6)
    win.grab().save(str(out / f"{name}.png"))


def step(name, fn):
    try:
        fn()
        steps_ok.append(name)
        print("  ok  ", name, flush=True)
    except Exception:
        errors.append(f"step failed: {name}\n{traceback.format_exc()}")
        print("  FAIL", name, flush=True)
        traceback.print_exc()


def s_start():
    pump(3)
    assert g.viewer is not None and win.windowTitle().startswith("FigureCraft")
    assert "DEMO" in win.status_label.text()
    assert not g.mode_img.isEnabled(), "image mode must be disabled for the demo provider"
    shot("01_start")


def s_generate():
    g._insert_example()
    g.start_generation()
    wait(lambda: g._generating, 10)
    wait(lambda: not g._generating, 180, "generation")
    assert g.ws is not None and g.ws.is_demo and not shown_errors
    st = [g.stages._state[x] for x in Stage]
    assert all(x == StageState.DONE for x in st), st
    assert g.current_height() == 120 and g.color_group.checkedId() == 4
    assert [p.name for p in g.ws.palette] == ["IVORY", "PURPLE", "BLACK", "YELLOW"]
    pump(2.5)
    shot("02_generated")


def s_views():
    for k in ("front", "back", "left", "right", "top", "bottom"):
        g.viewer.set_view(k)
    g.viewer.reset_view()
    g.mode_btns["print"].click(); g.mode_btns["parts"].click(); g.mode_btns["original"].click()
    g.btn_wire.click(); g.btn_bbox.click()
    pump(1.5)
    g.mode_btns["print"].click(); g.btn_wire.click()
    pump(1.5)
    shot("03_modes")


def s_colors():
    for k in (2, 3, 1, 4):
        g.k_btns[k].click()
        wait(lambda: not g._busy, 60)
        assert len(g.ws.palette) == k, (k, len(g.ws.palette))
    g.k_btns[0].click()
    parts = g.ws.parts_summary()
    before = g.ws.labels.copy()
    g._region_to_color(parts[0]["region"], (parts[0]["label"] + 1) % len(g.ws.palette))
    pump(0.5)
    assert (g.ws.labels != before).any()
    g.ws.palette[1].locked = True
    g._rename_color = None
    g.ws.rename_color(2, "Jet Black")
    g._commit_color_edit()
    g._merge(3, 0); pump(0.3)
    assert len(g.ws.palette) == 3
    g.ws.reduce(4, "auto"); g._commit_color_edit()
    shot("04_colors")


def s_tools():
    g._flat_preview(); wait(lambda: not g._busy, 90)
    assert g._flat_plan is not None
    if g._flat_plan.needed:
        g._flat_apply(); wait(lambda: not g._busy, 90)
    g._do_base(); wait(lambda: not g._busy, 90)
    assert g.ws.report.extents_mm[2] > 120.5
    g._do_repair(); wait(lambda: not g._busy, 90)
    g._do_strengthen(); wait(lambda: not g._busy, 90)
    g._do_revert(); wait(lambda: not g._busy, 90)
    assert abs(g.ws.report.extents_mm[2] - 120) < 0.5
    assert not shown_errors, shown_errors
    shot("05_tools")


def s_export():
    import zipfile
    p3 = home / "x.3mf"
    g.rb_multi.setChecked(True)
    g._ensure_exports(lambda rep: None)
    wait(lambda: not g._busy, 90)
    assert zipfile.is_zipfile(g.ws.project.file("model.3mf"))
    g._open_bambu()
    wait(lambda: not g._busy, 90)
    wait(lambda: rec.exists(), 10, "bambu stub launch")
    assert rec.read_text().strip().endswith("model.3mf")
    from figurecraft.core import export
    export.export_stl(home / "a.stl", g.ws.mesh)
    assert (home / "a.stl").stat().st_size > 1000
    assert "3MF" in g.export_note.text()


def s_revision_versions():
    n0 = len(g.ws.project.meta.versions)
    g.rev_text.setPlainText("귀를 더 크게 만들어줘")
    g._send_revision()
    wait(lambda: g._generating, 10)
    wait(lambda: not g._generating, 180)
    assert len(g.ws.project.meta.versions) == n0 + 1
    g.version_combo.setCurrentIndex(0)
    g._open_version(); wait(lambda: not g._busy, 60)
    assert g.ws.project.meta.active_version == 1
    shot("06_versions")


def s_library():
    win.show_page(1); pump(1)
    assert win.lib.list.count() == 1
    win.lib.list.setCurrentRow(0)
    shot("07_library")
    win.lib.duplicate(); pump(0.5)
    assert win.lib.list.count() == 2
    win.lib.list.setCurrentRow(1); win.lib.delete(); pump(0.5)
    assert win.lib.list.count() == 1
    win.lib.open_selected(); wait(lambda: g.ws is not None and not g._busy, 60)


def s_settings_and_language():
    win.show_page(2); pump(1)
    sp = win.settings_page
    shot("08_settings")
    sp.test_result.setText(""); sp._test(); wait(lambda: sp.btn_test.isEnabled(), 30, "demo connection test")
    assert "✓" in sp.test_result.text()
    sp.lang.setCurrentIndex(sp.lang.findData("en"))
    sp.theme.setCurrentIndex(sp.theme.findData("light"))
    sp.save()
    pump(3)
    assert i18n.get_language() == "en" and win.nav_btns[0].text() == "Generate"
    shot("09_english_light")
    win.show_page(0); pump(2)
    shot("10_english_generate")
    # error dialog path (patched exec) with a localised message
    from figurecraft.errors import ProviderError
    win.gen.show_error(ProviderError("invalid_api_key"), retry=lambda: None)
    assert shown_errors
    win.gen.on_settings_changed()


for name, fn in (("start", s_start), ("generate example prompt", s_generate), ("viewer modes/views", s_views),
                 ("colour preview/edit", s_colors), ("repair/flat/base/thin/revert", s_tools),
                 ("export + open in Bambu (stub)", s_export), ("revision + versions", s_revision_versions),
                 ("library", s_library), ("settings + English/light", s_settings_and_language)):
    step(name, fn)

print(f"\nscreenshots: {out}")
print(f"{len(steps_ok)} steps ok; {len(errors)} errors")
for e in errors:
    print(e)
sys.exit(1 if errors else 0)
