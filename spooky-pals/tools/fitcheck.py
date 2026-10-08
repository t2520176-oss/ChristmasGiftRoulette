#!/usr/bin/env python3
"""Fit check for one assembled set (STLs exported with --layout assembled).

Reports pairwise interference volumes (mm3) between all parts and the smallest gap
between the mating pairs. Parts that touch on a flat face (hat on crown, pet on deck) report ~0.

  python3 fitcheck.py <dir with pet.stl hat.stl cape.stl charm.stl base.stl ...>
"""
import itertools
import os
import sys

import numpy as np
import trimesh

try:
    import manifold3d as m3d
except Exception:      # pragma: no cover
    m3d = None


def to_manifold(mesh):
    return m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(mesh.vertices, dtype=np.float32),
                                 tri_verts=np.asarray(mesh.faces, dtype=np.uint32)))


def main(d):
    names = [f[:-4] for f in sorted(os.listdir(d)) if f.endswith(".stl")]
    meshes = {n: trimesh.load(os.path.join(d, n + ".stl"), force="mesh") for n in names}
    # pieces that are meant to be one body in multicolor mode are merged for the comparison
    groups = {}
    for n in names:
        g = {"marks": "pet", "face": "pet", "shine": "pet", "trim": "hat", "deco": "base", "text": "base"}.get(n, n)
        groups.setdefault(g, []).append(n)
    print(f"parts: {names}")
    worst = 0.0
    for a, b in itertools.combinations(groups, 2):
        # merge each group (no boolean needed: they do not overlap by construction)
        ma = trimesh.util.concatenate([meshes[n] for n in groups[a]])
        mb = trimesh.util.concatenate([meshes[n] for n in groups[b]])
        vol = None
        if m3d is not None:
            inter = to_manifold(ma) ^ to_manifold(mb)     # intersection
            vol = inter.volume()
        # smallest distance between the two surfaces (sampled)
        pts = ma.sample(4000)
        dist = trimesh.proximity.closest_point(mb, pts)[1]
        flag = "" if (vol or 0) < 0.5 else "   <-- INTERFERENCE"
        worst = max(worst, vol or 0)
        print(f"  {a:6s} x {b:6s}  interference {vol if vol is not None else float('nan'):8.3f} mm3   "
              f"min gap {dist.min():5.2f} mm{flag}")
    print("worst interference: %.3f mm3" % worst)


if __name__ == "__main__":
    main(sys.argv[1])
