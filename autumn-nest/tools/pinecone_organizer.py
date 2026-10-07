#!/usr/bin/env python3
"""Pinecone organizer (entry #2): a dome of scales with tilted holes laid out by the golden angle.

    python pinecone_organizer.py out.stl [--holes holes.json] [--seed N]
    blender -b -P pinecone_organizer.py -- out.stl

The scales sit on a Vogel spiral: hole k is at radius r0 + (R - r0) ((k - 0.5)/N)^p and angle k * 137.5077 degrees,
exactly like the scales of a pinecone. Every hole is tilted outwards in proportion to its radius, so tools and
pens fan out. The hole size follows the position in the spiral: pins and needles in the middle, pencils and big
markers on the outside. The body is a drum with a dome on top, built as a watertight surface of revolution with a
raised, rounded scale around every hole; Blender (Manifold booleans) cuts all holes at once. Nothing has an
overhang: the outside is a height field, the tilted holes lean less than 45 degrees from vertical and every
hole bottom faces up.
"""
import argparse
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bark_shell import smoothstep  # noqa: E402

GOLDEN = 137.50776405003785
R = 64.0              # radius of the drum and of the dome base
H_DRUM = 8.0          # height of the drum
H_DOME = 44.0         # height of the dome above the drum
Q_DOME = 1.8          # 2 = ellipse, 1 = cone: 1.7 is a rounded cone, like a pinecone
R_HOLES = 42.0        # radius of the outermost hole axis on the surface
SCALE_A = 1.8         # depth of the grooves between the scales
WALL = 2.0            # smallest wall between two holes at the surface and at the bottom
SIZES = [3.6, 6.5, 9.5, 13.5]     # hole diameters: pins and needles / thin pens and brushes / pencils and pens / big markers
WANT = {13.5: 4, 9.5: 12, 6.5: 14}   # how many holes of each bigger size; all the others are 3.6
N_HOLES = 42
TILT_MAX = 18.0       # degrees from vertical at the outermost hole
P_EXP = 0.70


def dome_z(r):
    """Height of the smooth top surface at radius r (without scale bumps)."""
    return H_DRUM + H_DOME * np.clip(1 - (r / R) ** Q_DOME, 0, 1) ** (1 / Q_DOME)


def axis_of(h):
    a = math.radians(h["tilt"])
    return np.array([math.sin(a) * math.cos(h["theta"]), math.sin(a) * math.sin(h["theta"]), -math.cos(a)])


def hole_gap(a, b, n=40):
    """Smallest wall between two holes along their whole length (axis segments of 36 mm from the surface)."""
    ta = np.linspace(0, 36.0, n)[:, None]
    pa = np.array([a["x"], a["y"], float(dome_z(a["r"]))]) + ta * axis_of(a)
    pb = np.array([b["x"], b["y"], float(dome_z(b["r"]))]) + ta * axis_of(b)
    d = np.linalg.norm(pa[:, None, :] - pb[None, :, :], axis=2).min()
    return d - (a["d"] + b["d"]) / 2


def layout():
    """Positions, sizes and tilts of the holes. Returns a list of dicts."""
    n = N_HOLES
    holes = []
    for k in range(1, n + 1):
        r = R_HOLES * ((k - 0.5) / n) ** P_EXP
        th = math.radians(k * GOLDEN)
        holes.append(dict(k=k, r=r, theta=th, x=r * math.cos(th), y=r * math.sin(th), d=SIZES[0],
                          tilt=TILT_MAX * (r / R_HOLES) ** 1.1))

    def conflict(i):
        for j, b in enumerate(holes):
            if j != i and hole_gap(holes[i], b) < WALL:
                return True
        return False

    order = sorted(range(n), key=lambda i: -holes[i]["r"])          # the outside first: the big holes go there
    for size in sorted(WANT, reverse=True):
        placed = 0
        for i in order:
            if placed >= WANT[size]:
                break
            if holes[i]["d"] != SIZES[0]:
                continue
            holes[i]["d"] = size
            if conflict(i):
                holes[i]["d"] = SIZES[0]
            else:
                placed += 1
    return holes


