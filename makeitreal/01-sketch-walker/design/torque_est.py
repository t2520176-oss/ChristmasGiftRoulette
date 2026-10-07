#!/usr/bin/env python3
"""Frictionless crank torque from virtual work (gravity of the body + links).  Friction is NOT included - see README."""
import json
import numpy as np
import linkage6 as S, linkage_opt as L

c = json.load(open("candidates6_refined.json"))[1]; a = 9.0
g = S.sixbar(c["params"]); n = len(g["A"]); P = g["P"] * a
m = L.stance_metrics(g["P"]); stance = np.zeros(n, bool); stance[m["stance_idx"]] = True
W_total = 0.225 * 9.81              # N, whole walker
legs = [(0, 0), (360, 0), (0, 0)]   # (phase offset in samples of the three shafts: F, M, R) ; right side adds 360
def ph(i, side): return ((360 if i == 1 else 0) + (360 if side else 0)) % n
th = 2 * np.pi * np.arange(n) / n
dth = th[1] - th[0]
# link masses [kg] from volumes (PETG, ~95 % fill): crank .38, coupler .75, rocker .73, foot 2.13, elink .75 cm3
mass = dict(crank=.38, coupler=.75, rocker=.73, foot=2.13, elink=.75)
mass = {k: v * 1.25 * 0.95 / 1000 for k, v in mass.items()}
def link_com_z(k):
    A, B, C, E, Pp = (g[q][k] * a for q in ("A", "B", "C", "E", "P")); O4, O6 = g["O4"] * a, g["O6"] * a
    return dict(crank=A[1] / 2, coupler=(A[1] + B[1] + C[1]) / 3, rocker=(O4[1] + B[1]) / 2, foot=(C[1] + E[1] + 2 * Pp[1]) / 4, elink=(E[1] + O6[1]) / 2)
zc = np.array([[link_com_z(k)[nm] for nm in mass] for k in range(n)])          # mm
tau = np.zeros(n)                                                                   # N*mm, torque on the crank *shaft* of ONE leg set (3 shafts summed below)
# Total potential energy U(theta_motor) = body height term + links; tau = dU/dtheta
U = np.zeros(n)
for k in range(n):
    # body height: mean of supporting feet (ground fixed): body_z = -mean(foot_z of supporting feet)  (feet are on the ground)
    sup = [P[(k + ph(i, s)) % n, 1] for i in range(3) for s in (0, 1) if stance[(k + ph(i, s)) % n]]
    body_z = -np.mean(sup) if sup else 0
    Ulinks = sum(zc[(k + ph(i, s)) % n, j] * list(mass.values())[j] * 9.81 for i in range(3) for s in (0, 1) for j in range(5))   # N*mm
    U[k] = W_total * body_z + Ulinks
dU = np.gradient(np.concatenate([U, U, U]), dth)[n:2 * n]          # N*mm per rad of crank angle (all shafts turn together)
print(f"frictionless gravity torque on the main shaft: peak {np.abs(dU).max():.1f} N*mm  ({np.abs(dU).max()/98.1:.3f} kg*cm), rms {np.sqrt((dU**2).mean()):.1f} N*mm")
print("=> friction in 36 printed pin joints + 5 gear meshes dominates; size the motor by that, not by gravity (see README).")
