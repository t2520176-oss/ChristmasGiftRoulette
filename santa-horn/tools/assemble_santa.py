#!/usr/bin/env python3
"""Santa parts (made in Blender) -> the four printable colour regions, self-supporting.

  python3 assemble_santa.py --parts <build>/parts --out ../stl [--dz 0.25] [--angle 56] [--no-fill] [--reuse]

 1. the parts are united by priority  black > peach (nose, cheeks) > white > peach (face) > red   (a colour region never overlaps another one)
 2. the sound path (phone slot, chamber, tunnel, funnel) is cut out of every region, everything below the floor is cut off
 3. the figure is sliced into layers and rastered (one pixel = dz / tan(angle)): tools/selfsupport.py adds under every overhang the
    material that a slicer would need support for - a skirt that slopes at `angle` or more - and never fills the sound path
 4. every colour region is meshed from that layer stack (marching cubes, in slabs), decimated, and written as  santa__<role>.stl
    into  <out>/assembled  and  <out>/print  (the figure prints as it stands, no support)
"""
import argparse
import fnmatch
import json
import os
import sys
import time

import manifold3d as m3d
import numpy as np
import trimesh
from PIL import Image, ImageDraw
from scipy import ndimage
from skimage import measure

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import selfsupport  # noqa: E402

ROLES = ["red", "white", "peach", "black"]
# (role, glob of part names) strongest first
ORDER = [("black", "black__*"), ("peach", "peach__feat*"), ("white", "white__*"), ("peach", "peach__face*"), ("red", "red__*")]


def to_m(mesh):
    man = m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(mesh.vertices, dtype=np.float32), tri_verts=np.asarray(mesh.faces, dtype=np.uint32)))
    if man.status() != m3d.Error.NoError:
        raise SystemExit("not a manifold: %s (%s)" % (man.status(), mesh))
    return man


def to_t(man):
    me = man.to_mesh()
    return trimesh.Trimesh(np.asarray(me.vert_properties)[:, :3].astype(float), np.asarray(me.tri_verts), process=False)


def load_dir(d):
    out = {}
    for f in sorted(os.listdir(d)):
        if f.endswith(".stl"):
            m = trimesh.load(os.path.join(d, f), force="mesh")
            m.merge_vertices()
            out[f[:-4]] = m
    return out


def union_all(ms):
    ms = list(ms)
    while len(ms) > 1:
        ms = [ms[i] + ms[i + 1] if i + 1 < len(ms) else ms[i] for i in range(0, len(ms), 2)]
    return ms[0]


def raster(man, z, x0, y0, dx, shape, ss=2):
    """bool image of the cut of a manifold at height z; painted ss times finer and averaged (outlines largest first: counter clockwise = material)"""
    H, W = shape
    img = Image.new("L", (W * ss, H * ss), 0)
    dr = ImageDraw.Draw(img)
    rings = []
    for ring in man.slice(float(z)).to_polygons():
        ring = np.asarray(ring, dtype=float)
        if len(ring) < 3:
            continue
        x, y = ((ring[:, 0] - x0) / dx + 0.5) * ss - 0.5, ((ring[:, 1] - y0) / dx + 0.5) * ss - 0.5
        area = 0.5 * float(np.sum(x * np.roll(y, -1) - np.roll(x, -1) * y))
        rings.append((abs(area), area > 0, list(zip(x.tolist(), y.tolist()))))
    for _, ccw, pts in sorted(rings, key=lambda t: -t[0]):
        dr.polygon(pts, fill=1 if ccw else 0)
    a = np.asarray(img, dtype=np.float32).reshape(H, ss, W, ss).mean(axis=(1, 3))
    return a >= 0.5


