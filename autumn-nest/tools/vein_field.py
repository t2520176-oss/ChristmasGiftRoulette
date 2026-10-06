#!/usr/bin/env python3
"""Wall-thickness map of a leaf-vein lithophane shade (entry #10).

    python vein_field.py field.npz [--leaves 3] [--seed 7] [--height 120] [--r-bottom 36]
                         [--r-max 44] [--r-top 31] [--res 0.7] [--t-vein 0.8] [--t-mem 1.8] [--t-bg 2.8]
                         [--preview field.png]

A lithophane works because thin plastic passes more light than thick plastic. Here the veins
are thin (bright), the leaf blade is medium (dim glow) and the background is thick (dark).
The shade is a surface of revolution; the map is drawn on the unrolled surface (u = arc length,
v = height). Output: thickness T[v, u] in mm, plus the parameters, for blender_vein_shade.py.

The vein network is built from three levels: a midrib per leaf, curved secondary veins and a
ladder of tertiary cross veins; each level is a distance field turned into thickness by a
smoothstep, so the wall changes gradually (steep steps would print as overhangs on the inside).
"""
import argparse
import math

import numpy as np
from scipy import ndimage

ALPHA = 1.2  # assumed absorption of white PLA, 1/mm (T = exp(-alpha * t)); calibrate with the step wedge


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def profile(z, h, r0, rmax, rtop, zm_frac=0.45):
    """Outer radius and slope of the shade: vertical at both ends, widest at zm_frac."""
    zm = zm_frac * h
    z = np.asarray(z, dtype=float)
    lo = z <= zm
    s1 = np.pi / 2 * np.clip(z / zm, 0, 1)
    s2 = np.pi / 2 * np.clip((z - zm) / (h - zm), 0, 1)
    r = np.where(lo, r0 + (rmax - r0) * np.sin(s1) ** 2, rmax - (rmax - rtop) * np.sin(s2) ** 2)
    dr = np.where(lo, (rmax - r0) * np.sin(2 * s1) * (np.pi / 2) / zm,
                  -(rmax - rtop) * np.sin(2 * s2) * (np.pi / 2) / (h - zm))
    return r, dr


def leaf_halfwidth(s, wmax):
    s = np.clip(s, 0, 1)
    return wmax * np.sin(np.pi * s ** 0.8) ** 0.85


def march_secondary(rng, uk, vi, side, alpha0, v0, lf, wmax, res):
    """A curved secondary vein from the midrib toward the leaf margin."""
    pts = [(uk, vi)]
    u, v = uk, vi
    for m in range(400):
        theta = math.radians(alpha0) * (1 - 0.55 * min(1.0, m / 60.0))
        u += side * math.sin(theta) * 0.8
        v += math.cos(theta) * 0.8
        s = (v - v0) / lf
        if s >= 0.97 or abs(u - uk) > 0.93 * leaf_halfwidth(s, wmax):
            break
        pts.append((u, v))
    return np.array(pts)


def draw(img, pts, res, nu, nv):
    """Mark the pixels of a polyline (u wraps around the shade)."""
    for (u0, v0), (u1, v1) in zip(pts[:-1], pts[1:]):
        n = max(2, int(math.hypot(u1 - u0, v1 - v0) / (res * 0.5)))
        us = np.linspace(u0, u1, n)
        vs = np.linspace(v0, v1, n)
        iu = np.round(us / res).astype(int) % nu
        iv = np.round(vs / res).astype(int)
        ok = (iv >= 0) & (iv < nv)
        img[iv[ok], iu[ok]] = True


def periodic_distance(img, res):
    """Distance (mm) to the nearest True pixel, wrapping around in u."""
    nu = img.shape[1]
    big = np.concatenate([img, img, img], axis=1)
    d = ndimage.distance_transform_edt(~big) * res
    return d[:, nu:2 * nu]


