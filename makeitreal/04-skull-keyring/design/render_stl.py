#!/usr/bin/env python3
"""Tiny z-buffer renderer for ASCII/binary STL (no GPU needed).  python3 render_stl.py in.stl out.png [az] [el] [W]"""
import re, struct, sys
import numpy as np
from PIL import Image

def load_stl(path):
    raw = open(path, "rb").read()
    if raw[:5] == b"solid" and b"vertex" in raw[:400]:
        v = np.array(re.findall(rb"vertex\s+(\S+)\s+(\S+)\s+(\S+)", raw), dtype=float)
        return v.reshape(-1, 3, 3)
    n = struct.unpack("<I", raw[80:84])[0]
    a = np.frombuffer(raw[84:], dtype=np.dtype([("n", "<3f4"), ("v", "<9f4"), ("a", "<u2")]), count=n)
    return a["v"].reshape(-1, 3, 3).astype(float)

def render(tri, az=-60, el=25, W=1400, H=900, bg=(250, 250, 252), base=(150, 160, 175), pad=0.04, tri_colors=None):
    az, el = np.radians(az), np.radians(el)
    # camera basis: view direction d (from camera to scene)
    d = np.array([np.cos(el) * np.cos(az), np.cos(el) * np.sin(az), np.sin(el)])   # camera position direction
    right = np.array([-np.sin(az), np.cos(az), 0.0])
    up = np.cross(d, right)
    P = tri.reshape(-1, 3)
    X = P @ right; Y = P @ up; Z = P @ d          # larger Z = closer to the camera
    cx, cy = (X.min() + X.max()) / 2, (Y.min() + Y.max()) / 2
    s = min(W * (1 - 2 * pad) / (X.max() - X.min()), H * (1 - 2 * pad) / (Y.max() - Y.min()))
    sx = (X - cx) * s + W / 2; sy = H / 2 - (Y - cy) * s
    sx = sx.reshape(-1, 3); sy = sy.reshape(-1, 3); sz = Z.reshape(-1, 3)
    nrm = np.cross(tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0])
    ln = np.linalg.norm(nrm, axis=1, keepdims=True); nrm = nrm / np.where(ln == 0, 1, ln)
    light = np.array([0.35, -0.5, 0.8]); light /= np.linalg.norm(light)
    shade = 0.35 + 0.65 * np.abs(nrm @ (0.6 * d + 0.4 * light) / np.linalg.norm(0.6 * d + 0.4 * light))
    img = np.zeros((H, W, 3), float); img[:] = bg
    zb = np.full((H, W), -1e18)
    order = np.argsort(sz.mean(1))
    for i in order:
        x, y, z = sx[i], sy[i], sz[i]
        x0, x1 = int(max(0, np.floor(x.min()))), int(min(W - 1, np.ceil(x.max())))
        y0, y1 = int(max(0, np.floor(y.min()))), int(min(H - 1, np.ceil(y.max())))
        if x1 < x0 or y1 < y0: continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        den = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
        if abs(den) < 1e-9: continue
        l0 = ((y[1] - y[2]) * (gx - x[2]) + (x[2] - x[1]) * (gy - y[2])) / den
        l1 = ((y[2] - y[0]) * (gx - x[2]) + (x[0] - x[2]) * (gy - y[2])) / den
        l2 = 1 - l0 - l1
        m = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
        if not m.any(): continue
        zz = l0 * z[0] + l1 * z[1] + l2 * z[2]
        sub = zb[y0:y1 + 1, x0:x1 + 1]
        upd = m & (zz > sub)
        sub[upd] = zz[upd]
        col = np.array(base if tri_colors is None else tri_colors[i], float) * shade[i]
        img[y0:y1 + 1, x0:x1 + 1][upd] = col
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))

if __name__ == "__main__":
    tri = load_stl(sys.argv[1])
    az = float(sys.argv[3]) if len(sys.argv) > 3 else -60
    el = float(sys.argv[4]) if len(sys.argv) > 4 else 25
    W = int(sys.argv[5]) if len(sys.argv) > 5 else 1400
    render(tri, az, el, W, int(W * 0.64)).save(sys.argv[2])
    print("wrote", sys.argv[2], len(tri), "triangles")
