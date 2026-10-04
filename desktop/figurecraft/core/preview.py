"""Build compact binary payloads for the Three.js viewer (positions + per-face colours/labels)."""
from __future__ import annotations

import base64
import json

import numpy as np
import trimesh

from . import mesh_io

MAX_PREVIEW_FACES = 180_000


def preview_mesh(mesh: trimesh.Trimesh, max_faces: int = MAX_PREVIEW_FACES):
    """Lighter mesh for display only. Returns (mesh, face_map) where face_map[i] = source face index."""
    F = len(mesh.faces)
    if F <= max_faces:
        return mesh, np.arange(F)
    try:
        import fast_simplification as fs
        from scipy.spatial import cKDTree
        v, f = fs.simplify(np.asarray(mesh.vertices, np.float32), np.asarray(mesh.faces, np.int32),
                           target_count=max_faces)
        pm = trimesh.Trimesh(v, f, process=False)
        _, idx = cKDTree(mesh.triangles_center).query(pm.triangles_center)
        return pm, idx
    except Exception:
        return mesh, np.arange(F)


def build_payload(mesh: trimesh.Trimesh, orig_colors: np.ndarray, print_colors: np.ndarray | None,
                  labels: np.ndarray | None, regions: np.ndarray | None, info: dict | None = None) -> dict:
    """JSON-able dict: {meta, b64}. All arrays are little-endian and non-indexed (flat shading per face)."""
    pm, fmap = preview_mesh(mesh)
    tri = np.asarray(pm.vertices)[np.asarray(pm.faces)]               # (F,3,3)
    pos = tri.reshape(-1, 3).astype(np.float32)
    F = len(pm.faces)

    def per_vertex(c):
        return np.repeat(np.asarray(c, np.uint8)[fmap][:, :3], 3, axis=0)

    try:                                   # smooth shading: per-corner vertex normals as int8
        vn = np.asarray(pm.vertex_normals)[np.asarray(pm.faces)].reshape(-1, 3)
        nrm = np.clip(np.round(vn * 127), -127, 127).astype(np.int8)
    except Exception:
        nrm = np.zeros_like(pos, np.int8)
    parts = {"pos": pos.tobytes(), "nrm": nrm.tobytes(), "orig": per_vertex(orig_colors).tobytes()}
    parts["print"] = per_vertex(print_colors if print_colors is not None else orig_colors).tobytes()
    parts["label"] = (np.asarray(labels, np.uint8)[fmap] if labels is not None else np.zeros(F, np.uint8)).tobytes()
    parts["region"] = (np.asarray(regions, np.uint32)[fmap] if regions is not None else np.zeros(F, np.uint32)).tobytes()
    meta = {"faces": int(F), "sizes": {k: len(v) for k, v in parts.items()}, "info": info or {},
            "bounds": [list(map(float, b)) for b in mesh.bounds]}
    blob = b"".join(parts[k] for k in ("pos", "nrm", "orig", "print", "label", "region"))
    return {"meta": meta, "b64": base64.b64encode(blob).decode("ascii")}


def payload_json(payload: dict) -> str:
    return json.dumps(payload, separators=(",", ":"))


def glb_b64(path) -> str:
    with open(path, "rb") as fh:
        return base64.b64encode(fh.read()).decode("ascii")
