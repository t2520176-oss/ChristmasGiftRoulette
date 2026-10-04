"""Printable colour reduction (1-4 colours) on the real mesh faces.

Pipeline: face colours -> CIELAB -> weighted k-means (perceptual) -> optional snapping to
colours named in the prompt -> nearest-palette face labels -> smoothing / island clean-up.
"""
from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np
import trimesh
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import connected_components

from . import mesh_io
from .models import PaletteEntry

L_WEIGHT = 0.75   # lighting baked into textures should matter a bit less than hue/chroma

# ------------------------------------------------------------------ Lab
_M = np.array([[0.4124564, 0.3575761, 0.1804375],
               [0.2126729, 0.7151522, 0.0721750],
               [0.0193339, 0.1191920, 0.9503041]])
_WHITE = np.array([0.95047, 1.0, 1.08883])


def srgb_to_lab(rgb: np.ndarray) -> np.ndarray:
    c = np.asarray(rgb, np.float64) / 255.0
    lin = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    xyz = lin @ _M.T / _WHITE
    f = np.where(xyz > 216 / 24389, np.cbrt(xyz), (24389 / 27 * xyz + 16) / 116)
    L = 116 * f[..., 1] - 16
    a = 500 * (f[..., 0] - f[..., 1])
    b = 200 * (f[..., 1] - f[..., 2])
    return np.stack([L, a, b], axis=-1)


def lab_to_srgb(lab: np.ndarray) -> np.ndarray:
    lab = np.asarray(lab, np.float64)
    fy = (lab[..., 0] + 16) / 116
    fx = fy + lab[..., 1] / 500
    fz = fy - lab[..., 2] / 200
    f = np.stack([fx, fy, fz], axis=-1)
    xyz = np.where(f ** 3 > 216 / 24389, f ** 3, (116 * f - 16) * 27 / 24389) * _WHITE
    lin = xyz @ np.linalg.inv(_M).T
    lin = np.clip(lin, 0, 1)
    c = np.where(lin <= 0.0031308, lin * 12.92, 1.055 * lin ** (1 / 2.4) - 0.055)
    return np.clip(np.round(c * 255), 0, 255).astype(np.uint8)


def _chroma_scale(L: np.ndarray) -> np.ndarray:
    """Hue/chroma of near-black colours is imperceptible (and just noise in baked textures): fade it out."""
    return np.clip(np.asarray(L, np.float64) / 40.0, 0.2, 1.0)


def _w(lab: np.ndarray) -> np.ndarray:
    """Distance space: CIELAB with lighting weighted down and chroma faded for very dark colours."""
    out = np.array(lab, np.float64, copy=True)
    out[..., 1:] *= _chroma_scale(out[..., 0])[..., None]
    out[..., 0] *= L_WEIGHT
    return out


def _unw(c: np.ndarray) -> np.ndarray:
    """Inverse of _w (for turning k-means centres back into sRGB)."""
    out = np.array(c, np.float64, copy=True)
    out[..., 0] /= L_WEIGHT
    out[..., 1:] /= _chroma_scale(out[..., 0])[..., None]
    return out


def hex_to_rgb(h: str) -> tuple[int, int, int]:
    h = h.strip().lstrip("#")
    if len(h) == 3:
        h = "".join(c * 2 for c in h)
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def rgb_to_hex(rgb) -> str:
    r, g, b = (int(v) for v in rgb[:3])
    return f"#{r:02X}{g:02X}{b:02X}"


# --------------------------------------------------------------- naming
NAMED = {
    "WHITE": "#FFFFFF", "IVORY": "#F2EDE0", "CREAM": "#FAE8BE", "BEIGE": "#D9C3A0", "GRAY": "#8A8A8A",
    "BLACK": "#111111", "RED": "#D32F2F", "ORANGE": "#F57C00", "YELLOW": "#FFD21F", "GOLD": "#C9A227",
    "GREEN": "#2E8B57", "LIME": "#8BC34A", "TEAL": "#1FA79B", "SKYBLUE": "#6EC6F0", "BLUE": "#1E5FD0",
    "NAVY": "#1B2A5C", "PURPLE": "#7A3FC4", "LAVENDER": "#B9A2E0", "PINK": "#F48FB1", "BROWN": "#795548",
    "SKIN": "#F1C7A5",
}
_NAMED_LAB = {k: srgb_to_lab(np.array(hex_to_rgb(v))) for k, v in NAMED.items()}

