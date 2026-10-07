#!/usr/bin/env python3
"""Bracket-fungus display shelf (entry #4): the hoof-shaped shelf and the bark plaque it hangs on.

    python bracket_fungus.py shelf  shelf.stl  [--size L|M|S]
    python bracket_fungus.py plaque plaque.stl [--size L|M|S]
    blender -b -P bracket_fungus.py -- shelf shelf.stl --size M

The shelf is a half ellipse, flat on top (the display surface) and thick like a hoof at the wall, thin at the
rim. It is made to be printed UPSIDE DOWN: the flat top on the bed, the hoof underside facing up, so nothing
needs support. The top carries engraved growth rings. A dovetail groove runs through its back, a card slot is
cut behind it. The plaque is a bark slab: bark FURROWS are cut into a flat front plane (so the shelf's flat back
can slide over it), a dovetail rail runs from the top edge down to a stop block, and two keyhole slots in the
back hang it on screws. The shelf slides down the rail from above and rests on the stop.
Everything is built as a watertight heightfield with numpy and Blender (Manifold booleans) cuts the details.
"""
import argparse
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bark_shell import smoothstep, vnoise  # noqa: E402

# ---- shared geometry (mm). The checker reads the same numbers from the meshes, not from here ----------------
NECK = 4.0            # half width of the rail at the plaque
HEAD = 8.0            # half width of the rail at its top (45 degree flanks)
RAIL_H = HEAD - NECK  # rail height above the plaque front
CLR = 0.3             # clearance groove / rail
T0 = 14.0             # shelf thickness on the plateau at the wall
TE = 2.4              # shelf thickness at the rim
PLATEAU = 0.5         # normalised radius up to which the shelf keeps its full thickness
PLAQUE_T = 10.0       # thickness of the plaque plate
FURROW = 2.4          # depth of the bark furrows below the flat front plane
STOP_W, STOP_OUT, STOP_Z = 26.0, 7.0, 6.0   # stop block: width, height out of the plaque, size along the rail
Z_STOP = 14.0         # height of the stop top above the bottom edge of the plaque
PLAQUE_H = 110.0
SLIT = (9.0, 11.4, 9.0)   # card slot: y from, y to, depth into the shelf
SIZES = {"L": dict(rx=75.0, d=62.0, seed=3), "M": dict(rx=65.0, d=54.0, seed=5), "S": dict(rx=55.0, d=46.0, seed=8)}


# ---------------------------------------------------------------------------------------------------------
# mesh helpers
# ---------------------------------------------------------------------------------------------------------
def signed_volume(verts, faces):
    v = np.asarray(verts, float)
    tot = 0.0
    for f in faces:
        for k in range(1, len(f) - 1):
            a, b, c = v[f[0]], v[f[k]], v[f[k + 1]]
            tot += np.dot(a, np.cross(b, c)) / 6.0
    return tot


def orient(verts, faces):
    """Make the faces point outwards (positive volume)."""
    if signed_volume(verts, faces) < 0:
        faces = [tuple(reversed(f)) for f in faces]
    return verts, faces


def prism(poly, c0, c1, mapping):
    """Extrude a 2D polygon (any orientation, simple) between c0 and c1 along the third axis; mapping(a, b, c) -> xyz."""
    import mapbox_earcut as earcut
    p = np.asarray(poly, float)
    if np.sum(p[:, 0] * np.roll(p[:, 1], -1) - np.roll(p[:, 0], -1) * p[:, 1]) < 0:
        p = p[::-1]
    n = len(p)
    idx = earcut.triangulate_float64(p, np.array([n], dtype=np.uint32)).reshape(-1, 3)
    verts = [mapping(a, b, c0) for a, b in p] + [mapping(a, b, c1) for a, b in p]
    faces = []
    for t in idx:
        faces.append((n + t[0], n + t[1], n + t[2]))
        faces.append((t[2], t[1], t[0]))
    for i in range(n):
        j = (i + 1) % n
        faces.append((i, j, n + j, n + i))
    return orient(verts, faces)


def ring_prism(rxo, do, rxi, di, z0, z1, n=240):
    th = np.linspace(0, 2 * np.pi, n, endpoint=False) + np.pi / n      # no vertex exactly on the plane y = 0
    ob = [(rxo * np.cos(t), do * np.sin(t), z0) for t in th]
    ot = [(rxo * np.cos(t), do * np.sin(t), z1) for t in th]
    ib = [(rxi * np.cos(t), di * np.sin(t), z0) for t in th]
    it = [(rxi * np.cos(t), di * np.sin(t), z1) for t in th]
    verts = ob + ot + ib + it
    faces = []
    for i in range(n):
        j = (i + 1) % n
        faces.append((i, j, n + j, n + i))                         # outer wall
        faces.append((2 * n + j, 2 * n + i, 3 * n + i, 3 * n + j))  # inner wall
        faces.append((n + i, n + j, 3 * n + j, 3 * n + i))         # top
        faces.append((2 * n + i, 2 * n + j, j, i))                 # bottom
    return orient(verts, faces)


