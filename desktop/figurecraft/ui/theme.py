"""Original dark UI theme (Qt style sheets). A light variant is provided for the Theme setting."""
from __future__ import annotations

DARK = dict(bg="#13151a", panel="#1a1d24", card="#21252e", card2="#272c37", border="#2f3541", text="#e8ebf2",
            muted="#8e96aa", accent="#7c5cff", accent_h="#9378ff", accent_t="#ffffff", good="#37c78b",
            warn="#f2b134", bad="#ef5a5a", input="#161920", sel="#343a48")
LIGHT = dict(bg="#eef0f5", panel="#f8f9fc", card="#ffffff", card2="#f0f2f7", border="#d4d8e2", text="#1c2030",
             muted="#6a7288", accent="#6b49f2", accent_h="#8366ff", accent_t="#ffffff", good="#1a9b64",
             warn="#c78a0a", bad="#d03a3a", input="#ffffff", sel="#e2e6f0")

_QSS = """
* { font-family: "Segoe UI", "Malgun Gothic", "Noto Sans CJK KR", "NanumGothic", sans-serif; font-size: 13px; }
QWidget { background: %(bg)s; color: %(text)s; }
QMainWindow, QDialog { background: %(bg)s; }
QLabel { background: transparent; }
QFrame#card { background: %(card)s; border: 1px solid %(border)s; border-radius: 10px; }
QFrame#topbar { background: %(panel)s; border-bottom: 1px solid %(border)s; }
QLabel#brand { font-size: 18px; font-weight: 700; letter-spacing: 0.5px; }
QLabel#brandSub { color: %(muted)s; font-size: 11px; }
QLabel#h { font-weight: 600; font-size: 13px; color: %(text)s; }
QLabel#muted, QLabel#hint { color: %(muted)s; font-size: 12px; }
QLabel#demoBadge { background: %(warn)s; color: #1a1300; font-weight: 800; border-radius: 6px; padding: 2px 10px; }
QLabel#pillGOOD { background: %(good)s; color: #04210f; font-weight: 700; border-radius: 9px; padding: 2px 12px; }
QLabel#pillCHECK { background: %(warn)s; color: #231a00; font-weight: 700; border-radius: 9px; padding: 2px 12px; }
QLabel#pillFAILED { background: %(bad)s; color: #2a0505; font-weight: 700; border-radius: 9px; padding: 2px 12px; }
QPushButton { background: %(card2)s; border: 1px solid %(border)s; border-radius: 8px; padding: 6px 12px; }
QPushButton:hover { background: %(sel)s; }
QPushButton:disabled { color: %(muted)s; background: %(card)s; }
QPushButton#primary { background: %(accent)s; color: %(accent_t)s; border: none; font-weight: 700; padding: 10px 16px; font-size: 14px; }
QPushButton#primary:hover { background: %(accent_h)s; }
QPushButton#primary:disabled { background: %(card2)s; color: %(muted)s; }
QPushButton#danger { background: %(bad)s; color: #ffffff; border: none; font-weight: 700; }
QPushButton#nav { background: transparent; border: none; border-radius: 8px; padding: 8px 16px; font-weight: 600; color: %(muted)s; }
QPushButton#nav:hover { background: %(card2)s; color: %(text)s; }
QPushButton#nav:checked { background: %(accent)s; color: %(accent_t)s; }
QPushButton#chip { background: %(card2)s; border: 1px solid %(border)s; border-radius: 13px; padding: 3px 11px; font-size: 12px; }
QPushButton#chip:hover { border-color: %(accent)s; }
QPushButton#seg { border-radius: 0; padding: 5px 10px; margin: 0; }
QPushButton#seg:checked { background: %(accent)s; color: %(accent_t)s; border-color: %(accent)s; }
QPushButton#tool { padding: 4px 9px; }
QPushButton#tool:checked { background: %(accent)s; color: %(accent_t)s; border-color: %(accent)s; }
QPushButton#swatch { border: 2px solid %(border)s; border-radius: 8px; min-width: 34px; max-width: 34px; min-height: 26px; max-height: 26px; padding: 0; }
QPushButton#iconbtn { padding: 3px 7px; min-width: 24px; }
QLineEdit, QPlainTextEdit, QTextEdit, QSpinBox, QDoubleSpinBox, QComboBox {
  background: %(input)s; border: 1px solid %(border)s; border-radius: 8px; padding: 5px 8px; selection-background-color: %(accent)s; }
QPlainTextEdit:focus, QLineEdit:focus, QComboBox:focus, QSpinBox:focus, QDoubleSpinBox:focus { border-color: %(accent)s; }
QComboBox QAbstractItemView { background: %(card)s; border: 1px solid %(border)s; selection-background-color: %(accent)s; }
QCheckBox, QRadioButton { spacing: 8px; background: transparent; }
QCheckBox::indicator, QRadioButton::indicator { width: 16px; height: 16px; }
QCheckBox::indicator { border: 1px solid %(border)s; border-radius: 4px; background: %(input)s; }
QCheckBox::indicator:checked { background: %(accent)s; border-color: %(accent)s; image: url(%(check)s); }
QRadioButton::indicator { border: 1px solid %(border)s; border-radius: 8px; background: %(input)s; }
QRadioButton::indicator:checked { background: %(accent)s; border-color: %(accent)s; }
QScrollArea { border: none; background: transparent; }
QScrollBar:vertical { background: transparent; width: 10px; margin: 2px; }
QScrollBar::handle:vertical { background: %(border)s; border-radius: 4px; min-height: 30px; }
QScrollBar:horizontal { background: transparent; height: 10px; }
QScrollBar::handle:horizontal { background: %(border)s; border-radius: 4px; min-width: 30px; }
QScrollBar::add-line, QScrollBar::sub-line { width: 0; height: 0; }
QListWidget, QTableWidget { background: %(input)s; border: 1px solid %(border)s; border-radius: 8px; }
QListWidget::item:selected, QTableWidget::item:selected { background: %(sel)s; color: %(text)s; }
QHeaderView::section { background: %(card2)s; border: none; padding: 4px; }
QSplitter::handle { background: %(bg)s; width: 6px; }
QToolTip { background: %(card2)s; color: %(text)s; border: 1px solid %(border)s; padding: 4px; }
QMenu { background: %(card)s; border: 1px solid %(border)s; }
QMenu::item:selected { background: %(accent)s; }
QMenuBar { background: %(panel)s; } QMenuBar::item:selected { background: %(card2)s; }
QProgressBar { background: %(input)s; border: 1px solid %(border)s; border-radius: 6px; text-align: center; height: 10px; }
QProgressBar::chunk { background: %(accent)s; border-radius: 5px; }
QTabBar::tab { background: %(card2)s; padding: 6px 14px; border-top-left-radius: 8px; border-top-right-radius: 8px; }
QTabBar::tab:selected { background: %(accent)s; color: %(accent_t)s; }
"""


def palette(name: str) -> dict:
    return LIGHT if name == "light" else DARK


def stylesheet(name: str) -> str:
    from .. import paths
    vals = dict(palette(name))
    vals["check"] = paths.resource_path("assets/check.svg").as_posix()
    return _QSS % vals
