#!/usr/bin/env python3
"""Verify the Vein-Glow Lantern (entry #10): shade, base, how they fit, heat clearance and light.

    python lantern_check.py 10-vein-lantern [--led-d 38.5 --led-h 20 --base-h 12]

Reads stl/shade.stl and stl/base.stl (and field.npz if given with --field), then checks:
  - each part: one watertight body, overhang area
  - wall thickness of the shade measured on the mesh (rays along the surface normal) against the map
  - fit: the shade sits on the base without any interference, clearance of the centring lip
  - heat: gap between the LED body and the shade wall, and between the LED and the open top
  - light: brightness of the veins at the bottom and top of the shade (lamp_light.py model)
Exit code 1 if a check fails.
"""
import argparse
import os
import sys

import numpy as np
import trimesh

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lamp_light import brightness  # noqa: E402
from vein_field import profile  # noqa: E402


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--field", required=True, help="field.npz the shade was built from")
    ap.add_argument("--led-d", type=float, default=38.5)
    ap.add_argument("--led-h", type=float, default=20.0)
    ap.add_argument("--base-h", type=float, default=12.0, help="height of the base top (the shade seat)")
    ap.add_argument("--riser-h", type=float, default=17.0)
    ap.add_argument("--pocket", type=float, default=3.0)
    ap.add_argument("--led-emit", type=float, default=17.0)
    a = ap.parse_args()
    ok = True

    shade = trimesh.load(os.path.join(a.folder, "stl", "shade.stl"), force="mesh")
    base = trimesh.load(os.path.join(a.folder, "stl", "base.stl"), force="mesh")
    f = np.load(a.field)

    print("parts")
    for name, m in (("shade", shade), ("base", base)):
        bodies = [b for b in m.split(only_watertight=False) if abs(b.volume) > 1.0]
        nz, zc = m.face_normals[:, 2], m.triangles_center[:, 2]
        over = m.area_faces[(nz < -np.sin(np.radians(46.5))) & (zc > 0.05)].sum()
        good = len(bodies) == 1 and m.is_watertight
        ok &= good
        lo, hi = m.bounds
        print(f"  {name:5s} bodies {len(bodies)}, watertight {m.is_watertight}, size "
              f"{hi[0]-lo[0]:.0f} x {hi[1]-lo[1]:.0f} x {hi[2]-lo[2]:.0f} mm, volume {m.volume/1000:.1f} cm3, "
              f"overhang {over:.0f} mm2 of {m.area:.0f} ({100*over/m.area:.2f} %)  {'ok' if good else 'FAIL'}")

    # wall thickness on the mesh
    t, v = f["t"], f["v"]
    nv1, nu = t.shape
    h, r0, rmax, rtop = (float(f[k]) for k in ("height", "r_bottom", "r_max", "r_top"))
    r, dr = profile(v, h, r0, rmax, rtop)
    rng = np.random.default_rng(3)
    J, I = rng.integers(10, nv1 - 10, 1200), rng.integers(0, nu, 1200)
    th = 2 * np.pi * I / nu
    P = np.stack([r[J] * np.cos(th), r[J] * np.sin(th), v[J]], -1)
    n = np.stack([np.cos(th), np.sin(th), -dr[J]], -1)
    n /= np.linalg.norm(n, axis=1, keepdims=True)
    origins = P + n * 0.02
    locs, ray_i, _ = shade.ray.intersects_location(origins, -n, multiple_hits=True)
    first = {}
    for loc, ri in zip(locs, ray_i):
        d = np.linalg.norm(loc - origins[ri])
        if d > 0.05 and (ri not in first or d < first[ri]):
            first[ri] = d
    meas = np.array([first.get(i, np.nan) for i in range(len(J))]) - 0.02
    good_rays = ~np.isnan(meas)
    err = np.abs(meas[good_rays] - t[J, I][good_rays])
    good = good_rays.mean() > 0.99 and err.max() < 0.05
    ok &= good
    print(f"\nshade wall thickness on the mesh: {good_rays.sum()} rays, min {meas[good_rays].min():.2f} mm, "
          f"largest difference to the map {err.max():.3f} mm  {'ok' if good else 'FAIL'}")

    # fit: the shade sits on the base top. Only the bottom 14 mm of the shade can touch the base, so that slice
    # (closed with a cap) is tested; the whole 270k-face shade would need too much memory
    rim = shade.slice_plane([0, 0, 14.0], [0, 0, -1], cap=True)
    rim.apply_translation([0, 0, a.base_h])
    s2 = shade.copy()
    s2.apply_translation([0, 0, a.base_h])
    pts_rim = rim.sample(20000)
    pts_b = base.sample(40000)
    near = (np.hypot(pts_b[:, 0], pts_b[:, 1]) > 25) & (pts_b[:, 2] > a.base_h - 1.0)
    inside_b = int(base.contains(pts_rim).sum())
    inside_s = int(rim.contains(pts_b[near]).sum())
    lip_pts = pts_b[near & (pts_b[:, 2] > a.base_h + 0.5)]
    _, d_lip, _ = trimesh.proximity.closest_point(rim, lip_pts)
    good = inside_b == 0 and inside_s == 0 and d_lip.min() > 0.2
    ok &= good
    print(f"\nfit: shade-rim points inside the base {inside_b}, base points inside the shade rim {inside_s}, "
          f"lip to shade clearance {d_lip.min():.2f} mm  {'ok' if good else 'FAIL'}")

    # heat: LED body against the shade wall and the open top
    led_bottom = a.base_h + a.riser_h - a.pocket
    led_top = led_bottom + a.led_h
    rr = np.hypot(s2.vertices[:, 0], s2.vertices[:, 1])
    band = (s2.vertices[:, 2] > led_bottom) & (s2.vertices[:, 2] < led_top)
    wall_gap = rr[band].min() - a.led_d / 2
    top_gap = s2.bounds[1][2] - led_top
    good = wall_gap > 8 and top_gap > 40
    ok &= good
    print(f"heat: LED body {led_bottom:.0f}..{led_top:.0f} mm, nearest wall {wall_gap:.1f} mm away, "
          f"open top {top_gap:.0f} mm above it  {'ok' if good else 'TOO CLOSE'}")

    # light
    z_led = a.riser_h - a.pocket + a.led_emit
    print(f"\nlight (simple model, LED {z_led:.0f} mm above the shade bottom, post shades the wall below):")
    veins = t <= 1.0 + 1e-6
    rows = np.repeat(v[:, None], nu, 1)
    lo_m, hi_m = (rows > 20) & (rows < 45) & veins, (rows > 85) & (rows < 108) & veins
    for zz in (z_led - 9, z_led, z_led + 9):
        b = brightness(f, zz, occluder_r=a.led_d / 2 + 2.75, up_only=True)
        bl, bh = b[lo_m].mean(), b[hi_m].mean()
        print(f"  LED at {zz:3.0f} mm: bottom veins {bl*1e4:5.2f}, top veins {bh*1e4:5.2f}, top/bottom {bh/bl:4.2f}")

    print("\nRESULT:", "all checks passed" if ok else "CHECK FAILED")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
