#!/usr/bin/env python3
"""Verify the Tree-Ring Rotary Organizer (entry #1): parts, fit, lid window, detent and capacity.

    python organizer_check.py 01-tree-ring-organizer [--plan plan.png]

Reads stl/*.stl (print position) and asm/*.stl (assembled position) and checks
  - each part: one watertight body, overhang area
  - fit: radial clearance between nested rings, ring B in the base lip, the lid skirt around ring B, the lid
    resting on the ring tops
  - the lid window: with the lid turned to each of the 6 positions, which sectors can be reached from above
    (ray casting through the real meshes); and halfway between two positions
  - the detent: the ribs inside the skirt against the grooves of ring B (free in the groove, pressed between)
  - capacity: volume of every compartment and how many AAA, AA, CR2032 and coins fit standing
Exit code 1 if a check fails.
"""
import argparse
import os
import sys

import numpy as np
import shapely
import trimesh
from shapely.geometry import Point, Polygon

SECTORS = 6


def sections(mesh, z):
    sec = mesh.section(plane_origin=[0, 0, z], plane_normal=[0, 0, 1])
    p2d, _ = sec.to_2D(to_2D=np.eye(4))
    return list(p2d.polygons_full)


def holes_of(mesh, z, skip_centre=True):
    """Inner loops (compartments) of the section at height z, as shapely polygons.
    The bore of a ring (the hole the next ring stands in, which contains the axis) is not a compartment."""
    out = []
    for p in sections(mesh, z):
        out += [Polygon(r) for r in p.interiors]
    if skip_centre and len(out) > 1:
        out = [q for q in out if not q.contains(Point(0, 0))]
    return out


def pack_circles(poly, d, step=0.5):
    """Greedy lower bound for the number of circles of diameter d that fit (4 scan orders)."""
    region = poly.buffer(-d / 2)
    if region.is_empty:
        return 0
    minx, miny, maxx, maxy = region.bounds
    xs, ys = np.meshgrid(np.arange(minx, maxx + 1e-9, step), np.arange(miny, maxy + 1e-9, step))
    xs, ys = xs.ravel(), ys.ravel()
    keep = shapely.contains_xy(region, xs, ys)
    cand = np.stack([xs[keep], ys[keep]], 1)
    best = 0
    cx, cy = poly.centroid.x, poly.centroid.y
    for key in (cand[:, 0] * 1000 + cand[:, 1], cand[:, 1] * 1000 + cand[:, 0],
                -cand[:, 0] * 1000 + cand[:, 1], np.hypot(cand[:, 0] - cx, cand[:, 1] - cy)):
        picked = []
        for p in cand[np.argsort(key)]:
            if all((p[0] - q[0]) ** 2 + (p[1] - q[1]) ** 2 >= d * d - 1e-6 for q in picked):
                picked.append(p)
        best = max(best, len(picked))
    return best