# prompt words (Korean + English) -> canonical name
HINT_WORDS = {
    "흰": "WHITE", "하얀": "WHITE", "흰색": "WHITE", "화이트": "WHITE", "white": "WHITE",
    "아이보리": "IVORY", "ivory": "IVORY", "크림": "CREAM", "cream": "CREAM", "베이지": "BEIGE", "beige": "BEIGE",
    "회색": "GRAY", "gray": "GRAY", "grey": "GRAY", "검정": "BLACK", "검은": "BLACK", "검정색": "BLACK", "black": "BLACK",
    "빨강": "RED", "빨간": "RED", "빨간색": "RED", "red": "RED", "주황": "ORANGE", "orange": "ORANGE",
    "노랑": "YELLOW", "노란": "YELLOW", "노란색": "YELLOW", "yellow": "YELLOW", "금색": "GOLD", "gold": "GOLD",
    "초록": "GREEN", "초록색": "GREEN", "green": "GREEN", "연두": "LIME", "청록": "TEAL", "teal": "TEAL",
    "하늘색": "SKYBLUE", "skyblue": "SKYBLUE", "파랑": "BLUE", "파란": "BLUE", "파란색": "BLUE", "blue": "BLUE",
    "남색": "NAVY", "navy": "NAVY", "보라": "PURPLE", "보라색": "PURPLE", "purple": "PURPLE", "violet": "PURPLE",
    "라벤더": "LAVENDER", "lavender": "LAVENDER", "분홍": "PINK", "분홍색": "PINK", "핑크": "PINK", "pink": "PINK",
    "갈색": "BROWN", "brown": "BROWN", "살구": "SKIN", "살색": "SKIN",
}


def name_color(rgb) -> str:
    lab = srgb_to_lab(np.array(rgb[:3]))
    return min(_NAMED_LAB, key=lambda k: float(np.linalg.norm(_w(_NAMED_LAB[k]) - _w(lab))))


# --------------------------------------------------------------- k-means
def _kmeans(points: np.ndarray, weights: np.ndarray, k: int, seed: int = 0, iters: int = 40) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = len(points)
    k = min(k, n)
    centers = np.empty((k, points.shape[1]))
    p = weights / weights.sum()
    centers[0] = points[rng.choice(n, p=p)]
    d2 = ((points - centers[0]) ** 2).sum(1)
    for i in range(1, k):                                      # weighted k-means++
        pr = d2 * weights
        if pr.sum() <= 0:
            centers[i] = points[rng.integers(n)]
        else:
            centers[i] = points[rng.choice(n, p=pr / pr.sum())]
        d2 = np.minimum(d2, ((points - centers[i]) ** 2).sum(1))
    for _ in range(iters):
        dist = ((points[:, None, :] - centers[None, :, :]) ** 2).sum(2)
        lab = dist.argmin(1)
        new = centers.copy()
        for j in range(k):
            m = lab == j
            if m.any():
                new[j] = (points[m] * weights[m, None]).sum(0) / weights[m].sum()
        if np.allclose(new, centers, atol=1e-3):
            break
        centers = new
    return centers


@dataclass
class ColorResult:
    labels: np.ndarray                      # (F,) palette index per face
    palette: list[PaletteEntry]
    preview: np.ndarray                     # (F,3) uint8 palette colour per face
    k_requested: int = 4
    notes: list[str] = field(default_factory=list)


def _unique_weighted(colors: np.ndarray, areas: np.ndarray):
    q = (colors.astype(np.int64) >> 2)                         # 6 bits / channel
    key = (q[:, 0] << 12) | (q[:, 1] << 6) | q[:, 2]
    uniq, inv = np.unique(key, return_inverse=True)
    w = np.bincount(inv, weights=areas)
    rep = np.zeros((len(uniq), 3))
    cnt = np.bincount(inv)
    for ch in range(3):
        rep[:, ch] = np.bincount(inv, weights=colors[:, ch].astype(float) * areas) / np.maximum(w, 1e-12)
    return rep, w


