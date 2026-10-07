#!/usr/bin/env python3
"""Verify the bracket-fungus shelf (entry #4): every size, the shelf and the plaque together.

    python shelf_check.py 04-fungus-shelf [--sizes L M S] [--plan plan.png]

Reads stl/shelf_<S>.stl and stl/plaque_<S>.stl (both in print position) and checks
  - each part: one watertight body, overhang area
  - assembly (the shelf in place on the plaque, rigid transforms from the print positions):
      * the shelf can slide down the rail from the top edge to the stop without touching the plaque
        (cross sections at 40 heights, the groove against the rail), and the smallest gap there
      * it rests on the stop: contact area, the shelf underside and the stop top at the same height
  - the keyholes (lip slot, head pocket) measured on the mesh
  - the shelf: growth rings, card slot, groove, largest circle that fits on the display surface
  - a load estimate with ASSUMED material numbers
Exit code 1 if a check fails.
"""
import argparse
import os
import sys

import numpy as np
import trimesh
from shapely.geometry import Point, Polygon, box
from shapely.ops import unary_union

T = 10.0        # plaque plate thickness
Z_STOP = 14.0
T0 = 14.0


def polys(mesh, origin, normal, to_2d):
    sec = mesh.section(plane_origin=origin, plane_normal=normal)
    if sec is None:
        return []
    p2d, _ = sec.to_2D(to_2D=to_2d)
    return list(p2d.polygons_full)


