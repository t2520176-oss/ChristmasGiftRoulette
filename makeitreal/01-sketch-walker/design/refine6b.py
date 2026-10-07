import json
from multiprocessing import Pool
import numpy as np
import linkage_opt as L
import linkage6 as S
import refine6 as R   # sets the tightened constants + cost_ref

XMAX = 1.5            # |mean stance foot x| must stay within this many crank lengths
base = R.cost_ref

def cost_c(prm):
    c = base(prm)
    if c >= 1e3:
        return c
    g = S.sixbar(list(prm), S.TH_OPT)
    m = L.stance_metrics(g["P"])
    xm = g["P"][m["stance_idx"], 0].mean()
    return c + max(0.0, abs(xm) - XMAX) * 6

def run(arg):
    sd, x0 = arg
    r = S.differential_evolution(cost_c, S.BOUNDS, seed=sd, maxiter=320, popsize=20, tol=1e-9, mutation=(0.3, 0.9),
                                 recombination=0.85, polish=False, updating="immediate", x0=x0)
    prm = list(r.x); prm[12] = 1.0 if prm[12] >= 0 else -1.0; prm[13] = 1.0 if prm[13] >= 0 else -1.0
    g = S.sixbar(prm); m = L.stance_metrics(g["P"])
    return dict(seed=sd, cost=float(r.fun), params=[float(v) for v in prm], duty=float(m["duty"]),
                dev=float(m["dev"] / m["stride_ref"]), mism=float(m["mismatch"]), lift_rel=float(m["lift"] / m["stride_ref"]),
                stride=float(m["stride"]), mu=float(S.mu_min(g)), hang=float(S.joints_z(g, prm) - m["zmin"]),
                arm=float(np.linalg.norm(g["P"] - g["C"], axis=1).max()), xfoot=float(g["P"][m["stance_idx"], 0].mean()), ok=g["ok"])

if __name__ == "__main__":
    prev = json.load(open("candidates6_refined.json"))
    jobs = []
    for i in (1, 4, 2, 0):
        for k in range(2):
            jobs.append((200 + i * 10 + k, np.array(prev[i]["params"])))
    with Pool(4) as p:
        res = p.map(run, jobs)
    res.sort(key=lambda r: r["cost"])
    json.dump(res, open("candidates6_centered.json", "w"), indent=1)
    for r in res:
        print({k: (round(v, 3) if isinstance(v, float) else v) for k, v in r.items() if k != "params"})
