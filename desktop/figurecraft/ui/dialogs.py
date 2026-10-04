from __future__ import annotations

import shutil
from pathlib import Path

from PySide6.QtCore import QUrl
from PySide6.QtGui import QDesktopServices
from PySide6.QtWidgets import (QDialog, QFileDialog, QHBoxLayout, QLabel, QPlainTextEdit, QPushButton, QVBoxLayout)

from .. import i18n, paths
from ..errors import FigureCraftError, describe
from ..logging_setup import redact
from .. import APP_NAME

tr = i18n.tr


def open_log_folder() -> None:
    QDesktopServices.openUrl(QUrl.fromLocalFile(str(paths.logs_dir())))


def save_log_as(parent) -> Path | None:
    src = paths.logs_dir() / "figurecraft.log"
    dest, _ = QFileDialog.getSaveFileName(parent, tr("ui.save_log"), str(Path.home() / "figurecraft.log"), "Log (*.log *.txt)")
    if not dest:
        return None
    if src.exists():
        Path(dest).write_text(redact(src.read_text(encoding="utf-8", errors="replace")), encoding="utf-8")
    return Path(dest)


class ErrorDialog(QDialog):
    """Korean/English error box with Retry / Save log / Open settings."""

    RETRY, SETTINGS, CLOSE = 1, 2, 0

    def __init__(self, parent, exc: BaseException, allow_retry: bool = True, title: str | None = None):
        super().__init__(parent)
        self.setWindowTitle(title or tr("ui.error"))
        self.setMinimumWidth(480)
        self.choice = self.CLOSE
        lay = QVBoxLayout(self)
        head = QLabel(describe(exc))
        head.setWordWrap(True)
        head.setStyleSheet("font-size: 14px; font-weight: 600;")
        lay.addWidget(head)
        detail = getattr(exc, "detail", "") or str(exc)
        if detail and not isinstance(exc, FigureCraftError) or (isinstance(exc, FigureCraftError) and exc.detail):
            box = QPlainTextEdit(redact(str(detail)))
            box.setReadOnly(True)
            box.setMaximumHeight(90)
            lay.addWidget(QLabel(tr("ui.details")))
            lay.addWidget(box)
        row = QHBoxLayout()
        if allow_retry:
            b = QPushButton(tr("ui.retry")); b.setObjectName("primary"); b.clicked.connect(lambda: self._done(self.RETRY)); row.addWidget(b)
        b = QPushButton(tr("ui.save_log")); b.clicked.connect(lambda: save_log_as(self)); row.addWidget(b)
        b = QPushButton(tr("ui.open_settings")); b.clicked.connect(lambda: self._done(self.SETTINGS)); row.addWidget(b)
        row.addStretch()
        b = QPushButton(tr("ui.close")); b.clicked.connect(lambda: self._done(self.CLOSE)); row.addWidget(b)
        lay.addLayout(row)

    def _done(self, c: int):
        self.choice = c
        self.accept()