def mesh_role(P, r, x0, y0, dx, dz, sig_mm=0.1, slab=40, halo=3, min_voxels=300):
    """closed triangle mesh of the voxels with label r (Gaussian smoothed marching cubes, cells in z slabs that share their data layer)"""
    nz, H, W = P.shape
    mask = P == r
    lab, n = ndimage.label(mask)                                     # drop crumbs
    if n > 1:
        sizes = ndimage.sum(mask, lab, index=np.arange(1, n + 1))
        small = np.zeros(n + 1, bool)
        small[1:] = sizes < min_voxels
        mask[small[lab]] = False
    del lab
    sx, sz = sig_mm / dx, sig_mm / dz
    V, F, off = [], [], 0
    for c0 in range(-1, nz, slab):
        c1 = min(c0 + slab, nz)
        g0, g1 = c0 - halo, c1 + halo
        blk = np.zeros((g1 - g0 + 1, H, W), np.float32)
        a, b = max(g0, 0), min(g1, nz - 1)
        if a <= b:
            blk[a - g0:b - g0 + 1] = mask[a:b + 1]
        if not blk.any():
            continue
        sm = ndimage.gaussian_filter(blk, sigma=(sz, sx, sx))
        sub = sm[c0 - g0:c1 - g0 + 1]
        if sub.max() < 0.5 or sub.min() > 0.5:
            continue
        v, f, _, _ = measure.marching_cubes(sub, 0.5, allow_degenerate=False)
        xyz = np.stack([x0 + v[:, 2] * dx, y0 + v[:, 1] * dx, (c0 + v[:, 0] + 0.5) * dz], 1)
        V.append(xyz)
        F.append(f + off)
        off += len(xyz)
    if not V:
        return None
    m = trimesh.Trimesh(np.vstack(V), np.vstack(F), process=False)
    m.merge_vertices(digits_vertex=4)
    if m.volume < 0:
        m.invert()
    return m


