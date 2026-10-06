#!/usr/bin/env python3
"""Printability check for print-in-place STL files.

Usage: check_mesh.py model.stl [--limit 45] [--tol 1.5] [--min-gap 0.25]

Reports, per connected body: watertightness, volume, bounding box.
Then checks:
  * the smallest gap between separate bodies (moving parts must not be fused)
  * overhang area: downward faces steeper than --limit degrees from vertical
    (plus --tol degrees to absorb polygon approximation of 45 degree cones),
    ignoring faces lying on the bed (z ~ 0). Listed per 5 mm height band.
Exit code is 1 if a body is not watertight or two bodies are closer than --min-gap.
"""
import argparse
import sys

import numpy as np
import trimesh


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("stl")
    ap.add_argument("--limit", type=float, default=45.0, help="max overhang angle from vertical (deg)")
    ap.add_argument("--tol", type=float, default=1.5, help="extra tolerance on the overhang angle (deg)")
    ap.add_argument("--min-gap", type=float, default=0.25, help="smallest allowed gap between bodies (mm)")
    args = ap.parse_args()

    mesh = trimesh.load(args.stl, force="mesh")
    parts = mesh.split(only_watertight=False)
    bodies = [b for b in parts if abs(b.volume) > 1.0]
    junk = len(parts) - len(bodies)
    print(f"{args.stl}: {len(mesh.faces)} faces, {len(bodies)} bodies"
          + (f" (+{junk} negligible fragments)" if junk else ""))
    ok = True

    for i, b in enumerate(bodies):
        lo, hi = b.bounds
        print(f"  body {i}: watertight={b.is_watertight} volume={b.volume:.0f} mm3 "
              f"size={hi[0]-lo[0]:.1f} x {hi[1]-lo[1]:.1f} x {hi[2]-lo[2]:.1f} mm")
        if not b.is_watertight or b.volume <= 0:
            ok = False
    if junk:
        print(f"  WARN: {junk} negligible fragments (slivers); slicers ignore them but the mesh is not clean")

    worst = np.inf
    for i in range(len(bodies)):
        for j in range(i + 1, len(bodies)):
            pts = bodies[i].sample(20000)
            _, d, _ = trimesh.proximity.closest_point(bodies[j], pts)
            gap = d.min()
            worst = min(worst, gap)
            print(f"  gap body {i} <-> body {j}: {gap:.3f} mm")
    if worst < args.min_gap:
        print(f"  FAIL: gap {worst:.3f} mm < {args.min_gap} mm (parts would fuse)")
        ok = False

    # a face needs support if its normal points down more than sin(limit)
    nz = mesh.face_normals[:, 2]
    zc = mesh.triangles_center[:, 2]
    thresh = -np.sin(np.radians(args.limit + args.tol))
    bad = (nz < thresh) & (zc > 0.05)
    area = mesh.area_faces
    total = area[bad].sum()
    print(f"  overhang (> {args.limit:.0f}+{args.tol:g} deg from vertical, excluding bed): {total:.1f} mm2 "
          f"of {area.sum():.0f} mm2 total")
    if total > 0:
        zb = (zc[bad] // 5).astype(int)
        for band in sorted(set(zb)):
            sel = zb == band
            print(f"    z {band*5:>4}-{band*5+5:<4} mm: {area[bad][sel].sum():7.1f} mm2")

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
