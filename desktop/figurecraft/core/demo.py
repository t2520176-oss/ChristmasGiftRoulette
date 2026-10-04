"""Procedural DEMO figures. These are hand-built shapes shipped with the app - NOT AI output."""
from __future__ import annotations

import numpy as np
import trimesh

from . import mesh_io, mesh_ops

Color = tuple[int, int, int]

IVORY: Color = (242, 237, 224)
PURPLE: Color = (122, 63, 196)
BLACK: Color = (17, 17, 17)
YELLOW: Color = (255, 210, 31)
GREEN: Color = (46, 139, 87)
BROWN: Color = (121, 85, 61)
RED: Color = (200, 40, 45)
TEAL: Color = (60, 170, 160)
CREAM: Color = (250, 232, 190)

DEMO_FIGURES = ("cat_wizard", "baby_dragon", "xmas_ornament")
DEMO_TITLES = {
    "cat_wizard": ("DEMO 고양이 마법사", "DEMO Cat Wizard"),
    "baby_dragon": ("DEMO 아기 드래곤", "DEMO Baby Dragon"),
    "xmas_ornament": ("DEMO 트리 장식", "DEMO Tree Ornament"),
}


# ----------------------------------------------------------- primitives
def _ellipsoid(c, r, sub=3) -> trimesh.Trimesh:
    m = trimesh.creation.icosphere(subdivisions=sub, radius=1.0)
    m.vertices = m.vertices * np.array(r, float) + np.array(c, float)
    return m


def _cone(base_c, radius, height, sections=48) -> trimesh.Trimesh:
    m = trimesh.creation.cone(radius=radius, height=height, sections=sections)
    m.apply_translation(base_c)
    return m


def _cyl(c, radius, height, sections=48) -> trimesh.Trimesh:
    m = trimesh.creation.cylinder(radius=radius, height=height, sections=sections)
    m.apply_translation(c)
    return m


def _tilt(m: trimesh.Trimesh, angle_deg: float, axis, about) -> trimesh.Trimesh:
    T = trimesh.transformations.rotation_matrix(np.radians(angle_deg), axis, point=about)
    m.apply_transform(T)
    return m


def _star(c, r_out, r_in, thick, points=5) -> trimesh.Trimesh:
    """Star prism facing -Y (front), built by hand so it needs no triangulation backend."""
    ang = np.linspace(0, 2 * np.pi, points * 2, endpoint=False) + np.pi / 2
    rad = np.where(np.arange(points * 2) % 2 == 0, r_out, r_in)
    xs, zs = rad * np.cos(ang), rad * np.sin(ang)
    n = len(xs)
    front = np.column_stack([xs, np.full(n, -thick / 2), zs])
    back = np.column_stack([xs, np.full(n, thick / 2), zs])
    cf, cb = [0, -thick / 2, 0], [0, thick / 2, 0]
    verts = np.vstack([front, back, [cf], [cb]])
    fi, bi = 2 * n, 2 * n + 1
    faces = []
    for i in range(n):
        j = (i + 1) % n
        faces += [[fi, j, i], [bi, n + i, n + j], [i, j, n + j], [i, n + j, n + i]]
    m = trimesh.Trimesh(verts + np.array(c, float), np.array(faces), process=False)
    m.fix_normals()
    return m


def _tube(points, radii, sub=2) -> list[trimesh.Trimesh]:
    """A tapered chain of overlapping spheres (tail, horns...)."""
    return [_ellipsoid(p, (r, r, r), sub) for p, r in zip(points, radii)]


def _cut_floor(parts: list[tuple[trimesh.Trimesh, Color]], z0: float = 0.0):
    """Trim every part at z0 so the model rests on a real flat bottom."""
    out = []
    for m, c in parts:
        if m.bounds[0][2] < z0:
            cut = mesh_ops.trim_below_z(m, z0) if mesh_ops.HAVE_MANIFOLD else None
            if cut is None:
                continue
            m = cut
        out.append((m, c))
    return out


def _finish(parts, height_mm: float) -> trimesh.Trimesh:
    parts = _cut_floor(parts, 0.0)
    mesh = mesh_ops.union_colored(parts)
    # scale to the requested height (design units are ~mm already)
    h = mesh.bounds[1][2] - mesh.bounds[0][2]
    mesh.apply_scale(height_mm / h)
    c = mesh.bounds.mean(axis=0)
    mesh.apply_translation([-c[0], -c[1], -mesh.bounds[0][2]])
    return mesh


