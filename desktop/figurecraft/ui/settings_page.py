"""Settings page."""
from __future__ import annotations

from pathlib import Path

from PySide6.QtCore import Qt, QUrl
from PySide6.QtGui import QDesktopServices
from PySide6.QtWidgets import (QCheckBox, QComboBox, QDoubleSpinBox, QFileDialog, QFormLayout, QHBoxLayout, QLabel,
                               QLineEdit, QMessageBox, QPushButton, QScrollArea, QSpinBox, QVBoxLayout, QWidget)

from .. import i18n
from ..core import bambu
from ..core.models import CloudStatus
from ..errors import describe
from ..settings import DEFAULT_AMS
from .context import AppContext
from .widgets import Card, SwatchButton
from .workers import run_async

tr = i18n.tr


class SettingsPage(QWidget):
    languageChanged = None   # set by MainWindow (callable)

    def __init__(self, ctx: AppContext, parent=None):
        super().__init__(parent)
        self.ctx = ctx
        outer = QVBoxLayout(self)
        sa = QScrollArea(); sa.setWidgetResizable(True)
        outer.addWidget(sa)
        body = QWidget(); sa.setWidget(body)
        lay = QVBoxLayout(body)
        lay.setContentsMargins(24, 16, 24, 16)
        lay.setSpacing(12)
        s = ctx.settings
        t = QLabel(tr("set.title")); t.setStyleSheet("font-size: 18px; font-weight: 700;")
        lay.addWidget(t)

        # general
        c = Card(tr("set.general")); f = QFormLayout(); f.setLabelAlignment(Qt.AlignmentFlag.AlignLeft)
        self.lang = QComboBox(); self.lang.addItem("한국어", "ko"); self.lang.addItem("English", "en")
        self.lang.setCurrentIndex(self.lang.findData(s.language))
        self.theme = QComboBox(); self.theme.addItem(tr("set.theme_dark"), "dark"); self.theme.addItem(tr("set.theme_light"), "light")
        self.theme.setCurrentIndex(self.theme.findData(s.theme))
        self.proj = QLineEdit(str(s.projects_path()))
        b = QPushButton(tr("ui.browse")); b.clicked.connect(self._browse_proj)
        r = QHBoxLayout(); r.addWidget(self.proj); r.addWidget(b)
        f.addRow(tr("set.language"), self.lang); f.addRow(tr("set.theme"), self.theme); f.addRow(tr("set.project_dir"), r)
        c.add_layout(f); lay.addWidget(c)

        # cloud
        c = Card(tr("set.cloud")); f = QFormLayout()
        self.provider = QComboBox()
        for pid in ("mock", "generic_http", "runpod"):
            self.provider.addItem(tr(f"prov.{pid}"), pid)
        self.provider.setCurrentIndex(self.provider.findData(s.provider))
        self.endpoint = QLineEdit(s.endpoint); self.endpoint.setPlaceholderText("https://my-gpu.example.com  /  RunPod endpoint id")
        self.api_key = QLineEdit(); self.api_key.setEchoMode(QLineEdit.EchoMode.Password)
        has_env = bool(__import__("os").environ.get("FIGURECRAFT_API_KEY"))
        self.api_key.setPlaceholderText(tr("set.key_env") if has_env else (tr("set.key_saved") if s.api_key_enc else tr("set.key_ph")))
        kb = QPushButton(tr("set.key_delete")); kb.clicked.connect(self._delete_key)
        kr = QHBoxLayout(); kr.addWidget(self.api_key); kr.addWidget(kb)
        self.rp_key = QLineEdit(); self.rp_key.setEchoMode(QLineEdit.EchoMode.Password)
        self.rp_key.setPlaceholderText(tr("set.key_saved") if s.runpod_api_key_enc else tr("set.rp_key_ph"))
        self.engine = QComboBox()
        for e in ("trellis", "hunyuan3d", "mock"):
            self.engine.addItem(tr(f"engine.{e}"), e)
        self.engine.setCurrentIndex(self.engine.findData(s.engine))
        self.timeout = QSpinBox(); self.timeout.setRange(30, 7200); self.timeout.setSuffix(" s"); self.timeout.setValue(s.timeout_s)
        self.autostop = QCheckBox(tr("set.auto_stop")); self.autostop.setChecked(s.auto_stop_gpu)
        self.pod = QLineEdit(s.runpod_pod_id); self.pod.setPlaceholderText(tr("set.pod_ph"))
        f.addRow(tr("set.provider"), self.provider); f.addRow(tr("set.endpoint"), self.endpoint)
        f.addRow(tr("set.api_key"), kr); f.addRow(tr("set.engine"), self.engine); f.addRow(tr("set.timeout"), self.timeout)
        f.addRow(tr("set.pod"), self.pod); f.addRow(tr("set.rp_key"), self.rp_key); f.addRow("", self.autostop)
        c.add_layout(f)
        row = QHBoxLayout()
        self.btn_test = QPushButton(tr("set.test")); self.btn_test.clicked.connect(self._test)
        self.test_result = QLabel(""); self.test_result.setWordWrap(True)
        row.addWidget(self.btn_test); row.addWidget(self.test_result, 1)
        c.add_layout(row)
        hint = QLabel(tr("set.cloud_hint")); hint.setObjectName("hint"); hint.setWordWrap(True); c.add(hint)
        lay.addWidget(c)

        # printing
        c = Card(tr("set.printing")); f = QFormLayout()
        self.nozzle = QDoubleSpinBox(); self.nozzle.setRange(0.1, 1.2); self.nozzle.setSingleStep(0.05); self.nozzle.setSuffix(" mm"); self.nozzle.setValue(s.nozzle_mm)
        self.feature = QDoubleSpinBox(); self.feature.setRange(0.2, 5); self.feature.setSingleStep(0.1); self.feature.setSuffix(" mm"); self.feature.setValue(s.min_feature_mm)
        self.wall = QDoubleSpinBox(); self.wall.setRange(0.4, 6); self.wall.setSingleStep(0.1); self.wall.setSuffix(" mm"); self.wall.setValue(s.wall_mm)
        self.height = QDoubleSpinBox(); self.height.setRange(10, 400); self.height.setSuffix(" mm"); self.height.setValue(s.default_height_mm)
        self.colors = QSpinBox(); self.colors.setRange(1, 4); self.colors.setValue(s.default_colors)
        f.addRow(tr("gen.nozzle"), self.nozzle); f.addRow(tr("gen.min_feature"), self.feature); f.addRow(tr("gen.wall"), self.wall)
        f.addRow(tr("set.default_height"), self.height); f.addRow(tr("set.default_colors"), self.colors)
        c.add_layout(f); lay.addWidget(c)

        # bambu
        c = Card(tr("set.bambu")); f = QFormLayout()
        self.bambu = QLineEdit(s.bambu_path)
        found = bambu.detect()
        self.bambu.setPlaceholderText(str(found) if found else tr("set.bambu_ph"))
        bb = QPushButton(tr("ui.browse")); bb.clicked.connect(self._browse_bambu)
        bd = QPushButton(tr("set.bambu_detect")); bd.clicked.connect(self._detect_bambu)
        br = QHBoxLayout(); br.addWidget(self.bambu); br.addWidget(bb); br.addWidget(bd)
        self.bambu_status = QLabel(""); self.bambu_status.setObjectName("hint")
        self.mode3mf = QComboBox(); self.mode3mf.addItem(tr("right.mode_multipart"), "multipart"); self.mode3mf.addItem(tr("right.mode_color"), "color")
        self.mode3mf.setCurrentIndex(self.mode3mf.findData(s.default_3mf_mode))
        f.addRow(tr("set.bambu_path"), br); f.addRow("", self.bambu_status); f.addRow(tr("set.default_3mf"), self.mode3mf)
        c.add_layout(f)
        c.add(QLabel(tr("set.ams_presets")))
        row = QHBoxLayout()
        preset = s.ams_presets.get(s.ams_active_preset) or DEFAULT_AMS
        self.ams_sw: list[SwatchButton] = []
        for i in range(4):
            col = QVBoxLayout()
            sw = SwatchButton(preset[i]["hex"] if i < len(preset) else "#FFFFFF")
            self.ams_sw.append(sw)
            lab = QLabel(f"AMS {i + 1}"); lab.setObjectName("hint")
            col.addWidget(sw); col.addWidget(lab); row.addLayout(col)
        row.addStretch()
        c.add_layout(row)
        lay.addWidget(c)

        row = QHBoxLayout()
        save = QPushButton(tr("set.save")); save.setObjectName("primary"); save.clicked.connect(self.save)
        row.addWidget(save); row.addStretch()
        lay.addLayout(row)
        lay.addStretch()
        self._detect_bambu(silent=True)

    # ------------------------------------------------------------
    def _browse_proj(self):
        d = QFileDialog.getExistingDirectory(self, tr("set.project_dir"), self.proj.text())
        if d:
            self.proj.setText(d)

    def _browse_bambu(self):
        f, _ = QFileDialog.getOpenFileName(self, tr("set.bambu_path"), "", "Executable (*.exe *)")
        if f:
            self.bambu.setText(f)
            self._detect_bambu(silent=True)

    def _detect_bambu(self, silent=False):
        p = bambu.resolve(self.bambu.text().strip())
        if p:
            self.bambu_status.setText("✓ " + str(p))
            if not self.bambu.text().strip() and not silent:
                self.bambu.setText(str(p))
        else:
            self.bambu_status.setText("✕ " + tr("err.bambu_not_found"))

    def _delete_key(self):
        self.ctx.settings.set_api_key("")
        self.ctx.settings.save()
        self.api_key.clear()
        self.api_key.setPlaceholderText(tr("set.key_ph"))

    def save(self, quiet: bool = False):
        s = self.ctx.settings
        old_lang, old_theme = s.language, s.theme
        s.language = self.lang.currentData(); s.theme = self.theme.currentData()
        s.project_dir = self.proj.text().strip()
        s.provider = self.provider.currentData(); s.endpoint = self.endpoint.text().strip()
        if self.api_key.text().strip():
            s.set_api_key(self.api_key.text())
            self.api_key.clear(); self.api_key.setPlaceholderText(tr("set.key_saved"))
        if self.rp_key.text().strip():
            s.set_runpod_api_key(self.rp_key.text())
            self.rp_key.clear(); self.rp_key.setPlaceholderText(tr("set.key_saved"))
        s.engine = self.engine.currentData(); s.timeout_s = self.timeout.value(); s.auto_stop_gpu = self.autostop.isChecked()
        s.runpod_pod_id = self.pod.text().strip()
        s.nozzle_mm, s.min_feature_mm, s.wall_mm = self.nozzle.value(), self.feature.value(), self.wall.value()
        s.default_height_mm, s.default_colors = self.height.value(), self.colors.value()
        s.bambu_path = self.bambu.text().strip()
        s.default_3mf_mode = self.mode3mf.currentData()
        names = [d["name"] for d in DEFAULT_AMS]
        s.ams_presets[s.ams_active_preset] = [{"hex": sw.hex(), "name": names[i] if i < len(names) else f"C{i + 1}"} for i, sw in enumerate(self.ams_sw)]
        s.save()
        self.ctx.reload()
        if (old_lang, old_theme) != (s.language, s.theme) and self.languageChanged:
            self.languageChanged()
        elif not quiet:
            QMessageBox.information(self, tr("ui.info"), tr("set.saved"))

    def _test(self):
        self.save(quiet=True)
        self.btn_test.setEnabled(False)
        self.test_result.setText(tr("set.testing"))
        self.ctx.set_cloud(CloudStatus.CONNECTING)
        prov = self.ctx.provider()

        def ok(info):
            self.btn_test.setEnabled(True)
            caps = info.capabilities
            txt = tr("set.test_ok", ms=f"{(info.latency_ms or 0):.0f}", engine=info.engine or "-",
                     img=tr("ui.yes") if (caps.image or caps.text_image) else tr("ui.no"),
                     life=tr("ui.yes") if caps.lifecycle else tr("ui.no"))
            self.test_result.setText("✓ " + txt)
            self.test_result.setStyleSheet("color: #37c78b;")
            self.ctx.set_cloud(CloudStatus.READY, "DEMO" if caps.is_demo else "")
            self.ctx.settingsChanged.emit()

        def bad(exc):
            self.btn_test.setEnabled(True)
            self.test_result.setText("✕ " + describe(exc))
            self.test_result.setStyleSheet("color: #ef5a5a;")
            self.ctx.set_cloud(CloudStatus.DISCONNECTED)
        run_async(prov.test_connection, ok, bad, self)
