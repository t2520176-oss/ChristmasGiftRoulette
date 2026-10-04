"""Background workers so the UI never blocks."""
from __future__ import annotations

import threading
import traceback
from typing import Any, Callable

from PySide6.QtCore import QObject, QThread, Signal, Slot

from ..core import pipeline
from ..core.models import CloudStatus, GenerationRequest, Stage, StageState
from ..logging_setup import get_logger

log = get_logger("worker")


class GenerationWorker(QObject):
    stage = Signal(object, object, str)
    cloud = Signal(object)
    info = Signal(str)
    hints = Signal(object)
    gpuWarning = Signal(str)
    finished = Signal(object)          # PipelineResult
    failed = Signal(object)            # exception

    def __init__(self, request: GenerationRequest, provider, settings, store, project=None):
        super().__init__()
        self.request, self.provider, self.settings, self.store, self.project = request, provider, settings, store, project
        self._cancel = threading.Event()

    def cancel(self):
        self._cancel.set()

    def run(self):
        hooks = pipeline.Hooks(
            stage=lambda s, st, d: self.stage.emit(s, st, d),
            cloud=lambda c: self.cloud.emit(c),
            info=lambda t: self.info.emit(t),
            cancelled=self._cancel.is_set,
            hints=lambda h: self.hints.emit(h),
            gpu_warning=lambda t: self.gpuWarning.emit(t),
        )
        try:
            res = pipeline.run_generation(self.request, self.provider, self.settings, self.store, hooks, self.project)
            self.finished.emit(res)
        except BaseException as exc:   # noqa: BLE001 - report everything to the UI
            log.error("generation failed: %s\n%s", exc, traceback.format_exc())
            self.failed.emit(exc)


class TaskWorker(QObject):
    """Run any callable off the UI thread."""

    done = Signal(object)
    failed = Signal(object)

    def __init__(self, fn: Callable[[], Any]):
        super().__init__()
        self.fn = fn

    def run(self):
        try:
            self.done.emit(self.fn())
        except BaseException as exc:   # noqa: BLE001
            log.error("task failed: %s\n%s", exc, traceback.format_exc())
            self.failed.emit(exc)


def start_in_thread(worker: QObject, parent: QObject) -> QThread:
    th = QThread(parent)
    worker.moveToThread(th)
    th.started.connect(worker.run)
    for sig in ("finished", "failed", "done"):
        if hasattr(worker, sig):
            getattr(worker, sig).connect(th.quit)
    th.finished.connect(th.deleteLater)
    th.start()
    return th


class _Receiver(QObject):
    """Lives in the UI thread, so queued signal delivery runs the callbacks there (lambdas would not)."""

    def __init__(self, ok, bad, registry: set, key):
        super().__init__()
        self._ok, self._bad, self._registry, self._key = ok, bad, registry, key

    @Slot(object)
    def on_done(self, res):
        try:
            self._ok(res)
        finally:
            pass

    @Slot(object)
    def on_failed(self, exc):
        self._bad(exc)

    @Slot()
    def on_thread_finished(self):
        self._registry.discard(self._key)
        _KEEP.pop(self._key, None)


_ACTIVE: set = set()


def run_async(fn: Callable[[], Any], ok: Callable[[Any], None], bad: Callable[[BaseException], None], parent: QObject) -> None:
    """Run fn in a background thread; ok/bad are invoked in the UI thread."""
    worker = TaskWorker(fn)
    key = object()
    recv = _Receiver(ok, bad, _ACTIVE, key)
    worker.done.connect(recv.on_done)
    worker.failed.connect(recv.on_failed)
    th = start_in_thread(worker, parent)
    th.finished.connect(recv.on_thread_finished)
    _ACTIVE.add(key)
    recv._keep = (th, worker)          # keep python references alive until the receiver is released
    _KEEP[key] = (recv, th, worker)
    th.finished.connect(lambda k=key: None)


_KEEP: dict = {}
