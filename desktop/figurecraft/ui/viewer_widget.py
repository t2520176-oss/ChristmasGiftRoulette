"""3D viewer: Three.js inside QWebEngineView, bundled locally (works offline)."""
from __future__ import annotations

import json
from pathlib import Path

from PySide6.QtCore import QObject, QUrl, Signal, Slot
from PySide6.QtWebChannel import QWebChannel
from PySide6.QtWebEngineCore import QWebEnginePage, QWebEngineSettings
from PySide6.QtWebEngineWidgets import QWebEngineView

from .. import paths
from ..core import preview
from ..logging_setup import get_logger

log = get_logger("viewer")


class _Bridge(QObject):
    ready = Signal()
    regionPicked = Signal(int)
    error = Signal(str)

    @Slot()
    def viewerReady(self):
        self.ready.emit()

    @Slot(str)
    def viewerError(self, what):
        self.error.emit(what)


class _Page(QWebEnginePage):
    def javaScriptConsoleMessage(self, level, message, line, source):  # noqa: N802
        log.debug("viewer js[%s]: %s (%s:%d)", level.name if hasattr(level, "name") else level, message, source, line)


class ViewerWidget(QWebEngineView):
    regionPicked = Signal(int)
    loadFailed = Signal(str)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._ready = False
        self._queue: list[str] = []
        self.setPage(_Page(self))
        s = self.settings()
        s.setAttribute(QWebEngineSettings.WebAttribute.LocalContentCanAccessFileUrls, True)
        s.setAttribute(QWebEngineSettings.WebAttribute.WebGLEnabled, True)
        s.setAttribute(QWebEngineSettings.WebAttribute.Accelerated2dCanvasEnabled, True)
        self.page().setBackgroundColor(Qt_color(0x14, 0x16, 0x1b))
        self._bridge = _Bridge()
        self._bridge.ready.connect(self._on_ready)
        self._bridge.error.connect(self.loadFailed)
        self._bridge.regionPicked.connect(self.regionPicked)
        self._channel = QWebChannel(self.page())
        self._channel.registerObject("bridge", self._bridge)
        self.page().setWebChannel(self._channel)
        self.setContextMenuPolicy(Qt_NoContextMenu())
        html = paths.resource_path("viewer/index.html")
        self.load(QUrl.fromLocalFile(str(html)))

    # ---- plumbing
    def _on_ready(self):
        self._ready = True
        for js in self._queue:
            self.page().runJavaScript(js)
        self._queue.clear()

    def _js(self, code: str):
        if self._ready:
            self.page().runJavaScript(code)
        else:
            self._queue.append(code)

    # ---- API
    def show_payload(self, payload: dict):
        self._js("window.fc.loadPayload(" + json.dumps(preview.payload_json(payload)) + ");")

    def show_glb_file(self, path: str | Path):
        self._js("window.fc.loadGLB(" + json.dumps(preview.glb_b64(path)) + ");")

    def clear(self):
        self._js("window.fc.clear();")

    def set_mode(self, mode: str):
        self._js(f"window.fc.setMode({json.dumps(mode)});")

    def set_wireframe(self, on: bool):
        self._js(f"window.fc.setWireframe({str(bool(on)).lower()});")

    def set_bbox(self, on: bool):
        self._js(f"window.fc.setBBox({str(bool(on)).lower()});")

    def set_view(self, name: str):
        self._js(f"window.fc.setView({json.dumps(name)});")

    def reset_view(self):
        self._js("window.fc.resetView();")

    def set_label_visible(self, label: int, visible: bool):
        self._js(f"window.fc.setLabelVisible({int(label)}, {str(bool(visible)).lower()});")

    def select_region(self, region: int):
        self._js(f"window.fc.setSelectedRegion({int(region)});")

    def set_cut_plane(self, z: float | None):
        self._js("window.fc.setCutPlane(" + ("null" if z is None else repr(float(z))) + ");")


def Qt_color(r, g, b):
    from PySide6.QtGui import QColor
    return QColor(r, g, b)


def Qt_NoContextMenu():
    from PySide6.QtCore import Qt
    return Qt.ContextMenuPolicy.NoContextMenu
