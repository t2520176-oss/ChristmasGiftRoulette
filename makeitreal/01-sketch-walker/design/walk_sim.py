#!/usr/bin/env python3
"""
Kinematic walk simulation of the whole walker (side view): six six-bar legs, tripod gait.
Checks the *physical* gait: foot heights on the ground, slip between legs, body height ripple and speed.
Writes walk_sim.gif and prints metrics.
"""
import json, sys
import numpy as np
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation, PillowWriter
import linkage6 as S, linkage_opt as L

c = json.load(open("candidates6_refined.json"))[1]
a = 9.0
g = S.sixbar(c["params"]); n = len(g["A"])
P = g["P"] * a
spacing = 60.0
m = L.stance_metrics(g["P"])
stance = np.zeros(n, bool); stance[m["stance_idx"]] = True
legs = [("F", +spacing, 0), ("M", 0.0, 360), ("R", -spacing, 0)]    # (name, x0, phase in samples = 180 deg)

def feet(k):
    out = []
    for name, x0, ph in legs:
        for side in (0, 1):
            kk = (k + ph + (360 if side else 0)) % n
            out.append((name, side, x0 + P[kk, 0], P[kk, 1], stance[kk], kk))
    return out

# ---- body motion: the body advances so that the supporting feet stay fixed on the ground ----
# per step dk (0.5 deg): body dx = -mean over stance feet of d(foot_x)
ks = np.arange(0, 2 * n)                  # two revolutions
bx = np.zeros(len(ks)); bz = np.zeros(len(ks)); slip_max = 0.0; nstance_min = 99
for i in range(1, len(ks)):
    k0, k1 = ks[i - 1] % n, ks[i] % n
    f0, f1 = feet(k0), feet(k1)
    sup = [j for j in range(6) if f0[j][4] and f1[j][4]]
    nstance_min = min(nstance_min, len(sup))
    if sup:
        dxs = np.array([-(f1[j][2] - f0[j][2]) for j in sup])    # body displacement implied by each supporting foot
        bx[i] = bx[i - 1] + dxs.mean()
        slip_max = max(slip_max, np.abs(dxs - dxs.mean()).max())
        bz[i] = -np.mean([f1[j][3] for j in sup])                  # body height above ground (ground = foot z)
    else:
        bx[i] = bx[i - 1]; bz[i] = bz[i - 1]
step = bx[n] - bx[0]
print(f"body advance per crank turn: {step:.1f} mm  (stride {m['stride']*a:.1f} mm, duty {m['duty']:.2f})")
print(f"min #supporting feet during the cycle (of 6): {nstance_min}")
print(f"max foot slip between simultaneously supporting feet per 0.5 deg step: {slip_max:.3f} mm")
z = bz[n:2 * n]; z = z[z > 0]
print(f"body height ripple (supporting feet): {z.min():.1f} .. {z.max():.1f} mm  (p-p {z.max()-z.min():.2f} mm)")
# vertical mismatch: stance feet z relative to the lowest one
zz = []
for k in range(0, n, 2):
    f = feet(k); zs = [x[3] for x in f if x[4]]
    zz.append(max(zs) - min(zs))
print(f"max height difference between feet that are both 'in stance': {max(zz):.2f} mm")

