#!/usr/bin/env python3
"""Verify the pinecone organizer (entry #2).

    python pinecone_check.py 02-pinecone-organizer [--plan plan.png]

Reads stl/pinecone.stl and holes.json (made by pinecone_organizer.py) and checks
  - one watertight body, overhang area
  - every hole is open and has its depth: three rays along the hole axis per hole, from outside
  - the volume removed equals the volume of the 40 cylinders (so no hole was lost or merged)
  - the golden angle: successive hole angles differ by 137.508 degrees
  - walls: between neighbouring holes (axis segments) and between a hole and the outside of the body (distance from the
    hole wall to the uncut surface), the floor under the deepest holes
  - the hole sizes against what they are meant to hold
Exit code 1 if a check fails.
"""
import argparse
import json
import math
import os
import sys

import numpy as np
import trimesh

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pinecone_organizer as po  # noqa: E402


def seg_dist(p0, p1, q0, q1, n=60):
    t = np.linspace(0, 1, n)[:, None]
    a = p0 + t * (p1 - p0)
    b = q0 + t * (q1 - q0)
    d = np.linalg.norm(a[:, None, :] - b[None, :, :], axis=2)
    return d.min()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--plan")
    ap.add_argument("--section", help="write a vertical cut through the middle and through the most tilted hole")
    a = ap.parse_args()
    ok = True
    mesh = trimesh.load(os.path.join(a.folder, "stl", "pinecone.stl"), force="mesh")
    H = json.load(open(os.path.join(a.folder, "holes.json")))
    holes = H["holes"]
    n = len(holes)

    bodies = [b for b in mesh.split(only_watertight=False) if abs(b.volume) > 1.0]
    nz, zc = mesh.face_normals[:, 2], mesh.triangles_center[:, 2]
    bad = (nz < -np.sin(np.radians(46.5))) & (zc > 0.05)
    over = mesh.area_faces[bad].sum()
    good = len(bodies) == 1 and mesh.is_watertight
    ok &= good
    lo, hi = mesh.bounds
    print(f"body: {len(bodies)} body, watertight {mesh.is_watertight}, {hi[0]-lo[0]:.0f} x {hi[1]-lo[1]:.0f} x {hi[2]-lo[2]:.1f} mm, "
          f"{mesh.volume/1000:.0f} cm3 ({mesh.volume/1000*1.24:.0f} g if solid PLA), overhang {over:.0f} mm2 of {mesh.area:.0f} "
          f"({100*over/mesh.area:.2f} %)  {'ok' if good else 'FAIL'}")
    if over > 0:
        c = mesh.triangles_center[bad]
        print(f"      overhang at z {c[:,2].min():.0f}..{c[:,2].max():.0f} mm, radius {np.hypot(c[:,0], c[:,1]).min():.0f}..{np.hypot(c[:,0], c[:,1]).max():.0f} mm: "
              "slivers where a hole breaks through a groove between two scales")

    # ---- holes open, depth ------------------------------------------------------------------------------
    results = []
    for h in holes:
        u = np.array(h["axis"])
        p_s = np.array([h["x"], h["y"], h["ztop"]])
        ref = np.array([0, 0, 1.0]) if abs(u[2]) < 0.9 else np.array([1.0, 0, 0])
        e1 = np.cross(u, ref)
        e1 /= np.linalg.norm(e1)
        e2 = np.cross(u, e1)
        r = h["d"] / 2
        start = min(12.0, h["depth"] - 6.0)                 # start inside the hole, past the opening on the slope
        origins = np.array([p_s + start * u, p_s + start * u + 0.7 * r * e1, p_s + start * u - 0.7 * r * e2])
        locs, ray_i, _ = mesh.ray.intersects_location(origins, np.tile(u, (3, 1)), multiple_hits=False)
        dd = np.full(3, np.nan)
        dd[ray_i] = np.linalg.norm(locs - origins[ray_i], axis=1)
        results.append(dd + start)
    results = np.array(results)
    depth_err = np.abs(results - np.array([h["depth"] for h in holes])[:, None])
    n_open = int(np.sum(~np.isnan(results).any(axis=1) & (depth_err < 0.2).all(axis=1)))
    good = n_open == n
    ok &= good
    print(f"\nholes: {n_open} of {n} open with the designed depth (3 rays each, started inside the hole, depth error at most {np.nanmax(depth_err):.2f} mm)  {'ok' if good else 'FAIL'}")

    # ---- volume removed -----------------------------------------------------------------------------------
    verts, faces = po.build_body(holes)
    tris = []
    for q in faces:
        for k in range(1, len(q) - 1):
            tris.append((q[0], q[k], q[k + 1]))
    uncut = trimesh.Trimesh(np.array(verts), np.array(tris), process=False)
    if uncut.volume < 0:
        uncut.invert()
    removed = uncut.volume - mesh.volume
    expected = sum(math.pi * (h["d"] / 2) ** 2 * h["depth"] for h in holes)
    ratio = removed / expected
    good = 0.95 < ratio < 1.05
    ok &= good
    print(f"volume removed {removed/1000:.1f} cm3 against {expected/1000:.1f} cm3 for the {n} cylinders (ratio {ratio:.3f})  {'ok' if good else 'FAIL'}")

    # ---- golden angle ---------------------------------------------------------------------------------
    th = np.array([h["theta"] for h in holes])
    steps = np.degrees(np.diff(th)) % 360
    err = np.abs(steps - po.GOLDEN).max()
    good = err < 0.01
    ok &= good
    print(f"golden angle: hole k+1 is {steps.mean():.4f} degrees round from hole k (largest error {err:.4f})  {'ok' if good else 'FAIL'}")

    # ---- walls --------------------------------------------------------------------------------------------
    segs = []
    for h in holes:
        u = np.array(h["axis"])
        p0 = np.array([h["x"], h["y"], h["ztop"]])
        segs.append((p0, p0 + h["depth"] * u, h["d"] / 2))
    worst = (1e9, None)
    for i in range(n):
        for j in range(i + 1, n):
            d = seg_dist(segs[i][0], segs[i][1], segs[j][0], segs[j][1]) - segs[i][2] - segs[j][2]
            if d < worst[0]:
                worst = (d, (holes[i]["k"], holes[j]["k"]))
    good = worst[0] >= 1.6
    ok &= good
    print(f"wall between two holes: at least {worst[0]:.2f} mm (holes {worst[1][0]} and {worst[1][1]}; 4 perimeters of 0.4 mm need 1.6)  {'ok' if good else 'FAIL'}")

    rng = np.random.default_rng(2)
    pts, depth_along, kinds = [], [], []
    for p0, p1, r in segs:
        u = (p1 - p0) / np.linalg.norm(p1 - p0)
        ref = np.array([0, 0, 1.0]) if abs(u[2]) < 0.9 else np.array([1.0, 0, 0])
        e1 = np.cross(u, ref)
        e1 /= np.linalg.norm(e1)
        e2 = np.cross(u, e1)
        L = np.linalg.norm(p1 - p0)
        for _ in range(150):
            t, ang = rng.uniform(0.02, 1.0), rng.uniform(0, 2 * np.pi)
            pts.append(p0 + t * (p1 - p0) + r * (np.cos(ang) * e1 + np.sin(ang) * e2))
            depth_along.append(t * L)
            kinds.append(0)
        for ang in np.linspace(0, 2 * np.pi, 24, endpoint=False):        # the bottom edge
            pts.append(p1 + r * (np.cos(ang) * e1 + np.sin(ang) * e2))
            depth_along.append(L)
            kinds.append(1)
    pts, depth_along, kinds = np.array(pts), np.array(depth_along), np.array(kinds)
    _, dist, _ = trimesh.proximity.closest_point(uncut, pts)
    # the uncut body is a height field: a point is inside when it is below the surface height there (no ray casting needed)
    inside = (pts[:, 2] < po.top_z(pts[:, 0], pts[:, 1], holes)) & (np.hypot(pts[:, 0], pts[:, 1]) < po.R) & (pts[:, 2] > 0)
    depth_of = np.repeat([h["depth"] for h in holes], 150 + 24)
    deep = inside & (depth_along >= 0.5 * depth_of)
    wall_out = dist[deep].min()
    skin = inside & (dist < 0.8) & (depth_along < 0.5 * depth_of)
    bottom = dist[kinds == 1].min()
    z_low = min(p[2] for p in pts)
    good = wall_out >= 1.6 and bottom >= 2.4 and z_low >= 2.9
    ok &= good
    print(f"wall between a hole and the outside of the body, lower half of every hole: at least {wall_out:.2f} mm "
          f"({deep.sum()} sample points); bottom edge of every hole at least {bottom:.2f} mm from the surface; lowest point of any hole "
          f"{z_low:.1f} mm above the bed  {'ok' if good else 'FAIL'}")
    outside = ~inside
    print(f"      {100*outside.mean():.0f} % of the sample points on the hole walls are where the hole opens through the slope of the dome "
          f"(the lower lip of a leaning hole is a notch, which is what lets a pen lean); {skin.sum()} points of the upper half are within 0.8 mm of the surface "
          "(a knife edge at the end of that notch, harmless)")

    # ---- what the holes are for -----------------------------------------------------------------------------
    print("\nholes by size (diameter includes the clearance; the items are typical, not measured)")
    use = {3.6: "needles, pins, earring posts, hair pins, SIM tool (up to 3.2 mm)",
           6.5: "thin pens, mechanical pencils, fine brushes, tweezers (up to 6 mm)",
           9.5: "pencils (7.2 round, 8.7 across a hexagon's corners), ball pens, markers, screwdrivers (up to 9 mm)",
           13.5: "thick markers, highlighters, big brushes (up to 13 mm)"}
    for d in po.SIZES:
        hs = [h for h in holes if h["d"] == d]
        if hs:
            print(f"  {d:4.1f} mm: {len(hs):2d} holes, depth {min(h['depth'] for h in hs):.0f}..{max(h['depth'] for h in hs):.0f} mm, "
                  f"tilt {min(h['tilt'] for h in hs):.0f}..{max(h['tilt'] for h in hs):.0f} deg: {use[d]}")

    if a.plan:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        from matplotlib.patches import Circle
        fig, ax = plt.subplots(figsize=(7.6, 7.6), dpi=120)
        ax.set_aspect("equal")
        ax.add_patch(Circle((0, 0), po.R, fc="#f1dfc8", ec="#7a4a1c", lw=1.2))
        cols = {3.6: "#2b6f8f", 6.5: "#3d8f4a", 9.5: "#c27a1c", 13.5: "#a3342a"}
        for h in holes:
            ax.add_patch(Circle((h["x"], h["y"]), h["d"] / 2, fc=cols[h["d"]], ec="k", lw=0.5))
            u = np.array(h["axis"])
            ax.plot([h["x"], h["x"] + u[0] * 14], [h["y"], h["y"] + u[1] * 14], color="k", lw=0.5)
            ax.text(h["x"], h["y"], str(h["k"]), fontsize=5, ha="center", va="center", color="white")
        ax.set_xlim(-62, 62)
        ax.set_ylim(-62, 62)
        ax.set_title("Pinecone organizer, top view: holes on the golden-angle spiral (137.5 deg), numbered in order;\n"
                     "line = direction the hole leans (outwards)", fontsize=8)
        handles = [Circle((0, 0), 1, fc=cols[d], ec="k") for d in po.SIZES]
        ax.legend(handles, [f"{d} mm" for d in po.SIZES], loc="lower right", fontsize=7)
        fig.tight_layout()
        fig.savefig(a.plan)
        print("wrote", a.plan)

    if a.section:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        from matplotlib.patches import Polygon as MP
        h0 = max(holes, key=lambda q: (q["d"], q["tilt"]))
        th = h0["theta"]
        c, sn = math.cos(th), math.sin(th)
        sec = mesh.section(plane_origin=[0, 0, 0], plane_normal=[-sn, c, 0])
        T = np.array([[c, sn, 0, 0], [0, 0, 1, 0], [-sn, c, 0, 0], [0, 0, 0, 1.0]])    # u along the hole direction, v = z
        p2d, _ = sec.to_2D(to_2D=T)
        fig, ax = plt.subplots(figsize=(9.5, 5.2), dpi=120)
        ax.set_aspect("equal")
        for q in p2d.polygons_full:
            ax.add_patch(MP(np.asarray(q.exterior.coords), fc="#e0742f", ec="#4a2c14", lw=0.7))
            for r in q.interiors:
                ax.add_patch(MP(np.asarray(r.coords), fc="white", ec="#4a2c14", lw=0.7))
        u = np.array(h0["axis"])
        p0 = np.array([h0["x"], h0["y"], h0["ztop"]])
        p1 = p0 + h0["depth"] * u
        ur = lambda p: p[0] * c + p[1] * sn
        ax.plot([ur(p0), ur(p1)], [p0[2], p1[2]], color="#1b5e20", lw=1.2, ls="--")
        ax.text(ur(p1) + 3, p1[2] - 4, f"hole {h0['k']}: \u00d8{h0['d']} mm, leans {h0['tilt']:.0f}\u00b0 outwards,\n{h0['depth']:.0f} mm deep", fontsize=8, color="#1b5e20")
        ax.text(-po.R + 3, po.H_DRUM + po.H_DOME + 3, "the middle holes stand upright, the outer ones lean more (up to 18\u00b0),\nso pens and brushes fan out like the scales of a pinecone", fontsize=8)
        ax.set_xlim(-po.R - 4, po.R + 4)
        ax.set_ylim(-3, po.H_DRUM + po.H_DOME + 12)
        ax.set_xlabel("mm (the cut goes through the axis of the hole and the middle of the body)")
        ax.set_ylabel("mm above the bed")
        ax.grid(alpha=0.15)
        fig.tight_layout()
        fig.savefig(a.section)
        print("wrote", a.section)

    print("\nRESULT:", "all checks passed" if ok else "CHECK FAILED")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
