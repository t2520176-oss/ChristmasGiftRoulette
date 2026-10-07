#!/usr/bin/env python3
"""Sanity-check STL files: bed fit (Bambu P2S 256^3), closed-mesh test (every edge shared by exactly 2 triangles), volume."""
import re, sys, struct, collections
import numpy as np
BED = np.array([256.0, 256.0, 256.0])
def load(path):
    raw = open(path, "rb").read()
    if raw[:5] == b"solid":
        v = np.array(re.findall(rb"vertex\s+(\S+)\s+(\S+)\s+(\S+)", raw), dtype=float)
        return v.reshape(-1, 3, 3)
    n = struct.unpack("<I", raw[80:84])[0]
    a = np.frombuffer(raw[84:], dtype=np.dtype([("n", "<3f4"), ("v", "<9f4"), ("a", "<u2")]), count=n)
    return a["v"].reshape(-1, 3, 3).astype(float)
bad = 0
for p in sys.argv[1:]:
    t = load(p)
    lo, hi = t.reshape(-1, 3).min(0), t.reshape(-1, 3).max(0)
    size = hi - lo
    q = np.round(t * 1e4).astype(np.int64)
    edges = collections.Counter()
    for tri in q:
        for i in range(3):
            a, b = tuple(tri[i]), tuple(tri[(i + 1) % 3])
            edges[(a, b) if a < b else (b, a)] += 1
    open_e = sum(1 for c in edges.values() if c != 2)
    vol = abs(np.sum(np.einsum('ij,ij->i', t[:, 0], np.cross(t[:, 1], t[:, 2]))) / 6) / 1000
    fits = bool(np.all(size <= BED))
    ok = fits and open_e == 0
    bad += (not ok)
    print(f"{'OK ' if ok else 'BAD'} {p.split('/')[-1]:28s} size {size.round(1)} mm  vol {vol:6.1f} cm3  tris {len(t):6d}  non-manifold edges {open_e}  fits P2S {fits}")
sys.exit(1 if bad else 0)
