import json, sys
from multiprocessing import Pool
import numpy as np
import linkage_opt as L

def run(args):
    sd, mind, minlift = args
    L.MIN_DUTY, L.MIN_LIFT = mind, minlift
    r = L.optimise(sd, maxiter=200, popsize=25)
    prm = list(r.x); prm[6] = 1.0 if prm[6] >= 0 else -1.0
    A, B, P, ok = L.fourbar(prm); m = L.stance_metrics(P)
    mu = L.transmission_min(A, B, np.array(prm[2:4]))
    jz = min(A[:, 1].min(), B[:, 1].min(), 0.0, prm[3])
    return dict(seed=sd, mind=mind, minlift=minlift, cost=round(float(r.fun), 2), duty=round(float(m["duty"]), 3),
                dev=round(float(m["dev"] / m["stride_ref"]), 3), lift_rel=round(float(m["lift"] / m["stride_ref"]), 3),
                stride=round(float(m["stride"]), 2), mu=round(float(mu), 1), clear=round(float(jz - m["zmin"]), 2), params=[float(v) for v in prm])

if __name__ == "__main__":
    jobs = [(sd, md, ml) for md, ml in [(0.5, 0.2), (0.54, 0.2), (0.58, 0.16)] for sd in (11, 12, 13, 14)]
    with Pool(4) as p:
        res = p.map(run, jobs)
    json.dump(res, open("sweep.json", "w"), indent=1)
    for r in res:
        print({k: v for k, v in r.items() if k != "params"})
