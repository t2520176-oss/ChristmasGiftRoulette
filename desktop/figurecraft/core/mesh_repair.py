"""Conservative mesh repair. Every step only removes/adds obviously safe geometry and reports what it did."""
from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np
import trimesh

from ..logging_setup import get_logger
from . import mesh_inspect as mi
from . import mesh_io
from .models import PrintOptions

log = get_logger("repair")


@dataclass
class Action:
    code: str                     # i18n key suffix: repair.<code>
    params: dict = field(default_factory=dict)


def _subset(mesh: trimesh.Trimesh, keep: np.ndarray) -> trimesh.Trimesh:
    fc = mesh_io.get_face_colors(mesh)
    m = mesh_io.make_mesh(mesh.vertices, mesh.faces[keep], fc[keep])
    m.remove_unreferenced_vertices()
    return m


def fill_simple_holes(mesh: trimesh.Trimesh, max_diag_frac: float = 0.25, max_loop_edges: int = 400):
    """Close small boundary loops with a centroid fan. Large openings are deliberately left alone."""
    F = mesh.faces
    he = np.vstack([F[:, [0, 1]], F[:, [1, 2]], F[:, [2, 0]]])
    face_of = np.tile(np.arange(len(F)), 3)
    und = np.sort(he, axis=1)
    _, inv, counts = np.unique(und, axis=0, return_inverse=True, return_counts=True)
    inv = inv.reshape(-1)
    bmask = counts[inv] == 1
    if not bmask.any():
        return mesh, 0, 0
    bedges, bfaces = he[bmask], face_of[bmask]
    nxt: dict[int, list[tuple[int, int]]] = {}
    for (a, b), f in zip(bedges.tolist(), bfaces.tolist()):
        nxt.setdefault(a, []).append((b, f))
    diag = float(np.linalg.norm(mesh.extents))
    verts = [mesh.vertices]
    new_faces, new_cols = [], []
    fc = mesh_io.get_face_colors(mesh)
    seen: set[int] = set()
    filled = skipped = 0
    nv = len(mesh.vertices)
    for start in list(nxt):
        if start in seen:
            continue
        loop, cur, ok = [], start, True
        edge_faces = []
        while True:
            if cur in seen and cur != start:
                ok = False
                break
            outs = nxt.get(cur, [])
            if len(outs) != 1:                       # branching boundary: not a simple loop
                ok = False
                break
            seen.add(cur)
            loop.append(cur)
            edge_faces.append(outs[0][1])
            cur = outs[0][0]
            if cur == start:
                break
            if len(loop) > max_loop_edges:
                ok = False
                break
        if not ok or len(loop) < 3:
            skipped += 1
            continue
        pts = mesh.vertices[loop]
        if np.linalg.norm(pts.max(0) - pts.min(0)) > max_diag_frac * diag:
            skipped += 1
            continue
        center = pts.mean(0)
        ci = nv + sum(len(v) for v in verts[1:])
        verts.append(center[None, :])
        for k in range(len(loop)):
            a, b = loop[k], loop[(k + 1) % len(loop)]
            new_faces.append([b, a, ci])
            new_cols.append(fc[edge_faces[k]])
        filled += 1
    if not new_faces:
        return mesh, 0, skipped
    V = np.vstack(verts)
    out = mesh_io.make_mesh(V, np.vstack([F, np.array(new_faces)]), np.vstack([fc, np.array(new_cols)]))
    return out, filled, skipped


def safe_repair(mesh: trimesh.Trimesh, opts: PrintOptions) -> tuple[trimesh.Trimesh, list[Action]]:
    """Return a repaired COPY. The caller keeps the original (original.glb is never touched)."""
    actions: list[Action] = []
    m = mesh_io.copy_mesh(mesh)
    if not mesh_io.has_color_info(m):
        mesh_io.set_face_colors(m, mesh_io.get_face_colors(m))

    finite = np.isfinite(m.vertices).all(axis=1)
    if not finite.all():
        keep = finite[m.faces].all(axis=1)
        m = _subset(m, keep)
        actions.append(Action("nonfinite", {"n": int((~keep).sum())}))

    deg = mi.degenerate_mask(m)
    if deg.any():
        m = _subset(m, ~deg)
        actions.append(Action("degenerate", {"n": int(deg.sum())}))

    dup = mi.duplicate_face_mask(m)
    if dup.any():
        m = _subset(m, ~dup)
        actions.append(Action("duplicates", {"n": int(dup.sum())}))

    before = len(m.vertices)
    m = mesh_io.weld(m, rel_tol=2e-6)
    if len(m.vertices) < before:
        actions.append(Action("weld", {"n": before - len(m.vertices)}))

    try:
        if not m.is_winding_consistent:
            trimesh.repair.fix_winding(m)
            actions.append(Action("winding"))
        if m.is_watertight and m.volume < 0:
            trimesh.repair.fix_inversion(m)
            actions.append(Action("inversion"))
    except Exception as exc:
        log.warning("normal fix skipped: %s", exc)

    labels, n = mi.component_info(m)
    if n > 1:
        tiny, floating = mi.classify_components(m, labels, n, opts.min_feature_mm)
        drop = set(tiny)
        if opts.remove_floating and floating:
            area_tot = m.area_faces.sum()
            for c in floating:
                if m.area_faces[labels == c].sum() < 0.03 * area_tot:
                    drop.add(c)
        if drop:
            keep = ~np.isin(labels, list(drop))
            m = _subset(m, keep)
            actions.append(Action("fragments", {"n": len(drop)}))

    if opts.close_holes:
        m, filled, skipped = fill_simple_holes(m)
        if filled:
            actions.append(Action("holes_filled", {"n": filled}))
        if skipped:
            actions.append(Action("holes_left", {"n": skipped}))

    if opts.simplify_tiny_details and len(m.faces) > 2000:
        try:
            fc = mesh_io.get_face_colors(m)
            trimesh.smoothing.filter_taubin(m, iterations=4)
            mesh_io.set_face_colors(m, fc)
            actions.append(Action("smoothed"))
        except Exception as exc:
            log.warning("smoothing skipped: %s", exc)

    log.info("repair actions: %s", [(a.code, a.params) for a in actions])
    return m, actions