# ---------------------------------------------------------------------------------------------------------
# shelf
# ---------------------------------------------------------------------------------------------------------
def thickness(rho, t0=T0, te=TE, plateau=PLATEAU):
    s = smoothstep(plateau, 1.0, rho)
    return te + (t0 - te) * (1 - s) ** 1.15


def build_shelf(rx, d, seed, n_phi=301, n_rho=101, tex=0.45):
    """Heightfield solid in print position: top face on z = 0, hoof underside up. Returns verts, faces."""
    phi = np.linspace(0, np.pi, n_phi)
    rho = np.linspace(0, 1, n_rho)
    R, P = np.meshgrid(rho, phi, indexing="ij")
    X, Y = rx * R * np.cos(P), d * R * np.sin(P)
    mask = smoothstep(0.35, 0.55, R) * (1 - smoothstep(0.93, 1.0, R))
    n = vnoise(X / 3.2, Y / 3.2, np.full_like(X, 0.5), seed) + 0.5 * vnoise(X / 1.4, Y / 1.4, np.full_like(X, 1.5), seed + 1)
    zu = thickness(R) - tex * mask * np.clip((n - 0.3) / 1.2, 0, 1)      # pores only make it thinner: the plateau stays the maximum
    verts = [(0.0, 0.0, 0.0), (0.0, 0.0, float(zu[0, 0]))]      # 0 = centre top, 1 = centre underside
    top = np.zeros((n_rho, n_phi), int)
    und = np.zeros((n_rho, n_phi), int)
    top[0, :], und[0, :] = 0, 1
    for i in range(1, n_rho):
        for j in range(n_phi):
            top[i, j] = len(verts)
            verts.append((float(X[i, j]), float(Y[i, j]), 0.0))
        for j in range(n_phi):
            und[i, j] = len(verts)
            verts.append((float(X[i, j]), float(Y[i, j]), float(zu[i, j])))
    faces = []
    for i in range(n_rho - 1):
        for j in range(n_phi - 1):
            if i == 0:
                faces.append((top[0, 0], top[1, j + 1], top[1, j]))
                faces.append((und[0, 0], und[1, j], und[1, j + 1]))
            else:
                faces.append((top[i, j], top[i, j + 1], top[i + 1, j + 1], top[i + 1, j]))
                faces.append((und[i, j], und[i + 1, j], und[i + 1, j + 1], und[i, j + 1]))
    last = n_rho - 1
    for j in range(n_phi - 1):                                    # rim wall
        faces.append((top[last, j], top[last, j + 1], und[last, j + 1], und[last, j]))
    for i in range(n_rho - 1):                                    # back wall y = 0, two halves
        faces.append((top[i, 0], top[i + 1, 0], und[i + 1, 0], und[i, 0]))
        faces.append((top[i, n_phi - 1], und[i, n_phi - 1], und[i + 1, n_phi - 1], top[i + 1, n_phi - 1]))
    return orient(verts, faces)


def groove_polygon():
    """Female dovetail: the rail's trapezoid grown by the clearance, open towards y < 0."""
    from shapely.geometry import Polygon
    rail = Polygon([(-NECK, -3.0), (NECK, -3.0), (NECK, 0.0), (HEAD, RAIL_H), (-HEAD, RAIL_H), (-NECK, 0.0)])
    g = rail.buffer(CLR, join_style=2, mitre_limit=5)
    return list(g.exterior.coords)[:-1]


def ring_radii(seed, n=14, lo=0.30, hi=0.97):
    rng = np.random.default_rng(seed)
    w = rng.uniform(0.6, 1.9, n)
    r = lo + (hi - lo) * np.cumsum(w) / np.sum(w)
    return r


