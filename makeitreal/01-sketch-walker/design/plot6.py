import json, sys
import numpy as np
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
import linkage_opt as L
import linkage6 as S

cands = json.load(open(sys.argv[1] if len(sys.argv) > 1 else "candidates6_refined.json"))
idx = [int(a) for a in sys.argv[2:]] or [1]
fig, axes = plt.subplots(len(idx), 2, figsize=(14, 5.4 * len(idx)), squeeze=False)
for row, i in enumerate(idx):
    c = cands[i]; prm = c["params"]
    g = S.sixbar(prm); P = g["P"]; m = L.stance_metrics(P)
    ax = axes[row][0]
    ax.plot(P[:, 0], P[:, 1], "k-", lw=1.4)
    sl = m["stance_idx"]; ax.plot(P[sl, 0], P[sl, 1], "r-", lw=3)
    ax.axhline(m["zmin"], color="g", ls=":", lw=1)
    for k in range(0, 720, 60):
        A, B, C, E, Pk = g["A"][k], g["B"][k], g["C"][k], g["E"][k], g["P"][k]
        O4, O6 = g["O4"], g["O6"]
        ax.plot([0, A[0], B[0], O4[0]], [0, A[1], B[1], O4[1]], "-", lw=0.7, alpha=0.7, color="tab:blue")
        ax.plot([C[0], E[0], O6[0]], [C[1], E[1], O6[1]], "-", lw=0.7, alpha=0.7, color="tab:orange")
        ax.plot([C[0], Pk[0]], [C[1], Pk[1]], "-", lw=0.7, alpha=0.7, color="tab:green")
    ax.plot([0], [0], "ko"); ax.plot(*g["O4"], "ks"); ax.plot(*g["O6"], "k^")
    ax.set_aspect("equal"); ax.set_title(f"cand {i} seed {c['seed']}: duty={c['duty']:.2f} stride={c['stride']:.2f}a hang={c['hang']:.2f}a mu={c['mu']:.0f}")
    ax2 = axes[row][1]; th = np.degrees(L.TH)
    ax2.plot(th, P[:, 1] - m["zmin"], label="foot height [a]"); ax2.plot(th, P[:, 0] - P[:, 0].mean(), label="foot x [a]")
    ax2.legend(); ax2.grid(alpha=.3); ax2.set_xlabel("crank angle [deg]")
    ax.set_xlabel("x [crank lengths a=1;  a = 9 mm]"); ax.set_ylabel("z")
plt.tight_layout(); plt.savefig("../out/foot_path_final.png", dpi=100)