def build(args):
    h, res = args.height, args.res
    r_ref = args.r_max
    circ = 2 * math.pi * r_ref
    nu = int(round(circ / res))
    res_u = circ / nu
    nv = int(round(h / res))
    res_v = h / nv
    # use one pixel size for the distance transforms (they are nearly equal)
    res_px = 0.5 * (res_u + res_v)
    u = (np.arange(nu) + 0.0) * res_u
    v = np.arange(nv + 1) * res_v
    U, V = np.meshgrid(u, v)
    rng = np.random.default_rng(args.seed)

    sector = circ / args.leaves
    wmax = 0.36 * sector
    rim_b = 8.0
    lf = 0.80 * h
    mask = np.zeros_like(U, dtype=bool)
    mid = np.zeros_like(mask)
    sec = np.zeros_like(mask)
    ter = np.zeros_like(mask)

    for k in range(args.leaves):
        uk = (k + 0.5) * sector + rng.uniform(-0.04, 0.04) * sector
        v0 = rim_b + 5 + rng.uniform(-2, 3)
        lfk = lf * rng.uniform(0.93, 1.0)
        # blade
        du = ((U - uk + circ / 2) % circ) - circ / 2
        s = (V - v0) / lfk
        mask |= (s > 0) & (s < 1) & (np.abs(du) < leaf_halfwidth(s, wmax))
        # midrib with a petiole down to the bottom rim
        draw(mid, np.array([[uk, rim_b + 1.5], [uk, v0 + 0.97 * lfk]]), res_px, nu, nv + 1)
        # secondary veins
        n_sec = 8
        secs = {-1: [], 1: []}
        for i in range(n_sec):
            si = 0.12 + 0.76 * i / (n_sec - 1)
            vi = v0 + si * lfk
            for side in (-1, 1):
                a0 = 60 - 20 * si + rng.uniform(-3, 3)
                pts = march_secondary(rng, uk, vi, side, a0, v0, lfk, wmax, res_px)
                secs[side].append(pts)
                if len(pts) > 1:
                    draw(sec, pts, res_px, nu, nv + 1)
        # tertiary cross veins between neighbouring secondaries
        for side in (-1, 1):
            for a, b in zip(secs[side][:-1], secs[side][1:]):
                for f in (0.3, 0.55, 0.8):
                    pa = a[min(len(a) - 1, int(f * (len(a) - 1)))]
                    pb = b[min(len(b) - 1, int(f * (len(b) - 1)))]
                    draw(ter, np.array([pa, pb]), res_px, nu, nv + 1)

    # blade vs background: smooth edge so the inside wall slope stays gentle
    d_to_bg = periodic_distance(~mask, res_px)   # for a blade pixel: distance to the nearest background pixel
    d_to_blade = periodic_distance(mask, res_px)  # for a background pixel: distance to the nearest blade pixel
    sd = np.where(mask, d_to_bg, -d_to_blade)   # signed distance, positive inside the blade
    blend = smoothstep(-2.5, 2.5, sd)
    t = args.t_bg + (args.t_mem - args.t_bg) * blend

    # veins: core (full depth) then a smooth fall-off back to the blade thickness
    for img, core, wid, tv in ((mid, 0.8, 3.4, args.t_vein), (sec, 0.5, 2.4, args.t_vein + 0.1),
                               (ter, 0.35, 1.8, args.t_vein + 0.3)):
        d = periodic_distance(img, res_px)
        f = 1 - smoothstep(core, wid, d)
        t = np.minimum(t, t * (1 - f) + tv * f)

    # plain rims keep the shade stiff and give the base something to grip
    wr = smoothstep(rim_b, rim_b + 3.0, V) * (1 - smoothstep(h - 7.0, h - 4.0, V))
    t = 2.4 * (1 - wr) + t * wr
    t = np.clip(t, args.t_vein, args.t_bg)
    return u, v, t, dict(circ=circ, nu=nu, nv=nv)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("out")
    ap.add_argument("--leaves", type=int, default=3)
    ap.add_argument("--seed", type=int, default=7)
    ap.add_argument("--height", type=float, default=120.0)
    ap.add_argument("--r-bottom", type=float, default=36.0)
    ap.add_argument("--r-max", type=float, default=44.0)
    ap.add_argument("--r-top", type=float, default=31.0)
    ap.add_argument("--res", type=float, default=0.7, help="grid size (mm)")
    ap.add_argument("--t-vein", type=float, default=0.8)
    ap.add_argument("--t-mem", type=float, default=1.8)
    ap.add_argument("--t-bg", type=float, default=2.8)
    ap.add_argument("--preview")
    a = ap.parse_args()

    u, v, t, info = build(a)
    np.savez_compressed(a.out, u=u, v=v, t=t, height=a.height, r_bottom=a.r_bottom, r_max=a.r_max,
                        r_top=a.r_top, alpha=ALPHA, circ=info["circ"])
    print(f"wrote {a.out}: grid {t.shape[1]} x {t.shape[0]}, thickness {t.min():.2f}..{t.max():.2f} mm")
    if a.preview:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        light = np.exp(-ALPHA * t)
        fig, ax = plt.subplots(2, 1, figsize=(11, 8), dpi=110)
        im0 = ax[0].imshow(t, origin="lower", cmap="viridis_r", extent=[0, info["circ"], 0, a.height], aspect="equal")
        ax[0].set_title("wall thickness (mm), unrolled shade")
        fig.colorbar(im0, ax=ax[0], shrink=0.8)
        ax[1].imshow(light, origin="lower", cmap="afmhot", vmin=0, vmax=light.max(),
                     extent=[0, info["circ"], 0, a.height], aspect="equal")
        ax[1].set_title(f"simulated glow, exp(-{ALPHA} * thickness) (assumed absorption, see README)")
        fig.tight_layout()
        fig.savefig(a.preview)
        print("wrote", a.preview)


if __name__ == "__main__":
    main()