def extract_palette(face_colors: np.ndarray, areas: np.ndarray, k: int,
                    hints: list[tuple[str, str]] | None = None, snap_de: float = 32.0,
                    locked: list[PaletteEntry] | None = None, seed: int = 0, merge_de: float = 26.0) -> list[PaletteEntry]:
    """Auto palette of up to `k` colours (CIELAB weighted k-means). `hints` = [(NAME, '#hex')] from the prompt."""
    k = max(1, min(4, int(k)))
    rep, w = _unique_weighted(face_colors, areas)
    lab = _w(srgb_to_lab(rep))
    # sqrt-weights: keep small but important details (eyes, buttons) from being swallowed by big areas
    wk = np.sqrt(w)
    n_free = k - len(locked or [])
    centers_lab = []
    if n_free > 0:
        best = None
        for s in range(3):                                      # a few restarts, keep the lowest inertia
            c = _kmeans(lab, wk, n_free, seed=seed + s)
            inertia = (wk * ((lab[:, None, :] - c[None]) ** 2).sum(2).min(1)).sum()
            if best is None or inertia < best[0]:
                best = (inertia, c)
        centers_lab = list(best[1])
    # undo L weighting, back to sRGB
    cl = [_w(_unw(np.array(c))) for c in centers_lab]          # weighted-space centres (for hint snapping)
    rgb = [tuple(int(v) for v in lab_to_srgb(_unw(np.array(c)))) for c in centers_lab]

    entries: list[PaletteEntry] = []
    used_hints: set[str] = set()
    for c, col in zip(cl, rgb):
        entry = PaletteEntry(hex=rgb_to_hex(col), name=name_color(col))
        if hints:
            best_h, best_d = None, snap_de
            for hn, hx in hints:
                if hn in used_hints:
                    continue
                d = np.linalg.norm(_w(srgb_to_lab(np.array(hex_to_rgb(hx)))) - _w(c))
                if d < best_d:
                    best_h, best_d = (hn, hx), d
            if best_h:
                used_hints.add(best_h[0])
                entry = PaletteEntry(hex=best_h[1].upper(), name=best_h[0])
        entries.append(entry)
    # merge near-duplicates (fewer real colours than requested)
    out: list[PaletteEntry] = list(locked or [])
    for e in entries:
        lab_e = _w(srgb_to_lab(np.array(e.rgb)))
        if all(np.linalg.norm(lab_e - _w(srgb_to_lab(np.array(o.rgb)))) > merge_de for o in out):
            out.append(e)
    # order: biggest area first (AMS 1 = main colour)
    lab_f = _w(srgb_to_lab(face_colors))
    pal_lab = np.stack([_w(srgb_to_lab(np.array(e.rgb))) for e in out])
    lab_idx = ((lab_f[:, None, :] - pal_lab[None]) ** 2).sum(2).argmin(1)
    area_by = np.bincount(lab_idx, weights=areas, minlength=len(out))
    order = np.argsort(-area_by)
    out = [out[i] for i in order]
    return _unique_names(out)


def _unique_names(entries: list[PaletteEntry]) -> list[PaletteEntry]:
    seen: dict[str, int] = {}
    for e in entries:
        n = e.name
        seen[n] = seen.get(n, 0) + 1
        if seen[n] > 1:
            e.name = f"{n}{seen[n]}"
    return entries


def assign_labels(face_colors: np.ndarray, palette: list[PaletteEntry]) -> np.ndarray:
    lab_f = _w(srgb_to_lab(face_colors))
    pal = np.stack([_w(srgb_to_lab(np.array(e.rgb))) for e in palette])
    out = np.empty(len(face_colors), np.int32)
    step = 400_000
    for i in range(0, len(lab_f), step):
        out[i:i + step] = ((lab_f[i:i + step, None, :] - pal[None]) ** 2).sum(2).argmin(1)
    return out


# ------------------------------------------------------------- smoothing
def _adjacency(mesh: trimesh.Trimesh) -> np.ndarray:
    return np.asarray(mesh.face_adjacency)


def smooth_labels(mesh: trimesh.Trimesh, labels: np.ndarray, k: int, iters: int = 2) -> np.ndarray:
    adj = _adjacency(mesh)
    if len(adj) == 0:
        return labels
    labels = labels.copy()
    F = len(labels)
    for _ in range(iters):
        cnt = np.zeros((F, k), np.float32)
        np.add.at(cnt, (adj[:, 0], labels[adj[:, 1]]), 1)
        np.add.at(cnt, (adj[:, 1], labels[adj[:, 0]]), 1)
        cnt[np.arange(F), labels] += 0.5
        new = cnt.argmax(1).astype(labels.dtype)
        if (new == labels).all():
            break
        labels = new
    return labels


def label_regions(mesh: trimesh.Trimesh, labels: np.ndarray) -> np.ndarray:
    """Connected regions of equal label (the 'parts' the user can reassign). Region 0 = biggest area."""
    adj = _adjacency(mesh)
    F = len(labels)
    same = labels[adj[:, 0]] == labels[adj[:, 1]]
    a = adj[same]
    g = coo_matrix((np.ones(len(a), np.int8), (a[:, 0], a[:, 1])), shape=(F, F))
    _, reg = connected_components(g, directed=False)
    areas = np.bincount(reg, weights=mesh.area_faces)
    order = np.argsort(-areas)
    remap = np.empty_like(order)
    remap[order] = np.arange(len(order))
    return remap[reg]


