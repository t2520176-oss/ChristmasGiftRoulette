"""Export orchestration: 3MF (colour / multipart), STL (single / per colour), GLB, OBJ."""
from __future__ import annotations

import shutil
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
import trimesh

from .. import paths
from ..errors import ExportError
from ..logging_setup import get_logger
from . import color as colormod
from . import color_parts, mesh_io, threemf
from .models import PaletteEntry

log = get_logger("export")


@dataclass
class ExportReport:
    files: list[Path] = field(default_factory=list)
    backend: str = ""
    method: str = ""                      # solid | shell | per-triangle
    validation: threemf.ValidationResult | None = None
    notes: list[str] = field(default_factory=list)


def check_disk_space(target: Path, need_mb: float) -> None:
    free = shutil.disk_usage(target if target.exists() else target.parent).free / 1e6
    if free < need_mb:
        raise ExportError("disk_space", f"{free:.0f}MB free", need=int(need_mb), free=int(free))


def _meta(title: str, desc: str, demo: bool) -> threemf.Meta:
    d = (desc or "").strip().replace("\n", " ")[:400]
    if demo:
        d = "[DEMO - not AI generated] " + d
    return threemf.Meta(title=title + (" (DEMO)" if demo else ""), description=d)


def export_3mf(path, mesh: trimesh.Trimesh, labels: np.ndarray, palette: list[PaletteEntry],
               mode: str = "multipart", title: str = "FigureCraft model", description: str = "",
               demo: bool = False, parts_cache: list | None = None) -> ExportReport:
    """mode = 'multipart' (one assembly of closed colour parts) | 'color' (per-triangle colours)."""
    path = Path(path)
    rep = ExportReport()
    check_disk_space(path, max(50, len(mesh.faces) * 0.0002))
    meta = _meta(title, description, demo)
    if mode == "color" or len(palette) == 1 and mode != "multipart":
        spec = [(threemf.ams_name(i, p.name), p.rgb) for i, p in enumerate(palette)]
        rep.backend = threemf.write_color_3mf(path, mesh.vertices, mesh.faces, labels, spec, meta)
        rep.method = "per-triangle"
    else:
        parts = parts_cache or color_parts.solid_parts(mesh, labels, palette)
        rep.method = "solid"
        if parts is None:
            parts = color_parts.shell_parts(mesh, labels, palette)
            rep.method = "shell"
            rep.notes.append("shell_parts")
        rep.backend = threemf.write_multipart_3mf(path, parts, meta)
    rep.validation = threemf.validate_3mf(path)
    if not rep.validation.ok:
        raise ExportError("export_3mf_failed", "; ".join(rep.validation.problems))
    rep.files.append(path)
    log.info("3MF written: %s (%s/%s) objects=%d triangles=%d", path.name, rep.backend, rep.method,
             rep.validation.objects, rep.validation.triangles)
    return rep


def export_stl(path, mesh: trimesh.Trimesh) -> Path:
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    check_disk_space(path, 20)
    try:
        trimesh.Trimesh(mesh.vertices, mesh.faces, process=False).export(str(path), file_type="stl")
    except Exception as exc:
        raise ExportError("export_failed", str(exc)) from exc
    return path


def export_stl_by_color(folder, base: str, mesh: trimesh.Trimesh, labels: np.ndarray,
                        palette: list[PaletteEntry]) -> list[Path]:
    folder = Path(folder)
    folder.mkdir(parents=True, exist_ok=True)
    parts = color_parts.solid_parts(mesh, labels, palette) or color_parts.shell_parts(mesh, labels, palette)
    out = []
    for p in parts:
        f = folder / f"{paths.sanitize_filename(base)}_{p.name}.stl"
        trimesh.Trimesh(p.vertices, p.faces, process=False).export(str(f), file_type="stl")
        out.append(f)
    return out


def export_glb(path, mesh: trimesh.Trimesh, face_colors: np.ndarray | None = None) -> Path:
    check_disk_space(Path(path), 20)
    return mesh_io.save_glb(mesh, path, face_colors)


def export_obj(path, mesh: trimesh.Trimesh, labels: np.ndarray, palette: list[PaletteEntry]) -> list[Path]:
    """Wavefront OBJ with one group + material per colour (colours in the .mtl)."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    mtl = path.with_suffix(".mtl")
    V, F = np.asarray(mesh.vertices), np.asarray(mesh.faces)
    with open(mtl, "w", encoding="utf-8") as m:
        for i, p in enumerate(palette):
            r, g, b = (c / 255 for c in p.rgb)
            m.write(f"newmtl {threemf.ams_name(i, p.name)}\nKd {r:.4f} {g:.4f} {b:.4f}\nKa 0 0 0\nKs 0 0 0\nd 1\n\n")
    with open(path, "w", encoding="utf-8") as o:
        o.write(f"# FigureCraft export (mm)\nmtllib {mtl.name}\n")
        np.savetxt(o, V, fmt="v %.5f %.5f %.5f")
        for i, p in enumerate(palette):
            sel = F[labels == i]
            if len(sel) == 0:
                continue
            o.write(f"o {threemf.ams_name(i, p.name)}\nusemtl {threemf.ams_name(i, p.name)}\n")
            np.savetxt(o, sel + 1, fmt="f %d %d %d")
    return [path, mtl]
