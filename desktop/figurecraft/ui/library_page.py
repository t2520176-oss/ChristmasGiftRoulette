"""Project library: thumbnails, open / rename / duplicate / delete / export."""
from __future__ import annotations

import shutil
from pathlib import Path

from PySide6.QtCore import QSize, Qt
from PySide6.QtGui import QIcon, QPixmap
from PySide6.QtWidgets import (QFileDialog, QHBoxLayout, QInputDialog, QLabel, QListWidget, QListWidgetItem,
                               QMessageBox, QPushButton, QVBoxLayout, QWidget)

from .. import i18n
from ..errors import describe
from .context import AppContext

tr = i18n.tr


class LibraryPage(QWidget):
    def __init__(self, ctx: AppContext, parent=None):
        super().__init__(parent)
        self.ctx = ctx
        lay = QVBoxLayout(self)
        lay.setContentsMargins(16, 12, 16, 12)
        head = QHBoxLayout()
        t = QLabel(tr("lib.title")); t.setObjectName("h"); t.setStyleSheet("font-size: 18px; font-weight: 700;")
        head.addWidget(t); head.addStretch()
        self.count = QLabel(""); self.count.setObjectName("muted"); head.addWidget(self.count)
        lay.addLayout(head)
        self.list = QListWidget()
        self.list.setViewMode(QListWidget.ViewMode.IconMode)
        self.list.setIconSize(QSize(180, 180))
        self.list.setGridSize(QSize(220, 280))
        self.list.setResizeMode(QListWidget.ResizeMode.Adjust)
        self.list.setMovement(QListWidget.Movement.Static)
        self.list.setWordWrap(True)
        self.list.setSpacing(6)
        self.list.itemDoubleClicked.connect(lambda _: self.open_selected())
        lay.addWidget(self.list, 1)
        self.empty = QLabel(tr("lib.empty")); self.empty.setObjectName("muted"); self.empty.setAlignment(Qt.AlignmentFlag.AlignCenter)
        lay.addWidget(self.empty)
        row = QHBoxLayout()
        for key, fn in (("lib.open", self.open_selected), ("lib.rename", self.rename), ("lib.duplicate", self.duplicate),
                        ("lib.delete", self.delete), ("lib.export", self.export_zip), ("lib.open_folder", self.open_folder)):
            b = QPushButton(tr(key)); b.clicked.connect(fn)
            if key == "lib.open":
                b.setObjectName("primary")
            row.addWidget(b)
        row.addStretch()
        lay.addLayout(row)
        self.ctx.projectsChanged.connect(self.refresh)
        self.ctx.settingsChanged.connect(self.refresh)
        self.refresh()

    def _selected_id(self) -> str | None:
        it = self.list.currentItem()
        return it.data(Qt.ItemDataRole.UserRole) if it else None

    def refresh(self):
        self.list.clear()
        projects = self.ctx.store.list()
        for p in projects:
            m = p.meta
            thumb = p.file("preview.png")
            icon = QIcon(QPixmap(str(thumb))) if thumb.exists() else QIcon()
            size = "×".join(f"{v:.0f}" for v in m.size_mm) + " mm" if any(m.size_mm) else "—"
            title = ("[DEMO] " if m.is_demo and not m.title.startswith("DEMO") else "") + (m.title or "Untitled")
            status = tr(f"lib.status.{m.status}") if m.status in ("ready", "failed", "running", "empty") else m.status
            txt = f"{title}\n{m.updated[:16].replace('T', ' ')}\n{size} • {tr('lib.colors', n=m.colors)}\n{status}"
            it = QListWidgetItem(icon, txt)
            it.setData(Qt.ItemDataRole.UserRole, m.id)
            it.setToolTip(m.prompt[:300])
            self.list.addItem(it)
        self.count.setText(tr("lib.count", n=len(projects)))
        self.empty.setVisible(not projects)

    def open_selected(self):
        pid = self._selected_id()
        if pid:
            self.ctx.openProjectRequested.emit(pid)

    def rename(self):
        pid = self._selected_id()
        if not pid:
            return
        p = self.ctx.store.open(pid)
        name, ok = QInputDialog.getText(self, tr("lib.rename"), tr("lib.rename_prompt"), text=p.meta.title)
        if ok and name.strip():
            self.ctx.store.rename(pid, name)
            self.refresh()

    def duplicate(self):
        pid = self._selected_id()
        if pid:
            try:
                self.ctx.store.duplicate(pid)
            except Exception as exc:  # noqa: BLE001
                QMessageBox.warning(self, tr("ui.error"), describe(exc))
            self.refresh()

    def delete(self):
        pid = self._selected_id()
        if not pid:
            return
        if QMessageBox.question(self, tr("lib.delete"), tr("lib.confirm_delete")) == QMessageBox.StandardButton.Yes:
            self.ctx.store.delete(pid)
            self.refresh()

    def export_zip(self):
        pid = self._selected_id()
        if not pid:
            return
        p = self.ctx.store.open(pid)
        f, _ = QFileDialog.getSaveFileName(self, tr("lib.export"), str(Path.home() / f"{p.meta.title or 'figure'}.zip"), "ZIP (*.zip)")
        if f:
            try:
                self.ctx.store.export_zip(pid, f)
                QMessageBox.information(self, tr("ui.info"), tr("export.saved", f=f))
            except Exception as exc:  # noqa: BLE001
                QMessageBox.warning(self, tr("ui.error"), describe(exc))

    def open_folder(self):
        from PySide6.QtCore import QUrl
        from PySide6.QtGui import QDesktopServices
        QDesktopServices.openUrl(QUrl.fromLocalFile(str(self.ctx.store.root)))
