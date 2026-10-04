"""Small reusable widgets."""
from __future__ import annotations

from PySide6.QtCore import QPoint, QRect, QSize, Qt, Signal
from PySide6.QtGui import QColor
from PySide6.QtWidgets import (QColorDialog, QFrame, QHBoxLayout, QLabel, QLayout, QPushButton, QSizePolicy,
                               QVBoxLayout, QWidget)

from .. import i18n
from ..core.models import Stage, StageState


class FlowLayout(QLayout):
    """Wrapping layout for chips."""

    def __init__(self, parent=None, spacing=6):
        super().__init__(parent)
        self._items = []
        self.setSpacing(spacing)
        self.setContentsMargins(0, 0, 0, 0)

    def addItem(self, item):
        self._items.append(item)

    def count(self):
        return len(self._items)

    def itemAt(self, i):
        return self._items[i] if 0 <= i < len(self._items) else None

    def takeAt(self, i):
        return self._items.pop(i) if 0 <= i < len(self._items) else None

    def expandingDirections(self):
        return Qt.Orientation(0)

    def hasHeightForWidth(self):
        return True

    def heightForWidth(self, w):
        return self._do(QRect(0, 0, w, 0), True)

    def setGeometry(self, rect):
        super().setGeometry(rect)
        self._do(rect, False)

    def sizeHint(self):
        return self.minimumSize()

    def minimumSize(self):
        s = QSize()
        for it in self._items:
            s = s.expandedTo(it.minimumSize())
        m = self.contentsMargins()
        return s + QSize(m.left() + m.right(), m.top() + m.bottom())

    def _do(self, rect, test):
        x, y, lh = rect.x(), rect.y(), 0
        sp = self.spacing()
        for it in self._items:
            w = it.sizeHint().width()
            if x + w > rect.right() and lh > 0:
                x, y, lh = rect.x(), y + lh + sp, 0
            if not test:
                it.setGeometry(QRect(QPoint(x, y), it.sizeHint()))
            x += w + sp
            lh = max(lh, it.sizeHint().height())
        return y + lh - rect.y()


class Card(QFrame):
    """Titled section card."""

    def __init__(self, title: str = "", parent=None):
        super().__init__(parent)
        self.setObjectName("card")
        self.lay = QVBoxLayout(self)
        self.lay.setContentsMargins(12, 10, 12, 12)
        self.lay.setSpacing(8)
        self.title_label = QLabel(title)
        self.title_label.setObjectName("h")
        if title:
            self.lay.addWidget(self.title_label)

    def add(self, w):
        self.lay.addWidget(w)
        return w

    def add_layout(self, l):
        self.lay.addLayout(l)
        return l


class SwatchButton(QPushButton):
    """Colour swatch; click opens a colour dialog."""

    colorChanged = Signal(str)

    def __init__(self, hex_: str = "#FFFFFF", parent=None, editable=True):
        super().__init__(parent)
        self.setObjectName("swatch")
        self._hex = hex_
        self._editable = editable
        self.setCursor(Qt.CursorShape.PointingHandCursor if editable else Qt.CursorShape.ArrowCursor)
        self.clicked.connect(self._pick)
        self._apply()

    def hex(self) -> str:
        return self._hex

    def set_hex(self, h: str):
        self._hex = h.upper()
        self._apply()

    def _apply(self):
        self.setStyleSheet(f"QPushButton#swatch {{ background: {self._hex}; }}")
        self.setToolTip(self._hex)

    def _pick(self):
        if not self._editable:
            return
        c = QColorDialog.getColor(QColor(self._hex), self, i18n.tr("ui.pick_color"))
        if c.isValid():
            self.set_hex(c.name().upper())
            self.colorChanged.emit(self._hex)


class StageList(QWidget):
    """The 11 pipeline stages with state icons. Stage based progress - no fake percentages."""

    ICON = {StageState.PENDING: "○", StageState.ACTIVE: "◔", StageState.DONE: "✓", StageState.SKIPPED: "–",
            StageState.FAILED: "✕"}

    def __init__(self, parent=None):
        super().__init__(parent)
        self._lay = FlowLayout(self, spacing=4)
        self._labels: dict[Stage, QLabel] = {}
        self._detail: dict[Stage, str] = {}
        self._state: dict[Stage, StageState] = {}
        for s in Stage:
            lb = QLabel()
            lb.setObjectName("chipLabel")
            lb.setContentsMargins(8, 2, 8, 2)
            self._labels[s] = lb
            self._lay.addWidget(lb)
        self.reset()

    def reset(self):
        for s in Stage:
            self.set_state(s, StageState.PENDING, "")

    def set_state(self, s: Stage, st: StageState, detail: str = ""):
        self._state[s] = st
        self._detail[s] = detail
        self._render(s)

    def retranslate(self):
        for s in Stage:
            self._render(s)

    def _render(self, s: Stage):
        st = self._state.get(s, StageState.PENDING)
        colors = {StageState.PENDING: "#8e96aa", StageState.ACTIVE: "#7c5cff", StageState.DONE: "#37c78b",
                  StageState.SKIPPED: "#8e96aa", StageState.FAILED: "#ef5a5a"}
        det = self._detail.get(s, "")
        text = f"{self.ICON[st]} {i18n.tr(s.i18n_key)}"
        lb = self._labels[s]
        lb.setText(text)
        lb.setToolTip(det)
        c = colors[st]
        weight = "700" if st == StageState.ACTIVE else "500"
        bg = "rgba(124,92,255,0.18)" if st == StageState.ACTIVE else "transparent"
        lb.setStyleSheet(f"QLabel#chipLabel {{ color: {c}; font-weight: {weight}; border: 1px solid {c}; "
                         f"border-radius: 10px; background: {bg}; font-size: 11px; }}")


def hline() -> QFrame:
    f = QFrame()
    f.setFrameShape(QFrame.Shape.HLine)
    f.setStyleSheet("color: #2f3541; background: #2f3541; max-height: 1px;")
    return f