if "--gif" in sys.argv:
    import re
    from shapely.geometry import Polygon, box
    from matplotlib.patches import Polygon as MPoly
    outline = open("../scad/body_outline.scad").read()
    pts = np.array([[float(u), float(v)] for u, v in re.findall(r"\[(-?[0-9.]+),(-?[0-9.]+)\]", re.search(r"body_pts = \[(.*?)\];", outline, re.S).group(1))])
    paths = eval(re.search(r"body_paths = (\[\[.*?\]\]);", outline, re.S).group(1))
    body_poly = Polygon(pts[paths[0]], [pts[p] for p in paths[1:]]) if len(paths) > 1 else Polygon(pts[paths[0]])
    def scad_vars(text):
        e = {}
        for name, expr in re.findall(r"\b([A-Za-z_]\w*)\s*=\s*([^;=]+);", re.sub(r"//.*", "", text)):
            try: e[name] = eval(expr, {}, dict(e))
            except Exception: pass
        return e
    env = scad_vars(open("../scad/walker.scad").read())
    env["front_end"] = 74.0
    clipped = body_poly.intersection(box(-1, env["body_cut"], 1000, 1000))
    top = env["deck_z"] + env["deck_t"]
    fig, ax = plt.subplots(figsize=(9, 6.2))
    frames = list(range(0, n, 6))
    def draw(fi):
        ax.clear(); ax.set_aspect("equal"); ax.set_xlim(-190, 150); ax.set_ylim(-60, 140)
        k = frames[fi]; xb = bx[k]
        gz = P[:, 1].min() - 0.0
        ax.fill_between([-300, 400], [gz - 12] * 2, [gz - 0.6] * 2, color="#d9d4c7"); ax.axhline(gz - 0.6, color="#8a8473", lw=1)
        for i in range(-6, 12): ax.plot([i * 40 - (xb % 40), i * 40 - (xb % 40) - 8], [gz - 0.6, gz - 6], color="#8a8473", lw=0.7)
        # body + deck
        for geom in ([clipped] if clipped.geom_type == "Polygon" else list(clipped.geoms)):
            xy = np.array(geom.exterior.coords); xy = np.c_[xb + env["body_x0"] + xy[:, 0], top + xy[:, 1] - env["body_cut"]]
            ax.add_patch(MPoly(xy, closed=True, fc="#f2e3a1", ec="#7a6a2a", lw=1.2, zorder=3))
        ax.add_patch(MPoly([[xb + env["rear_end"], 0 + env["deck_z"]], [xb + 66, env["deck_z"]], [xb + 66, top], [xb + env["rear_end"], top]], fc="#b0b6bf", ec="#555", zorder=2))
        ax.plot([xb + env["rear_end"] - 6, xb + 74], [0, 0], "-", color="#999", lw=0.8, zorder=1)
        for side, alpha, lw in ((1, 0.35, 1.5), (0, 1.0, 2.2)):
            for name, x0, ph in legs:
                kk = (k + ph + (360 if side else 0)) % n
                gg = S.sixbar(c["params"], np.array([2 * np.pi * kk / n]))
                O4 = gg["O4"] * a; O6 = gg["O6"] * a
                def X(p): return (xb + x0 + p[0], p[1])
                A, B, C, E, Pp = (gg[q][0] * a for q in ("A", "B", "C", "E", "P"))
                z = 4 if side == 0 else 3
                ax.plot(*zip(X([0, 0]), X(A)), color="tomato", lw=lw + 0.6, alpha=alpha, zorder=z)
                ax.plot(*zip(X(A), X(C), X(B), X(A)), color="goldenrod", lw=lw, alpha=alpha, zorder=z)
                ax.plot(*zip(X(B), X(O4)), color="deepskyblue", lw=lw, alpha=alpha, zorder=z)
                ax.plot(*zip(X(C), X(E), X(O6)), color="orchid", lw=lw, alpha=alpha, zorder=z)
                ax.plot(*zip(X(C), X(Pp)), color="seagreen", lw=lw + 1.2, alpha=alpha, zorder=z)
                ax.plot(*X(Pp), "ko", ms=5 if side == 0 else 4, alpha=alpha, zorder=z + 1)
        ax.set_title(f"Sketch Walker - tripod gait, crank {360*k/n:5.1f} deg   (left side solid, right side faded)", fontsize=10)
        ax.set_xticks([]); ax.set_yticks([])
    ani = FuncAnimation(fig, draw, frames=len(frames))
    ani.save("../out/walk_sim.gif", writer=PillowWriter(fps=20), dpi=80)
    print("wrote walk_sim.gif")
