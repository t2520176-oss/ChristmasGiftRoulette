"""Shared application state for the UI pages."""
from __future__ import annotations

from PySide6.QtCore import QObject, Signal

from ..core.models import CloudStatus
from ..core.project_store import ProjectStore
from ..logging_setup import get_logger
from ..providers.base import GenerationProvider
from ..providers.registry import create_provider
from ..settings import Settings

log = get_logger("ctx")


class AppContext(QObject):
    settingsChanged = Signal()
    cloudStatusChanged = Signal(object, str)      # (CloudStatus, extra text)
    openSettingsRequested = Signal()
    projectsChanged = Signal()
    openProjectRequested = Signal(str)
    gpuRisk = Signal(bool)                        # True while a paid GPU may still be running

    def __init__(self, settings: Settings):
        super().__init__()
        self.settings = settings
        self.store = ProjectStore(settings.projects_path())
        self._provider: GenerationProvider | None = None
        self.cloud_status = CloudStatus.DISCONNECTED
        self.cloud_text = ""
        self.gpu_may_be_running = False

    def provider(self) -> GenerationProvider:
        if self._provider is None or self._provider.id != self.settings.provider:
            self._provider = create_provider(self.settings)
        return self._provider

    def reload(self) -> None:
        """Call after settings changed."""
        self.store = ProjectStore(self.settings.projects_path())
        self._provider = create_provider(self.settings)
        if self.settings.provider == "mock":
            self.set_cloud(CloudStatus.READY, "DEMO")
        else:
            self.set_cloud(CloudStatus.DISCONNECTED)
        self.settingsChanged.emit()

    def set_cloud(self, status: CloudStatus, text: str = "") -> None:
        self.cloud_status, self.cloud_text = status, text
        self.cloudStatusChanged.emit(status, text)

    def set_gpu_risk(self, risk: bool) -> None:
        self.gpu_may_be_running = risk
        self.gpuRisk.emit(risk)
