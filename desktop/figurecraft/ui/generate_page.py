"""The Generate page: prompt/controls (left), 3D preview (centre), colours/parts/export (right)."""
from __future__ import annotations

import time
from pathlib import Path

from PySide6.QtCore import Qt, QThread, QTimer
from PySide6.QtWidgets import (QAbstractItemView, QButtonGroup, QCheckBox, QComboBox, QDoubleSpinBox, QFileDialog,
                               QFrame, QGridLayout, QHBoxLayout, QHeaderView, QLabel, QMenu, QMessageBox,
                               QPlainTextEdit, QPushButton, QRadioButton, QScrollArea, QSpinBox, QSplitter,
                               QTableWidget, QTableWidgetItem, QVBoxLayout, QWidget, QInputDialog)

from .. import i18n, paths
from ..core import bambu, color, export, preview
from ..core import mesh_inspect
from ..core.models import (STYLES, CloudStatus, GenerationRequest, PaletteEntry, PrintOptions, Stage, StageState)
from ..core.project_store import Project
from ..core.workspace import Workspace
from ..errors import Cancelled, FigureCraftError, describe
from ..logging_setup import get_logger
from .context import AppContext
from .dialogs import ErrorDialog
from .viewer_widget import ViewerWidget
from .widgets import Card, FlowLayout, StageList, SwatchButton, hline
from .workers import GenerationWorker, run_async, start_in_thread

log = get_logger("ui.generate")
tr = i18n.tr

CHIPS = [("cat", "고양이", "cat"), ("dog", "강아지", "dog"), ("rabbit", "토끼", "rabbit"), ("dragon", "드래곤", "dragon"),
         ("wizard", "마법사", "wizard"), ("xmas", "크리스마스", "Christmas"), ("halloween", "할로윈", "Halloween"),
         ("fantasy", "판타지", "fantasy"), ("cute_animal", "귀여운 동물", "cute animal"),
         ("game_char", "게임 캐릭터", "game character"), ("orig_char", "오리지널 캐릭터", "original character")]

EXAMPLE_KO = ("귀여운 고양이 마법사 피규어.\n큰 머리와 작은 몸의 치비 스타일.\n고양이는 앉아 있고 앞발이 보이게 해줘.\n"
              "모자는 보라색.\n몸은 아이보리.\n눈과 리본은 검정색.\n별 장식은 노란색.\n높이 120mm.\n최대 4색.\n평평한 바닥.\n"
              "얇은 부분은 최소 1.5mm 이상.\nFDM 3D 프린팅하기 쉽게 만들고\n서포트는 가능한 적게 해줘.")
EXAMPLE_EN = ("A cute cat wizard figurine.\nChibi style with a big head and small body.\nThe cat is sitting and its front paws are visible.\n"
              "Purple hat. Ivory body. Black eyes and ribbon. Yellow star decoration.\nHeight 120mm.\nMax 4 colors.\nFlat bottom.\n"
              "Thin parts at least 1.5mm.\nMake it easy to print on FDM and use as few supports as possible.")

REVISION_EXAMPLES = ["귀를 더 크게 만들어줘", "머리카락을 단순화해줘", "팔을 몸에 더 붙여줘", "지팡이를 더 두껍게 해줘",
                     "받침대를 추가해줘", "서포트가 적게 나오도록 포즈를 수정해줘"]

STATUS_COLORS = {CloudStatus.DISCONNECTED: "#8e96aa", CloudStatus.CONNECTING: "#f2b134", CloudStatus.READY: "#37c78b",
                 CloudStatus.GENERATING: "#7c5cff", CloudStatus.STOPPING: "#f2b134"}


def _mm(v: float) -> str:
    return f"{v:.1f}"


