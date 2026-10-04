"""Safe mesh loading/saving. Internal convention: Z-up, millimetres, per-face RGB colours on the mesh."""
from __future__ import annotations

import io
import os
from pathlib import Path

import numpy as np
import trimesh
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import connected_components

from ..errors import MeshError
from ..logging_setup import get_logger

log = get_logger("mesh")

MAX_LOAD_BYTES = 600 * 1024 * 1024
GLB_MAGIC = b"glTF"
DEFAULT_GRAY = (205, 205, 205)

# glTF is Y-up; slicers (and this app internally) are Z-up.
_YUP_TO_ZUP = trimesh.transformations.rotation_matrix(np.pi / 2, [1, 0, 0])
_ZUP_TO_YUP = trimesh.transformations.rotation_matrix(-np.pi / 2, [1, 0, 0])


# ------------------------------------------------------------------ colours
def get_face_colors(mesh: trimesh.Trimesh) -> np.ndarray:
    """(F,3) uint8 colours of the mesh faces; gray if the mesh has no colour information."""
    try:
        vis = mesh.visual
        if vis.kind in ("face", "vertex"):
            fc = np.asarray(vis.face_colors)
            if len(fc) == len(mesh.faces):
                return fc[:, :3].astype(np.uint8)
    except Exception:
        pass
    return np.tile(np.array(DEFAULT_GRAY, np.uint8), (len(mesh.faces), 1))


def has_color_info(mesh: trimesh.Trimesh) -> bool:
    try:
        return mesh.visual.kind in ("face", "vertex") and bool(len(mesh.faces))
    except Exception:
        return False


def set_face_colors(mesh: trimesh.Trimesh, colors: np.ndarray) -> trimesh.Trimesh:
    colors = np.asarray(colors, dtype=np.uint8)
    if colors.shape[1] == 3:
        colors = np.hstack([colors, np.full((len(colors), 1), 255, np.uint8)])
    mesh.visual = trimesh.visual.ColorVisuals(mesh=mesh, face_colors=colors)
    return mesh


def make_mesh(vertices, faces, face_colors=None) -> trimesh.Trimesh:
    m = trimesh.Trimesh(np.asarray(vertices, np.float64), np.asarray(faces, np.int64), process=False)
    if face_colors is not None:
        set_face_colors(m, face_colors)
    return m


def _texture_face_colors(g: trimesh.Trimesh) -> np.ndarray:
    """Sample the base-colour texture at every face centre (colour stays tied to the real mesh)."""
    vis = g.visual
    mat = getattr(vis, "material", None)
    img = getattr(mat, "baseColorTexture", None) or getattr(mat, "image", None)
    factor = getattr(mat, "baseColorFactor", None)
    if factor is None:
        factor = getattr(mat, "diffuse", None)
    factor = np.asarray(factor if factor is not None else (255, 255, 255, 255), float)[:3] / 255.0
    uv = getattr(vis, "uv", None)
    if img is None or uv is None or len(uv) != len(g.vertices):
        base = np.clip(factor * 255, 0, 255).astype(np.uint8)
        return np.tile(base, (len(g.faces), 1))
    arr = np.asarray(img.convert("RGB"), dtype=np.float32)
    h, w = arr.shape[:2]
    fuv = np.asarray(uv)[g.faces].mean(axis=1)
    x = np.clip(np.round((fuv[:, 0] % 1.0 if fuv[:, 0].max() > 1.0 else fuv[:, 0]) * (w - 1)), 0, w - 1).astype(int)
    y = np.clip(np.round((1.0 - (fuv[:, 1] % 1.0 if fuv[:, 1].max() > 1.0 else fuv[:, 1])) * (h - 1)), 0, h - 1).astype(int)
    col = arr[y, x] * factor
    return np.clip(col, 0, 255).astype(np.uint8)


# --------------------------------------------------------------- topology
def weld(mesh: trimesh.Trimesh, rel_tol: float = 1e-6, drop_collapsed: bool = True) -> trimesh.Trimesh:
    """Merge vertices that are numerically identical (UV seams split vertices in textured GLBs).

    drop_collapsed=False keeps every face (so per-face data such as saved colour labels stays aligned)."""
    ext = float(np.max(mesh.extents)) if len(mesh.vertices) else 1.0
    digits = max(0, int(-np.log10(max(ext * rel_tol, 1e-12))))
    fc = get_face_colors(mesh) if has_color_info(mesh) else None
    m = trimesh.Trimesh(mesh.vertices.copy(), mesh.faces.copy(), process=False)
    m.merge_vertices(digits_vertex=digits)
    # drop faces collapsed by welding
    keep = (m.faces[:, 0] != m.faces[:, 1]) & (m.faces[:, 1] != m.faces[:, 2]) & (m.faces[:, 0] != m.faces[:, 2])
    if fc is not None:
        set_face_colors(m, fc)
    if drop_collapsed and not keep.all():
        m.update_faces(keep)
    m.remove_unreferenced_vertices()
    return m


def face_components(mesh: trimesh.Trimesh) -> np.ndarray:
    """Component label per face using shared (welded) vertices."""
    F = len(mesh.faces)
    if F == 0:
        return np.zeros(0, np.int64)
    # label vertices by connectivity then map to faces
    V = len(mesh.vertices)
    f = mesh.faces
    rows = np.concatenate([f[:, 0], f[:, 1]])
    cols = np.concatenate([f[:, 1], f[:, 2]])
    g = coo_matrix((np.ones(len(rows), np.int8), (rows, cols)), shape=(V, V))
    _, vlab = connected_components(g, directed=False)
    return vlab[f[:, 0]]


