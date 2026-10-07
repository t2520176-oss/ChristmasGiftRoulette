#!/usr/bin/env python3
"""
Sketch Walker - leg linkage synthesis.

Synthesises a planar crank-rocker four-bar whose coupler point (the foot) follows a
"D"-shaped path: a long, flat, constant-speed stance stroke on the ground and a short
lifted swing stroke. Link lengths are found by numerical optimisation from scratch
(nothing copied from existing walkers).

Frame: x forward, z up, crank pivot O2 at the origin, crank length a = 1 (normalised).
Foot P = A + p*u + q*n   (u = unit vector A->B, n = u rotated +90 deg).
"""
import json
import sys
import numpy as np
from scipy.optimize import differential_evolution

N = 720                                   # samples per crank revolution
TH = np.linspace(0.0, 2 * np.pi, N, endpoint=False)


def fourbar(params, th=TH):
    """Return (A, B, P, ok). params = b, d, dx, dz, p, q, branch(+-1) ; a == 1."""
    b, d, dx, dz, p, q, br = params
    A = np.stack([np.cos(th), np.sin(th)], axis=1)
    O4 = np.array([dx, dz])
    v = O4 - A
    dist = np.linalg.norm(v, axis=1)
    ok = np.all(dist <= b + d - 1e-6) and np.all(dist >= abs(b - d) + 1e-6)
    dist = np.clip(dist, abs(b - d) + 1e-9, b + d - 1e-9)
    # circle-circle intersection of circle(A,b) and circle(O4,d)
    l = (b * b - d * d + dist ** 2) / (2 * dist)          # distance from A along A->O4
    h = np.sqrt(np.maximum(b * b - l * l, 0.0))
    e = v / dist[:, None]
    nrm = np.stack([-e[:, 1], e[:, 0]], axis=1)
    B = A + e * l[:, None] + br * nrm * h[:, None]
    u = (B - A) / b
    n = np.stack([-u[:, 1], u[:, 0]], axis=1)
    P = A + p * u + q * n
    return A, B, P, ok


def longest_run(mask):
    """Length of longest circular run of True."""
    if mask.all():
        return len(mask)
    if not mask.any():
        return 0
    m = np.concatenate([mask, mask])
    best = cur = 0
    for x in m:
        cur = cur + 1 if x else 0
        best = max(best, cur)
    return min(best, len(mask))


TOL_FRAC = 0.04


def stance_metrics(P, tol_frac=None):
    tol_frac = TOL_FRAC if tol_frac is None else tol_frac
    """Duty factor, flatness, speed uniformity, lift for the foot path P (N,2)."""
    x, z = P[:, 0], P[:, 1]
    stride_ref = x.max() - x.min()
    if stride_ref < 1e-6:
        return None
    zmin = z.min()
    tol = tol_frac * stride_ref
    mask = z <= zmin + tol
    duty = longest_run(mask) / len(z)
    # locate the stance run (circular) to measure uniformity of x(theta)
    m2 = np.concatenate([mask, mask]); idx = np.arange(2 * len(z))
    best = (0, 0); cur = 0
    for i, f in enumerate(m2):
        cur = cur + 1 if f else 0
        if cur > best[0]:
            best = (cur, i)
    ln, end = min(best[0], len(z)), best[1]
    sl = np.arange(end - ln + 1, end + 1) % len(z)
    xs = x[sl]
    t = np.arange(len(sl))
    if len(sl) > 3:
        coef = np.polyfit(t, xs, 1)
        dev = np.max(np.abs(xs - np.polyval(coef, t)))
        slope = coef[0]
        stride = abs(xs[-1] - xs[0])
    else:
        dev, slope, stride = 1e9, 0, 0
    lift = z.max() - zmin
    # ground speed (dx/dtheta) at the start / end of stance: tripod hand-over must match
    k = 3
    if len(sl) > 2 * k + 2:
        v0 = (xs[k] - xs[0]) / k
        v1 = (xs[-1] - xs[-1 - k]) / k
        vm = (xs[-1] - xs[0]) / (len(sl) - 1)
        mismatch = abs(v1 - v0) / (abs(vm) + 1e-9)
    else:
        mismatch = 9.9
    return dict(duty=duty, stride=stride, stride_ref=stride_ref, dev=dev, slope=slope,
                lift=lift, zmin=zmin, stance_idx=sl, mismatch=mismatch)