class GeneratePage(QWidget):
    def __init__(self, ctx: AppContext, parent=None):
        super().__init__(parent)
        self.ctx = ctx
        self.ws: Workspace | None = None
        self._thread: QThread | None = None
        self._worker: GenerationWorker | None = None
        self._busy = False
        self._generating = False
        self._dirty_exports = False
        self._last_request: GenerationRequest | None = None
        self._flat_plan = None
        self._view_mode = "original"
        self._t0 = 0.0
        self._build()
        self.ctx.settingsChanged.connect(self.on_settings_changed)
        self.on_settings_changed()

    # ===================================================================== UI
    def _build(self):
        root = QHBoxLayout(self)
        root.setContentsMargins(8, 8, 8, 8)
        split = QSplitter(Qt.Orientation.Horizontal)
        root.addWidget(split)
        split.addWidget(self._scroll(self._build_left(), 350))
        split.addWidget(self._build_center())
        split.addWidget(self._scroll(self._build_right(), 380))
        split.setStretchFactor(0, 0)
        split.setStretchFactor(1, 1)
        split.setStretchFactor(2, 0)
        split.setSizes([360, 700, 390])

    def _scroll(self, w: QWidget, width: int) -> QScrollArea:
        sa = QScrollArea()
        sa.setWidgetResizable(True)
        sa.setWidget(w)
        sa.setMinimumWidth(width - 20)
        sa.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        return sa

    # ------------------------------------------------------------- left
    def _build_left(self) -> QWidget:
        w = QWidget()
        lay = QVBoxLayout(w)
        lay.setContentsMargins(0, 0, 6, 0)
        lay.setSpacing(10)
        s = self.ctx.settings

        # mode
        card = Card(tr("gen.prompt_title"))
        row = QHBoxLayout()
        self.mode_text = QPushButton(tr("gen.mode_text")); self.mode_text.setObjectName("seg"); self.mode_text.setCheckable(True)
        self.mode_img = QPushButton(tr("gen.mode_image")); self.mode_img.setObjectName("seg"); self.mode_img.setCheckable(True)
        self.mode_text.setChecked(True)
        g = QButtonGroup(self); g.setExclusive(True); g.addButton(self.mode_text); g.addButton(self.mode_img)
        self.mode_text.clicked.connect(self._mode_changed); self.mode_img.clicked.connect(self._mode_changed)
        row.addWidget(self.mode_text); row.addWidget(self.mode_img)
        card.add_layout(row)

        self.prompt = QPlainTextEdit()
        self.prompt.setPlaceholderText(tr("gen.prompt_placeholder"))
        self.prompt.setMinimumHeight(150)
        card.add(self.prompt)
        ex = QPushButton(tr("gen.insert_example")); ex.clicked.connect(self._insert_example)
        card.add(ex)

        chips_host = QWidget()
        fl = FlowLayout(chips_host, spacing=6)
        for key, ko, en in CHIPS:
            b = QPushButton(ko if i18n.get_language() == "ko" else en)
            b.setObjectName("chip")
            b.clicked.connect(lambda _=False, t=(ko if i18n.get_language() == "ko" else en): self._add_chip(t))
            fl.addWidget(b)
        card.add(chips_host)

        # reference image
        self.img_row = QWidget()
        ir = QVBoxLayout(self.img_row); ir.setContentsMargins(0, 0, 0, 0)
        r2 = QHBoxLayout()
        self.img_btn = QPushButton(tr("gen.pick_image")); self.img_btn.clicked.connect(self._pick_image)
        self.img_clear = QPushButton("✕"); self.img_clear.setObjectName("iconbtn"); self.img_clear.clicked.connect(lambda: self._set_image(None))
        r2.addWidget(self.img_btn); r2.addWidget(self.img_clear)
        ir.addLayout(r2)
        self.img_label = QLabel(); self.img_label.setObjectName("hint"); self.img_label.setWordWrap(True)
        ir.addWidget(self.img_label)
        self.img_row.setVisible(False)
        self._image_path: str | None = None
        card.add(self.img_row)
        lay.addWidget(card)

        # style + size
        card = Card(tr("gen.style_size"))
        self.style_combo = QComboBox()
        for st in STYLES:
            self.style_combo.addItem(tr(f"style.{st}"), st)
        card.add(QLabel(tr("gen.style")))
        card.add(self.style_combo)
        h = QHBoxLayout()
        h.addWidget(QLabel(tr("gen.height")))
        self.height_combo = QComboBox()
        for v in (80, 100, 120, 150):
            self.height_combo.addItem(f"{v} mm", v)
        self.height_combo.addItem(tr("gen.custom"), -1)
        self.height_combo.currentIndexChanged.connect(self._height_changed)
        h.addWidget(self.height_combo)
        self.height_spin = QDoubleSpinBox(); self.height_spin.setRange(10, 400); self.height_spin.setSuffix(" mm"); self.height_spin.setDecimals(1)
        self.height_spin.setValue(s.default_height_mm); self.height_spin.setVisible(False)
        h.addWidget(self.height_spin)
        self.set_height(s.default_height_mm)
        card.add_layout(h)
        wd = QHBoxLayout()
        wd.addWidget(QLabel(tr("gen.width_auto"))); wd.addWidget(QLabel(tr("gen.depth_auto"))); wd.addStretch()
        card.add_layout(wd)
        lay.addWidget(card)

        # printability toggles
        card = Card(tr("gen.printability"))
        self.cb: dict[str, QCheckBox] = {}
        defs = [("flat_bottom", True), ("add_base", False), ("min_thickness_protection", True), ("support_friendly", True),
                ("strengthen_thin_parts", True), ("remove_floating", True), ("close_holes", True),
                ("make_watertight", True), ("simplify_tiny_details", False)]
        for key, on in defs:
            c = QCheckBox(tr(f"opt.{key}")); c.setChecked(on); c.setToolTip(tr(f"opt.{key}.tip"))
            self.cb[key] = c
            card.add(c)
        g3 = QGridLayout()
        self.sp_nozzle = QDoubleSpinBox(); self.sp_nozzle.setRange(0.1, 1.2); self.sp_nozzle.setSingleStep(0.05); self.sp_nozzle.setSuffix(" mm"); self.sp_nozzle.setValue(s.nozzle_mm)
        self.sp_feature = QDoubleSpinBox(); self.sp_feature.setRange(0.2, 5); self.sp_feature.setSingleStep(0.1); self.sp_feature.setSuffix(" mm"); self.sp_feature.setValue(s.min_feature_mm)
        self.sp_wall = QDoubleSpinBox(); self.sp_wall.setRange(0.4, 6); self.sp_wall.setSingleStep(0.1); self.sp_wall.setSuffix(" mm"); self.sp_wall.setValue(s.wall_mm)
        for r, (lbl, sp) in enumerate([(tr("gen.nozzle"), self.sp_nozzle), (tr("gen.min_feature"), self.sp_feature), (tr("gen.wall"), self.sp_wall)]):
            g3.addWidget(QLabel(lbl), r, 0); g3.addWidget(sp, r, 1)
        card.add_layout(g3)
        lay.addWidget(card)

        # colours
        card = Card(tr("gen.colors_title"))
        row = QHBoxLayout()
        self.color_group = QButtonGroup(self)
        self.color_radios: dict[int, QRadioButton] = {}
        for n in (1, 2, 3, 4):
            rb = QRadioButton(tr("gen.n_colors", n=n)); self.color_radios[n] = rb
            self.color_group.addButton(rb, n); row.addWidget(rb)
        self.color_radios[max(1, min(4, s.default_colors))].setChecked(True)
        card.add_layout(row)
        row = QHBoxLayout()
        self.pal_auto = QRadioButton(tr("gen.pal_auto")); self.pal_custom = QRadioButton(tr("gen.pal_custom")); self.pal_auto.setChecked(True)
        self.pal_auto.toggled.connect(self._pal_mode_changed)
        row.addWidget(self.pal_auto); row.addWidget(self.pal_custom)
        card.add_layout(row)
        self.custom_row = QWidget()
        cr = QHBoxLayout(self.custom_row); cr.setContentsMargins(0, 0, 0, 0)
        self.custom_sw: list[SwatchButton] = []
        preset = s.ams_presets.get(s.ams_active_preset) or []
        for i in range(4):
            sw = SwatchButton(preset[i]["hex"] if i < len(preset) else "#FFFFFF")
            self.custom_sw.append(sw)
            lab = QLabel(f"AMS {i + 1}"); lab.setObjectName("hint")
            col = QVBoxLayout(); col.addWidget(sw); col.addWidget(lab)
            cr.addLayout(col)
        cr.addStretch()
        self.custom_row.setVisible(False)
        card.add(self.custom_row)
        lay.addWidget(card)

        # base
        card = Card(tr("gen.base_title"))
        b = QHBoxLayout()
        self.base_combo = QComboBox()
        for k in ("none", "round", "oval", "square", "custom"):
            self.base_combo.addItem(tr(f"base.{k}"), k)
        self.base_combo.setCurrentIndex(1)
        b.addWidget(QLabel(tr("gen.base_shape"))); b.addWidget(self.base_combo)
        card.add_layout(b)
        g4 = QGridLayout()
        self.sp_base_t = QDoubleSpinBox(); self.sp_base_t.setRange(0.6, 20); self.sp_base_t.setValue(3.0); self.sp_base_t.setSuffix(" mm")
        self.sp_base_m = QDoubleSpinBox(); self.sp_base_m.setRange(0, 30); self.sp_base_m.setValue(3.0); self.sp_base_m.setSuffix(" mm")
        g4.addWidget(QLabel(tr("gen.base_thickness")), 0, 0); g4.addWidget(self.sp_base_t, 0, 1)
        g4.addWidget(QLabel(tr("gen.base_margin")), 1, 0); g4.addWidget(self.sp_base_m, 1, 1)
        card.add_layout(g4)
        lay.addWidget(card)

        # cloud
        card = Card(tr("gen.cloud_title"))
        self.prov_combo = QComboBox()
        for pid in ("mock", "generic_http", "runpod"):
            self.prov_combo.addItem(tr(f"prov.{pid}"), pid)
        self.prov_combo.currentIndexChanged.connect(self._prov_changed)
        self.engine_combo = QComboBox()
        for e in ("trellis", "hunyuan3d", "mock"):
            self.engine_combo.addItem(tr(f"engine.{e}"), e)
        self.engine_combo.currentIndexChanged.connect(self._engine_changed)
        card.add(QLabel(tr("set.provider"))); card.add(self.prov_combo)
        card.add(QLabel(tr("set.engine"))); card.add(self.engine_combo)
        self.cb_autostop = QCheckBox(tr("set.auto_stop")); self.cb_autostop.setChecked(s.auto_stop_gpu)
        self.cb_autostop.toggled.connect(self._autostop_changed)
        card.add(self.cb_autostop)
        self.cloud_note = QLabel(); self.cloud_note.setObjectName("hint"); self.cloud_note.setWordWrap(True)
        card.add(self.cloud_note)
        lay.addWidget(card)

        self.btn_generate = QPushButton(tr("gen.generate")); self.btn_generate.setObjectName("primary")
        self.btn_generate.clicked.connect(self.start_generation)
        self.btn_stop = QPushButton(tr("gen.stop")); self.btn_stop.setObjectName("danger"); self.btn_stop.setVisible(False)
        self.btn_stop.clicked.connect(self.stop_generation)
        lay.addWidget(self.btn_generate)
        lay.addWidget(self.btn_stop)
        lay.addStretch()
        return w

    # ----------------------------------------------------------- centre
    def _build_center(self) -> QWidget:
        w = QWidget()
        lay = QVBoxLayout(w)
        lay.setContentsMargins(0, 0, 0, 0)
        lay.setSpacing(6)
        # toolbar 1: views
        t1 = QHBoxLayout()
        for key, name in (("front", "view.front"), ("back", "view.back"), ("left", "view.left"), ("right", "view.right"),
                          ("top", "view.top"), ("bottom", "view.bottom")):
            b = QPushButton(tr(name)); b.setObjectName("tool"); b.clicked.connect(lambda _=False, k=key: self.viewer.set_view(k)); t1.addWidget(b)
        b = QPushButton(tr("view.reset")); b.setObjectName("tool"); b.clicked.connect(lambda: self.viewer.reset_view()); t1.addWidget(b)
        t1.addStretch()
        self.demo_badge = QLabel("DEMO"); self.demo_badge.setObjectName("demoBadge"); self.demo_badge.setVisible(False)
        self.demo_badge.setToolTip(tr("msg.demo_badge_tip"))
        t1.addWidget(self.demo_badge)
        lay.addLayout(t1)
        # toolbar 2: display modes
        t2 = QHBoxLayout()
        self.mode_btns: dict[str, QPushButton] = {}
        grp = QButtonGroup(self); grp.setExclusive(True)
        for key, name in (("original", "view.original"), ("print", "view.print"), ("parts", "view.parts")):
            b = QPushButton(tr(name)); b.setObjectName("tool"); b.setCheckable(True)
            b.clicked.connect(lambda _=False, k=key: self._set_view_mode(k))
            grp.addButton(b); self.mode_btns[key] = b; t2.addWidget(b)
        self.mode_btns["original"].setChecked(True)
        self.btn_wire = QPushButton(tr("view.wire")); self.btn_wire.setObjectName("tool"); self.btn_wire.setCheckable(True)
        self.btn_wire.toggled.connect(lambda on: self.viewer.set_wireframe(on)); t2.addWidget(self.btn_wire)
        self.btn_bbox = QPushButton(tr("view.bbox")); self.btn_bbox.setObjectName("tool"); self.btn_bbox.setCheckable(True)
        self.btn_bbox.toggled.connect(lambda on: self.viewer.set_bbox(on)); t2.addWidget(self.btn_bbox)
        t2.addStretch()
        hint = QLabel(tr("view.mouse_hint")); hint.setObjectName("hint"); t2.addWidget(hint)
        lay.addLayout(t2)

        self.viewer = ViewerWidget()
        self.viewer.setMinimumHeight(360)
        self.viewer.regionPicked.connect(self._region_picked)
        self.viewer.loadFailed.connect(lambda what: self.info_label.setText(tr("msg.webgl_failed")))
        lay.addWidget(self.viewer, 1)

        # info bar
        self.info_label = QLabel(tr("gen.no_model")); self.info_label.setObjectName("muted"); self.info_label.setWordWrap(True)
        lay.addWidget(self.info_label)

        card = Card()
        self.stages = StageList()
        card.add(self.stages)
        self.status_line = QLabel(""); self.status_line.setObjectName("muted"); self.status_line.setWordWrap(True)
        card.add(self.status_line)
        self.timing_line = QLabel(""); self.timing_line.setObjectName("muted"); self.timing_line.setWordWrap(True)
        card.add(self.timing_line)
        lay.addWidget(card)
        return w

    # ------------------------------------------------------------ right
    def _build_right(self) -> QWidget:
        w = QWidget()
        lay = QVBoxLayout(w)
        lay.setContentsMargins(6, 0, 0, 0)
        lay.setSpacing(10)

        # colours and parts
        card = Card(tr("right.colors_parts"))
        seg = QHBoxLayout(); seg.setSpacing(0)
        self.k_group = QButtonGroup(self); self.k_group.setExclusive(True)
        self.k_btns: dict[int, QPushButton] = {}
        b = QPushButton(tr("right.k_original")); b.setObjectName("seg"); b.setCheckable(True); b.setChecked(True)
        b.clicked.connect(lambda: self._preview_k(0)); self.k_group.addButton(b); self.k_btns[0] = b; seg.addWidget(b)
        for k in (1, 2, 3, 4):
            b = QPushButton(tr("right.k_n", n=k)); b.setObjectName("seg"); b.setCheckable(True)
            b.clicked.connect(lambda _=False, kk=k: self._preview_k(kk)); self.k_group.addButton(b); self.k_btns[k] = b; seg.addWidget(b)
        card.add_layout(seg)
        self.palette_box = QVBoxLayout(); self.palette_box.setSpacing(4)
        card.add_layout(self.palette_box)
        row = QHBoxLayout()
        self.btn_add_color = QPushButton(tr("right.add_color")); self.btn_add_color.clicked.connect(self._add_color)
        self.btn_auto_pal = QPushButton(tr("right.auto_palette")); self.btn_auto_pal.clicked.connect(self._auto_palette)
        row.addWidget(self.btn_add_color); row.addWidget(self.btn_auto_pal)
        card.add_layout(row)
        card.add(hline())
        lab = QLabel(tr("right.parts_hint")); lab.setObjectName("hint"); lab.setWordWrap(True); card.add(lab)
        self.parts_table = QTableWidget(0, 3)
        self.parts_table.setHorizontalHeaderLabels([tr("right.part"), tr("right.area"), tr("right.ams")])
        self.parts_table.verticalHeader().setVisible(False)
        self.parts_table.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows)
        self.parts_table.setEditTriggers(QAbstractItemView.EditTrigger.NoEditTriggers)
        hh = self.parts_table.horizontalHeader()
        hh.setSectionResizeMode(0, QHeaderView.ResizeMode.ResizeToContents)
        hh.setSectionResizeMode(1, QHeaderView.ResizeMode.ResizeToContents)
        hh.setSectionResizeMode(2, QHeaderView.ResizeMode.Stretch)
        self.parts_table.setMinimumHeight(170); self.parts_table.setMaximumHeight(240)
        self.parts_table.itemSelectionChanged.connect(self._part_selected)
        card.add(self.parts_table)
        lay.addWidget(card)

        # validation
        card = Card(tr("right.validation"))
        top = QHBoxLayout()
        self.pill = QLabel("—"); self.pill.setObjectName("pillCHECK")
        top.addWidget(self.pill); top.addStretch()
        card.add_layout(top)
        self.issue_label = QLabel(tr("right.no_report")); self.issue_label.setWordWrap(True); self.issue_label.setObjectName("muted")
        card.add(self.issue_label)
        d = QLabel(tr("msg.final_check")); d.setObjectName("hint"); d.setWordWrap(True); card.add(d)
        lay.addWidget(card)

        # repair / print tools
        card = Card(tr("right.tools"))
        self.btn_repair = QPushButton(tr("right.repair")); self.btn_repair.clicked.connect(self._do_repair)
        self.btn_revert = QPushButton(tr("right.revert")); self.btn_revert.clicked.connect(self._do_revert)
        row = QHBoxLayout(); row.addWidget(self.btn_repair); row.addWidget(self.btn_revert); card.add_layout(row)
        self.btn_flat_prev = QPushButton(tr("right.flat_preview")); self.btn_flat_prev.clicked.connect(self._flat_preview)
        self.btn_flat_apply = QPushButton(tr("right.flat_apply")); self.btn_flat_apply.clicked.connect(self._flat_apply); self.btn_flat_apply.setEnabled(False)
        row = QHBoxLayout(); row.addWidget(self.btn_flat_prev); row.addWidget(self.btn_flat_apply); card.add_layout(row)
        self.flat_note = QLabel(""); self.flat_note.setObjectName("hint"); self.flat_note.setWordWrap(True); card.add(self.flat_note)
        self.btn_base = QPushButton(tr("right.add_base")); self.btn_base.clicked.connect(self._do_base)
        self.btn_thin = QPushButton(tr("right.strengthen")); self.btn_thin.clicked.connect(self._do_strengthen)
        row = QHBoxLayout(); row.addWidget(self.btn_base); row.addWidget(self.btn_thin); card.add_layout(row)
        self.repair_note = QLabel(""); self.repair_note.setObjectName("hint"); self.repair_note.setWordWrap(True); card.add(self.repair_note)
        lay.addWidget(card)

        # revision / versions
        card = Card(tr("right.revision"))
        self.rev_text = QPlainTextEdit(); self.rev_text.setMaximumHeight(70); self.rev_text.setPlaceholderText(tr("right.revision_ph"))
        card.add(self.rev_text)
        ex = QComboBox(); ex.addItem(tr("right.revision_examples"), "")
        for e in REVISION_EXAMPLES:
            ex.addItem(e, e)
        ex.currentIndexChanged.connect(lambda i, c=ex: (self.rev_text.setPlainText(c.itemData(i)) if c.itemData(i) else None))
        card.add(ex)
        self.btn_revise = QPushButton(tr("right.send_revision")); self.btn_revise.clicked.connect(self._send_revision)
        card.add(self.btn_revise)
        row = QHBoxLayout()
        self.version_combo = QComboBox(); row.addWidget(self.version_combo, 1)
        self.btn_open_version = QPushButton(tr("right.open_version")); self.btn_open_version.clicked.connect(self._open_version); row.addWidget(self.btn_open_version)
        card.add_layout(row)
        lay.addWidget(card)

        # export
        card = Card(tr("right.export"))
        row = QHBoxLayout()
        self.rb_multi = QRadioButton(tr("right.mode_multipart")); self.rb_color = QRadioButton(tr("right.mode_color"))
        (self.rb_multi if self.ctx.settings.default_3mf_mode == "multipart" else self.rb_color).setChecked(True)
        self.rb_multi.setToolTip(tr("right.mode_multipart.tip")); self.rb_color.setToolTip(tr("right.mode_color.tip"))
        row.addWidget(self.rb_multi); row.addWidget(self.rb_color)
        card.add_layout(row)
        g = QGridLayout()
        self.btn_3mf = QPushButton(tr("right.save_3mf")); self.btn_3mf.setObjectName("primary"); self.btn_3mf.clicked.connect(self._save_3mf)
        self.btn_stl = QPushButton("STL"); self.btn_stl.clicked.connect(self._save_stl)
        self.btn_stl_c = QPushButton(tr("right.stl_by_color")); self.btn_stl_c.clicked.connect(self._save_stl_by_color)
        self.btn_glb = QPushButton("GLB"); self.btn_glb.clicked.connect(self._save_glb)
        self.btn_obj = QPushButton("OBJ"); self.btn_obj.clicked.connect(self._save_obj)
        g.addWidget(self.btn_3mf, 0, 0, 1, 2); g.addWidget(self.btn_stl, 1, 0); g.addWidget(self.btn_stl_c, 1, 1)
        g.addWidget(self.btn_glb, 2, 0); g.addWidget(self.btn_obj, 2, 1)
        card.add_layout(g)
        self.btn_bambu = QPushButton(tr("right.open_bambu")); self.btn_bambu.clicked.connect(self._open_bambu)
        card.add(self.btn_bambu)
        self.export_note = QLabel(""); self.export_note.setObjectName("hint"); self.export_note.setWordWrap(True); card.add(self.export_note)
        lay.addWidget(card)
        lay.addStretch()
        self._set_model_controls(False)
        return w

    # ================================================================ state
    def _set_model_controls(self, on: bool):
        for wdg in (self.btn_repair, self.btn_revert, self.btn_flat_prev, self.btn_base, self.btn_thin, self.btn_revise,
                    self.btn_3mf, self.btn_stl, self.btn_stl_c, self.btn_glb, self.btn_obj, self.btn_bambu,
                    self.btn_add_color, self.btn_auto_pal, self.btn_open_version, self.version_combo):
            wdg.setEnabled(on and not self._busy)
        for b in self.k_btns.values():
            b.setEnabled(on and not self._busy)
        if not on:
            self.btn_flat_apply.setEnabled(False)

    def set_busy(self, busy: bool):
        self._busy = busy
        self._set_model_controls(self.ws is not None)
        self.btn_generate.setEnabled(not busy and not self._generating)

    def on_settings_changed(self):
        s = self.ctx.settings
        for combo, val in ((self.prov_combo, s.provider), (self.engine_combo, s.engine)):
            combo.blockSignals(True)
            i = combo.findData(val)
            combo.setCurrentIndex(max(0, i))
            combo.blockSignals(False)
        self.cb_autostop.blockSignals(True); self.cb_autostop.setChecked(s.auto_stop_gpu); self.cb_autostop.blockSignals(False)
        self._update_cloud_note()
        self._update_image_availability()

    def _update_cloud_note(self):
        p = self.ctx.provider()
        if self.ctx.settings.provider == "mock":
            self.cloud_note.setText(tr("msg.demo_note"))
        elif p.supports_lifecycle:
            self.cloud_note.setText(tr("msg.lifecycle_auto"))
        else:
            self.cloud_note.setText(tr("msg.lifecycle_manual"))

    def _update_image_availability(self):
        caps = self.ctx.provider().capabilities()
        ok = caps.image or caps.text_image
        self.mode_img.setEnabled(ok)
        self.mode_img.setToolTip("" if ok else tr("err.image_unsupported"))
        if not ok and self.mode_img.isChecked():
            self.mode_text.setChecked(True)
            self._mode_changed()
        self.img_label.setText(tr("msg.image_ok_hint") if ok else tr("err.image_unsupported"))

    # ---- simple handlers
    def _mode_changed(self):
        self.img_row.setVisible(self.mode_img.isChecked())

    def _insert_example(self):
        self.prompt.setPlainText(EXAMPLE_KO if i18n.get_language() == "ko" else EXAMPLE_EN)

    def _add_chip(self, text: str):
        cur = self.prompt.toPlainText().rstrip()
        self.prompt.setPlainText(f"{cur}, {text}" if cur else text)

    def _pick_image(self):
        f, _ = QFileDialog.getOpenFileName(self, tr("gen.pick_image"), str(Path.home()), "Images (*.png *.jpg *.jpeg *.webp)")
        if f:
            self._set_image(f)

    def _set_image(self, path: str | None):
        self._image_path = path
        self.img_label.setText(Path(path).name if path else (tr("msg.image_ok_hint")))

    def _height_changed(self):
        self.height_spin.setVisible(self.height_combo.currentData() == -1)

    def _pal_mode_changed(self):
        self.custom_row.setVisible(self.pal_custom.isChecked())

    def _prov_changed(self):
        self.ctx.settings.provider = self.prov_combo.currentData()
        self.ctx.settings.save()
        self.ctx.reload()

    def _engine_changed(self):
        self.ctx.settings.engine = self.engine_combo.currentData()
        self.ctx.settings.save()

    def _autostop_changed(self, on: bool):
        self.ctx.settings.auto_stop_gpu = on
        self.ctx.settings.save()

    def current_height(self) -> float:
        v = self.height_combo.currentData()
        return float(self.height_spin.value()) if v == -1 else float(v)

    def set_height(self, h: float):
        i = self.height_combo.findData(int(h)) if float(h).is_integer() else -1
        if i >= 0:
            self.height_combo.setCurrentIndex(i)
        else:
            self.height_combo.setCurrentIndex(self.height_combo.findData(-1))
            self.height_spin.setValue(h)

    def collect_request(self) -> GenerationRequest:
        o = PrintOptions(
            height_mm=self.current_height(), nozzle_mm=self.sp_nozzle.value(), min_feature_mm=self.sp_feature.value(),
            wall_mm=self.sp_wall.value(), base_shape=self.base_combo.currentData(),
            base_thickness_mm=self.sp_base_t.value(), base_margin_mm=self.sp_base_m.value(),
            **{k: c.isChecked() for k, c in self.cb.items()})
        if o.base_shape == "none":
            o.add_base = False
        mode = "image_text" if self.mode_img.isChecked() else "text"
        return GenerationRequest(
            prompt=self.prompt.toPlainText(), style=self.style_combo.currentData(), mode=mode,
            reference_image=self._image_path if mode == "image_text" else None, engine=self.ctx.settings.engine,
            print=o, max_colors=self.color_group.checkedId(),
            palette_mode="custom" if self.pal_custom.isChecked() else "auto",
            custom_palette=[s.hex() for s in self.custom_sw][: self.color_group.checkedId()])

    # ============================================================ generation
    def start_generation(self, project: Project | None = None, request: GenerationRequest | None = None):
        if self._generating or self._busy:
            return
        request = request or self.collect_request()
        if not request.prompt.strip():
            QMessageBox.information(self, tr("ui.info"), tr("msg.empty_prompt"))
            return
        if request.mode == "image_text" and not request.reference_image:
            QMessageBox.information(self, tr("ui.info"), tr("msg.need_image"))
            return
        self._last_request = request
        self._generating = True
        self.btn_generate.setVisible(False)
        self.btn_stop.setVisible(True)
        self.btn_stop.setEnabled(True)
        self._set_model_controls(self.ws is not None)
        self.stages.reset()
        self.status_line.setText("")
        self.timing_line.setText("")
        self._t0 = time.time()
        self._started_text = time.strftime("%H:%M:%S")
        self.timing_line.setText(tr("gen.started_at", t=self._started_text))
        self._worker = GenerationWorker(request, self.ctx.provider(), self.ctx.settings, self.ctx.store, project)
        self._worker.stage.connect(self._on_stage)
        self._worker.cloud.connect(self._on_cloud)
        self._worker.info.connect(self._on_info)
        self._worker.hints.connect(self._on_hints)
        self._worker.gpuWarning.connect(self._on_gpu_warning)
        self._worker.finished.connect(self._on_finished)
        self._worker.failed.connect(self._on_failed)
        self._thread = start_in_thread(self._worker, self)
        log.info("generation requested (provider=%s engine=%s mode=%s)", self.ctx.settings.provider, request.engine, request.mode)

    def stop_generation(self):
        if self._worker:
            self._worker.cancel()
            self.btn_stop.setEnabled(False)
            self.status_line.setText(tr("gen.stopping"))

    def _on_info(self, text: str):
        self.status_line.setText(text)

    def _on_stage(self, stage: Stage, state: StageState, detail: str):
        self.stages.set_state(stage, state, detail)

    def _on_cloud(self, status: CloudStatus):
        txt = "DEMO" if self.ctx.settings.provider == "mock" else ""
        self.ctx.set_cloud(status, txt)
        if status in (CloudStatus.READY, CloudStatus.GENERATING) and not self.ctx.provider().capabilities().is_demo:
            self.ctx.set_gpu_risk(True)
        if status == CloudStatus.DISCONNECTED:
            self.ctx.set_gpu_risk(False)

    def _on_hints(self, h):
        if h.height_mm:
            self.set_height(h.height_mm)
        if h.max_colors and h.max_colors in self.color_radios:
            self.color_radios[h.max_colors].setChecked(True)
        if h.min_wall_mm:
            self.sp_wall.setValue(h.min_wall_mm)
        if h.flat_bottom:
            self.cb["flat_bottom"].setChecked(True)

    def _on_gpu_warning(self, text: str):
        self.ctx.set_gpu_risk(True)
        QMessageBox.warning(self, tr("ui.gpu_warning"), text)

    def _generation_over(self):
        self._generating = False
        self.btn_stop.setVisible(False)
        self.btn_generate.setVisible(True)
        self.btn_generate.setEnabled(True)
        self._worker = None

    def _on_finished(self, res):
        self._generation_over()
        t_end = time.strftime("%H:%M:%S")
        gpu = res.gpu_seconds_est
        self.timing_line.setText(tr("gen.timing", s=self._started_text, e=t_end, g=(f"{gpu:.0f}s" if gpu else "0s"),
                                    src=(tr("gen.gpu_none") if self.ctx.settings.provider == "mock" else "")))
        if res.gpu_stop == "ok":
            self.ctx.set_gpu_risk(False)
        elif res.gpu_stop in ("manual", "failed"):
            self.ctx.set_gpu_risk(True)
        else:
            self.ctx.set_gpu_risk(False)
        self.ws = res.workspace
        self._dirty_exports = False
        self._show_workspace()
        self.ctx.projectsChanged.emit()
        self.status_line.setText(tr("gen.done_msg"))

    def _on_failed(self, exc):
        self._generation_over()
        self.ctx.set_cloud(CloudStatus.READY if self.ctx.settings.provider == "mock" else CloudStatus.DISCONNECTED,
                           "DEMO" if self.ctx.settings.provider == "mock" else "")
        if isinstance(exc, Cancelled):
            self.status_line.setText(tr("err.cancelled"))
            return
        self.status_line.setText(describe(exc))
        self.show_error(exc, retry=lambda: self.start_generation(request=self._last_request))

    def show_error(self, exc: BaseException, retry=None):
        dlg = ErrorDialog(self, exc, allow_retry=retry is not None)
        dlg.exec()
        if dlg.choice == ErrorDialog.RETRY and retry:
            QTimer.singleShot(0, retry)
        elif dlg.choice == ErrorDialog.SETTINGS:
            self.ctx.openSettingsRequested.emit()

    # ============================================================ project
    def open_project(self, project: Project):
        if self._generating:
            return
        self.set_busy(True)
        self.status_line.setText(tr("msg.loading"))

        def job():
            return Workspace.open(project, self.ctx.settings)

        def done(ws):
            self.ws = ws
            self._dirty_exports = False
            self.set_busy(False)
            self._restore_controls()
            self._show_workspace()
            self.stages.reset()
            for s in Stage:
                self.stages.set_state(s, StageState.DONE, "")
            self.status_line.setText("")
            t = project.meta.timing
            if t:
                self.timing_line.setText(tr("gen.timing", s=t.get("started", ""), e=t.get("finished", ""),
                                            g=f"{t.get('gpu_seconds', 0) or 0:.0f}s", src=""))
        self.run_task(job, done, quiet=True)

    def _restore_controls(self):
        ws = self.ws
        if ws is None:
            return
        req = ws.request
        self.prompt.setPlainText(req.prompt)
        i = self.style_combo.findData(req.style)
        if i >= 0:
            self.style_combo.setCurrentIndex(i)
        self.set_height(req.print.height_mm)
        if req.max_colors in self.color_radios:
            self.color_radios[req.max_colors].setChecked(True)
        for k, c in self.cb.items():
            c.setChecked(bool(getattr(req.print, k, c.isChecked())))

    def new_project(self):
        self.ws = None
        self.viewer.clear()
        self.prompt.clear()
        self.stages.reset()
        self.info_label.setText(tr("gen.no_model"))
        self.demo_badge.setVisible(False)
        self._set_model_controls(False)
        self._clear_palette_ui()
        self.parts_table.setRowCount(0)
        self.pill.setText("—")
        self.issue_label.setText(tr("right.no_report"))
        self.timing_line.setText("")

    # ---- run non-generation tasks on a worker thread
    def run_task(self, fn, on_done, quiet=False, busy=True):
        if busy:
            self.set_busy(True)

        def ok(res):
            if busy:
                self.set_busy(False)
            try:
                on_done(res)
            except Exception as exc:     # noqa: BLE001
                log.exception("task completion handler failed")
                self.show_error(exc)

        def bad(exc):
            if busy:
                self.set_busy(False)
            self.status_line.setText(describe(exc))
            self.show_error(exc)
        run_async(fn, ok, bad, self)

    # ============================================================== display
    def _show_workspace(self):
        ws = self.ws
        if ws is None:
            return
        self.demo_badge.setVisible(ws.is_demo)
        self._refresh_viewer()
        self._refresh_info()
        self._refresh_palette()
        self._refresh_parts()
        self._refresh_validation()
        self._refresh_versions()
        self._set_model_controls(True)
        self.k_btns[0 if self._view_mode == "original" else len(ws.palette)].setChecked(True)
        self.export_note.setText(tr("msg.demo_export_note") if ws.is_demo else "")

    def _refresh_viewer(self):
        ws = self.ws
        if ws is None:
            return
        payload = preview.build_payload(ws.mesh, ws.orig_colors, ws.print_colors, ws.labels, ws.regions(), None)
        self.viewer.show_payload(payload)
        self.viewer.set_mode(self._view_mode)
        for i, p in enumerate(ws.palette):
            if not p.visible:
                self.viewer.set_label_visible(i, False)

    def _refresh_info(self):
        ws = self.ws
        r = ws.report or ws.inspect(with_thin=False)
        x, y, z = r.extents_mm
        vol = f"{r.volume_mm3 / 1000:.1f} cm³" if r.volume_mm3 else "—"
        wt = tr("info.watertight_yes") if r.watertight else tr("info.watertight_no")
        self.info_label.setText(tr("info.line", x=_mm(x), y=_mm(y), z=_mm(z), v=f"{r.vertices:,}", t=f"{r.triangles:,}",
                                   wt=wt, c=r.components, vol=vol))

    def _refresh_validation(self):
        r = self.ws.report
        self.pill.setText(tr(f"status.{r.status}"))
        self.pill.setObjectName(f"pill{r.status}")
        self.pill.setStyleSheet("")
        self.pill.style().unpolish(self.pill); self.pill.style().polish(self.pill)
        if not r.issues:
            txt = tr("issue.none")
        else:
            lines = []
            for it in r.issues:
                mark = {"error": "⛔", "warn": "⚠", "info": "ℹ"}.get(it.severity, "•")
                lines.append(f"{mark} {tr('issue.' + it.code, **it.params)}")
            txt = "\n".join(lines)
        if r.thin_ratio is not None:
            txt += "\n" + tr("issue.thin_info", pct=f"{r.thin_ratio * 100:.1f}")
        self.issue_label.setText(txt)

    def _refresh_versions(self):
        self.version_combo.clear()
        for v in self.ws.project.meta.versions:
            note = v.get("note", "")
            self.version_combo.addItem(f"v{v['n']}  {v['created'][5:16].replace('T', ' ')}  {note[:28]}", v["n"])
        i = self.version_combo.findData(self.ws.project.meta.active_version)
        if i >= 0:
            self.version_combo.setCurrentIndex(i)

    def _clear_palette_ui(self):
        while self.palette_box.count():
            it = self.palette_box.takeAt(0)
            if it.widget():
                it.widget().deleteLater()
            elif it.layout():
                while it.layout().count():
                    w = it.layout().takeAt(0).widget()
                    if w:
                        w.deleteLater()

    def _refresh_palette(self):
        self._clear_palette_ui()
        ws = self.ws
        for i, p in enumerate(ws.palette):
            row = QWidget()
            h = QHBoxLayout(row); h.setContentsMargins(0, 0, 0, 0); h.setSpacing(4)
            lab = QLabel(f"AMS {i + 1}"); lab.setObjectName("hint"); lab.setMinimumWidth(42)
            sw = SwatchButton(p.hex)
            sw.colorChanged.connect(lambda hx, idx=i: self._palette_hex(idx, hx))
            name = QLabel(p.name); name.setMinimumWidth(70)
            ren = QPushButton("✎"); ren.setObjectName("iconbtn"); ren.setToolTip(tr("right.rename"))
            ren.clicked.connect(lambda _=False, idx=i: self._rename_color(idx))
            vis = QCheckBox(); vis.setChecked(p.visible); vis.setToolTip(tr("right.visible"))
            vis.toggled.connect(lambda on, idx=i: self._toggle_visible(idx, on))
            lock = QPushButton("🔒" if p.locked else "🔓"); lock.setObjectName("iconbtn"); lock.setCheckable(True); lock.setChecked(p.locked)
            lock.setToolTip(tr("right.lock"))
            lock.toggled.connect(lambda on, idx=i: self._toggle_lock(idx, on))
            mg = QPushButton("⇄"); mg.setObjectName("iconbtn"); mg.setToolTip(tr("right.merge"))
            mg.clicked.connect(lambda _=False, idx=i, b=mg: self._merge_menu(idx, b))
            dl = QPushButton("✕"); dl.setObjectName("iconbtn"); dl.setToolTip(tr("right.delete"))
            dl.clicked.connect(lambda _=False, idx=i: self._delete_color(idx))
            for wd in (lab, sw, name, ren, vis, lock, mg, dl):
                h.addWidget(wd)
            h.addStretch()
            self.palette_box.addWidget(row)
        self.btn_add_color.setEnabled(len(ws.palette) < 4 and not self._busy)

    def _refresh_parts(self):
        ws = self.ws
        parts = ws.parts_summary()
        self.parts_table.blockSignals(True)
        self.parts_table.setRowCount(len(parts))
        for r, pt in enumerate(parts):
            pal = ws.palette[min(pt["label"], len(ws.palette) - 1)]
            it = QTableWidgetItem(f"  #{pt['region'] + 1}")
            from PySide6.QtGui import QColor
            it.setBackground(QColor(pal.hex)); it.setForeground(QColor("#000000" if sum(pal.rgb) > 380 else "#ffffff"))
            it.setData(Qt.ItemDataRole.UserRole, pt["region"])
            self.parts_table.setItem(r, 0, it)
            self.parts_table.setItem(r, 1, QTableWidgetItem(f"{pt['area_mm2']:.0f} mm²"))
            combo = QComboBox()
            for i, p in enumerate(ws.palette):
                combo.addItem(f"{i + 1}·{p.name}", i)
            combo.setCurrentIndex(pt["label"])
            combo.currentIndexChanged.connect(lambda _=0, c=combo, reg=pt["region"]: self._region_to_color(reg, c.currentData()))
            self.parts_table.setCellWidget(r, 2, combo)
        self.parts_table.blockSignals(False)

    # ================================================================ events
    def _set_view_mode(self, mode: str):
        self._view_mode = mode
        self.viewer.set_mode(mode)

    def _preview_k(self, k: int):
        if self.ws is None:
            return
        if k == 0:
            self.mode_btns["original"].setChecked(True)
            self._set_view_mode("original")
            return

        def job():
            self.ws.reduce(k, "auto")
            self.ws.save(rebuild_exports=False)
            return True

        def done(_):
            self._dirty_exports = True
            self.mode_btns["print"].setChecked(True)
            self._view_mode = "print"
            self.k_btns[len(self.ws.palette)].setChecked(True)
            self._refresh_viewer(); self._refresh_palette(); self._refresh_parts()
        self.run_task(job, done)

    def _auto_palette(self):
        if self.ws:
            self._preview_k(max(1, min(4, len(self.ws.palette) or 4)))

    def _commit_color_edit(self, refresh_parts=True):
        self.ws.save(rebuild_exports=False)
        self._dirty_exports = True
        self._refresh_viewer(); self._refresh_palette()
        if refresh_parts:
            self._refresh_parts()

    def _palette_hex(self, i: int, hx: str):
        self.ws.set_palette_hex(i, hx)
        self._commit_color_edit()

    def _rename_color(self, i: int):
        name, ok = QInputDialog.getText(self, tr("right.rename"), tr("right.rename_prompt"), text=self.ws.palette[i].name)
        if ok and name.strip():
            self.ws.rename_color(i, name)
            self._commit_color_edit()

    def _toggle_visible(self, i: int, on: bool):
        self.ws.palette[i].visible = on
        self.viewer.set_label_visible(i, on)

    def _toggle_lock(self, i: int, on: bool):
        self.ws.palette[i].locked = on
        self._refresh_palette()

    def _merge_menu(self, i: int, btn: QPushButton):
        if len(self.ws.palette) < 2:
            return
        m = QMenu(self)
        for j, p in enumerate(self.ws.palette):
            if j != i:
                act = m.addAction(tr("right.merge_into", n=j + 1, name=p.name))
                act.triggered.connect(lambda _=False, jj=j: self._merge(i, jj))
        m.exec(btn.mapToGlobal(btn.rect().bottomLeft()))

    def _merge(self, src: int, dst: int):
        self.ws.merge_colors(src, dst)
        self._commit_color_edit()

    def _delete_color(self, i: int):
        if len(self.ws.palette) <= 1:
            return
        self.ws.delete_color(i)
        self._commit_color_edit()

    def _add_color(self):
        if self.ws.add_color("#FFFFFF"):
            self.ws.save(rebuild_exports=False)
            self._refresh_palette(); self._refresh_parts()

    def _region_to_color(self, region: int, label: int):
        self.ws.set_region_color(region, label)
        self._commit_color_edit(refresh_parts=False)
        QTimer.singleShot(0, self._refresh_parts)

    def _part_selected(self):
        rows = self.parts_table.selectionModel().selectedRows()
        if rows and self.ws:
            reg = self.parts_table.item(rows[0].row(), 0).data(Qt.ItemDataRole.UserRole)
            self.viewer.select_region(int(reg))

    def _region_picked(self, region: int):
        for r in range(self.parts_table.rowCount()):
            if self.parts_table.item(r, 0).data(Qt.ItemDataRole.UserRole) == region:
                self.parts_table.selectRow(r)
                return
        self.viewer.select_region(region)

    # ============================================================= mesh tools
    def _after_geometry(self, msg: str = ""):
        self.ws.save(rebuild_exports=False)
        self._dirty_exports = True
        self._refresh_viewer(); self._refresh_info(); self._refresh_palette(); self._refresh_parts(); self._refresh_validation()
        if msg:
            self.repair_note.setText(msg)

    def _do_repair(self):
        def job():
            acts = self.ws.repair()
            self.ws.inspect(with_thin=True)
            return acts

        def done(acts):
            if acts:
                txt = "; ".join(tr("repair." + a.code, **a.params) for a in acts)
            else:
                txt = tr("repair.nothing")
            self._after_geometry(tr("right.repair_done") + " " + txt)
        self.run_task(job, done)

    def _do_revert(self):
        if QMessageBox.question(self, tr("right.revert"), tr("msg.confirm_revert")) != QMessageBox.StandardButton.Yes:
            return

        def job():
            self.ws.revert_to_original()
            self.ws.inspect(with_thin=True)

        self.run_task(job, lambda _: self._after_geometry(tr("right.reverted")))

    def _flat_preview(self):
        def job():
            return self.ws.plan_flat_bottom()

        def done(plan):
            self._flat_plan = plan
            if plan.note == "already_flat":
                self.flat_note.setText(tr("flat.already", a=f"{plan.contact_before:.0f}"))
                self.btn_flat_apply.setEnabled(False)
                self.viewer.set_cut_plane(None)
            elif plan.needed:
                self.flat_note.setText(tr("flat.plan", z=f"{plan.cut_z:.2f}", a=f"{plan.contact_after:.0f}",
                                          v=f"{plan.removed_volume_pct:.1f}"))
                self.btn_flat_apply.setEnabled(True)
                self.viewer.set_cut_plane(plan.cut_z)
            else:
                self.flat_note.setText(tr("flat.none"))
                self.btn_flat_apply.setEnabled(False)
                self.viewer.set_cut_plane(None)
        self.run_task(job, done)

    def _flat_apply(self):
        plan = self._flat_plan
        self.viewer.set_cut_plane(None)
        if plan is None:
            return

        def job():
            ok = self.ws.apply_flat_bottom(plan)
            self.ws.inspect(with_thin=True)
            return ok

        def done(ok):
            self.btn_flat_apply.setEnabled(False)
            self._after_geometry(tr("flat.applied") if ok else tr("flat.failed"))
        self.run_task(job, done)

    def _do_base(self):
        self.ws.opts.base_thickness_mm = self.sp_base_t.value()
        self.ws.opts.base_margin_mm = self.sp_base_m.value()
        shape = self.base_combo.currentData()
        if shape == "none":
            shape = "round"

        def job():
            how = self.ws.add_base(shape)
            self.ws.inspect(with_thin=True)
            return how

        self.run_task(job, lambda how: self._after_geometry(tr("right.base_added", how=how)))

    def _do_strengthen(self):
        def job():
            n = self.ws.strengthen()
            self.ws.inspect(with_thin=True)
            return n

        self.run_task(job, lambda n: self._after_geometry(tr("right.strengthened", n=n) if n else tr("right.strengthen_none")))

    # ============================================================== versions
    def _send_revision(self):
        text = self.rev_text.toPlainText().strip()
        if not text:
            QMessageBox.information(self, tr("ui.info"), tr("msg.empty_revision"))
            return
        req = GenerationRequest.from_dict(self.ws.request.to_dict())
        req.revision_instruction = text
        req.revision_of = self.ws.project.meta.id
        req.previous_job_id = self.ws.project.meta.last_job_id
        req.palette_mode = self.ws.palette_mode
        if self.ws.is_demo or self.ctx.settings.provider == "mock":
            QMessageBox.information(self, tr("ui.info"), tr("msg.demo_revision"))
        self.start_generation(project=self.ws.project, request=req)

    def _open_version(self):
        n = self.version_combo.currentData()
        if n is None:
            return
        proj = self.ws.project

        def job():
            proj.activate_version(n)
            return Workspace.open(proj, self.ctx.settings)

        def done(ws):
            self.ws = ws
            self._show_workspace()
        self.run_task(job, done)

    # ================================================================ export
    def _mode(self) -> str:
        return "multipart" if self.rb_multi.isChecked() else "color"

    def _ensure_exports(self, then):
        """Rebuild model.3mf/stl/glb preview from the current state, then call `then(report)`."""
        mode = self._mode()

        def job():
            return self.ws.rebuild_outputs(mode)

        def done(rep):
            self._dirty_exports = False
            self._describe_export(rep)
            then(rep)
        self.run_task(job, done)

    def _describe_export(self, rep: export.ExportReport):
        v = rep.validation
        txt = tr("export.report", method=tr(f"export.method.{rep.method}"), objs=v.objects, tris=f"{v.triangles:,}",
                 backend=rep.backend)
        if "shell_parts" in rep.notes:
            txt += "\n" + tr("export.shell_note")
        self.export_note.setText(txt)

    def _save_dialog(self, title: str, name: str, flt: str) -> str | None:
        base = paths.sanitize_filename(self.ws.project.meta.title or "figure")
        f, _ = QFileDialog.getSaveFileName(self, title, str(Path.home() / f"{base}{name}"), flt)
        return f or None

    def _save_3mf(self):
        f = self._save_dialog(tr("right.save_3mf"), ".3mf", "3MF (*.3mf)")
        if not f:
            return

        def then(rep):
            import shutil
            shutil.copy2(self.ws.project.file("model.3mf"), f)
            self.status_line.setText(tr("export.saved", f=f))
        self._ensure_exports(then)

    def _save_stl(self):
        f = self._save_dialog("STL", ".stl", "STL (*.stl)")
        if not f:
            return

        def job():
            export.export_stl(f, self.ws.mesh)
            return f
        self.run_task(job, lambda f: self.status_line.setText(tr("export.saved", f=f)))

    def _save_stl_by_color(self):
        d = QFileDialog.getExistingDirectory(self, tr("right.stl_by_color"), str(Path.home()))
        if not d:
            return

        def job():
            return export.export_stl_by_color(d, self.ws.project.meta.title or "figure", self.ws.mesh, self.ws.labels, self.ws.palette)

        self.run_task(job, lambda files: self.status_line.setText(tr("export.saved_n", n=len(files), d=d)))

    def _save_glb(self):
        f = self._save_dialog("GLB", ".glb", "GLB (*.glb)")
        if not f:
            return

        def job():
            export.export_glb(f, self.ws.mesh, self.ws.print_colors)
            return f
        self.run_task(job, lambda f: self.status_line.setText(tr("export.saved", f=f)))

    def _save_obj(self):
        f = self._save_dialog("OBJ", ".obj", "OBJ (*.obj)")
        if not f:
            return

        def job():
            return export.export_obj(f, self.ws.mesh, self.ws.labels, self.ws.palette)
        self.run_task(job, lambda files: self.status_line.setText(tr("export.saved", f=f)))

    def _open_bambu(self):
        exe = bambu.resolve(self.ctx.settings.bambu_path)
        if exe is None:
            self.show_error(FigureCraftError("bambu_not_found"))
            self.ctx.openSettingsRequested.emit()
            return

        def then(rep):
            try:
                bambu.open_in_bambu(exe, self.ws.project.file("model.3mf"))
                self.status_line.setText(tr("export.bambu_opened"))
            except FigureCraftError as exc:
                self.show_error(exc)
        self._ensure_exports(then)

    # ============================================================== retranslate
    def retranslate_stages(self):
        self.stages.retranslate()