# ---------------------------------------------------------------------------------------------------------
# plaque
# ---------------------------------------------------------------------------------------------------------
def furrows(x, z, seed, width=9.0, length=42.0):
    """0 = bottom of a furrow, 1 = bark ridge (a flat plateau, so the shelf's back can slide on it)."""
    total = 0.0
    for freq, amp, s in ((1.0, 0.58, 0), (2.3, 0.28, 1), (5.2, 0.14, 2)):
        nn = vnoise(x / width * freq, z / length * freq, np.full_like(x, 0.5 * freq), seed + s)
        total = total + amp * np.abs(2 * nn - 1)
    total = total ** 0.75
    lo, hi = np.percentile(total, 3), np.percentile(total, 95)
    h = np.clip((total - lo) / (hi - lo), 0, 1)
    return smoothstep(0.0, 0.8, h) ** 1.3


def build_plaque(w, h, seed, step=0.6):
    """Plate in print position: back face on z = 0, front plane at z = PLAQUE_T, furrows cut below it."""
    nx, ny = int(round(w / step)) + 1, int(round(h / step)) + 1
    xs = np.linspace(-w / 2, w / 2, nx)
    ys = np.linspace(0, h, ny)
    X, Y = np.meshgrid(xs, ys, indexing="xy")                       # (ny, nx)
    relief = furrows(X, Y, seed)
    strip = smoothstep(14.0, 22.0, np.abs(X))                       # flat strip for the rail and the stop
    border = smoothstep(2.0, 7.0, np.minimum(np.minimum(X + w / 2, w / 2 - X), np.minimum(Y, h - Y)))
    eff = 1 - strip * border * (1 - relief)
    zf = PLAQUE_T - FURROW * (1 - eff)
    verts = []
    front = np.zeros((ny, nx), int)
    back = np.zeros((ny, nx), int)
    for j in range(ny):
        for i in range(nx):
            front[j, i] = len(verts)
            verts.append((float(X[j, i]), float(Y[j, i]), float(zf[j, i])))
    for j in range(ny):
        for i in range(nx):
            back[j, i] = len(verts)
            verts.append((float(X[j, i]), float(Y[j, i]), 0.0))
    faces = []
    for j in range(ny - 1):
        for i in range(nx - 1):
            faces.append((front[j, i], front[j, i + 1], front[j + 1, i + 1], front[j + 1, i]))   # +z
            faces.append((back[j, i], back[j + 1, i], back[j + 1, i + 1], back[j, i + 1]))       # -z
    for i in range(nx - 1):
        faces.append((back[0, i], back[0, i + 1], front[0, i + 1], front[0, i]))                 # bottom edge, -y
        faces.append((back[ny - 1, i + 1], back[ny - 1, i], front[ny - 1, i], front[ny - 1, i + 1]))  # top edge, +y
    for j in range(ny - 1):
        faces.append((back[j + 1, 0], back[j, 0], front[j, 0], front[j + 1, 0]))                 # left, -x
        faces.append((back[j, nx - 1], back[j + 1, nx - 1], front[j + 1, nx - 1], front[j, nx - 1]))  # right, +x
    return orient(verts, faces), relief


# ---------------------------------------------------------------------------------------------------------
# Blender
# ---------------------------------------------------------------------------------------------------------
def to_blender(name, verts, faces):
    import bpy
    me = bpy.data.meshes.new(name)
    tri = []
    for f in faces:
        for k in range(1, len(f) - 1):
            tri.append((f[0], f[k], f[k + 1]))
    me.from_pydata([tuple(map(float, v)) for v in verts], [], tri)
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def boolean(target, cutter, op):
    import bpy
    mod = target.modifiers.new("b", "BOOLEAN")
    mod.operation = op
    mod.object = cutter
    mod.solver = "MANIFOLD"
    bpy.context.view_layer.objects.active = target
    bpy.ops.object.modifier_apply(modifier=mod.name)
    bpy.data.objects.remove(cutter)


def export(ob, path):
    import bpy
    bpy.ops.object.select_all(action="DESELECT")
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob
    bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True)
    print(f"wrote {path}: {len(ob.data.polygons)} faces")


def make_shelf(size, out, jitter=0.0):
    import bpy
    p = SIZES[size]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    ob = to_blender("shelf", *build_shelf(p["rx"], p["d"], p["seed"]))
    # dovetail groove through the whole thickness at the back
    gp = groove_polygon()
    boolean(ob, to_blender("groove", *prism(gp, -1.0, T0 + 1.0, lambda a, b, c: (a, b, c))), "DIFFERENCE")
    # card slot
    y0, y1, depth = SLIT
    half = 0.5 * p["rx"] * 0.93
    slit = [(-half, y0), (half, y0), (half, y1), (-half, y1)]
    boolean(ob, to_blender("slit", *prism(slit, -1.0, depth, lambda a, b, c: (a, b, c))), "DIFFERENCE")
    # engraved growth rings in the top face (the bed side); 0.5 mm deep, the last two a bit deeper
    radii = ring_radii(p["seed"])
    for k, r in enumerate(radii):
        w = 0.4
        r = r + jitter * 0.01 * (k % 3)
        rxo, do_ = p["rx"] * r + w, p["d"] * r + w
        rxi, di = p["rx"] * r - w, p["d"] * r - w
        depth_r = 0.9 if k in (3, 8, 12) else 0.5
        boolean(ob, to_blender("ring", *ring_prism(rxo, do_, rxi, di, -1.0, depth_r)), "DIFFERENCE")
    export(ob, out)


