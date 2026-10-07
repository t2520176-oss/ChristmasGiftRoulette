#!/usr/bin/env python3
"""Verify the Stump Planter (entry #8): shell, inner pot and dipstick, and how they work together.

    python planter_check.py 08-bark-planter [--pot-z 38 --soil-z 60]

Reads stl/shell.stl, stl/pot.stl, stl/dipstick.stl, stl/dipstick_asm.stl and checks
  - each part: one watertight body, overhang area (the dipstick in its flat print orientation)
  - the shell wall measured by ray casting: never thinner than the smooth wall; the overflow hole is open
    and its lower edge is the highest water level
  - reservoir volume up to that level, soil volume in the inner pot (section areas, integrated)
  - the inner pot hangs from the rim: slot between the pots, air gap between the highest water and the pot floor,
    no part of the pot inside the shell
  - wick and drain holes are through holes
  - the dipstick stands in the slot between the tabs with clearance and its MAX groove is at the overflow level
Exit code 1 if a check fails.
"""
import argparse
import os
import sys

import numpy as np
import trimesh
from shapely.geometry import Polygon


def section_polys(mesh, z):
    sec = mesh.section(plane_origin=[0, 0, z], plane_normal=[0, 0, 1])
    if sec is None:
        return []
    p2d, _ = sec.to_2D(to_2D=np.eye(4))
    return list(p2d.polygons_full)


def cavity_area(mesh, z):
    """Area enclosed by the innermost loop at height z (the hollow of a cup)."""
    polys = section_polys(mesh, z)
    best = 0.0
    for p in polys:
        for ring in p.interiors:
            best = max(best, Polygon(ring).area)
    return best


def simpson(f, a, b, n=40):
    xs = np.linspace(a, b, n + 1)
    ys = np.array([f(x) for x in xs])
    h = (b - a) / n
    return h / 3 * (ys[0] + ys[-1] + 4 * ys[1:-1:2].sum() + 2 * ys[2:-1:2].sum())


