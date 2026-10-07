#!/usr/bin/env python3
"""Static stability of the tripod gait: which centre-of-mass x positions stay inside the support polygon."""
import json, sys
import numpy as np
from shapely.geometry import MultiPoint, Point
import linkage6 as S, linkage_opt as L

def analyse(c, a_mm, spacing, track_mm, track_mid_mm=None):
    track_mid_mm = track_mm if track_mid_mm is None else track_mid_mm
    g = S.sixbar(c["params"]); P = g["P"] * a_mm
    m = L.stance_metrics(g["P"]); n = len(P)
    stance = np.zeros(n, bool); stance[m["stance_idx"]] = True
    legs = [("F", +spacing, 0, +1), ("M", 0.0, 360, +1), ("R", -spacing, 0, +1)]
    xs = np.arange(-150, 60, 1.0)           # candidate COM x (chassis frame, crank axis of mid leg = 0)
    ok = np.ones_like(xs, bool)
    margin_min = np.full_like(xs, 1e9)
    for k in range(0, n, 2):
        feet = []
        for side in (+1, -1):
            for name, x0, ph, _ in legs:
                kk = (k + ph + (360 if side < 0 else 0)) % n
                if stance[kk]:
                    feet.append((x0 + P[kk, 0], side * (track_mid_mm if name == 'M' else track_mm) / 2))
        if len(feet) < 3:
            ok[:] = False; continue
        hull = MultiPoint(feet).convex_hull
        for i, x in enumerate(xs):
            if not hull.contains(Point(x, 0)):
                ok[i] = False
            else:
                margin_min[i] = min(margin_min[i], hull.exterior.distance(Point(x, 0)))
    good = xs[ok]
    return xs, ok, margin_min, (good.min(), good.max()) if good.size else None

if __name__ == "__main__":
    c = json.load(open("candidates6_refined.json"))[1]
    for track, tmid in ((77.6, 111.2), (70, 70), (100, 100)):
        xs, ok, mg, rng = analyse(c, 9.0, 60.0, track, tmid)
        best = xs[np.argmax(np.where(ok, mg, -1))]
        print(f"track end/mid={track}/{tmid} mm: stable COM x range = {rng}  best x={best:.0f} mm (margin {mg[np.argmax(np.where(ok, mg, -1))]:.1f} mm)")
