#!/usr/bin/env python3
"""Can the penguins walk through the door?   python3 check_door.py --print-dir ../stl/print --scad ../openscad/penguin_home.scad [--min-gap 1.5]

The door is the arch cut out of the igloo (BORE_W, BORE_H1 and SLOPE are read from the .scad); every penguin
is projected flat from the front (its print STL lies on its back, so the silhouette is the XY outline above the
tab) and the clearance to the walls and the roof is measured.  Exit code 1 if any gap is smaller than --min-gap.
"""
import argparse
import glob
import os
import re
import sys

import trimesh
from shapely import affinity
from shapely.geometry import LineString, Polygon, box
from shapely.ops import unary_union


def scad_num(text, name):
    m = re.search(r"\b%s\s*=\s*([-0-9.]+)" % name, text)
    if not m:
        raise SystemExit("cannot find %s in the .scad" % name)
    return float(m.group(1))


def silhouette(files):
    polys = []
    for f in files:
        m = trimesh.load(f, force="mesh")
        tri = m.vertices[m.faces][:, :, :2]
        for t in tri:
            p = Polygon(t)
            if p.area > 1e-9:
                polys.append(p)
    return unary_union(polys).buffer(0.001)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--print-dir", required=True)
    ap.add_argument("--scad", required=True)
    ap.add_argument("--min-gap", type=float, default=1.5)
    a = ap.parse_args()
    txt = open(a.scad).read()
    w, h1, slope = scad_num(txt, "BORE_W"), scad_num(txt, "BORE_H1"), scad_num(txt, "SLOPE")
    apex = h1 + w / slope
    door = Polygon([(-w, 0), (w, 0), (w, h1), (0, apex), (-w, h1)])
    walls = LineString([(-w, 0), (-w, h1), (0, apex), (w, h1), (w, 0)])
    print("door: %.1f wide, %.1f high (walls %.1f, pointed roof)" % (2 * w, apex, h1))
    bad = 0
    for p in sorted({os.path.basename(f).split("__")[0] for f in glob.glob(os.path.join(a.print_dir, "peng*__*.stl"))}):
        files = glob.glob(os.path.join(a.print_dir, p + "__*.stl"))
        sil = silhouette(files)
        # only what stands above the tab, and centred on x = 0
        sil = sil.intersection(box(-100, 0, 100, 200))
        x0, y0, x1, y1 = sil.bounds
        cx = (x0 + x1) / 2
        sil = affinity.translate(sil, -cx, 0)
        gap = walls.distance(sil) if door.contains(sil) else -walls.distance(sil)
        ok = gap >= a.min_gap
        bad += not ok
        print("  %-6s %.1f high x %.1f wide  ->  clearance %.1f mm  %s" % (p, y1, x1 - x0, gap, "OK" if ok else "TOO TIGHT"))
    print("RESULT:", "every penguin fits through the door" if not bad else "%d penguin(s) do not fit" % bad)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