def first_hits(mesh, origins, direction):
    d = np.broadcast_to(np.asarray(direction, float), origins.shape)
    locs, ray_i, _ = mesh.ray.intersects_location(origins, d, multiple_hits=False)
    out = np.full(len(origins), np.nan)
    if len(ray_i) == 0:
        return out
    out[ray_i] = np.linalg.norm(locs - origins[ray_i], axis=1)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--pot-z", type=float, default=38.0, help="height of the pot's underside above the shell floor")
    ap.add_argument("--soil-z", type=float, default=60.0, help="soil level in the pot's own frame (rim is at 66)")
    ap.add_argument("--overflow-z", type=float, default=32.5)
    ap.add_argument("--overflow-d", type=float, default=5.0)
    ap.add_argument("--ri", type=float, default=47.0)
    ap.add_argument("--wall", type=float, default=3.2)
    ap.add_argument("--floor", type=float, default=3.0)
    a = ap.parse_args()
    ok = True
    L = lambda n: trimesh.load(os.path.join(a.folder, "stl", n + ".stl"), force="mesh")
    shell, pot, stick, stick_asm = L("shell"), L("pot"), L("dipstick"), L("dipstick_asm")
    max_water = a.overflow_z - a.overflow_d / 2

    print("parts")
    for name, m in (("shell", shell), ("pot", pot), ("dipstick", stick)):
        bodies = [b for b in m.split(only_watertight=False) if abs(b.volume) > 1.0]
        nz, zc = m.face_normals[:, 2], m.triangles_center[:, 2]
        over_m = (nz < -np.sin(np.radians(46.5))) & (zc > 0.05)
        over = m.area_faces[over_m].sum()
        good = len(bodies) == 1 and m.is_watertight
        ok &= good
        lo, hi = m.bounds
        print(f"  {name:9s} bodies {len(bodies)}, watertight {m.is_watertight}, size {hi[0]-lo[0]:.0f} x {hi[1]-lo[1]:.0f} x "
              f"{hi[2]-lo[2]:.0f} mm, volume {m.volume/1000:.1f} cm3, overhang {over:.1f} mm2  {'ok' if good else 'FAIL'}")
        if name == "shell" and over > 0:
            where = m.triangles_center[over_m]
            print(f"            the overhang is the roof of the {a.overflow_d:.0f} mm overflow hole: "
                  f"z {where[:,2].min():.1f}..{where[:,2].max():.1f} mm, a {a.overflow_d:.0f} mm bridge")

    # ---- shell wall ----------------------------------------------------------------
    rng = np.random.default_rng(5)
    n = 3000
    th = rng.uniform(0, 2 * np.pi, n)
    z = rng.uniform(8, 96, n)
    far = np.abs(((np.degrees(th) - 270 + 180) % 360) - 180) > 6        # keep away from the overflow hole
    th, z = th[far], z[far]
    o = np.stack([(a.ri + 0.02) * np.cos(th), (a.ri + 0.02) * np.sin(th), z], -1)
    dirs = np.stack([np.cos(th), np.sin(th), np.zeros_like(th)], -1)
    locs, ray_i, _ = shell.ray.intersects_location(o, dirs, multiple_hits=False)
    thick = np.linalg.norm(locs - o[ray_i], axis=1) + 0.02
    good = len(thick) > 0.99 * len(th) and thick.min() >= a.wall - 0.02
    ok &= good
    print(f"\nshell wall: {len(thick)} rays, thinnest {thick.min():.2f} mm, thickest {thick.max():.2f} mm (smooth wall {a.wall} mm + bark relief)  "
          f"{'ok' if good else 'FAIL'}")

    # the floor
    o = np.stack([rng.uniform(-40, 40, 300), rng.uniform(-40, 40, 300), np.full(300, a.floor - 0.02)], -1)
    inside = np.hypot(o[:, 0], o[:, 1]) < 44
    o = o[inside]
    d = first_hits(shell, o, [0, 0, -1]) + 0.02
    good = np.nanmin(d) >= a.floor - 0.03
    ok &= good
    print(f"shell floor: {np.nanmin(d):.2f} mm  {'ok' if good else 'FAIL'}")

    # overflow hole: ray along -y at its centre must leave, and the hole edges must be where they should be
    def blocked(zz, xx=0.0):
        h = first_hits(shell, np.array([[xx, -30.0, zz]]), [0, -1, 0])
        return not np.isnan(h[0])
    centre_open = not blocked(a.overflow_z)
    lower_edge = [zz for zz in np.arange(a.overflow_z - 4, a.overflow_z, 0.05) if not blocked(zz)]
    top_edge = [zz for zz in np.arange(a.overflow_z, a.overflow_z + 4, 0.05) if not blocked(zz)]
    low_e, up_e = min(lower_edge), max(top_edge)
    good = centre_open and abs(low_e - max_water) < 0.2 and abs(up_e - (a.overflow_z + a.overflow_d / 2)) < 0.2
    ok &= good
    print(f"overflow hole: open {centre_open}, from z = {low_e:.2f} to {up_e:.2f} mm; the water cannot rise above {low_e:.1f} mm  "
          f"{'ok' if good else 'FAIL'}")

    # ---- volumes ------------------------------------------------------------------
    v_res = simpson(lambda zz: cavity_area(shell, zz), a.floor + 0.01, max_water) / 1000
    soil = simpson(lambda zz: cavity_area(pot, zz), 3.01, a.soil_z) / 1000
    print(f"\nreservoir up to the overflow ({max_water:.1f} mm): {v_res:.0f} mL; soil in the pot up to {a.soil_z:.0f} mm: {soil:.0f} mL")

    # ---- the pot hangs from the rim ------------------------------------------------
    lo_pot = pot.vertices[pot.vertices[:, 2] < 54]
    pot_r = np.hypot(lo_pot[:, 0], lo_pot[:, 1]).max()
    slot = a.ri - pot_r
    air = a.pot_z - max_water
    shell_top = shell.bounds[1][2]
    # seat: the cone must pass through the rim edge at the height of the shell top
    # tab points compared to the rim: radius at the height of the shell top
    tab_v = pot.vertices[(pot.vertices[:, 2] > 55)]
    rr = np.hypot(tab_v[:, 0], tab_v[:, 1])
    # cone: radius = z + k; at the shell top height (pot frame z = shell_top - pot_z) it must be >= ri
    k = np.median(rr[(rr > 44) & (rr < 52)] - tab_v[(rr > 44) & (rr < 52)][:, 2]) if len(rr) else np.nan
    seat_r = (shell_top - a.pot_z) + k
    good = slot >= 4.0 and air >= 5.0 and abs(seat_r - a.ri) < 0.3
    ok &= good
    print(f"inner pot: body radius {pot_r:.1f} mm, slot to the shell wall {slot:.1f} mm; pot floor {a.pot_z:.1f} mm above the shell floor = "
          f"{air:.1f} mm above the highest water; cone seat radius at the rim {seat_r:.2f} mm (rim edge {a.ri:.1f})  {'ok' if good else 'FAIL'}")

    # the pot in place against the real shell: points of the pot below the rim must be clear of the shell
    p3 = pot.copy()
    p3.apply_translation([0, 0, a.pot_z])
    sub = p3.sample(5000)
    sub = sub[sub[:, 2] < a.pot_z + 54]                              # body and floor; above this the tabs sit on the rim
    _, dist, _ = trimesh.proximity.closest_point(shell, sub)
    good = dist.min() > 4.0
    ok &= good
    print(f"            {len(sub)} sampled points of the pot body and floor to the shell: closest {dist.min():.1f} mm  {'ok' if good else 'FAIL'}")

    # wick and drain holes are through holes
    def thru(x, y):
        d1 = first_hits(pot, np.array([[x, y, -5.0]]), [0, 0, 1])[0]
        return np.isnan(d1) or d1 > 20
    holes = []
    for i in range(3):
        ang = np.radians(i * 120 + 30)
        holes.append(thru(15 * np.cos(ang), 15 * np.sin(ang)))
    for i in range(6):
        ang = np.radians(i * 60)
        holes.append(thru(26 * np.cos(ang), 26 * np.sin(ang)))
    good = all(holes)
    ok &= good
    print(f"wick holes (3) and drain holes (6) open: {sum(holes)} of {len(holes)}  {'ok' if good else 'FAIL'}")

    # ---- dipstick -------------------------------------------------------------------
    sv = stick_asm.vertices
    r_s = np.hypot(sv[:, 0], sv[:, 1])
    ang = np.degrees(np.arctan2(sv[:, 1], sv[:, 0])) % 360
    stem = sv[:, 2] < 100
    p4 = pot.copy()
    p4.apply_translation([0, 0, a.pot_z])
    pts_s = stick_asm.sample(3000)
    _, d_pot, _ = trimesh.proximity.closest_point(p4, pts_s)
    _, d_shell, _ = trimesh.proximity.closest_point(shell, pts_s[pts_s[:, 2] > a.floor + 3])   # the tip stands 0.4 mm above the floor
    tip = sv[:, 2].min()
    good = d_pot.min() > 0.8 and d_shell.min() > 0.8 and abs(tip - (a.floor + 0.4)) < 0.05 and 143 < ang[stem].min() and ang[stem].max() < 157
    ok &= good
    print(f"\ndipstick standing in the slot at {ang[stem].mean():.0f} degrees: radius {r_s[stem].min():.1f} to {r_s[stem].max():.1f} mm, "
          f"closest to the pot {d_pot.min():.2f} mm, to the shell wall {d_shell.min():.2f} mm, tip {tip:.1f} mm above the floor, "
          f"top {sv[:,2].max():.0f} mm  {'ok' if good else 'FAIL'}")
    # the groove for MAX: a full-width cut at the right height
    # ray down onto the top face of the flat stick at x positions; the groove floor is 0.7 mm lower
    xs = np.arange(2.0, 60.0, 0.05)
    pts_top = np.stack([xs, np.full_like(xs, -1.0), np.full_like(xs, 10.0)], -1)    # y = -1: the half-width LOW line is cut here too
    h = first_hits(stick, pts_top, [0, 0, -1])
    top = 10.0 - np.nanmin(h)
    deep = xs[(10.0 - h) < top - 0.5]
    # group into runs
    runs = np.split(deep, np.where(np.diff(deep) > 0.2)[0] + 1) if len(deep) else []
    centres = [r.mean() + 0.4 + a.floor for r in runs]          # x -> height above the shell floor
    max_c = [c for c in centres if abs(c - max_water) < 1.5]
    good = bool(max_c) and abs(max_c[0] - max_water) < 0.3
    ok &= good
    print("dipstick grooves at heights (mm above the floor): " + ", ".join(f"{c:.1f}" for c in centres)
          + f"; MAX should be {max_water:.1f}  {'ok' if good else 'FAIL'}")

    print("\nRESULT:", "all checks passed" if ok else "CHECK FAILED")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