def sort_components_by_size(labels: np.ndarray, mesh: trimesh.Trimesh) -> np.ndarray:
    """Relabel components so 0 is the largest (by area)."""
    if len(labels) == 0:
        return labels
    areas = np.bincount(labels, weights=mesh.area_faces)
    order = np.argsort(-areas)
    remap = np.empty_like(order)
    remap[order] = np.arange(len(order))
    return remap[labels]


# ------------------------------------------------------------------- load
def _validate_file(path: Path) -> None:
    if not path.is_file():
        raise MeshError("file_missing", str(path))
    size = path.stat().st_size
    if size == 0:
        raise MeshError("corrupt_mesh", "empty file")
    if size > MAX_LOAD_BYTES:
        raise MeshError("file_too_large", f"{size} bytes", mb=size // (1024 * 1024))


def load_mesh(path: str | os.PathLike, weld_vertices: bool = True, keep_all_faces: bool = False) -> trimesh.Trimesh:
    """Load GLB/GLTF/OBJ/STL/PLY/3MF into one Z-up mesh with per-face colours.

    keep_all_faces=True: weld vertices but never drop faces (used for our own saved projects)."""
    path = Path(path)
    _validate_file(path)
    ext = path.suffix.lower().lstrip(".")
    if ext not in {"glb", "gltf", "obj", "stl", "ply", "3mf", "off"}:
        raise MeshError("unsupported_format", ext, ext=ext)
    if ext == "glb":
        with open(path, "rb") as fh:
            if fh.read(4) != GLB_MAGIC:
                raise MeshError("corrupt_glb", "bad magic")
    try:
        loaded = trimesh.load(str(path), force="scene", process=False)
    except Exception as exc:
        raise MeshError("corrupt_glb" if ext in ("glb", "gltf") else "corrupt_mesh", f"{type(exc).__name__}: {exc}") from exc

    geoms = [g for g in loaded.dump() if isinstance(g, trimesh.Trimesh) and len(g.faces)] if hasattr(loaded, "dump") else []
    if not geoms:
        raise MeshError("no_mesh", str(path.name))

    verts, faces, cols, off = [], [], [], 0
    for g in geoms:
        if g.visual.kind == "texture":
            fc = _texture_face_colors(g)
        elif has_color_info(g):
            fc = get_face_colors(g)
        else:
            fc = np.tile(np.array(DEFAULT_GRAY, np.uint8), (len(g.faces), 1))
        verts.append(np.asarray(g.vertices, np.float64))
        faces.append(np.asarray(g.faces, np.int64) + off)
        cols.append(fc)
        off += len(g.vertices)
    V, F, C = np.vstack(verts), np.vstack(faces), np.vstack(cols)
    if not np.isfinite(V).all():
        raise MeshError("corrupt_mesh", "non-finite vertex coordinates")
    mesh = make_mesh(V, F, C)
    if ext in ("glb", "gltf"):
        mesh.apply_transform(_YUP_TO_ZUP)
    if weld_vertices:
        mesh = weld(mesh, drop_collapsed=not keep_all_faces)
    if len(mesh.faces) < 4:
        raise MeshError("corrupt_mesh", "fewer than 4 faces")
    log.info("loaded %s: %d vertices, %d faces", path.name, len(mesh.vertices), len(mesh.faces))
    return mesh


# ------------------------------------------------------------------- save
def mesh_to_glb_bytes(mesh: trimesh.Trimesh, face_colors: np.ndarray | None = None, name: str = "figure") -> bytes:
    """Export as GLB (Y-up per the glTF spec) with flat per-face colours."""
    fc = get_face_colors(mesh) if face_colors is None else face_colors
    F = np.asarray(mesh.faces)
    v = np.asarray(mesh.vertices)[F.reshape(-1)]
    f = np.arange(len(v)).reshape(-1, 3)
    rgba = np.repeat(np.hstack([fc[:, :3], np.full((len(fc), 1), 255, np.uint8)]), 3, axis=0)
    m = trimesh.Trimesh(v, f, vertex_colors=rgba, process=False)
    m.apply_transform(_ZUP_TO_YUP)
    out = trimesh.Scene()
    out.add_geometry(m, node_name=name, geom_name=name)
    data = out.export(file_type="glb")
    return data if isinstance(data, bytes) else bytes(data)


def save_glb(mesh: trimesh.Trimesh, path: str | os.PathLike, face_colors: np.ndarray | None = None) -> Path:
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(mesh_to_glb_bytes(mesh, face_colors))
    return path


def copy_mesh(mesh: trimesh.Trimesh) -> trimesh.Trimesh:
    m = trimesh.Trimesh(mesh.vertices.copy(), mesh.faces.copy(), process=False)
    if has_color_info(mesh):
        set_face_colors(m, get_face_colors(mesh))
    return m


def transfer_colors(src: trimesh.Trimesh, dst: trimesh.Trimesh) -> trimesh.Trimesh:
    """Give `dst` faces the colour of the nearest `src` face (used after cutting/remeshing)."""
    from scipy.spatial import cKDTree

    if not has_color_info(src):
        return dst
    tree = cKDTree(src.triangles_center)
    _, idx = tree.query(dst.triangles_center)
    set_face_colors(dst, get_face_colors(src)[idx])
    return dst
