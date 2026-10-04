"""Print optimisation: scale (mm), flat bottom, bases, thin-part strengthening."""
from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np
import trimesh
from scipy import ndimage
from scipy.sparse import coo_matrix

from ..logging_setup import get_logger
from . import mesh_inspect as mi
from . import mesh_io, mesh_ops
from .models import PrintOptions

log = get_logger("print")
_trapz = getattr(np, "trapezoid", None) or getattr(np, "trapz")


def ground(mesh: trimesh.Trimesh) -> trimesh.Trimesh:
    """Centre on XY and put the lowest point on Z=0 (in place)."""
    b = mesh.bounds
    mesh.apply_translation([-(b[0][0] + b[1][0]) / 2, -(b[0][1] + b[1][1]) / 2, -b[0][2]])
    return mesh


def fit_to_height(mesh: trimesh.Trimesh, height_mm: float) -> trimesh.Trimesh:
    h = float(mesh.extents[2])
    if h <= 0:
        return mesh
    mesh.apply_scale(float(height_mm) / h)
    return ground(mesh)


# ------------------------------------------------------------ flat bottom
@dataclass
class FlatBottomPlan:
    needed: bool
    cut_z: float = 0.0
    contact_before: float = 0.0
    contact_after: float = 0.0
    removed_volume_pct: float = 0.0
    note: str = ""


def _section_area(mesh: trimesh.Trimesh, man, z: float) -> float:
    try:
        if man is not None:
            return float(man.slice(float(z)).area())
        sec = mesh.section(plane_origin=[0, 0, z], plane_normal=[0, 0, 1])
        if sec is None:
            return 0.0
        planar, _ = sec.to_2D()
        return float(sum(p.area for p in planar.polygons_full))
    except Exception:
        return 0.0


def plan_flat_bottom(mesh: trimesh.Trimesh, max_cut_frac: float = 0.06) -> FlatBottomPlan:
    """Pick the shallowest cut that gives a stable print plane. Does not modify the mesh."""
    height = float(mesh.extents[2])
    footprint = float(mesh.extents[0] * mesh.extents[1])
    target = max(150.0, 0.10 * footprint)
    zmin = float(mesh.bounds[0][2])
    rep = mi.inspect_mesh(mesh, with_thin=False)
    plan = FlatBottomPlan(needed=False, contact_before=rep.flat_contact_area_mm2)
    if rep.flat_contact_area_mm2 >= target:
        plan.contact_after = rep.flat_contact_area_mm2
        plan.note = "already_flat"
        return plan
    man = mesh_ops.try_manifold(mesh) if mesh_ops.HAVE_MANIFOLD else None
    cap = max(0.5, max_cut_frac * height)
    ts = np.linspace(0.15, cap, 24)
    areas = np.array([_section_area(mesh, man, zmin + t) for t in ts])
    ok = np.where(areas >= target)[0]
    if len(ok):
        k = int(ok[0])
    elif areas.max() >= max(20.0, 0.03 * footprint):
        k = int(areas.argmax())
        plan.note = "partial"
    else:
        plan.note = "no_stable_plane"
        return plan
    plan.needed = True
    plan.cut_z = float(zmin + ts[k])
    plan.contact_after = float(areas[k])
    plan.removed_volume_pct = float(_trapz(areas[: k + 1], ts[: k + 1]) / max(abs(mesh.volume), 1e-9) * 100) \
        if rep.watertight else 0.0
    return plan


def apply_flat_bottom(mesh: trimesh.Trimesh, plan: FlatBottomPlan) -> tuple[trimesh.Trimesh, bool]:
    """Cut at plan.cut_z. Returns (mesh, applied). Falls back to the input if the cut cannot be made cleanly."""
    if not plan.needed:
        return mesh, False
    out = mesh_ops.trim_below_z(mesh, plan.cut_z)
    if out is None:
        try:
            cut = mesh.slice_plane([0, 0, plan.cut_z], [0, 0, 1], cap=True)
            if cut is not None and len(cut.faces) and cut.is_watertight == mesh.is_watertight:
                out = mesh_io.transfer_colors(mesh, mesh_io.weld(cut))
        except Exception as exc:
            log.warning("flat bottom cut failed: %s", exc)
    if out is None or len(out.faces) < 4:
        return mesh, False
    ground(out)
    return out, True


# ------------------------------------------------------------------ bases
def _dominant_bottom_color(mesh: trimesh.Trimesh) -> tuple[int, int, int]:
    z = mesh.triangles[:, :, 2].min(axis=1)
    sel = z <= mesh.bounds[0][2] + 0.12 * mesh.extents[2]
    if not sel.any():
        sel[:] = True
    cols = mesh_io.get_face_colors(mesh)[sel].astype(int)
    q = cols >> 4
    key = q[:, 0] * 256 + q[:, 1] * 16 + q[:, 2]
    w = mesh.area_faces[sel]
    uniq, inv = np.unique(key, return_inverse=True)
    best = np.bincount(inv, weights=w).argmax()
    return tuple(int(c) for c in cols[inv == best].mean(axis=0))


