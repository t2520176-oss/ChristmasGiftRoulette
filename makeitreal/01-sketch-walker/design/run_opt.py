import json, sys
from multiprocessing import Pool
import numpy as np
import linkage_opt as L

def one(sd):
    r = L.optimise(sd, maxiter=250, popsize=30)
    prm = list(r.x); prm[6] = 1.0 if prm[6] >= 0 else -1.0
    A, B, P, ok = L.fourbar(prm)
    m = L.stance_metrics(P)
    mu = L.transmission_min(A, B, np.array(prm[2:4]))
    jz = min(A[:, 1].min(), B[:, 1].min(), 0.0, prm[3])
    return dict(seed=sd, cost=float(r.fun), params=[float(v) for v in prm], ok=bool(ok),
                duty=float(m["duty"]), stride=float(m["stride"]), dev_rel=float(m["dev"] / m["stride_ref"]),
                lift=float(m["lift"]), lift_rel=float(m["lift"] / m["stride_ref"]), mu_min=float(mu),
                clear=float(jz - m["zmin"]))

if __name__ == "__main__":
    seeds = [int(x) for x in sys.argv[1:]] or list(range(1, 9))
    with Pool(min(len(seeds), 8)) as p:
        out = p.map(one, seeds)
    out.sort(key=lambda r: r["cost"])
    json.dump(out, open("candidates.json", "w"), indent=1)
    for o in out:
        print({k: (round(v, 3) if isinstance(v, float) else v) for k, v in o.items() if k != "params"})
