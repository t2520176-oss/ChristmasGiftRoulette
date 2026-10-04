"""Boolean helpers built on manifold3d (optional). Everything degrades gracefully when it is missing."""
from __future__ import annotations

import numpy as np
import trimesh

from . import mesh_io

try:  # optional native dependency
    from manifold3d import Manifold, Mesh as MMesh, OpType
    HAVE_MANIFOLD = True
except Exception:  # pragma: no cover
    HAVE_MANIFOLD = False


def _to_manifold(m: trimesh.Trimesh):
    return Manifold(MMesh(np.asarray(m.vertices, np.float32), np.asarray(m.faces, np.uint32)))


def _from_manifold(mm) -> trimesh.Trimesh:
    msh = mm.to_mesh()
    return trimesh.Trimesh(np.asarray(msh.vert_properties[:, :3], np.float64), np.asarray(msh.tri_verts, np.int64), process=False)


def union_colored(parts: list[tuple[trimesh.Trimesh, tuple[int, int, int]]]) -> trimesh.Trimesh:
    """Union closed parts into one solid; every output triangle keeps the colour of the part it came from."""
    if not HAVE_MANIFOLD:
        verts, faces, cols, off = [], [], [], 0
        for m, c in parts:
            verts.append(m.vertices); faces.append(m.faces + off); off += len(m.vertices)
            cols.append(np.tile(np.array(c, np.uint8), (len(m.faces), 1)))
        return mesh_io.make_mesh(np.vstack(verts), np.vstack(faces), np.vstack(cols))
    mans, ids = [], []
    for m, _ in parts:
        mf = _to_manifold(m).as_original()
        mans.append(mf)
        ids.append(mf.original_id())
    u = Manifold.batch_boolean(mans, OpType.Add)
    msh = u.to_mesh()
    tri = np.asarray(msh.tri_verts, np.int64)
    colors = np.zeros((len(tri), 3), np.uint8)
    id2col = {i: parts[k][1] for k, i in enumerate(ids)}
    run_index = list(msh.run_index)
    for r, oid in enumerate(msh.run_original_id):
        a, b = run_index[r] // 3, run_index[r + 1] // 3
        colors[a:b] = id2col.get(int(oid), parts[0][1])
    return mesh_io.make_mesh(np.asarray(msh.vert_properties[:, :3], np.float64), tri, colors)


def try_manifold(mesh: trimesh.Trimesh):
    """Return a Manifold for the mesh, or None when the mesh is not a valid closed manifold."""
    if not HAVE_MANIFOLD:
        return None
    try:
        m = _to_manifold(mesh)
        return None if m.is_empty() else m  # invalid / non-manifold input yields an empty Manifold
    except Exception:
        return None


def trim_below_z(mesh: trimesh.Trimesh, z: float) -> trimesh.Trimesh | None:
    """Remove everything below plane z (closed result). None if impossible with manifold3d."""
    m = try_manifold(mesh)
    if m is None:
        return None
    out = m.trim_by_plane([0, 0, 1], float(z))
    if out.is_empty():
        return None
    res = _from_manifold(out)
    return mesh_io.transfer_colors(mesh, res)


def union_meshes_keep_colors(a: trimesh.Trimesh, b: trimesh.Trimesh, color_b: tuple[int, int, int]) -> trimesh.Trimesh | None:
    """Union mesh `a` (with its per-face colours) with `b` (solid colour); None if a is not manifold."""
    ma, mb = try_manifold(a), try_manifold(b)
    if ma is None or mb is None:
        return None
    ma, mb = ma.as_original(), mb.as_original()
    ia, ib = ma.original_id(), mb.original_id()
    u = ma + mb
    msh = u.to_mesh()
    tri = np.asarray(msh.tri_verts, np.int64)
    res = trimesh.Trimesh(np.asarray(msh.vert_properties[:, :3], np.float64), tri, process=False)
    base = mesh_io.transfer_colors(a, res)
    cols = mesh_io.get_face_colors(base).copy()
    ri = list(msh.run_index)
    for r, oid in enumerate(msh.run_original_id):
        if int(oid) == ib:
            cols[ri[r] // 3: ri[r + 1] // 3] = np.array(color_b, np.uint8)
    return mesh_io.set_face_colors(res, cols)
