"""In-memory state of the open project + all mesh/colour operations (Qt-free, unit-testable)."""
from __future__ import annotations

import json
from dataclasses import asdict
from pathlib import Path

import numpy as np
import trimesh

from ..errors import FigureCraftError
from ..logging_setup import get_logger
from ..settings import Settings
from . import color, export, mesh_inspect, mesh_io, mesh_print, mesh_repair, render
from .models import GenerationRequest, PaletteEntry, PrintOptions
from .project_store import Project

log = get_logger("workspace")


def _pal_to_json(pal: list[PaletteEntry]) -> list[dict]:
    return [asdict(p) for p in pal]


def _pal_from_json(items: list[dict]) -> list[PaletteEntry]:
    return [PaletteEntry(**{k: v for k, v in d.items() if k in PaletteEntry.__dataclass_fields__}) for d in items]


class Workspace:
    def __init__(self, project: Project, settings: Settings):
        self.project = project
        self.settings = settings
        self.request = GenerationRequest.from_dict(project.meta.request) if project.meta.request else GenerationRequest()
        self.mesh: trimesh.Trimesh | None = None
        self.labels: np.ndarray = np.zeros(0, np.int32)
        self.palette: list[PaletteEntry] = _pal_from_json(project.meta.palette)
        self.report: mesh_inspect.InspectionReport | None = None
        self.repair_actions: list[mesh_repair.Action] = []
        self.palette_mode = self.request.palette_mode
        self.hints: list[tuple[str, str]] = []
        self._regions: np.ndarray | None = None
        self._print_cache: dict[int, color.ColorResult] = {}

    # ------------------------------------------------------------ props
    @property
    def opts(self) -> PrintOptions:
        return self.request.print

    @property
    def is_demo(self) -> bool:
        return self.project.meta.is_demo

    @property
    def orig_colors(self) -> np.ndarray:
        return mesh_io.get_face_colors(self.mesh)

    @property
    def print_colors(self) -> np.ndarray:
        return color.recolor_from_labels(self.labels, self.palette)

    def regions(self) -> np.ndarray:
        if self._regions is None or len(self._regions) != len(self.labels):
            self._regions = color.label_regions(self.mesh, self.labels)
        return self._regions

    # ------------------------------------------------------------- load
    @classmethod
    def open(cls, project: Project, settings: Settings) -> "Workspace":
        ws = cls(project, settings)
        rp = project.file("repaired.glb")
        if not rp.exists():
            raise FigureCraftError("no_mesh", "repaired.glb missing")
        ws.mesh = mesh_io.load_mesh(rp, keep_all_faces=True)       # face order == saved label order
        lp = project.file("labels.npy")
        if lp.exists():
            ws.labels = np.load(lp)
        f = ws.mesh.faces                                          # float32 rounding can collapse a few slivers
        ok = (f[:, 0] != f[:, 1]) & (f[:, 1] != f[:, 2]) & (f[:, 0] != f[:, 2])
        if not ok.all():
            if len(ws.labels) == len(ok):
                ws.labels = ws.labels[ok]
            ws.mesh.update_faces(ok)
            ws.mesh.remove_unreferenced_vertices()
        if len(ws.labels) != len(ws.mesh.faces) or not ws.palette:
            ws.reduce(ws.request.max_colors)
        sp = project.file("state.json")
        if sp.exists():
            try:
                st = json.loads(sp.read_text(encoding="utf-8"))
                ws.palette_mode = st.get("palette_mode", "auto")
                ws.hints = [tuple(h) for h in st.get("hints", [])]
                if st.get("inspection"):
                    ws.report = mesh_inspect.InspectionReport.from_dict(st["inspection"])
            except Exception as exc:
                log.warning("state.json unreadable: %s", exc)
        if ws.report is None:
            ws.report = mesh_inspect.inspect_mesh(ws.mesh, ws.opts)
        return ws

    # ----------------------------------------------------------- colours
    def reduce(self, k: int, mode: str | None = None, custom: list[str] | None = None) -> color.ColorResult:
        mode = mode or self.palette_mode
        locked = [p for p in self.palette if p.locked] if mode == "auto" else None
        res = color.reduce_colors(self.mesh, k, mode, custom or self.request.custom_palette, self.hints or None,
                                  locked=locked, min_island_mm2=max(1.0, (self.opts.min_feature_mm * 0.9) ** 2))
        self.labels, self.palette, self.palette_mode = res.labels, res.palette, mode
        self.request.max_colors = k
        self._regions = None
        return res

    def preview_for_k(self, k: int) -> color.ColorResult:
        """Colour reduction preview for k colours without touching the working palette."""
        if k not in self._print_cache:
            self._print_cache[k] = color.reduce_colors(self.mesh, k, "auto", None, self.hints or None)
        return self._print_cache[k]

    def apply_preview_k(self, k: int) -> None:
        self.reduce(k, "auto")

    def reassign_labels(self) -> None:
        """Re-label the (possibly changed) mesh against the current palette."""
        if not self.palette:
            self.reduce(self.request.max_colors)
            return
        lab = color.assign_labels(self.orig_colors, self.palette)
        if len(self.palette) > 1:
            lab = color.smooth_labels(self.mesh, lab, len(self.palette))
            lab = color.remove_islands(self.mesh, lab, max(1.0, (self.opts.min_feature_mm * 0.9) ** 2))
        self.labels = lab
        self._regions = None
        self._print_cache.clear()

    def set_region_color(self, region_id: int, label: int) -> None:
        reg = self.regions()
        self.labels = self.labels.copy()
        self.labels[reg == region_id] = label
        self._regions = None

    def set_palette_hex(self, i: int, hex_: str) -> None:
        self.palette[i].hex = hex_.upper()
        self.palette[i].name = color.name_color(color.hex_to_rgb(hex_))
        color._unique_names(self.palette)

    def rename_color(self, i: int, name: str) -> None:
        self.palette[i].name = "".join(c for c in name.strip().upper().replace(" ", "_") if c.isalnum() or c == "_") or self.palette[i].name

    def delete_color(self, i: int) -> None:
        if len(self.palette) <= 1:
            return
        self.labels, self.palette = color.delete_color(self.labels, self.palette, i, self.orig_colors)
        self._regions = None

    def merge_colors(self, src: int, dst: int) -> None:
        if src == dst or len(self.palette) <= 1:
            return
        self.labels, self.palette = color.merge_colors(self.labels, self.palette, src, dst)
        self._regions = None

    def add_color(self, hex_: str) -> bool:
        if len(self.palette) >= 4:
            return False
        e = PaletteEntry(hex=hex_.upper(), name=color.name_color(color.hex_to_rgb(hex_)))
        self.palette.append(e)
        color._unique_names(self.palette)
        return True

    # ---------------------------------------------------------- geometry
    def _after_geometry_change(self) -> None:
        self.reassign_labels()
        self.report = mesh_inspect.inspect_mesh(self.mesh, self.opts, with_thin=False)

    def inspect(self, with_thin: bool = True) -> mesh_inspect.InspectionReport:
        self.report = mesh_inspect.inspect_mesh(self.mesh, self.opts, with_thin=with_thin)
        return self.report

    def repair(self) -> list[mesh_repair.Action]:
        self.mesh, actions = mesh_repair.safe_repair(self.mesh, self.opts)
        self.repair_actions = actions
        mesh_print.ground(self.mesh)
        self._after_geometry_change()
        return actions

    def plan_flat_bottom(self) -> mesh_print.FlatBottomPlan:
        return mesh_print.plan_flat_bottom(self.mesh)

    def apply_flat_bottom(self, plan: mesh_print.FlatBottomPlan | None = None) -> bool:
        plan = plan or self.plan_flat_bottom()
        self.mesh, ok = mesh_print.apply_flat_bottom(self.mesh, plan)
        if ok:
            self._after_geometry_change()
        return ok

    def add_base(self, shape: str | None = None) -> str:
        if shape:
            self.opts.base_shape = shape
        self.mesh, how = mesh_print.add_base(self.mesh, self.opts)
        self._after_geometry_change()
        return how

    def strengthen(self) -> int:
        self.mesh, n = mesh_print.strengthen_thin_parts(self.mesh, self.opts)
        if n:
            self._after_geometry_change()
        return n

    def scale_to_height(self, h: float) -> None:
        mesh_print.fit_to_height(self.mesh, h)
        self.opts.height_mm = h
        self.report = mesh_inspect.inspect_mesh(self.mesh, self.opts, with_thin=False)

    def revert_to_original(self) -> None:
        """Back to the untouched download (only scaled to the target height and put on the build plate)."""
        orig = mesh_io.load_mesh(self.project.file("original.glb"))
        mesh_print.fit_to_height(orig, self.opts.height_mm)
        self.mesh = orig
        self.repair_actions = []
        self._after_geometry_change()

    # ----------------------------------------------------------- outputs
    def _normalize_for_save(self) -> None:
        """Round to float32 (what GLB/3MF store), weld and drop collapsed faces - together with their colour labels -
        so that the saved project reloads to exactly the same mesh."""
        m = mesh_io.make_mesh(self.mesh.vertices.astype(np.float32).astype(np.float64), self.mesh.faces, self.orig_colors)
        m = mesh_io.weld(m, drop_collapsed=False)
        f = m.faces
        ok = (f[:, 0] != f[:, 1]) & (f[:, 1] != f[:, 2]) & (f[:, 0] != f[:, 2])
        if not ok.all():
            if len(self.labels) == len(ok):
                self.labels = self.labels[ok]
            m.update_faces(ok)
            m.remove_unreferenced_vertices()
        if len(m.faces) != len(self.mesh.faces) or len(m.vertices) != len(self.mesh.vertices):
            self._regions = None
        self.mesh = m

    def save(self, rebuild_exports: bool = True, mode: str | None = None) -> None:
        p = self.project
        self._normalize_for_save()
        mesh_io.save_glb(self.mesh, p.file("repaired.glb"), face_colors=self.orig_colors)
        np.save(p.file("labels.npy"), self.labels)
        p.meta.palette = _pal_to_json(self.palette)
        p.meta.colors = len(self.palette)
        p.meta.size_mm = [round(float(x), 2) for x in self.mesh.extents]
        p.meta.request = self.request.to_dict()
        p.meta.inspection = self.report.to_dict() if self.report else {}
        p.file("state.json").write_text(json.dumps({
            "palette_mode": self.palette_mode, "hints": self.hints,
            "inspection": p.meta.inspection}, ensure_ascii=False), encoding="utf-8")
        p.file("prompt.txt").write_text(self.request.full_prompt(), encoding="utf-8")
        if rebuild_exports:
            self.rebuild_outputs(mode)
        p.save()

    def rebuild_outputs(self, mode: str | None = None) -> export.ExportReport:
        p = self.project
        mesh_io.save_glb(self.mesh, p.file("color_preview.glb"), face_colors=self.print_colors)
        export.export_stl(p.file("model.stl"), self.mesh)
        rep = export.export_3mf(p.file("model.3mf"), self.mesh, self.labels, self.palette,
                                mode=mode or self.settings.default_3mf_mode, title=p.meta.title or "FigureCraft",
                                description=self.request.full_prompt(), demo=self.is_demo)
        render.render_png(self.mesh, p.file("preview.png"), face_colors=self.print_colors)
        p.update_version_files()
        return rep

    def parts_summary(self, limit: int = 40) -> list[dict]:
        """Colour regions ('parts') sorted by area, for the right-hand parts list."""
        reg = self.regions()
        n = int(reg.max()) + 1 if len(reg) else 0
        area = np.bincount(reg, weights=self.mesh.area_faces, minlength=n)
        first = np.full(n, -1)
        first[reg[::-1]] = np.arange(len(reg))[::-1]
        out = []
        for r in np.argsort(-area)[:limit]:
            out.append({"region": int(r), "label": int(self.labels[first[r]]), "area_mm2": float(area[r])})
        return out
