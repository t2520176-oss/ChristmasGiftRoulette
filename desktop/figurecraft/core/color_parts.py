"""Split a coloured mesh into closed, printable per-colour solids.

Cutting the surface along colour borders only gives open shells, which slicers cannot slice.
Instead every point of space is assigned the colour of its nearest surface sample (3D Voronoi),
each colour zone is meshed (marching cubes) and intersected with the exact figure solid (manifold3d):
    solid_c = figure  AND  zone_c
The solids tile the figure exactly, keep its precise outer surface and are closed manifolds.
"""
from __future__ import annotations

import numpy as np
import trimesh
from scipy import ndimage

from ..logging_setup import get_logger
from . import mesh_ops
from .models import PaletteEntry
from .threemf import Part, ams_name

log = get_logger("parts")


def _orient_outward(v: np.ndarray, f: np.ndarray) -> np.ndarray:
    m = trimesh.Trimesh(v, f, process=False)
    return f if m.volume > 0 else f[:, ::-1].copy()


def _clean_solid(v: np.ndarray, f: np.ndarray):
    """Boolean output can contain zero-area slivers. Weld vertices on their float32 position (what STL/3MF store),
    drop collapsed faces and keep the result only if it is still a closed 2-manifold."""
    v32 = np.asarray(v, np.float32)
    uniq, inv = np.unique(v32, axis=0, return_inverse=True)
    inv = inv.reshape(-1)
    f2 = inv[np.asarray(f)]
    ok = (f2[:, 0] != f2[:, 1]) & (f2[:, 1] != f2[:, 2]) & (f2[:, 0] != f2[:, 2])
    f2 = f2[ok]
    tm = trimesh.Trimesh(uniq.astype(np.float64), f2, process=False)
    tm.remove_unreferenced_vertices()
    tm.update_faces(tm.area_faces > 1e-12)
    tm.remove_unreferenced_vertices()
    e = np.sort(tm.edges, axis=1)
    _, counts = np.unique(e, axis=0, return_counts=True)
    closed = bool((counts == 2).all())
    return tm.vertices, tm.faces, closed


def shell_parts(mesh: trimesh.Trimesh, labels: np.ndarray, palette: list[PaletteEntry]) -> list[Part]:
    """Fallback: open surface shells per colour (valid 3MF, but may need 'repair' in a slicer)."""
    parts = []
    for i, p in enumerate(palette):
        m = labels == i
        if not m.any():
            continue
        f = mesh.faces[m]
        used, inv = np.unique(f, return_inverse=True)
        parts.append(Part(ams_name(i, p.name), p.rgb, mesh.vertices[used], inv.reshape(-1, 3)))
    return parts


def solid_parts(mesh: trimesh.Trimesh, labels: np.ndarray, palette: list[PaletteEntry],
                pitch: float | None = None) -> list[Part] | None:
    """Closed per-colour solids, or None if the figure is not a clean manifold / the result is inconsistent."""
    used = [i for i in range(len(palette)) if (labels == i).any()]
    if not used:
        return None
    fig = mesh_ops.try_manifold(mesh)
    if fig is None:
        log.info("solid parts unavailable (figure is not a closed manifold or manifold3d is missing)")
        return None
    if len(used) == 1:
        i = used[0]
        return [Part(ams_name(i, palette[i].name), palette[i].rgb, mesh.vertices, mesh.faces)]
    from manifold3d import Manifold, Mesh as MMesh, OpType
    from skimage import measure

    ext = float(max(mesh.extents))
    pitch = pitch or float(np.clip(ext / 160.0, 0.45, 1.0))
    pad = 4
    origin = mesh.bounds[0] - pad * pitch
    shape = np.ceil((mesh.bounds[1] + pad * pitch - origin) / pitch).astype(int) + 1

    # Voronoi labelling on the voxel grid: every cell takes the label of its nearest surface cell
    n = int(min(600_000, max(50_000, mesh.area / (pitch * 0.5) ** 2)))
    pts, fidx = trimesh.sample.sample_surface(mesh, n, seed=11)
    idx = np.clip(np.floor((pts - origin) / pitch).astype(int), 0, shape - 1)
    surf = np.zeros(tuple(shape), bool)
    lab = np.zeros(tuple(shape), np.int8)
    surf[idx[:, 0], idx[:, 1], idx[:, 2]] = True
    lab[idx[:, 0], idx[:, 1], idx[:, 2]] = labels[fidx]
    _, near = ndimage.distance_transform_edt(~surf, return_indices=True)
    field_lab = lab[near[0], near[1], near[2]]

    parts: list[Part] = []
    vol_sum = 0.0
    for i in used:
        ind = ndimage.gaussian_filter((field_lab == i).astype(np.float32), 1.0)
        ind = np.pad(ind, 1)                                    # zero border -> closed zone surfaces
        if ind.max() < 0.5:
            continue
        v, f, _, _ = measure.marching_cubes(ind, 0.5, spacing=(pitch, pitch, pitch))
        v = v + origin - pitch                                  # undo the 1-cell pad
        f = _orient_outward(v, f)
        zone = Manifold(MMesh(v.astype(np.float32), f.astype(np.uint32)))
        if zone.is_empty():
            log.warning("zone mesh for colour %d is not a manifold", i)
            return None
        part = Manifold.batch_boolean([fig, zone], OpType.Intersect)
        if part.is_empty():
            continue
        msh = part.to_mesh()
        pv, pf = np.asarray(msh.vert_properties[:, :3], np.float64), np.asarray(msh.tri_verts, np.int64)
        pv, pf, closed = _clean_solid(pv, pf)
        if not closed:
            log.warning("colour %d part is not a closed manifold after clean-up", i)
            return None
        tm = trimesh.Trimesh(pv, pf, process=False)
        if tm.volume < 1e-3:
            continue
        vol_sum += tm.volume
        parts.append(Part(ams_name(i, palette[i].name), palette[i].rgb, pv, pf))
    total = abs(float(mesh.volume))
    if not parts or abs(vol_sum - total) > 0.04 * total:
        log.warning("solid parts volume %.0f differs from figure volume %.0f - discarded", vol_sum, total)
        return None
    return parts