XY = np.eye(4)
YZ = np.array([[0, 1, 0, 0], [0, 0, 1, 0], [1, 0, 0, 0], [0, 0, 0, 1.0]])    # (x, y, z) -> (y, z, x)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--sizes", nargs="+", default=["L", "M", "S"])
    ap.add_argument("--plan")
    a = ap.parse_args()
    ok = True
    Mp = np.array([[1, 0, 0, 0], [0, 0, -1, 0], [0, 1, 0, 0], [0, 0, 0, 1.0]])          # plaque print -> wall frame
    Ms = np.array([[1, 0, 0, 0], [0, -1, 0, -T], [0, 0, -1, Z_STOP + T0], [0, 0, 0, 1.0]])  # shelf print -> wall frame

    for size in a.sizes:
        sh0 = trimesh.load(os.path.join(a.folder, "stl", f"shelf_{size}.stl"), force="mesh")
        pl0 = trimesh.load(os.path.join(a.folder, "stl", f"plaque_{size}.stl"), force="mesh")
        print(f"\n===== size {size} =====")
        for name, m in (("shelf", sh0), ("plaque", pl0)):
            bodies = [b for b in m.split(only_watertight=False) if abs(b.volume) > 1.0]
            nz, zc = m.face_normals[:, 2], m.triangles_center[:, 2]
            over = m.area_faces[(nz < -np.sin(np.radians(46.5))) & (zc > 0.05)].sum()
            good = len(bodies) == 1 and m.is_watertight and abs(m.bounds[0][2]) < 1e-3
            ok &= good
            lo, hi = m.bounds
            print(f"  {name:6s} bodies {len(bodies)}, watertight {m.is_watertight}, {hi[0]-lo[0]:.0f} x {hi[1]-lo[1]:.0f} x {hi[2]-lo[2]:.1f} mm, "
                  f"{m.volume/1000:.0f} cm3 ({m.volume/1000*1.24:.0f} g if solid PLA), overhang {over:.0f} mm2  {'ok' if good else 'FAIL'}")
        sh, pl = sh0.copy(), pl0.copy()
        sh.apply_transform(Ms)
        pl.apply_transform(Mp)
        w = pl.bounds[1][0] - pl.bounds[0][0]

        # ---- the shelf on the plate: slide path ---------------------------------------------------
        shelf_zs = np.linspace(Z_STOP + 1.0, Z_STOP + T0 - 1.0, 5)
        s_all = unary_union([q for z in shelf_zs for q in polys(sh, [0, 0, z], [0, 0, 1], XY)])
        worst, inter = 1e9, 0.0
        zs = np.linspace(Z_STOP + 0.3, 108.0, 40)
        for z in zs:
            for q in polys(pl, [0, 0, z], [0, 0, 1], XY):
                p_out = q.intersection(box(-w, -T - 25, w, -T - 0.05))      # what sticks out of the flat front plane
                if p_out.is_empty:
                    continue
                inter += s_all.intersection(p_out).area
                worst = min(worst, s_all.distance(p_out))
        good = inter < 0.01 and worst > 0.15
        ok &= good
        print(f"  slide: shelf against the rail at {len(zs)} heights from the stop to the top edge: overlap {inter:.3f} mm2, "
              f"smallest gap {worst:.2f} mm  {'ok' if good else 'FAIL'}")

        # ---- the stop -----------------------------------------------------------------------------
        sec_pl = polys(pl, [11.0, 0, 0], [1, 0, 0], YZ)                      # (y, z) at x = 11: beside the rail, on the stop
        stop_top = None
        for q in sec_pl:
            for (y, zz) in q.exterior.coords:
                if y < -T - 1.0:
                    stop_top = zz if stop_top is None else max(stop_top, zz)
        sec_sh = polys(sh, [11.0, 0, 0], [1, 0, 0], YZ)
        sh_bottom = min(zz for q in sec_sh for (y, zz) in q.exterior.coords)
        und = unary_union(polys(sh, [0, 0, Z_STOP + 0.05], [0, 0, 1], XY))    # the shelf at its lowest level: plateau only
        sec_stop = polys(pl, [0, 0, Z_STOP - 3.0], [0, 0, 1], XY)
        stop_poly = unary_union([q.intersection(box(-w, -T - 25, w, -T - 0.05)) for q in sec_stop])
        contact = und.intersection(stop_poly).area if not stop_poly.is_empty else 0.0
        good = stop_top is not None and abs(stop_top - sh_bottom) < 0.05 and contact > 60
        ok &= good
        print(f"  stop: top at {stop_top:.2f} mm, shelf underside at {sh_bottom:.2f} mm, contact area {contact:.0f} mm2 "
              f"({10.0/max(contact,1e-6):.2f} MPa for a 10 N load, 1 kg is 9.8 N)  {'ok' if good else 'FAIL'}")

        # ---- keyholes ----------------------------------------------------------------------------
        lips = polys(pl0, [0, 0, 1.5], [0, 0, 1], XY)
        pockets = polys(pl0, [0, 0, 5.0], [0, 0, 1], XY)
        holes_lip = sorted([Polygon(r) for q in lips for r in q.interiors], key=lambda p: p.centroid.x)
        holes_pk = sorted([Polygon(r) for q in pockets for r in q.interiors], key=lambda p: p.centroid.x)
        if len(holes_lip) == 2 and len(holes_pk) == 2:
            bp = holes_pk[0].bounds
            good = (bp[2] - bp[0]) >= 9.4 and abs(holes_lip[0].centroid.x + holes_lip[1].centroid.x) < 0.5
            lip_w = max(c[0] for c in holes_lip[0].exterior.coords) - min(c[0] for c in holes_lip[0].exterior.coords)
            ok &= good
            print(f"  keyholes: 2, circle {lip_w:.1f} mm wide (head passes), slot in the lip {4.6:.1f} mm, pocket {bp[2]-bp[0]:.1f} mm wide x "
                  f"{bp[3]-bp[1]:.1f} mm tall, centres {holes_lip[0].centroid.x:.0f} / {holes_lip[1].centroid.x:.0f} mm  {'ok' if good else 'FAIL'}")
        else:
            ok = False
            print(f"  keyholes: found {len(holes_lip)} in the lip and {len(holes_pk)} in the pocket  FAIL")

        # ---- the shelf itself -----------------------------------------------------------------------
        islands = polys(sh0, [0, 0, 0.25], [0, 0, 1], XY)
        slit_len = None
        for q in polys(sh0, [0, 0, 6.0], [0, 0, 1], XY):
            for r in q.interiors:
                pr = Polygon(r)
                b = pr.bounds
                if b[3] - b[1] < 4 and b[2] - b[0] > 20:
                    slit_len = (b[2] - b[0], b[3] - b[1])
        # display surface: the half ellipse minus the groove notch and the card slot
        disc = unary_union([Polygon(np.asarray(q.exterior.coords)) for q in polys(sh0, [0, 0, 1.2], [0, 0, 1], XY)])
        lo, hi = 0.0, 60.0
        while hi - lo > 0.25:
            mid_r = (lo + hi) / 2
            if not disc.buffer(-mid_r).is_empty:
                lo = mid_r
            else:
                hi = mid_r
        r_ins = lo
        n_rings = len(islands)
        good = slit_len is not None and 8 <= n_rings <= 30
        ok &= good
        print(f"  shelf: {n_rings} separate bands on the top face at 0.25 mm depth (rings are engraved, 0.5 and 0.9 mm), card slot "
              f"{slit_len[0]:.0f} x {slit_len[1]:.1f} mm, display surface {disc.area:.0f} mm2, a circle of {2*r_ins:.0f} mm fits  {'ok' if good else 'FAIL'}"
              if slit_len else "  shelf: card slot not found  FAIL")

        # ---- load estimate: ASSUMED numbers -------------------------------------------------------------------
        depth = sh0.bounds[1][1]
        W, L = 9.81, 0.7 * depth                       # 1 kg standing at 70 % of the depth
        pull = W * L / T0                               # the groove flanks pull the shelf onto the wall; lever = hoof thickness
        flank_area = 2 * T0 * (4.0 * np.sqrt(2))        # both flanks, 14 mm long, 4 mm rise at 45 degrees
        stress = pull / flank_area
        print(f"  load estimate (ASSUMED, not measured): 1 kg at {L:.0f} mm gives {pull:.0f} N on the dovetail flanks = {stress:.2f} MPa; "
              f"PLA is typically 20 to 40 MPa along the layers and less across them, so the dovetail is not the limit; the wall screws are")

    if a.plan:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        from matplotlib.patches import Polygon as MP
        fig, axs = plt.subplots(1, 3, figsize=(13, 4.6), dpi=110)
        for ax, size in zip(axs, a.sizes):
            sh0 = trimesh.load(os.path.join(a.folder, "stl", f"shelf_{size}.stl"), force="mesh")
            for q in polys(sh0, [0, 0, 0.25], [0, 0, 1], XY):
                ax.add_patch(MP(np.asarray(q.exterior.coords), fc="#e0742f", ec="#4a2c14", lw=0.5))
                for r in q.interiors:
                    ax.add_patch(MP(np.asarray(r.coords), fc="white", ec="#4a2c14", lw=0.5))
            ax.set_aspect("equal")
            ax.set_xlim(-80, 80)
            ax.set_ylim(-5, 70)
            ax.set_title(f"size {size}: top face (bed side), engraved rings", fontsize=9)
        fig.tight_layout()
        fig.savefig(a.plan)
        print("wrote", a.plan)

    print("\nRESULT:", "all checks passed" if ok else "CHECK FAILED")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
