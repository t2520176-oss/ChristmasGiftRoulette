import json, sys
from multiprocessing import Pool
import numpy as np
import linkage_opt as L
import linkage6 as S

S.MIN_DUTY, S.MIN_LIFT, S.MIN_HANG, S.MAX_HANG, S.MAX_EXTENT = 0.60, 0.25, 2.0, 4.0, 9.5
MIN_MU, MAX_ARM, MAX_LINK = 36.0, 6.6, 7.0
base_cost = S.cost6

def cost_ref(prm):
    c = base_cost(prm)
    if c >= 1e3:
        return c
    g = S.sixbar(list(prm), S.TH_OPT)
    mu = S.mu_min(g)
    arm = np.linalg.norm(g["P"] - g["C"], axis=1).max()
    lens = [prm[0], prm[1], prm[6], prm[7], np.hypot(prm[2], prm[3]), np.hypot(prm[8] - prm[2], prm[9] - prm[3])]
    pen = max(0, MIN_MU - mu) * 1.2 + max(0, arm - MAX_ARM) * 8 + max(0, max(lens) - MAX_LINK) * 8
    # keep the three ground pivots reasonably apart so the frame can carry them
    d46 = np.hypot(prm[8] - prm[2], prm[9] - prm[3])
    pen += max(0, 1.6 - d46) * 5 + max(0, 1.6 - np.hypot(prm[2], prm[3])) * 5
    return c + pen

S.cost6 = cost_ref

def run(arg):
    sd, x0 = arg
    r = S.differential_evolution(cost_ref, S.BOUNDS, seed=sd, maxiter=300, popsize=20, tol=1e-9, mutation=(0.3, 0.9),
                                 recombination=0.85, polish=False, updating="immediate", x0=x0)
    prm = list(r.x); prm[12] = 1.0 if prm[12] >= 0 else -1.0; prm[13] = 1.0 if prm[13] >= 0 else -1.0
    g = S.sixbar(prm); m = L.stance_metrics(g["P"])
    arm = np.linalg.norm(g["P"] - g["C"], axis=1).max()
    return dict(seed=sd, cost=float(r.fun), params=[float(v) for v in prm], duty=float(m["duty"]),
                dev=float(m["dev"] / m["stride_ref"]), mism=float(m["mismatch"]), lift_rel=float(m["lift"] / m["stride_ref"]),
                stride=float(m["stride"]), mu=float(S.mu_min(g)), hang=float(S.joints_z(g, prm) - m["zmin"]),
                arm=float(arm), ok=g["ok"])

if __name__ == "__main__":
    prev = json.load(open("candidates6.json"))
    jobs = []
    for i, c in enumerate(prev[:5]):
        for k in range(2):
            x0 = np.array(c["params"]); x0[12] = x0[12]; x0[13] = x0[13]
            jobs.append((100 + i * 10 + k, x0))
    with Pool(4) as p:
        res = p.map(run, jobs)
    res.sort(key=lambda r: r["cost"])
    json.dump(res, open("candidates6_refined.json", "w"), indent=1)
    for r in res:
        print({k: (round(v, 3) if isinstance(v, float) else v) for k, v in r.items() if k != "params"})