def transmission_min(A, B, O4):
    """Min transmission angle (deg) between coupler and rocker."""
    c = B - A
    r = B - O4
    cosang = np.sum(c * r, axis=1) / (np.linalg.norm(c, axis=1) * np.linalg.norm(r, axis=1))
    ang = np.degrees(np.arccos(np.clip(cosang, -1, 1)))
    ang = np.minimum(ang, 180 - ang)
    return ang.min()


TH_OPT = np.linspace(0.0, 2 * np.pi, 360, endpoint=False)

MAX_HANG = 4.5       # leg may hang at most this many crank-lengths below the joints
MIN_CLEAR = 1.1      # every joint must sit at least this many crank-lengths above the foot
MIN_DUTY = 0.56
MIN_LIFT = 0.24      # swing lift as a fraction of stride


def cost(params, w=None):
    b, d, dx, dz, p, q, br = params
    br = 1.0 if br >= 0 else -1.0
    prm = (b, d, dx, dz, p, q, br)
    A, B, P, ok = fourbar(prm, TH_OPT)
    if not ok:
        return 1e3
    # Grashof crank-rocker: crank (1) is the shortest link, s + l < p + q
    O4len = np.hypot(dx, dz)
    links = sorted([1.0, b, d, O4len])
    if not (links[0] == 1.0 and links[0] + links[3] < links[1] + links[2]):
        return 1e3
    m = stance_metrics(P)
    if m is None:
        return 1e3
    mu = transmission_min(A, B, np.array([dx, dz]))
    s = m["stride_ref"]
    # foot should move backwards (-x) during stance when the crank turns CCW -> slope<0
    wrong_dir = 1.0 if m["slope"] > 0 else 0.0
    duty_pen = max(0.0, MIN_DUTY - m["duty"]) * 60
    flat_pen = (m["dev"] / s) * 6 + max(0.0, m["mismatch"] - 0.12) * 10   # smooth tripod hand-over
    lift_pen = max(0.0, MIN_LIFT - m["lift"] / s) * 60    # swing must clear the ground
    mu_pen = max(0.0, 38 - mu) * 0.3
    size_pen = 0.05 * (b + d + np.hypot(dx, dz))      # prefer compact
    stride_pen = max(0.0, 3.2 - m["stride"]) * 40     # stride (in crank lengths) must be real
    # leg hangs BELOW the linkage: foot is the lowest point, all joints above it
    jz = min(A[:, 1].min(), B[:, 1].min(), 0.0, dz)
    hang = jz - m["zmin"]
    clear_pen = max(0.0, MIN_CLEAR - hang) * 25 + max(0.0, hang - MAX_HANG) * 8
    # prefer a slightly longer duty and a clearer lift, but not at the cost of the rest
    return (duty_pen + flat_pen + lift_pen + mu_pen + size_pen + stride_pen + clear_pen
            + wrong_dir * 5 - 3 * m["duty"] - 2 * min(m["lift"] / s, 0.35))


def optimise(seed, maxiter=300, popsize=40):
    bounds = [(1.5, 7), (1.5, 7), (-6, 6), (-7, 1), (-5, 8), (-8, 3), (-1, 1)]
    r = differential_evolution(cost, bounds, seed=seed, maxiter=maxiter, popsize=popsize,
                               tol=1e-8, mutation=(0.4, 1.0), recombination=0.8,
                               polish=False, updating="immediate")
    return r


if __name__ == "__main__":
    seeds = [int(s) for s in sys.argv[1:]] or [1, 2, 3, 4, 5, 6]
    out = []
    for sd in seeds:
        r = optimise(sd)
        prm = list(r.x); prm[6] = 1.0 if prm[6] >= 0 else -1.0
        A, B, P, ok = fourbar(prm)
        m = stance_metrics(P)
        mu = transmission_min(A, B, np.array(prm[2:4]))
        rec = dict(seed=sd, cost=float(r.fun), params=[float(v) for v in prm], ok=bool(ok),
                   duty=float(m["duty"]), stride=float(m["stride"]), dev_rel=float(m["dev"] / m["stride_ref"]),
                   lift=float(m["lift"]), mu_min=float(mu))
        out.append(rec)
        print(json.dumps(rec))
    out.sort(key=lambda r: r["cost"])
    json.dump(out, open("candidates.json", "w"), indent=1)
