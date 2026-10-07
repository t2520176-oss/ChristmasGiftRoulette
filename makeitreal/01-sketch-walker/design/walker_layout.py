#!/usr/bin/env python3
"""
Whole-walker layout check (one side; the other side is a mirror with phases swapped).
Legs: Front (x=+S), Mid (x=0), Rear (x=-S).  Gait: Mid is 180 deg out of phase with Front/Rear.
Layers (from the chassis plate outwards) are shared by Front and Rear; Mid uses its own set.
Checks: (1) links of different legs in a common layer never overlap,
        (2) fixed pins / crank shafts of one leg never touch links of another leg lying
            in a layer that the pin passes through.
"""
import json, sys, itertools
import numpy as np
from shapely.geometry import Point
import linkage6 as S
import layout_check as LC

LINKS = LC.LINKS


def leg_layers(base, layers):
    return {k: base + v for k, v in layers.items()}


def ground_span(jn, layers):
    own = {"O2": "crank", "O4": "rocker", "O6": "elink"}[jn]
    return range(0, layers[own])           # layers the pin crosses before reaching its link (plate = -1)


def check(c, a_mm, spacing, mid_outboard=True, step=3, verbose=True):
    g = S.sixbar(c["params"])
    M = LC.collision_matrix(g, a_mm)
    best = LC.assign_layers(M)
    lay = best[1]
    nl = best[0][0]
    base_end, base_mid = (0, nl) if mid_outboard else (nl, 0)
    legs = {"F": (+spacing, base_end, 0), "M": (0.0, base_mid, 360), "R": (-spacing, base_end, 0)}
    n = len(g["A"])
    problems = []
    for k in range(0, n, step):
        shp, pins = {}, {}
        for name, (x0, base, ph) in legs.items():
            kk = (k + ph) % n
            sh = LC.shapes_at(g, kk, a_mm)
            from shapely.affinity import translate
            shp[name] = {ln: (translate(sh[ln], x0, 0), base + lay[ln]) for ln in LINKS}
            pp = LC.pin_positions(g, kk, a_mm)
            pins[name] = {jn: (np.array([pp[jn][0] + x0, pp[jn][1]]), jn) for jn in pp}
        # (1) links in the same absolute layer
        for (n1, n2) in itertools.combinations(legs, 2):
            for l1 in LINKS:
                for l2 in LINKS:
                    s1, L1 = shp[n1][l1]; s2, L2 = shp[n2][l2]
                    if L1 == L2 and s1.intersection(s2).area > 0.5:
                        problems.append(("link-link", n1, l1, n2, l2, k))
        # (2) fixed pins / shafts of leg A against links of leg B
        for n1, (x0, base, ph) in legs.items():
            for jn in ("O2", "O4", "O6"):
                pos = pins[n1][jn][0]
                r = 3.2 if jn == "O2" else 1.8
                top = base + lay[{"O2": "crank", "O4": "rocker", "O6": "elink"}[jn]]
                disc = Point(*pos).buffer(r)
                for n2 in legs:
                    if n2 == n1:
                        continue
                    for ln in LINKS:
                        s2, L2 = shp[n2][ln]
                        if 0 <= L2 < top and s2.intersects(disc):
                            problems.append(("pin-link", n1, jn, n2, ln, k))
    return lay, nl, problems


if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else "candidates6_refined.json"
    idx = int(sys.argv[2]) if len(sys.argv) > 2 else 1
    a_mm = float(sys.argv[3]) if len(sys.argv) > 3 else 9.0
    c = json.load(open(path))[idx]
    for spacing in (60.0, 66.0, 72.0, 84.0):
        for mid_out in (True, False):
            lay, nl, pr = check(c, a_mm, spacing, mid_out, step=6)
            kinds = {}
            for p in pr:
                kinds.setdefault((p[0], p[1], p[2], p[3], p[4]), 0)
                kinds[(p[0], p[1], p[2], p[3], p[4])] += 1
            print(f"S={spacing:5.1f} mid_outboard={mid_out!s:5} layers/set={nl} conflicts={len(pr)}",
                  list(kinds.items())[:4])