def corner_poly(cx, cy, sx, uy, rc, n=18):
    """The piece to cut off a rectangle corner at (cx, cy) to round it with radius rc (sx: -1 left / +1 right, uy: +1 bottom / -1 top)."""
    ccx, ccy = cx - sx * rc, cy + uy * rc
    arc = [(ccx + rc * sx * np.cos(a), ccy - rc * uy * np.sin(a)) for a in np.linspace(0, np.pi / 2, n)]
    return [(cx + sx * 0.5, cy - uy * 0.5), (cx + sx * 0.5, cy + uy * rc)] + arc + [(cx - sx * rc, cy - uy * 0.5)]


def make_plaque(size, out, h=PLAQUE_H, jitter=0.0):
    import bpy
    p = SIZES[size]
    w = 2 * p["rx"]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    (verts, faces), _ = build_plaque(w, h, p["seed"] + 20)
    ob = to_blender("plaque", verts, faces)
    T = PLAQUE_T
    # rail: trapezoid in (x, z) of the print frame, extruded along y from the stop to the top edge
    rail = [(-NECK + 0.5, T - 0.5), (NECK - 0.5, T - 0.5), (HEAD, T + RAIL_H), (-HEAD, T + RAIL_H)]
    boolean(ob, to_blender("rail", *prism(rail, Z_STOP, h, lambda a, b, c: (a, c, b))), "UNION")
    stop = [(-STOP_W / 2, T - 0.5), (STOP_W / 2, T - 0.5), (STOP_W / 2, T + STOP_OUT), (-STOP_W / 2, T + STOP_OUT)]
    boolean(ob, to_blender("stop", *prism(stop, Z_STOP - STOP_Z, Z_STOP, lambda a, b, c: (a, c, b))), "UNION")
    # rounded corners (r = 12)
    for sx in (-1, 1):
        for sy in (0, 1):
            boolean(ob, to_blender("corner", *prism(corner_poly(sx * w / 2, sy * h, sx, 1 if sy == 0 else -1, 12.0),
                                                    -1.0, T + 8.0, lambda a, b, c: (a, b, c))), "DIFFERENCE")
    # keyhole slots from the back: lip (3 mm) with a narrow slot, pocket (4 mm) for the screw head above it
    from shapely.geometry import Point, box
    from shapely.ops import unary_union
    kx = w / 2 - 22.0 + jitter
    zc, slot = 86.0 + 0.7 * jitter, 11.0
    for sx in (-kx, kx):
        lip = unary_union([Point(sx, zc).buffer(4.75, 48), box(sx - 2.3, zc, sx + 2.3, zc + slot)])
        pocket = unary_union([Point(sx, zc).buffer(5.25, 48), box(sx - 4.8, zc, sx + 4.8, zc + slot)])
        boolean(ob, to_blender("lip", *prism(list(lip.exterior.coords)[:-1], -1.0, 3.0, lambda a, b, c: (a, b, c))), "DIFFERENCE")
        boolean(ob, to_blender("pocket", *prism(list(pocket.exterior.coords)[:-1], 2.9, 7.0, lambda a, b, c: (a, b, c))), "DIFFERENCE")
    export(ob, out)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("what", choices=["shelf", "plaque"])
    ap.add_argument("out")
    ap.add_argument("--size", choices=sorted(SIZES), default="M")
    a = ap.parse_args(argv)
    import trimesh
    for jitter in (0.0, 0.013, 0.029, 0.047, 0.071):          # a booleans that lands exactly on a mesh vertex can leave a bad edge: nudge and retry
        (make_shelf if a.what == "shelf" else make_plaque)(a.size, a.out, jitter=jitter)
        m = trimesh.load(a.out, force="mesh")
        if m.is_watertight and m.is_winding_consistent:
            break
        print(f"not watertight with jitter {jitter}, retrying")
    else:
        sys.exit("could not make a watertight mesh")


if __name__ == "__main__":
    main()
