#!/usr/bin/env python3
"""Colour preview of the cat keyring parts (software renderer).  python3 preview.py out/4color"""
import os, sys
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from render_stl import load_stl, render
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "out", "4color")
COL = {"lavender": (178, 153, 242), "black": (17, 17, 19), "yellow": (255, 199, 26), "pink": (255, 153, 178), "white": (255, 255, 255)}
tris, cols = [], []
for k, c in COL.items():
    p = os.path.join(OUT, f"{k}.stl")
    if os.path.exists(p):
        t = load_stl(p); tris.append(t); cols += [c] * len(t)
tri = np.concatenate(tris); cols = np.array(cols, float)
for name, az, el, W, hr in (("preview_top", -90, 89.9, 1000, 0.9), ("preview_iso", -70, 38, 1000, 0.8)):
    render(tri, az, el, W, int(W * hr), bg=(236, 236, 240), tri_colors=cols).save(os.path.join(OUT, name + ".png")); print("wrote", name)
