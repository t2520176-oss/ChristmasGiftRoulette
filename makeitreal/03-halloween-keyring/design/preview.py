#!/usr/bin/env python3
"""Colour preview of the keyring parts (software renderer).  python3 preview.py [out_dir]"""
import os, sys
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from render_stl import load_stl, render
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "out")
COL = {"cream": (238, 220, 180), "black": (28, 28, 32), "white": (250, 250, 250), "green": (132, 200, 52),
       "gold": (242, 190, 40), "purple": (140, 100, 210), "pink": (250, 150, 175)}
tris, cols = [], []
for k, c in COL.items():
    t = load_stl(os.path.join(OUT, f"{k}.stl")); tris.append(t); cols += [c] * len(t)
tri = np.concatenate(tris); cols = np.array(cols, float)
for name, az, el, W in (("preview_top", -90, 89.9, 1000), ("preview_iso", -70, 38, 1000)):
    img = render(tri, az, el, W, int(W * (1.05 if name == "preview_top" else 0.9)), bg=(236, 236, 240), tri_colors=cols)
    img.save(os.path.join(OUT, name + ".png")); print("wrote", name)