def decimate(m, target):
    if target <= 0 or len(m.faces) <= target:
        return m
    try:
        import fast_simplification
    except ImportError:
        print("   (fast_simplification missing: not decimated)")
        return m
    v, f = fast_simplification.simplify(np.asarray(m.vertices, np.float32), np.asarray(m.faces, np.int32), target_reduction=1.0 - target / len(m.faces))
    out = trimesh.Trimesh(v.astype(float), f, process=False)
    out.merge_vertices(digits_vertex=4)
    try:                                                              # manifold3d splits the pinched (4-face) edges that decimation leaves
        out = to_t(to_m(out))
        out.merge_vertices(digits_vertex=4)
    except SystemExit:
        pass
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--parts", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--dz", type=float, default=0.25, help="layer height of the raster (mm)")
    ap.add_argument("--angle", type=float, default=56.0, help="raster pitch: one pixel = dz / tan(angle) (the skirts are steeper than that by --step)")
    ap.add_argument("--step", type=float, default=0.7, help="how far (in raster pixels) a layer may reach beyond the layer below; with the 56 degree pixel 0.7 = a 65 degree skirt (1.0 = 56 degrees, too tight for a slicer)")
    ap.add_argument("--no-fill", action="store_true")
    ap.add_argument("--reuse", action="store_true", help="re-use the layer stack of the last run (only meshing is redone)")
    ap.add_argument("--tris", type=int, default=1200000, help="triangles of all regions together after decimation (0 = keep everything)")
    ap.add_argument("--sigma", type=float, default=0.1, help="smoothing of the voxel surface (mm)")
    a = ap.parse_args()
    t0 = time.time()

    def tick(msg):
        print("[%4.0fs] %s" % (time.time() - t0, msg), flush=True)

    os.makedirs(a.out, exist_ok=True)
    cache = os.path.join(a.out, "_layers_cache.npz")
    dz = a.dz
    dx = dz / np.tan(np.radians(a.angle))
    if a.reuse and os.path.exists(cache):
        z_ = np.load(cache)
        P, x0, y0, roles = z_["P"], float(z_["x0"]), float(z_["y0"]), [str(r) for r in z_["roles"]]
        tick("layer stack loaded from the cache: %s" % (P.shape,))
    else:
        parts = load_dir(a.parts)
        cav_names = [n for n in parts if n.startswith("cav__")]
        body_names = [n for n in parts if not n.startswith("cav__")]
        tick("loaded %d parts, %d cavities" % (len(body_names), len(cav_names)))
        man = {n: to_m(parts[n]) for n in parts}
        solids = {r: None for r in ROLES}
        taken = None
        for role, pat in ORDER:
            names = [n for n in body_names if fnmatch.fnmatch(n, pat)]
            if not names:
                continue
            u = union_all([man[n] for n in names])
            excl = u if taken is None else u - taken
            solids[role] = excl if solids[role] is None else solids[role] + excl
            taken = u if taken is None else taken + u
        cav = union_all([man[n] for n in cav_names]) if cav_names else None
        for r in ROLES:
            if solids[r] is not None:
                solids[r] = solids[r].trim_by_plane([0, 0, 1], 0.0)     # the figure stands on the floor
        roles = [r for r in ROLES if solids[r] is not None and solids[r].volume() > 1]
        for r in roles:
            print("   %-6s %8.1f cm3" % (r, solids[r].volume() / 1000))
        tick("regions united (the sound path is cut out of the finished meshes, so that its roofs stay exact)")
        allm = None
        for r in roles:
            allm = solids[r] if allm is None else allm + solids[r]
        bb = allm.bounding_box()
        pad = 4.0
        x0, y0 = bb[0] - pad, bb[1] - pad
        W, H = int(np.ceil((bb[3] - bb[0] + 2 * pad) / dx)), int(np.ceil((bb[4] - bb[1] + 2 * pad) / dx))
        nz = int(np.ceil(bb[5] / dz)) + 1
        tick("raster %d x %d px x %d layers (pixel %.3f mm, layer %.2f mm = %.0f deg)" % (W, H, nz, dx, dz, a.angle))
        sh = (H, W)
        P = np.zeros((nz, H, W), np.uint8)
        C = np.zeros((nz, H, W), bool)                  # (nothing is protected: the sound path is solid here, it is cut out afterwards)
        for k in range(nz):
            z = (k + 0.5) * dz
            for i, r in enumerate(roles):
                P[k][raster(solids[r], z, x0, y0, dx, sh)] = i + 1
            if k % 100 == 0:
                tick("   rastered layer %d/%d" % (k, nz))
        tick("raster done: %.1f cm3 of material" % ((P > 0).sum() * dx * dx * dz / 1000))
        if not a.no_fill:
            n = selfsupport.close_overhangs(P, C, step=a.step)
            tick("closure done: %.1f cm3 added" % (n * dx * dx * dz / 1000))
        del C
        np.savez_compressed(cache, P=P, x0=x0, y0=y0, roles=np.array(roles))
        tick("layer stack cached")

    cav_parts = [f for f in sorted(os.listdir(a.parts)) if f.startswith("cav__") and f.endswith(".stl")]
    cav = None
    if cav_parts:
        cm = [trimesh.load(os.path.join(a.parts, f), force="mesh") for f in cav_parts]
        for c in cm:
            c.merge_vertices()
        cav = union_all([to_m(c) for c in cm])
    meshes = {}
    for i, r in enumerate(roles):
        m = mesh_role(P, i + 1, x0, y0, dx, dz, a.sigma)
        if m is None:
            continue
        if cav is not None:                                           # the sound path: exact cones and planes, not voxels
            m = to_t(to_m(m) - cav)
            m.merge_vertices(digits_vertex=4)
        meshes[r] = m
        tick("   %-6s meshed: %d triangles, %.1f cm3, watertight %s" % (r, len(m.faces), m.volume / 1000, m.is_watertight))
    total = sum(len(m.faces) for m in meshes.values())
    for r, m in list(meshes.items()):
        tgt = int(a.tris * len(m.faces) / total) if a.tris else 0
        meshes[r] = decimate(m, tgt)
        print("   %-6s %d -> %d triangles, watertight %s" % (r, len(m.faces), len(meshes[r].faces), meshes[r].is_watertight))
    for sub in ("assembled", "print"):
        os.makedirs(os.path.join(a.out, sub), exist_ok=True)
        for r, m in meshes.items():
            m.export(os.path.join(a.out, sub, "santa__%s.stl" % r))
        json.dump([{"piece": "santa", "roles": list(meshes)}], open(os.path.join(a.out, sub, "manifest.json"), "w"), indent=1)
        json.dump({"family": "SANTA", "year": 2026, "members": ["santa"], "names": ["santa"], "layout": sub}, open(os.path.join(a.out, sub, "config.json"), "w"), indent=1)
    tick("done")


if __name__ == "__main__":
    main()
