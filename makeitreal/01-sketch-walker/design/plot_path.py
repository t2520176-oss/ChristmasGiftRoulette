import json, sys
import numpy as np
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
from linkage_opt import fourbar, stance_metrics, TH

cands = json.load(open(sys.argv[1] if len(sys.argv) > 1 else "candidates.json"))
prm = cands[0]["params"]
A, B, P, ok = fourbar(prm)
m = stance_metrics(P)
fig, ax = plt.subplots(1, 2, figsize=(13, 5.5))
a0 = ax[0]
a0.plot(P[:, 0], P[:, 1], "k-", lw=1.5, label="foot path")
sl = m["stance_idx"]
a0.plot(P[sl, 0], P[sl, 1], "r-", lw=3, label=f"stance ({m['duty']*100:.0f}% of cycle)")
a0.axhline(m["zmin"], color="g", ls=":", lw=1)
for k in range(0, len(TH), 90):
    a0.plot([0, A[k, 0], B[k, 0], prm[2]], [0, A[k, 1], B[k, 1], prm[3]], lw=0.8, alpha=0.7)
    a0.plot([A[k, 0], P[k, 0], B[k, 0]], [A[k, 1], P[k, 1], B[k, 1]], "k-", lw=0.4, alpha=0.5)
a0.plot([0], [0], "ko"); a0.plot([prm[2]], [prm[3]], "ks")
a0.set_aspect("equal"); a0.legend(); a0.set_title("linkage (a=1) and foot path")
a1 = ax[1]
a1.plot(np.degrees(TH), P[:, 1] - m["zmin"], label="foot height")
a1.plot(np.degrees(TH), P[:, 0] - P[:, 0].mean(), label="foot x")
a1.set_xlabel("crank angle [deg]"); a1.legend(); a1.grid(alpha=0.3)
a1.set_title(f"duty={m['duty']:.2f} stride={m['stride']:.2f}a lift={m['lift']:.2f}a")
plt.tight_layout(); plt.savefig("foot_path_candidate.png", dpi=110)
print({k: (float(v) if np.isscalar(v) else '...') for k, v in m.items()})