# ----------------------------------------------------------- figures
def cat_wizard(height_mm: float = 120.0) -> trimesh.Trimesh:
    P: list[tuple[trimesh.Trimesh, Color]] = []
    P.append((_ellipsoid((0, 0, 18), (20, 17, 20), 3), IVORY))            # body (sitting)
    P.append((_ellipsoid((0, -2, 52), (28, 24, 23), 4), IVORY))           # big chibi head
    for sx in (-1, 1):                                                      # ears poking out beside the hat
        ear = _cone((sx * 21, 0, 62), 7.5, 20, 32)
        _tilt(ear, -sx * 24, [0, 1, 0], [sx * 21, 0, 62])
        P.append((ear, IVORY))
    for sx in (-1, 1):                                                      # eyes
        P.append((_ellipsoid((sx * 11, -24.5, 50), (3.6, 2.6, 5), 2), BLACK))
    P.append((_ellipsoid((0, -25.5, 43), (3, 2.2, 2.2), 2), BLACK))        # nose
    for sx in (-1, 1):                                                      # front paws (visible)
        P.append((_ellipsoid((sx * 9, -15, 4), (6.5, 7.5, 6), 3), IVORY))
    P.append((_ellipsoid((0, -15, 33), (4, 3.5, 4), 2), BLACK))            # ribbon knot
    for sx in (-1, 1):
        bow = _ellipsoid((sx * 8, -15, 33), (7.5, 3, 4.5), 2)
        _tilt(bow, sx * 15, [0, 1, 0], [sx * 4, -15, 33])
        P.append((bow, BLACK))
    tail_pts = [(10 + i * 3.4, 10 + i * 3.0, 8 + i * 4.8) for i in range(8)]
    for t in _tube(tail_pts, np.linspace(6.5, 3.6, 8)):                    # tail curls up behind the body
        P.append((t, IVORY))
    P.append((_cyl((0, -2, 66), 32, 3.2), PURPLE))                          # hat brim
    P.append((_cone((0, -2, 67), 21, 42, 56), PURPLE))                      # hat cone
    P.append((_cyl((0, -2, 70.5), 20.4, 5), BLACK))                         # hat band
    P.append((_star((0, -14, 86), 7.5, 3.4, 3.5), YELLOW))                  # star on the hat front
    P.append((_star((0, -2, 111), 7, 3.2, 3.5), YELLOW))                    # star on the hat tip
    return _finish(P, height_mm)


def baby_dragon(height_mm: float = 100.0) -> trimesh.Trimesh:
    P: list[tuple[trimesh.Trimesh, Color]] = []
    P.append((_ellipsoid((0, 0, 25), (24, 20, 26), 3), TEAL))               # body
    P.append((_ellipsoid((0, -6, 28), (16, 12, 20), 3), CREAM))             # belly
    P.append((_ellipsoid((0, -5, 62), (27, 24, 22), 4), TEAL))              # head
    P.append((_ellipsoid((0, -24, 56), (13, 9, 8), 3), TEAL))               # snout
    for sx in (-1, 1):
        P.append((_ellipsoid((sx * 12, -24, 66), (4.2, 2.6, 5.5), 2), BLACK))   # eyes
        P.append((_ellipsoid((sx * 5, -32, 57), (1.6, 1.2, 1.6), 2), BLACK))    # nostrils
        horn = _cone((sx * 14, -3, 76), 5, 20, 28)
        _tilt(horn, -sx * 18, [0, 1, 0], [sx * 14, -3, 76])
        P.append((horn, YELLOW))
        wing = _ellipsoid((sx * 24, 10, 38), (3.5, 15, 19), 3)                  # flat wings on the back
        _tilt(wing, -sx * 22, [0, 1, 0], [sx * 20, 10, 30])
        P.append((wing, PURPLE))
        P.append((_ellipsoid((sx * 15, -13, 5), (9, 11, 6), 3), TEAL))          # feet
    tail_pts = [(0, 16 + i * 4.2, 10 + i * 1.6) for i in range(8)]
    for t in _tube(tail_pts, np.linspace(8, 3, 8)):
        P.append((t, TEAL))
    for i in range(5):                                                          # back spikes
        z = 46 - i * 6
        y = 20 * np.sqrt(max(0.0, 1 - ((z - 25) / 26) ** 2)) - 2
        P.append((_cone((0, y, z), 3.8, 9, 20), YELLOW))
    return _finish(P, height_mm)


def xmas_ornament(height_mm: float = 90.0) -> trimesh.Trimesh:
    P: list[tuple[trimesh.Trimesh, Color]] = []
    P.append((_cyl((0, 0, 4), 22, 8, 64), BROWN))                           # round base
    P.append((_cyl((0, 0, 14), 6, 16, 32), BROWN))                          # trunk
    for i, (r, h, z) in enumerate([(26, 26, 14), (21, 24, 32), (15, 22, 48)]):
        P.append((_cone((0, 0, z), r, h, 64), GREEN))
    P.append((_star((0, 0, 72), 10, 4.6, 4.5), YELLOW))
    rng = np.random.default_rng(7)
    for ang, rad, z in [(30, 17, 22), (150, 16, 24), (260, 18, 20), (80, 12, 38), (200, 12, 40), (330, 8, 54)]:
        a = np.radians(ang)
        P.append((_ellipsoid((rad * np.cos(a), rad * np.sin(a), z), (3.6, 3.6, 3.6), 2), RED))
    return _finish(P, height_mm)


BUILDERS = {"cat_wizard": cat_wizard, "baby_dragon": baby_dragon, "xmas_ornament": xmas_ornament}


def pick_demo(prompt: str) -> str:
    """Choose which bundled demo figure to show (keyword match; priority: dragon > cat/animal > decoration)."""
    p = (prompt or "").lower()
    if any(k in p for k in ("드래곤", "dragon", "판타지", "fantasy", "괴물", "monster")):
        return "baby_dragon"
    if any(k in p for k in ("고양이", "강아지", "토끼", "마법사", "동물", "cat", "kitten", "dog", "rabbit", "wizard", "animal")):
        return "cat_wizard"
    if any(k in p for k in ("크리스마스", "트리", "장식", "할로윈", "xmas", "christmas", "tree", "ornament", "decor")):
        return "xmas_ornament"
    return "cat_wizard"


def build_demo(name: str, height_mm: float | None = None) -> trimesh.Trimesh:
    fn = BUILDERS[name]
    return fn(height_mm) if height_mm else fn()
