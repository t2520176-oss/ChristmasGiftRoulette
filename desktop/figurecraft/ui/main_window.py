"""Main window: top bar (brand, navigation, cloud GPU status), pages, Help menu."""
from __future__ import annotations

from PySide6.QtCore import Qt, QUrl
from PySide6.QtGui import QAction, QDesktopServices
from PySide6.QtWidgets import (QApplication, QButtonGroup, QFrame, QHBoxLayout, QLabel, QMainWindow, QMessageBox,
                               QPushButton, QStackedWidget, QVBoxLayout, QWidget)

from .. import APP_NAME, APP_SUBTITLE, __version__, i18n, paths
from ..core.models import CloudStatus
from ..logging_setup import get_logger
from . import theme
from .context import AppContext
from .dialogs import open_log_folder, save_log_as
from .generate_page import STATUS_COLORS, GeneratePage
from .library_page import LibraryPage
from .settings_page import SettingsPage
from .workers import run_async

log = get_logger("ui.main")
tr = i18n.tr


class MainWindow(QMainWindow):
    def __init__(self, ctx: AppContext):
        super().__init__()
        self.ctx = ctx
        self.setWindowTitle(f"{APP_NAME} – {APP_SUBTITLE}")
        self.resize(1480, 900)
        self.setMinimumSize(1100, 700)
        self.ctx.cloudStatusChanged.connect(self._on_cloud)
        self.ctx.openSettingsRequested.connect(lambda: self.show_page(2))
        self.ctx.openProjectRequested.connect(self.open_project)
        self.ctx.gpuRisk.connect(self._on_gpu_risk)
        self._build()
        self.ctx.reload()

    # ----------------------------------------------------------------
    def _build(self):
        s = self.ctx.settings
        i18n.set_language(s.language)
        QApplication.instance().setStyleSheet(theme.stylesheet(s.theme))
        central = QWidget()
        v = QVBoxLayout(central); v.setContentsMargins(0, 0, 0, 0); v.setSpacing(0)

        bar = QFrame(); bar.setObjectName("topbar")
        h = QHBoxLayout(bar); h.setContentsMargins(16, 8, 16, 8); h.setSpacing(8)
        brand = QVBoxLayout(); brand.setSpacing(0)
        t = QLabel(APP_NAME); t.setObjectName("brand")
        sub = QLabel(APP_SUBTITLE); sub.setObjectName("brandSub")
        brand.addWidget(t); brand.addWidget(sub)
        h.addLayout(brand)
        h.addSpacing(24)
        self.nav = QButtonGroup(self); self.nav.setExclusive(True)
        self.nav_btns = []
        for i, key in enumerate(("nav.generate", "nav.library", "nav.settings")):
            b = QPushButton(tr(key)); b.setObjectName("nav"); b.setCheckable(True)
            b.clicked.connect(lambda _=False, idx=i: self.show_page(idx))
            self.nav.addButton(b); self.nav_btns.append(b); h.addWidget(b)
        self.nav_btns[0].setChecked(True)
        h.addStretch()
        self.btn_new = QPushButton(tr("nav.new")); self.btn_new.clicked.connect(self._new_project); h.addWidget(self.btn_new)
        self.gpu_stop = QPushButton(tr("top.stop_gpu")); self.gpu_stop.setObjectName("danger"); self.gpu_stop.setVisible(False)
        self.gpu_stop.clicked.connect(self._stop_gpu_now); h.addWidget(self.gpu_stop)
        self.status_label = QLabel(); self.status_label.setTextFormat(Qt.TextFormat.RichText); h.addWidget(self.status_label)
        v.addWidget(bar)

        self.stack = QStackedWidget()
        self.gen = GeneratePage(self.ctx)
        self.lib = LibraryPage(self.ctx)
        self.settings_page = SettingsPage(self.ctx)
        self.settings_page.languageChanged = self.rebuild
        for p in (self.gen, self.lib, self.settings_page):
            self.stack.addWidget(p)
        v.addWidget(self.stack, 1)
        self.setCentralWidget(central)
        self._build_menu()
        self._on_cloud(self.ctx.cloud_status, self.ctx.cloud_text)

    def _build_menu(self):
        mb = self.menuBar(); mb.clear()
        m = mb.addMenu(tr("menu.file"))
        a = QAction(tr("nav.new"), self); a.triggered.connect(self._new_project); m.addAction(a)
        a = QAction(tr("menu.open_projects"), self); a.triggered.connect(self.lib.open_folder); m.addAction(a)
        m.addSeparator()
        a = QAction(tr("menu.quit"), self); a.triggered.connect(self.close); m.addAction(a)
        m = mb.addMenu(tr("menu.help"))
        a = QAction(tr("menu.open_log"), self); a.triggered.connect(open_log_folder); m.addAction(a)
        a = QAction(tr("ui.save_log"), self); a.triggered.connect(lambda: save_log_as(self)); m.addAction(a)
        a = QAction(tr("menu.about"), self); a.triggered.connect(self._about); m.addAction(a)

    def rebuild(self):
        """Language/theme changed: rebuild all text. The open project is kept."""
        pid = self.gen.ws.project.meta.id if self.gen.ws else None
        self.setCentralWidget(None)
        self._build()
        self.show_page(2)
        self.nav_btns[2].setChecked(True)
        if pid:
            try:
                self.gen.open_project(self.ctx.store.open(pid))
            except Exception:  # noqa: BLE001
                log.exception("could not reopen project after rebuild")

    # ----------------------------------------------------------------
    def show_page(self, idx: int):
        self.stack.setCurrentIndex(idx)
        self.nav_btns[idx].setChecked(True)
        if idx == 1:
            self.lib.refresh()

    def open_project(self, pid: str):
        try:
            p = self.ctx.store.open(pid)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, tr("ui.error"), str(exc))
            return
        self.show_page(0)
        self.gen.open_project(p)

    def _new_project(self):
        self.show_page(0)
        self.gen.new_project()

    def _on_cloud(self, status, text: str = ""):
        color = STATUS_COLORS.get(status, "#8e96aa")
        label = tr(f"cloud.{status.value}")
        extra = f" · {text}" if text else ""
        self.status_label.setText(f'<span style="color:{color}; font-size:16px;">●</span> '
                                  f'<span style="font-weight:600;">{tr("top.cloud_gpu")}: {label}{extra}</span>')
        p = self.ctx.provider()
        self.gpu_stop.setVisible(p.supports_lifecycle and not p.capabilities().is_demo
                                 and status in (CloudStatus.READY, CloudStatus.GENERATING))

    def _on_gpu_risk(self, risk: bool):
        if risk and not self.ctx.provider().capabilities().is_demo:
            self.gpu_stop.setVisible(True)

    def _stop_gpu_now(self):
        prov = self.ctx.provider()
        self.ctx.set_cloud(CloudStatus.STOPPING)

        def ok(_):
            self.ctx.set_cloud(CloudStatus.DISCONNECTED)
            self.ctx.set_gpu_risk(False)
            self.gpu_stop.setVisible(False)
            QMessageBox.information(self, tr("ui.info"), tr("msg.gpu_stopped"))

        def bad(exc):
            self.ctx.set_cloud(CloudStatus.READY)
            QMessageBox.warning(self, tr("ui.gpu_warning"), tr("msg.gpu_stop_failed") + "\n\n" + prov.manual_instructions(i18n.get_language()))
        run_async(prov.stop_gpu, ok, bad, self)

    def _about(self):
        QMessageBox.about(self, APP_NAME, tr("about.text", v=__version__, p=str(paths.app_data_dir())))

    def closeEvent(self, e):  # noqa: N802
        if self.gen._generating:
            if QMessageBox.question(self, tr("ui.confirm"), tr("msg.quit_generating")) != QMessageBox.StandardButton.Yes:
                e.ignore()
                return
            self.gen.stop_generation()
        if self.ctx.gpu_may_be_running and not self.ctx.provider().capabilities().is_demo:
            r = QMessageBox.warning(self, tr("ui.gpu_warning"),
                                    tr("msg.quit_gpu_running") + "\n\n" + self.ctx.provider().manual_instructions(i18n.get_language()),
                                    QMessageBox.StandardButton.Close | QMessageBox.StandardButton.Cancel)
            if r == QMessageBox.StandardButton.Cancel:
                e.ignore()
                return
        super().closeEvent(e)