def remove_islands(mesh: trimesh.Trimesh, labels: np.ndarray, min_area_mm2: float) -> np.ndarray:
    """Merge colour regions smaller than min_area into the neighbouring colour they touch most."""
    adj = _adjacency(mesh)
    if len(adj) == 0:
        return labels
    labels = labels.copy()
    for _ in range(3):
        reg = label_regions(mesh, labels)
        n = int(reg.max()) + 1
        area = np.bincount(reg, weights=mesh.area_faces, minlength=n)
        small = np.where(area < min_area_mm2)[0]
        if len(small) == 0:
            break
        small_mask = np.zeros(n, bool); small_mask[small] = True
        # contact length between a small region and the other labels
        ra, rb = reg[adj[:, 0]], reg[adj[:, 1]]
        cross = ra != rb
        votes: dict[int, dict[int, float]] = {}
        for x, y, la, lb in zip(ra[cross], rb[cross], labels[adj[cross, 0]], labels[adj[cross, 1]]):
            if small_mask[x]:
                votes.setdefault(int(x), {}).setdefault(int(lb), 0.0)
                votes[int(x)][int(lb)] += 1
            if small_mask[y]:
                votes.setdefault(int(y), {}).setdefault(int(la), 0.0)
                votes[int(y)][int(la)] += 1
        changed = False
        for r, v in votes.items():
            best = max(v, key=v.get)
            labels[reg == r] = best
            changed = True
        if not changed:
            break
    return labels


def reduce_colors(mesh: trimesh.Trimesh, k: int, palette_mode: str = "auto",
                  custom_palette: list[str] | None = None, hints: list[tuple[str, str]] | None = None,
                  locked: list[PaletteEntry] | None = None, min_island_mm2: float = 1.5) -> ColorResult:
    """Reduce the mesh's face colours to a printable palette of at most `k` (1-4) colours."""
    k = max(1, min(4, int(k)))
    fc = mesh_io.get_face_colors(mesh)
    areas = mesh.area_faces
    notes: list[str] = []
    if palette_mode == "custom" and custom_palette:
        palette = [PaletteEntry(hex=h.upper(), name=name_color(hex_to_rgb(h))) for h in custom_palette[:k]]
        palette = _unique_names(palette)
    else:
        palette = extract_palette(fc, areas, k, hints=hints, locked=locked)
    if len(palette) < k:
        notes.append("fewer_colors")
    labels = assign_labels(fc, palette)
    if len(palette) > 1:
        labels = smooth_labels(mesh, labels, len(palette))
        labels = remove_islands(mesh, labels, min_island_mm2)
    prev = np.array([p.rgb for p in palette], np.uint8)[labels]
    return ColorResult(labels, palette, prev, k, notes)


def recolor_from_labels(labels: np.ndarray, palette: list[PaletteEntry]) -> np.ndarray:
    return np.array([p.rgb for p in palette], np.uint8)[np.clip(labels, 0, len(palette) - 1)]


def delete_color(labels: np.ndarray, palette: list[PaletteEntry], index: int, face_colors: np.ndarray):
    """Remove palette[index]; its faces move to the perceptually nearest remaining colour."""
    if len(palette) <= 1:
        return labels, palette
    keep = [i for i in range(len(palette)) if i != index]
    sub = [palette[i] for i in keep]
    mask = labels == index
    new = labels.copy()
    if mask.any():
        pal = np.stack([_w(srgb_to_lab(np.array(e.rgb))) for e in sub])
        lab_f = _w(srgb_to_lab(face_colors[mask]))
        pick = ((lab_f[:, None, :] - pal[None]) ** 2).sum(2).argmin(1)
        new[mask] = np.array(keep)[pick]
    remap = {old: n for n, old in enumerate(keep)}
    return np.array([remap[int(v)] for v in new], np.int32) if len(new) < 50_000 else \
        np.vectorize(remap.__getitem__)(new).astype(np.int32), sub


def merge_colors(labels: np.ndarray, palette: list[PaletteEntry], src: int, dst: int):
    new = labels.copy()
    new[new == src] = dst
    keep = [i for i in range(len(palette)) if i != src]
    remap = {old: n for n, old in enumerate(keep)}
    new = np.vectorize(remap.__getitem__)(new).astype(np.int32)
    return new, [palette[i] for i in keep]
