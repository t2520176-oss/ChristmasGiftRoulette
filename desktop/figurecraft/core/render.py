"""Tiny software renderer (painter's algorithm) used for preview.png thumbnails - no GPU, no Qt."""
from __future__ import annotations

from pathlib import Path

import numpy as np
import trimesh
from PIL import Image, ImageDraw

from . import mesh_io


def _look(az_deg: float, el_deg: float) -> np.ndarray:
    az, el = np.radians(az_deg), np.radians(el_deg)
    # camera sits in front (-Y) of the model, rotated by azimuth, raised by elevation (Z up)
    return np.array([np.sin(az) * np.cos(el), -np.cos(az) * np.cos(el), np.sin(el)])


def render_png(mesh: trimesh.Trimesh, path: str | Path, face_colors: np.ndarray | None = None,
               size: int = 512, az: float = 35.0, el: float = 18.0, max_faces: int = 40000,
               bg=(30, 33, 40)) -> Path:
    fc = mesh_io.get_face_colors(mesh) if face_colors is None else face_colors
    V, F = np.asarray(mesh.vertices), np.asarray(mesh.faces)
    if len(F) > max_faces:
        sel = np.random.default_rng(1).choice(len(F), max_faces, replace=False)
        F, fc = F[sel], fc[sel]
    S = size * 2
    eye = _look(az, el)
    up = np.array([0, 0, 1.0])
    right = np.cross(up, eye); right /= np.linalg.norm(right)
    cam_up = np.cross(eye, right)
    c = (V.min(0) + V.max(0)) / 2
    P = V - c
    x, y, d = P @ right, P @ cam_up, P @ eye
    radius = max(np.ptp(x), np.ptp(y)) / 2 * 1.12 or 1.0
    px = (x / radius * 0.5 + 0.5) * S
    py = (1 - (y / radius * 0.5 + 0.5)) * S
    tri2 = np.stack([px[F], py[F]], axis=-1).astype(np.int32)
    n = np.cross(V[F[:, 1]] - V[F[:, 0]], V[F[:, 2]] - V[F[:, 0]])
    ln = np.linalg.norm(n, axis=1, keepdims=True)
    n = n / np.where(ln == 0, 1, ln)
    key = np.array([0.4, -0.6, 0.7]); key /= np.linalg.norm(key)
    shade = 0.35 + 0.65 * np.clip(n @ key, 0, 1) * 0.8 + 0.25 * np.abs(n @ eye)
    shade = np.clip(shade, 0.25, 1.1)[:, None]
    col = np.clip(fc[:, :3].astype(float) * shade, 0, 255).astype(np.uint8)
    order = np.argsort(d[F].mean(axis=1))  # far (small d) first, near last
    img = Image.new("RGB", (S, S), tuple(int(c) for c in bg))
    draw = ImageDraw.Draw(img)
    pts = tri2.astype(float)
    for i in order:                                    # painter's algorithm, far to near
        t = pts[i]
        c = col[i]
        draw.polygon([(t[0][0], t[0][1]), (t[1][0], t[1][1]), (t[2][0], t[2][1])], fill=(int(c[0]), int(c[1]), int(c[2])))
    img = img.resize((size, size), Image.Resampling.LANCZOS)   # 2x supersampling = anti-aliasing
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")
    return path
