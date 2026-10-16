#!/usr/bin/env python3
"""Check the moon/star motif of the back plate against what really stands in front of it.

  python3 check_sky.py ../stl/assembled [--arch 100 96] [--clear 2.5] [--edge 3.5]

Projects the assembled pieces (trees, house, figures, lanterns) onto the sky plane (as seen from the front)
and measures, for the real exported mesh of the "stars" role:
  * the smallest distance of any moon/star/sparkle to those silhouettes        (>= --clear)
  * the smallest distance to the edge of the arch                               (>= --edge)
  * the number of separate elements and the thinnest part (area lost by a 0.3 mm opening)
Exit code 1 if a limit is violated.
"""
import argparse
import glob
import os
import sys

import numpy as np
import trimesh
from shapely.geometry import Polygon
from shapely.ops import unary_union


def footprint(meshes, top):
    tris = []
    for m in meshes:
        for t in m.triangles:
            p = Polygon(t[:, [0, 2]] - [0, top])
            if p.area > 1e-6 and p.is_valid:
                tris.append(p)
    return unary_union(tris).buffer(0.02)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--arch", type=float, nargs=2, default=[100, 96], metavar=("A", "B"), help="half width and height of the arch")
    ap.add_argument("--top", type=float, default=27.0)
    ap.add_argument("--clear", type=float, default=2.5)
    ap.add_argument("--edge", type=float, default=3.5)
    a = ap.parse_args()

    groups = {}
    for f in sorted(glob.glob(os.path.join(a.dir, "*.stl"))):
        groups.setdefault(os.path.basename(f).split("__")[0], []).append(trimesh.load(f, force="mesh"))
    skip = ("base", "sky", "plaque", "tag", "holly")
    fg = unary_union([footprint(ms, a.top) for p, ms in groups.items() if not p.startswith(skip)])
    star_meshes = [trimesh.load(f, force="mesh") for f in glob.glob(os.path.join(a.dir, "sky__stars.stl"))]
    ink = footprint(star_meshes, a.top)
    th = np.linspace(0, np.pi, 721)
    arch = Polygon([(a.arch[0] * np.cos(t), a.arch[1] * np.sin(t)) for t in th])

    parts = list(ink.geoms) if ink.geom_type == "MultiPolygon" else [ink]
    d_fg = min(p.distance(fg) for p in parts)
    d_edge = min(p.distance(arch.exterior) for p in parts)
    opened = ink.buffer(-0.3, join_style=1).buffer(0.3, join_style=1)
    thin = ink.difference(opened).area
    print(f"elements: {len(parts)}   ink area {ink.area:.0f} mm2   area in parts thinner than 0.6 mm: {thin:.1f} mm2")
    print(f"distance to foreground silhouettes: {d_fg:.1f} mm   (limit {a.clear})")
    print(f"distance to the arch edge:          {d_edge:.1f} mm   (limit {a.edge})")
    bad = []
    if d_fg < a.clear:
        bad.append("an element is too close to the trees / house / figures")
    if d_edge < a.edge:
        bad.append("an element is too close to the arch edge")
    if not arch.contains(ink):
        bad.append("an element leaves the arch")
    for t in bad:
        print("PROBLEM:", t)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
