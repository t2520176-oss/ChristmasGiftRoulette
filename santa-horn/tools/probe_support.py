#!/usr/bin/env python3
"""Would a slicer add support?  (PrusaSlicer CLI with its own auto-support, 45 degree threshold, one filament)

  python3 probe_support.py ../stl/print [--ini ../slicer/prusaslicer_4color_256.ini] [--keep]

All colour regions are merged into one mesh (the geometry is what matters), sliced with `--support-material-auto`, and the support
extrusion is summed and clustered by height.  "no support" = the slicer found nothing that it had to prop up.
"""
import argparse
import os
import re
import subprocess
import sys
import tempfile
from collections import defaultdict

import trimesh


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("print_dir")
    ap.add_argument("--ini", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "slicer", "prusaslicer_4color_256.ini"))
    ap.add_argument("--threshold", type=int, default=45)
    ap.add_argument("--keep", action="store_true", help="keep the merged STL and the G-code next to the print dir")
    a = ap.parse_args()
    files = sorted(f for f in os.listdir(a.print_dir) if f.endswith(".stl"))
    meshes = [trimesh.load(os.path.join(a.print_dir, f), force="mesh") for f in files]
    merged = trimesh.util.concatenate(meshes)
    tmp = tempfile.mkdtemp(prefix="probe_")
    stl = os.path.join(tmp, "all.stl")
    merged.export(stl)
    gcode = os.path.join(tmp, "all.gcode")
    cmd = ["prusa-slicer", "--export-gcode", "--load", os.path.abspath(a.ini), "--support-material", "--support-material-auto",
           "--support-material-threshold", str(a.threshold), "--support-material-extruder", "0", "--support-material-interface-extruder", "0",
           "--center", "128,128", "--output", gcode, stl]
    p = subprocess.run(cmd, capture_output=True, text=True)
    if not os.path.exists(gcode):
        sys.exit("slicing failed: %s %s" % (p.stdout[-400:], p.stderr[-400:]))
    typ, z, x, y = None, 0.0, 0.0, 0.0
    tot = defaultdict(float)
    seg = defaultdict(list)
    for line in open(gcode):
        if line.startswith(";TYPE:"):
            typ = line[6:].strip()
            continue
        if line.startswith("G1") or line.startswith("G0"):
            mz = re.search(r" Z([-\d.]+)", line)
            if mz:
                z = float(mz.group(1))
            mx, my = re.search(r" X([-\d.]+)", line), re.search(r" Y([-\d.]+)", line)
            nx = float(mx.group(1)) if mx else x
            ny = float(my.group(1)) if my else y
            if typ and typ.startswith("Support") and " E" in line:
                me = re.search(r" E(-?[\d.]+)", line)
                if me and float(me.group(1)) > 0:
                    tot[typ] += float(me.group(1))
                    seg[round(z)].append((nx - 128, ny - 128))
            x, y = nx, ny
    txt = open(gcode).read()
    t = re.search(r"; estimated printing time \(normal mode\) = (.*)", txt)
    f = re.search(r"; filament used \[g\] = (.*)", txt)
    print("slicer time %s   filament g %s" % (t.group(1) if t else "?", f.group(1) if f else "?"))
    if not tot:
        print("auto-support: none")
    else:
        print("auto-support extrusion (mm of filament):", {k: round(v) for k, v in tot.items()})
        zs = sorted(seg)
        for z0 in range(0, int(zs[-1]) + 4, 4):
            pts = [q for zz in zs if z0 <= zz < z0 + 4 for q in seg[zz]]
            if pts:
                print("   z %3d-%3d  n=%4d  x[%.0f,%.0f] y[%.0f,%.0f]" % (z0, z0 + 4, len(pts), min(q[0] for q in pts), max(q[0] for q in pts), min(q[1] for q in pts), max(q[1] for q in pts)))
    if a.keep:
        out = os.path.abspath(os.path.join(a.print_dir, "..", "_probe"))
        os.makedirs(out, exist_ok=True)
        os.replace(stl, os.path.join(out, "all.stl"))
        os.replace(gcode, os.path.join(out, "all.gcode"))
        print("kept in", out)
    sys.exit(0 if not tot else 1)


if __name__ == "__main__":
    main()
