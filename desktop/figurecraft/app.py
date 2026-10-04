"""Application entry point."""
from __future__ import annotations

import os
import sys


def _configure_webengine() -> None:
    """Chromium flags must be set before QApplication exists."""
    flags = os.environ.get("QTWEBENGINE_CHROMIUM_FLAGS", "")
    # The 3D viewer needs WebGL: don't let a conservative GPU blocklist (old/odd drivers, iGPUs, VMs) switch it off,
    # and allow Chromium's software rasteriser (SwiftShader) as a last resort so a preview always works.
    extra = "--ignore-gpu-blocklist --enable-unsafe-swiftshader " + os.environ.get("FIGURECRAFT_WEB_FLAGS", "")
    if sys.platform != "win32":
        if hasattr(os, "geteuid") and os.geteuid() == 0:
            extra += " --no-sandbox"
    os.environ["QTWEBENGINE_CHROMIUM_FLAGS"] = (flags + " " + extra).strip()


def main(argv: list[str] | None = None) -> int:
    argv = list(sys.argv if argv is None else argv)
    if "--self-test" in argv:                       # headless, no window (used by the build to verify packaging)
        from .selftest import run
        return run()
    _configure_webengine()
    from PySide6.QtCore import Qt, QCoreApplication
    QCoreApplication.setAttribute(Qt.ApplicationAttribute.AA_ShareOpenGLContexts, True)
    from PySide6.QtGui import QIcon
    from PySide6.QtWidgets import QApplication, QMessageBox

    from . import APP_NAME, __version__, i18n, logging_setup, paths
    from .settings import Settings

    log = logging_setup.setup_logging()
    app = QApplication(argv)
    app.setApplicationName(APP_NAME)
    app.setApplicationVersion(__version__)
    icon = paths.resource_path("assets/icon.png")
    if icon.exists():
        app.setWindowIcon(QIcon(str(icon)))
    try:
        settings = Settings.load()
        i18n.set_language(settings.language)
        from .ui.context import AppContext
        from .ui.main_window import MainWindow
        ctx = AppContext(settings)
        win = MainWindow(ctx)
    except Exception as exc:  # noqa: BLE001
        log.exception("startup failed")
        QMessageBox.critical(None, APP_NAME, f"Startup failed / 시작 실패:\n{exc}")
        return 1
    win.show()
    if "--smoke-test" in argv:                      # used by the build: start, render once, exit
        from PySide6.QtCore import QTimer
        QTimer.singleShot(int(os.environ.get("FIGURECRAFT_SMOKE_MS", "6000")), app.quit)
    shot = os.environ.get("FIGURECRAFT_AUTOSHOT")        # build/CI aid: demo-generate, screenshot, quit
    if shot and "--smoke-test" in argv:
        from PySide6.QtCore import QTimer

        def _go():
            win.gen._insert_example()
            win.gen.start_generation()

        def _poll():
            if win.gen._generating or win.gen.ws is None:
                QTimer.singleShot(1000, _poll)
                return
            QTimer.singleShot(4000, lambda: (win.grab().save(shot), app.quit()))
        QTimer.singleShot(1500, _go)
        QTimer.singleShot(4000, _poll)
    log.info("FigureCraft %s started (provider=%s, lang=%s)", __version__, settings.provider, settings.language)
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