def top_z(x, y, holes):
    """Surface height: the smooth dome with the scales. Every hole owns a scale, a cell of the power diagram of the
    hole centres (weights = hole radii), separated from its neighbours by a rounded groove. A height field, so no
    overhang anywhere."""
    x, y = np.asarray(x, float), np.asarray(y, float)
    r = np.hypot(x, y)
    z = dome_z(r)
    # the scales go on beyond the last hole: ghost centres continue the spiral (no holes in them) and close the outer cells
    ghosts = []
    for k in range(N_HOLES + 1, N_HOLES + 90):
        gr = R_HOLES * ((k - 0.5) / N_HOLES) ** P_EXP
        gt = math.radians(k * GOLDEN)
        ghosts.append((gr * math.cos(gt), gr * math.sin(gt), 4.0))
    cx = np.array([h["x"] for h in holes] + [g[0] for g in ghosts])
    cy = np.array([h["y"] for h in holes] + [g[1] for g in ghosts])
    w = np.array([0.5 * h["d"] + 1.0 for h in holes] + [g[2] for g in ghosts])
    flat = (x.reshape(-1, 1) - cx[None, :]) ** 2 + (y.reshape(-1, 1) - cy[None, :]) ** 2 - w[None, :] ** 2   # power distances
    order = np.argsort(flat, axis=1)
    i1, i2 = order[:, 0], order[:, 1]
    d1 = flat[np.arange(len(i1)), i1]
    d2 = flat[np.arange(len(i1)), i2]
    sep = np.hypot(cx[i1] - cx[i2], cy[i1] - cy[i2])
    border = (d2 - d1) / (2 * np.maximum(sep, 1e-6))           # distance to the groove line
    groove = smoothstep(0.0, 1.8, border).reshape(x.shape)      # 0 in the groove, 1 on the scale
    fade = 1 - smoothstep(R - 10.0, R - 3.0, r)                 # the scales end before the rim
    return z - SCALE_A * (1 - groove) * fade


def fit_depths(holes, max_depth=36.0, min_depth=14.0):
    """Depth of every hole: as deep as max_depth, but the lower half of the hole keeps 2 mm of wall to the outside of the
    body and its bottom edge 2.4 mm (distance to the uncut surface, found with a k-d tree of its vertices)."""
    from scipy.spatial import cKDTree
    verts, _ = build_body(holes, n_t=140, n_phi=420)
    tree = cKDTree(np.array(verts))
    for h in holes:
        u = axis_of(h)
        ref = np.array([0, 0, 1.0]) if abs(u[2]) < 0.9 else np.array([1.0, 0, 0])
        e1 = np.cross(u, ref)
        e1 /= np.linalg.norm(e1)
        e2 = np.cross(u, e1)
        ztop = float(top_z(np.array(h["x"]), np.array(h["y"]), holes))
        p0 = np.array([h["x"], h["y"], ztop])
        r = 0.5 * h["d"]
        ang = np.linspace(0, 2 * np.pi, 16, endpoint=False)
        ring = np.array([np.cos(t) * e1 + np.sin(t) * e2 for t in ang]) * r
        depth = max_depth
        while depth > min_depth:
            lower = [p0 + t * depth * u + v for t in (0.5, 0.65, 0.8) for v in ring]
            bottom = [p0 + depth * u + v for v in ring]
            d_low = tree.query(np.array(lower))[0].min()
            d_bot = tree.query(np.array(bottom))[0].min()
            floor_ok = min(q[2] for q in bottom) >= 3.0
            if d_low >= 2.0 and d_bot >= 2.4 and floor_ok:
                break
            depth -= 0.5
        h["ztop"], h["depth"] = ztop, depth
        h["axis"] = u.tolist()
    return holes