def rot_z(mesh, deg):
    m = mesh.copy()
    m.apply_transform(trimesh.transformations.rotation_matrix(np.radians(deg), [0, 0, 1]))
    return m


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--plan", help="write a top view with the compartments and the lid window")
    ap.add_argument("--ring-h", type=float, default=54.0)
    ap.add_argument("--floor", type=float, default=1.6)
    ap.add_argument("--base-t", type=float, default=3.0)
    a = ap.parse_args()
    ok = True
    names = ("base", "ring_b", "ring_a", "core", "lid")
    P = {n: trimesh.load(os.path.join(a.folder, "stl", n + ".stl"), force="mesh") for n in names}
    A = {n: trimesh.load(os.path.join(a.folder, "asm", n + ".stl"), force="mesh") for n in names}
    top_z = a.base_t + a.ring_h

    print("parts (print position)")
    total = 0.0
    for n in names:
        m = P[n]
        bodies = [b for b in m.split(only_watertight=False) if abs(b.volume) > 1.0]
        nz, zc = m.face_normals[:, 2], m.triangles_center[:, 2]
        down = (nz < -np.sin(np.radians(46.5))) & (zc > 0.05)
        over = m.area_faces[down].sum()
        good = len(bodies) == 1 and m.is_watertight
        ok &= good
        lo, hi = m.bounds
        total += m.volume
        print(f"  {n:7s} bodies {len(bodies)}, watertight {m.is_watertight}, {hi[0]-lo[0]:.0f} x {hi[1]-lo[1]:.0f} x {hi[2]-lo[2]:.1f} mm, "
              f"{m.volume/1000:.1f} cm3 ({m.volume/1000*1.24:.0f} g if solid PLA), overhang {over:.0f} mm2  {'ok' if good else 'FAIL'}")
        if n == "lid" and over > 0:
            zs = m.triangles_center[down][:, 2]
            print(f"          the lid overhang is at z = {zs.min():.2f}..{zs.max():.2f} mm: the roofs of the engraved tree rings, 0.8 mm wide bridges")
    print(f"  all parts together: {total/1000:.0f} cm3, {total/1000*1.24:.0f} g if solid (the slicer infill makes it less)")

    # ---- radial fit ------------------------------------------------------------------------------
    def r_of(mesh, zlo, zhi):
        v = mesh.vertices
        sel = (v[:, 2] > zlo) & (v[:, 2] < zhi)
        return np.hypot(v[sel, 0], v[sel, 1])

    wall_z = (a.base_t - 0.1, a.base_t + 0.1)  # the bottom edge of the walls: the meshes have vertices only at the ends of the walls
    lip_inner = r_of(A["base"], a.base_t + 2, 100).min()
    rb = r_of(A["ring_b"], *wall_z)
    ra = r_of(A["ring_a"], *wall_z)
    cr = r_of(A["core"], *wall_z)
    print("\nfit (radii measured on the meshes)")
    rows = [("core in ring A", ra.min() - cr.max()), ("ring A in ring B", rb.min() - ra.max()),
            ("ring B in the base lip", lip_inner - rb.max())]
    for name, g in rows:
        good = g >= 0.3
        ok &= good
        print(f"  {name:24s} clearance {g:.2f} mm  {'ok' if good else 'FAIL'}")
    lid = A["lid"]
    # inner face of the skirt: the ribs stick out of it, so take the radius most skirt vertices share
    vs = lid.vertices
    sk = vs[vs[:, 2] < top_z - 7.9]                       # the lower edge of the skirt, below the ribs
    rr = np.hypot(sk[:, 0], sk[:, 1])
    skirt_face = rr.min()
    g = skirt_face - rb.max()
    good = g >= 0.3
    ok &= good
    print(f"  lid skirt around ring B   clearance {g:.2f} mm  {'ok' if good else 'FAIL'}")
    tops = [A[n].bounds[1][2] for n in ("ring_b", "ring_a", "core")]
    lid_low_plate = np.sort(np.unique(np.round(lid.vertices[:, 2], 3)))
    good = max(tops) - min(tops) < 0.01 and abs(tops[0] - top_z) < 0.01 and any(abs(z - top_z) < 0.01 for z in lid_low_plate)
    ok &= good
    print(f"  ring tops at {tops[0]:.2f} / {tops[1]:.2f} / {tops[2]:.2f} mm, lid plate underside at {top_z:.2f} mm: the lid rests on all three  "
          f"{'ok' if good else 'FAIL'}")

    # ---- compartments ----------------------------------------------------------------------------
    depth = a.ring_h - a.floor
    comp = {"core": holes_of(A["core"], 30), "A": holes_of(A["ring_a"], 30), "B": holes_of(A["ring_b"], 30)}
    print("\ncompartments (section at mid height; depth %.1f mm)" % depth)
    ang = lambda p: np.degrees(np.arctan2(p.centroid.y, p.centroid.x)) % 360
    for key in ("core", "A", "B"):
        ps = sorted(comp[key], key=ang)
        ok &= (len(ps) == (1 if key == "core" else SECTORS))
        ar = [p.area for p in ps]
        r_in = min(np.hypot(*np.array(p.exterior.coords).T) .min() for p in ps)
        r_out = max(np.hypot(*np.array(p.exterior.coords).T).max() for p in ps)
        vol = np.array(ar) * depth / 1000
        print(f"  {key:4s}: {len(ps)} compartment(s), radius {r_in:.1f}..{r_out:.1f} mm, each {vol.min():.0f}..{vol.max():.0f} mL, "
              f"spread {100*(max(ar)-min(ar))/np.mean(ar):.1f} %")
    cap = {}
    print("  how many fit standing (greedy lower bound, per compartment):")
    items = (("AAA 10.5 x 44.5", 10.5, 44.5), ("AA 14.5 x 50.5", 14.5, 50.5), ("CR2032 20 x 3.2 (stack)", 20.0, 3.2), ("coin 25", 25.0, 2.0))
    for label, d, h in items:
        row = {}
        for key in ("core", "A", "B"):
            if h > depth and label.startswith(("AAA", "AA")):
                row[key] = 0
                continue
            row[key] = min(pack_circles(p, d) for p in comp[key])
        cap[label] = row
        print(f"    {label:24s} core {row['core']}, ring A {row['A']}, ring B {row['B']} per compartment")
    # what can actually be taken out: only the part of a sector that lies under the window (a standing battery
    # has to come out straight up). The window is read from the lid mesh, lid in position 0.
    win = None
    for p in sections(A["lid"], top_z + 0.2):
        for r in p.interiors:
            q = Polygon(r)
            if not q.contains(Point(0, 0)):
                win = q
    reach = {"A": [], "B": []}
    reach_poly = {}
    for key in ("A", "B"):
        reach_poly[key] = [p.intersection(win) for p in comp[key] if p.intersection(win).area > 1]
    print("  reachable through the window (the part of one sector under the opening):")
    for label, d, h in items[:2]:
        key = "A" if label.startswith("AAA") else "B"
        n = max(pack_circles(q, d) for q in reach_poly[key])
        reach[key] = n
        print(f"    {label:24s} ring {key}: {n} per opening  (compartment alone holds {cap[label][key]})")
    good = reach["A"] >= 1 and reach["B"] >= 1
    ok &= good
    print(f"  at least one AAA (ring A) and one AA (ring B) can be lifted out through the window  {'ok' if good else 'FAIL'}")
    if len(comp["A"]) == SECTORS and len(comp["B"]) == SECTORS:
        good = cap["AAA 10.5 x 44.5"]["A"] >= 1 and cap["AA 14.5 x 50.5"]["B"] >= 1 and 44.5 < depth and 50.5 < depth
        ok &= good
        print(f"  AAA stand in ring A and AA in ring B (depth {depth:.1f} mm > 50.5 mm)  {'ok' if good else 'FAIL'}")

    # ---- the lid window -------------------------------------------------------------------------
    print("\nlid window: share of each sector that is open from above (rays through the lid mesh)")
    rng = np.random.default_rng(1)

    def open_share(lid_mesh, polys):
        out = []
        for p in polys:
            q = p.buffer(-0.6)
            pts = []
            minx, miny, maxx, maxy = q.bounds
            while len(pts) < 220:
                x, y = rng.uniform(minx, maxx), rng.uniform(miny, maxy)
                if q.contains(Point(x, y)):
                    pts.append((x, y))
            pts = np.array(pts)
            o = np.column_stack([pts, np.full(len(pts), top_z + 20)])
            hit = lid_mesh.ray.intersects_any(o, np.tile([0, 0, -1.0], (len(o), 1)))
            out.append(1.0 - hit.mean())
        return np.array(out)

    polsA = sorted(comp["A"], key=ang)
    polsB = sorted(comp["B"], key=ang)
    polsC = comp["core"]
    ang_list = [round(ang(p)) for p in polsA]
    for k in range(SECTORS):
        lk = rot_z(lid, 60 * k)
        sa, sb, sc = open_share(lk, polsA), open_share(lk, polsB), open_share(lk, polsC)
        want = np.zeros(SECTORS)
        want[k] = 1
        # the window is in the middle of the sector at 30 + 60 k degrees
        good = (np.argmax(sa) == k and np.argmax(sb) == k and np.sort(sa)[-1] > 0.6 and np.sort(sb)[-1] > 0.6
                and np.sort(sa)[-2] < 0.02 and np.sort(sb)[-2] < 0.02 and sc[0] > 0.9)
        ok &= good
        print(f"  position {k}: ring A open sector at {ang_list[int(np.argmax(sa))]} deg ({sa.max()*100:.0f} % open, others at most {np.sort(sa)[-2]*100:.0f} %), "
              f"ring B ({sb.max()*100:.0f} % open, others {np.sort(sb)[-2]*100:.0f} %), core {sc[0]*100:.0f} % open  {'ok' if good else 'FAIL'}")
    lk = rot_z(lid, 30)
    sa, sb = open_share(lk, polsA), open_share(lk, polsB)
    two = np.argsort(sa)[-2:]
    print(f"  halfway (lid turned 30 deg): ring A sectors {[ang_list[i] for i in two]} open {[f'{sa[i]*100:.0f} %' for i in two]}, "
          f"ring B {[f'{sb[i]*100:.0f} %' for i in np.argsort(sb)[-2:]]}; the window straddles a divider")

    # ---- detent -------------------------------------------------------------------------------------
    print("\ndetent (ribs inside the lid skirt, grooves in the top of ring B)")
    vs = lid.vertices
    rib_pts = lid.sample(60000)
    rr = np.hypot(rib_pts[:, 0], rib_pts[:, 1])
    rib_pts = rib_pts[(rr < skirt_face - 0.02) & (rib_pts[:, 2] < top_z - 1.5) & (rib_pts[:, 2] > top_z - 7.5)]
    for deg, label in ((0, "at a groove (rest position)"), (30, "between two grooves")):
        d = trimesh.proximity.signed_distance(A["ring_b"], rot_z(trimesh.Trimesh(rib_pts, process=False), deg).vertices)
        pen = d.max()
        print(f"  {label:30s}: rib tips {'press ' + format(pen, '.2f') + ' mm into ring B' if pen > 0 else 'free of ring B by ' + format(-d.max(), '.2f') + ' mm'}")
        if deg == 0:
            good = pen <= 0.02
            ok &= good
    print("  (a 0.35 mm press is the designed click; how it feels in your plastic is not something a mesh check can tell)")

    if a.plan:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        from matplotlib.patches import Circle, Polygon as MP
        fig, ax = plt.subplots(figsize=(8.4, 8.4), dpi=120)
        ax.set_aspect("equal")
        base_poly = Point(0, 0).buffer(62)
        ax.add_patch(MP(np.array(base_poly.exterior.coords), fc="#ece6da", ec="#999", lw=0.8))
        for key, col in (("B", "#e0a46b"), ("A", "#d98c4a"), ("core", "#c9703a")):
            for p in comp[key]:
                ax.add_patch(MP(np.array(p.exterior.coords), fc=col, ec="#4a2c14", lw=0.8, alpha=0.9))
        # window outline from the lid section
        for p in sections(lid, top_z + 0.2):
            for r in p.interiors:
                c = np.array(r.coords)
                if np.hypot(*c.mean(0)) > 5:
                    ax.add_patch(MP(c, fc="none", ec="#1b5e20", lw=1.6, ls="--"))
        for key, d, col in (("A", 10.5, "#fff3c4"), ("B", 14.5, "#fff3c4")):
            p = max(reach_poly[key], key=lambda q: q.area)
            region = p.buffer(-d / 2)
            minx, miny, maxx, maxy = region.bounds
            xs, ys = np.meshgrid(np.arange(minx, maxx, 0.5), np.arange(miny, maxy, 0.5))
            keep = shapely.contains_xy(region, xs.ravel(), ys.ravel())
            cand = np.stack([xs.ravel()[keep], ys.ravel()[keep]], 1)
            picked = []
            for q in cand[np.argsort(cand[:, 0] * 1000 + cand[:, 1])]:
                if all((q[0] - r[0]) ** 2 + (q[1] - r[1]) ** 2 >= d * d - 1e-6 for r in picked):
                    picked.append(q)
            for q in picked:
                ax.add_patch(Circle(q, d / 2, fc=col, ec="#333", lw=0.6))
        ax.text(0, 0, "core\ncoins, CR2032", ha="center", va="center", fontsize=7)
        ax.set_xlim(-66, 66)
        ax.set_ylim(-66, 66)
        ax.set_title("Tree-Ring Organizer, top view (lid removed; dashed green = lid window)\n"
                     "ring A: AAA standing, ring B: AA standing (batteries drawn inside the part of the sector the window opens)", fontsize=9)
        ax.set_xlabel("mm")
        fig.tight_layout()
        fig.savefig(a.plan)
        print("wrote", a.plan)

    print("\nRESULT:", "all checks passed" if ok else "CHECK FAILED")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
