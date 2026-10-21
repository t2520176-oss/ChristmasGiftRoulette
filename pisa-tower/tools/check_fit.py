#!/usr/bin/env python3
"""Fit check of the ASSEMBLED ornament: pieces of different pieces must not occupy the same space
(they are glued / pushed together, so touching is fine, overlapping is not).

  python3 check_fit.py ../stl/assembled [--min-mm3 0.5]
"""
import argparse
import itertools
import json
import os

import manifold3d as m3d
import numpy as np
import trimesh


def to_manifold(mesh):
    return m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(mesh.vertices, dtype=np.float32), tri_verts=np.asarray(mesh.faces, dtype=np.uint32)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--min-mm3", type=float, default=0.5)
    a = ap.parse_args()
    items = []
    for f in sorted(os.listdir(a.dir)):
        if f.endswith(".stl"):
            m = trimesh.load(os.path.join(a.dir, f), force="mesh")
            items.append((f[:-4], f.split("__")[0], m, to_manifold(m)))
    bad = []
    for (na, pa, ma, xa), (nb, pb, mb, xb) in itertools.combinations(items, 2):
        if pa == pb:
            continue
        if np.any(ma.bounds[1] <= mb.bounds[0]) or np.any(mb.bounds[1] <= ma.bounds[0]):
            continue
        v = (xa ^ xb).volume()          # manifold3d: a ^ b is the intersection
        if v > a.min_mm3:
            bad.append((na, nb, round(float(v), 2)))
    for t in sorted(bad, key=lambda t: -t[2]):
        print(f"overlap {t[0]:22s} x {t[1]:22s} {t[2]:8.2f} mm3")
    print(f"{len(items)} meshes, {len(bad)} overlapping pairs above {a.min_mm3} mm3")


if __name__ == "__main__":
    main()
