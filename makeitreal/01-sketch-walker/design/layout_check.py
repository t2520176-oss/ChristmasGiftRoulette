#!/usr/bin/env python3
"""
Layer assignment + collision check for one six-bar walker leg.

Links (planar, side view), each lives in its own y-layer (plates stacked along the body width):
  crank  O2-A          coupler A-B-C (triangle plate)      rocker O4-B
  foot   C-E + C-P     elink  E-O6
Two links may share a layer only if their outlines never overlap during a full crank turn.
Links joined at a pin must be in different layers (pin passes through both).
"""
import itertools
import numpy as np
from shapely.geometry import Point, LineString, MultiPoint
from shapely.ops import unary_union
import linkage6 as S

LINKS = ["crank", "coupler", "rocker", "foot", "elink"]
JOINTS = {"A": ("crank", "coupler"), "B": ("coupler", "rocker"), "C": ("coupler", "foot"),
          "E": ("foot", "elink"), "O2": ("crank",), "O4": ("rocker",), "O6": ("elink",)}


def shapes_at(g, k, a_mm, r_link=4.0, r_hub_crank=5.0):
    s = a_mm
    O2 = np.array([0.0, 0.0]); O4 = g["O4"] * s; O6 = g["O6"] * s
    A, B, C, E, P = (g[n][k] * s for n in ("A", "B", "C", "E", "P"))
    cap = lambda p, q, r: LineString([tuple(p), tuple(q)]).buffer(r, cap_style=1, resolution=8)
    circ = lambda p, r: Point(*p).buffer(r, resolution=8)
    sh = {
        "crank": unary_union([cap(O2, A, r_link), circ(O2, r_hub_crank)]),
        "coupler": MultiPoint([tuple(x) for x in (A, B, C)]).convex_hull.buffer(r_link, resolution=8),
        "rocker": cap(O4, B, r_link),
        "foot": unary_union([cap(C, E, r_link), cap(C, P, r_link), circ(P, 5.5)]),
        "elink": cap(E, O6, r_link),
    }
    return sh


def pin_positions(g, k, a_mm):
    s = a_mm
    return {"A": g["A"][k] * s, "B": g["B"][k] * s, "C": g["C"][k] * s, "E": g["E"][k] * s,
            "O2": np.zeros(2), "O4": g["O4"] * s, "O6": g["O6"] * s}


def collision_matrix(g, a_mm, step=6, r_link=4.0):
    n = len(g["A"])
    M = {(i, j): 0.0 for i, j in itertools.combinations(LINKS, 2)}
    for k in range(0, n, step):
        sh = shapes_at(g, k, a_mm, r_link)
        for i, j in M:
            a = sh[i].intersection(sh[j]).area
            if a > M[(i, j)]:
                M[(i, j)] = a
    return M


def assign_layers(M, thr=1.0, max_layers=5):
    """thr: overlap area [mm^2] above which two links must be in different layers."""
    conflict = {frozenset(k) for k, v in M.items() if v > thr}
    # connected links always conflict
    for lk in JOINTS.values():
        if len(lk) == 2:
            conflict.add(frozenset(lk))
    best = None
    for nl in range(1, max_layers + 1):
        for lay in itertools.product(range(nl), repeat=len(LINKS)):
            if len(set(lay)) != nl:
                continue
            d = dict(zip(LINKS, lay))
            if any(d[a] == d[b] for a, b in (tuple(c) for c in conflict)):
                continue
            # prefer connected links adjacent
            span = sum(abs(d[a] - d[b]) - 1 for a, b in (v for v in JOINTS.values() if len(v) == 2))
            score = (nl, span)
            if best is None or score < best[0]:
                best = (score, d)
        if best is not None:
            break
    return best


def pin_clear(g, a_mm, layers, r_link=4.0, r_pin=1.6, step=6):
    """A pin spanning layers i..j must not touch any link sitting in a layer between them."""
    bad = []
    n = len(g["A"])
    for jn, lk in JOINTS.items():
        if len(lk) == 1:
            ls = [-1, layers[lk[0]]]
        else:
            ls = [layers[lk[0]], layers[lk[1]]]
        lo, hi = min(ls), max(ls)
        for k in range(0, n, step):
            sh = shapes_at(g, k, a_mm, r_link)
            pos = pin_positions(g, k, a_mm)[jn]
            pt = Point(*pos).buffer(r_pin)
            for name in LINKS:
                if name in lk:
                    continue
                if lo < layers[name] < hi and sh[name].intersects(pt):
                    bad.append((jn, name, k))
    return bad


if __name__ == "__main__":
    import json, sys
    path = sys.argv[1] if len(sys.argv) > 1 else "candidates6_refined.json"
    idx = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    a_mm = float(sys.argv[3]) if len(sys.argv) > 3 else 9.0
    c = json.load(open(path))[idx]
    g = S.sixbar(c["params"])
    M = collision_matrix(g, a_mm)
    print("overlap areas (mm^2):", {f"{i}-{j}": round(v, 1) for (i, j), v in M.items()})
    res = assign_layers(M)
    print("layers:", res)
    if res:
        print("pin conflicts:", pin_clear(g, a_mm, res[1])[:6])