# ---------------------------------------------------------------------------------------------------------
def build_body(holes, n_t=110, n_phi=360):
    t = np.linspace(0, np.pi / 2, n_t + 1)
    phi = np.linspace(0, 2 * np.pi, n_phi, endpoint=False)
    TT, PP = np.meshgrid(t, phi, indexing="ij")
    r = R * np.sin(TT)
    X, Y = r * np.cos(PP), r * np.sin(PP)
    Z = top_z(X, Y, holes)
    verts = [(0.0, 0.0, float(Z[0, 0]))]                  # apex
    ring = np.zeros((n_t + 1, n_phi), int)
    ring[0, :] = 0
    for i in range(1, n_t + 1):
        for j in range(n_phi):
            ring[i, j] = len(verts)
            verts.append((float(X[i, j]), float(Y[i, j]), float(Z[i, j])))
    rim_z = float(Z[-1, 0])
    # wall: ring at z = 1.5 (full radius) and the chamfered bottom ring at z = 0
    wall_top = n_t
    low = []
    for j in range(n_phi):
        low.append(len(verts))
        verts.append((R * math.cos(phi[j]), R * math.sin(phi[j]), 1.5))
    bot = []
    for j in range(n_phi):
        bot.append(len(verts))
        verts.append(((R - 1.5) * math.cos(phi[j]), (R - 1.5) * math.sin(phi[j]), 0.0))
    centre = len(verts)
    verts.append((0.0, 0.0, 0.0))
    faces = []
    for i in range(n_t):
        for j in range(n_phi):
            j2 = (j + 1) % n_phi
            if i == 0:
                faces.append((ring[0, 0], ring[1, j], ring[1, j2]))
            else:
                faces.append((ring[i, j], ring[i + 1, j], ring[i + 1, j2], ring[i, j2]))
    for j in range(n_phi):
        j2 = (j + 1) % n_phi
        faces.append((ring[n_t, j], low[j], low[j2], ring[n_t, j2]))     # drum wall
        faces.append((low[j], bot[j], bot[j2], low[j2]))                  # chamfer
        faces.append((centre, bot[j2], bot[j]))                           # bottom
    return verts, faces


def cylinder(p0, p1, r, n=48):
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    ax = p1 - p0
    ax /= np.linalg.norm(ax)
    ref = np.array([0, 0, 1.0]) if abs(ax[2]) < 0.9 else np.array([1.0, 0, 0])
    e1 = np.cross(ax, ref)
    e1 /= np.linalg.norm(e1)
    e2 = np.cross(ax, e1)
    th = np.linspace(0, 2 * np.pi, n, endpoint=False) + 0.07
    ring = [(np.cos(a) * e1 + np.sin(a) * e2) * r for a in th]
    verts = [p0 + v for v in ring] + [p1 + v for v in ring] + [p0, p1]
    faces = []
    for i in range(n):
        j = (i + 1) % n
        faces.append((i, j, n + j, n + i))
        faces.append((2 * n, j, i))
        faces.append((2 * n + 1, n + i, n + j))
    return verts, faces


def signed_volume(verts, faces):
    v = np.asarray(verts, float)
    tot = 0.0
    for f in faces:
        for k in range(1, len(f) - 1):
            tot += np.dot(v[f[0]], np.cross(v[f[k]], v[f[k + 1]])) / 6.0
    return tot


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


def make(out, holes_json=None):
    import bpy
    holes = fit_depths(layout())
    verts, faces = build_body(holes)
    if signed_volume(verts, faces) < 0:
        faces = [tuple(reversed(f)) for f in faces]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    body = to_blender("pinecone", verts, faces)
    cv, cf, off = [], [], 0
    for h in holes:
        u = np.array(h["axis"])
        p_s = np.array([h["x"], h["y"], h["ztop"]])
        v, f = cylinder(p_s - 5.0 * u, p_s + h["depth"] * u, h["d"] / 2)
        if signed_volume(v, f) < 0:
            f = [tuple(reversed(q)) for q in f]
        cv += v
        cf += [tuple(i + off for i in q) for q in f]
        off += len(v)
    cutter = to_blender("holes", cv, cf)
    mod = body.modifiers.new("b", "BOOLEAN")
    mod.operation = "DIFFERENCE"
    mod.object = cutter
    mod.solver = "MANIFOLD"
    bpy.context.view_layer.objects.active = body
    bpy.ops.object.modifier_apply(modifier=mod.name)
    bpy.data.objects.remove(cutter)
    bpy.ops.object.select_all(action="DESELECT")
    body.select_set(True)
    bpy.ops.wm.stl_export(filepath=out, export_selected_objects=True)
    print(f"wrote {out}: {len(body.data.polygons)} faces, {len(holes)} holes")
    if holes_json:
        with open(holes_json, "w") as f:
            json.dump(dict(R=R, H_DRUM=H_DRUM, H_DOME=H_DOME, sizes=SIZES, tilt_max=TILT_MAX, wall=WALL, holes=holes), f, indent=1)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("out")
    ap.add_argument("--holes", help="write the hole table (positions, sizes, tilts, depths) as JSON")
    a = ap.parse_args(argv)
    make(a.out, a.holes)


if __name__ == "__main__":
    main()
