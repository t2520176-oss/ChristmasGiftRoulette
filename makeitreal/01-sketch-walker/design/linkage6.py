#!/usr/bin/env python3
"""
Sketch Walker - six-bar leg synthesis (crank-rocker four-bar + a two-link dyad that drives the foot link).

Base four-bar : O2 (origin) -a=1- A -b- B -d- O4(dx,dz); C is a point on the coupler.
Foot link F   : hinged at C, length e1 to E; E is tied to ground pivot O6 by a link of length e2.
Foot P is a point rigidly attached to F (pf along C->E, qf perpendicular).
x forward, z up. All lengths in crank-lengths (a = 1).
"""
import json, sys
import numpy as np
from scipy.optimize import differential_evolution
import linkage_opt as L

TH = L.TH
TH_OPT = L.TH_OPT
NAMES = ["b", "d", "dx", "dz", "pc", "qc", "e1", "e2", "x6", "z6", "pf", "qf", "br1", "br2"]


def circle_hit(P0, r0, P1, r1, branch):
    v = P1 - P0
    dist = np.linalg.norm(v, axis=1)
    ok = np.all(dist <= r0 + r1 - 1e-6) and np.all(dist >= abs(r0 - r1) + 1e-6)
    dist = np.clip(dist, abs(r0 - r1) + 1e-9, r0 + r1 - 1e-9)
    l = (r0 * r0 - r1 * r1 + dist ** 2) / (2 * dist)
    h = np.sqrt(np.maximum(r0 * r0 - l * l, 0.0))
    e = v / dist[:, None]
    nrm = np.stack([-e[:, 1], e[:, 0]], axis=1)
    return P0 + e * l[:, None] + branch * nrm * h[:, None], ok


def sixbar(prm, th=TH):
    b, d, dx, dz, pc, qc, e1, e2, x6, z6, pf, qf, br1, br2 = prm
    br1 = 1.0 if br1 >= 0 else -1.0
    br2 = 1.0 if br2 >= 0 else -1.0
    A = np.stack([np.cos(th), np.sin(th)], axis=1)
    O4 = np.array([dx, dz]); O6 = np.array([x6, z6])
    B, ok1 = circle_hit(A, b, np.broadcast_to(O4, A.shape), d, br1)
    u = (B - A) / b
    n = np.stack([-u[:, 1], u[:, 0]], axis=1)
    C = A + pc * u + qc * n
    E, ok2 = circle_hit(C, e1, np.broadcast_to(O6, C.shape), e2, br2)
    u2 = (E - C) / e1
    n2 = np.stack([-u2[:, 1], u2[:, 0]], axis=1)
    P = C + pf * u2 + qf * n2
    return dict(A=A, B=B, C=C, E=E, P=P, O4=O4, O6=O6, ok=bool(ok1 and ok2))


def mu_min(g):
    c = g["B"] - g["A"]; r = g["B"] - g["O4"]
    ang = np.degrees(np.arccos(np.clip(np.sum(c * r, 1) / (np.linalg.norm(c, axis=1) * np.linalg.norm(r, axis=1)), -1, 1)))
    ang = np.minimum(ang, 180 - ang)
    c2 = g["E"] - g["C"]; r2 = g["E"] - g["O6"]
    ang2 = np.degrees(np.arccos(np.clip(np.sum(c2 * r2, 1) / (np.linalg.norm(c2, axis=1) * np.linalg.norm(r2, axis=1)), -1, 1)))
    ang2 = np.minimum(ang2, 180 - ang2)
    return min(ang.min(), ang2.min())


MIN_DUTY, MIN_LIFT, MIN_HANG, MAX_HANG, MIN_STRIDE = 0.56, 0.22, 1.6, 4.2, 3.2
MAX_EXTENT = 10.0      # overall mechanism bounding box limit (crank lengths)


def joints_z(g, prm):
    return min(g["A"][:, 1].min(), g["B"][:, 1].min(), g["C"][:, 1].min(), g["E"][:, 1].min(),
               0.0, prm[3], prm[9])


def cost6(prm):
    prm = list(prm)
    b, d, dx, dz = prm[:4]
    g = sixbar(prm, TH_OPT)
    if not g["ok"]:
        return 1e3
    links = sorted([1.0, b, d, np.hypot(dx, dz)])
    if not (links[0] == 1.0 and links[0] + links[3] < links[1] + links[2]):
        return 1e3
    m = L.stance_metrics(g["P"])
    if m is None:
        return 1e3
    s = m["stride_ref"]
    wrong = 1.0 if m["slope"] > 0 else 0.0
    hang = joints_z(g, prm) - m["zmin"]
    allx = np.concatenate([g["A"][:, 0], g["B"][:, 0], g["C"][:, 0], g["E"][:, 0], g["P"][:, 0], [0, dx, prm[8]]])
    allz = np.concatenate([g["A"][:, 1], g["B"][:, 1], g["C"][:, 1], g["E"][:, 1], g["P"][:, 1], [0, dz, prm[9]]])
    extent = max(allx.max() - allx.min(), allz.max() - allz.min())
    mu = mu_min(g)
    pen = (max(0, MIN_DUTY - m["duty"]) * 60
           + (m["dev"] / s) * 6 + max(0.0, m["mismatch"] - 0.12) * 10
           + max(0, MIN_LIFT - m["lift"] / s) * 60
           + max(0, MIN_STRIDE - m["stride"]) * 40
           + max(0, MIN_HANG - hang) * 25 + max(0, hang - MAX_HANG) * 8
           + max(0, 30 - mu) * 0.4
           + max(0, extent - MAX_EXTENT) * 5
           + wrong * 5)
    return pen - 3 * m["duty"] - 2 * min(m["lift"] / s, 0.35) + 0.03 * extent


BOUNDS = [(1.6, 6), (1.6, 6), (-5, 5), (-5, 3), (-3, 6), (-5, 5), (1.5, 7), (1.2, 7), (-6, 6), (-8, 3), (-3, 8), (-6, 6), (-1, 1), (-1, 1)]


def run(seed, maxiter=350, popsize=22):
    r = differential_evolution(cost6, BOUNDS, seed=seed, maxiter=maxiter, popsize=popsize, tol=1e-9,
                               mutation=(0.4, 1.0), recombination=0.85, polish=False, updating="immediate")
    prm = list(r.x); prm[12] = 1.0 if prm[12] >= 0 else -1.0; prm[13] = 1.0 if prm[13] >= 0 else -1.0
    g = sixbar(prm); m = L.stance_metrics(g["P"])
    hang = joints_z(g, prm) - m["zmin"]
    return dict(seed=seed, cost=float(r.fun), params=[float(v) for v in prm], duty=float(m["duty"]),
                dev=float(m["dev"] / m["stride_ref"]), mism=float(m["mismatch"]),
                lift_rel=float(m["lift"] / m["stride_ref"]), stride=float(m["stride"]),
                mu=float(mu_min(g)), hang=float(hang), ok=g["ok"])


if __name__ == "__main__":
    from multiprocessing import Pool
    seeds = [int(x) for x in sys.argv[1:]] or [1, 2, 3, 4]
    with Pool(4) as p:
        res = p.map(run, seeds)
    res.sort(key=lambda r: r["cost"])
    json.dump(res, open("candidates6.json", "w"), indent=1)
    for r in res:
        print({k: (round(v, 3) if isinstance(v, float) else v) for k, v in r.items() if k != "params"})