def make_base(mesh: trimesh.Trimesh, opts: PrintOptions) -> trimesh.Trimesh | None:
    if opts.base_shape == "none":
        return None
    lowv = mesh.vertices[mesh.vertices[:, 2] <= mesh.bounds[0][2] + 0.12 * mesh.extents[2]]
    lo, hi = lowv[:, :2].min(0), lowv[:, :2].max(0)
    cx, cy = (lo + hi) / 2
    w, d = hi - lo
    t, mg = float(opts.base_thickness_mm), float(opts.base_margin_mm)
    shape = opts.base_shape
    if shape == "custom":
        w, d, mg = opts.base_custom_w_mm, opts.base_custom_d_mm, 0.0
    if shape == "round":
        r = float(np.hypot(w, d)) / 2 + mg
        base = trimesh.creation.cylinder(radius=r, height=t, sections=96)
    elif shape in ("oval", "custom"):
        base = trimesh.creation.cylinder(radius=1.0, height=t, sections=96)
        base.vertices[:, 0] *= (w / 2 + mg) * (1.15 if shape == "oval" else 1.0)
        base.vertices[:, 1] *= (d / 2 + mg) * (1.15 if shape == "oval" else 1.0)
    else:  # square
        base = trimesh.creation.box(extents=[w + 2 * mg, d + 2 * mg, t])
    # base top sits 0.3 mm inside the figure so the slicer fuses them
    base.apply_translation([cx, cy, -t / 2 + 0.3])
    return base


def add_base(mesh: trimesh.Trimesh, opts: PrintOptions) -> tuple[trimesh.Trimesh, str]:
    """Attach a base. Returns (mesh, how) where how is 'union' | 'overlap' | 'none'."""
    base = make_base(mesh, opts)
    if base is None:
        return mesh, "none"
    color = _dominant_bottom_color(mesh)
    out = mesh_ops.union_meshes_keep_colors(mesh, base, color)
    how = "union"
    if out is None:                                   # figure is not a clean solid: overlap instead
        fc = mesh_io.get_face_colors(mesh)
        bc = np.tile(np.array(color, np.uint8), (len(base.faces), 1))
        out = mesh_io.make_mesh(np.vstack([mesh.vertices, base.vertices]),
                                np.vstack([mesh.faces, base.faces + len(mesh.vertices)]), np.vstack([fc, bc]))
        how = "overlap"
    ground(out)
    return out, how


# ------------------------------------------------------- thin-part boost
def strengthen_thin_parts(mesh: trimesh.Trimesh, opts: PrintOptions, passes: int = 2) -> tuple[trimesh.Trimesh, int]:
    """Gently inflate vertices that sit in regions thinner than the minimum feature size.

    Heuristic (voxel opening). Never moves a vertex more than 0.45 x min_feature in total."""
    m = mesh_io.copy_mesh(mesh)
    start = m.vertices.copy()
    touched = np.zeros(len(m.vertices), bool)
    step = 0.18 * opts.min_feature_mm
    V = len(m.vertices)
    e = m.edges_unique
    A = coo_matrix((np.ones(len(e)), (e[:, 0], e[:, 1])), shape=(V, V))
    A = (A + A.T).tocsr()
    deg = np.maximum(np.asarray(A.sum(axis=1)).ravel(), 1)
    for _ in range(passes):
        t = mi.analyze_thin(m, opts.min_feature_mm)
        if t is None or t.ratio < 0.004:
            break
        grid = ndimage.binary_dilation(t.thin, iterations=1)
        idx = np.floor((m.vertices - t.origin) / t.pitch).astype(int)
        idx = np.clip(idx, 0, np.array(grid.shape) - 1)
        w = grid[idx[:, 0], idx[:, 1], idx[:, 2]].astype(float)
        if not w.any():
            break
        for _s in range(3):                           # smooth the displacement field
            w = 0.5 * w + 0.5 * (A @ w) / deg
        n = m.vertex_normals
        disp = np.clip(start + 0, -1e9, 1e9)
        newv = m.vertices + n * (step * w)[:, None]
        delta = newv - start
        norm = np.linalg.norm(delta, axis=1)
        cap = 0.45 * opts.min_feature_mm
        scale = np.where(norm > cap, cap / np.maximum(norm, 1e-12), 1.0)
        m.vertices = start + delta * scale[:, None]
        touched |= w > 0.2
    changed = int(touched.sum())
    if changed:
        ground(m)
    return m, changed
