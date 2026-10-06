#!/usr/bin/env python3
"""How evenly does a small LED light the lithophane shade? (entry #10)

    python lamp_light.py field.npz [--led-z 65] [--sweep]

A point-like LED inside a tall shade lights the part of the wall near it far more than the part
far away (inverse square law). This estimates the relative brightness of every point of the wall:

    B = w_emit * cos_i / D^2 * exp(-alpha * t / cos_i)

  D        distance from the LED to the inner wall surface
  cos_i    cosine of the angle between the wall normal and the direction to the LED
  w_emit   0.5 * (1 + cos(angle from the LED axis)): a diffused LED dome lights the upper half most
  alpha    absorption of white PLA, ASSUMED 1.2 per mm; t is the wall thickness

It is a simple model (no reflections inside the shade, no scattering in the wall), good for
comparing LED heights and for the preview render, not for predicting absolute brightness.
`--sweep` prints how the brightness of the veins at the bottom and top of the shade compares for a
range of LED heights (measured from the bottom edge of the shade).

`--disc-r R` models a flat round LED puck instead of a point: a disc of radius R whose top face emits
(Lambertian: brightness ~ cos of the angle from the disc normal, nothing downward), sampled at many points.
The height given by --led-z / --sweep is then the height of the lit face above the bottom edge of the shade.
"""
import argparse
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vein_field import profile  # noqa: E402


def wall_points(f):
    t, v = f["t"], f["v"]
    nv1, nu = t.shape
    h, r0, rmax, rtop = float(f["height"]), float(f["r_bottom"]), float(f["r_max"]), float(f["r_top"])
    r, dr = profile(v, h, r0, rmax, rtop)
    th = 2 * np.pi * np.arange(nu) / nu
    c, s = np.cos(th)[None, :], np.sin(th)[None, :]
    outer = np.stack([r[:, None] * c, r[:, None] * s, np.repeat(v[:, None], nu, 1)], -1)
    n = np.stack([np.broadcast_to(c, (nv1, nu)), np.broadcast_to(s, (nv1, nu)), -np.repeat(dr[:, None], nu, 1)], -1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True)
    inner = outer - t[..., None] * n
    return inner, n, t


def brightness(f, led_z, alpha=None, occluder_r=0.0, up_only=False, disc_r=0.0):
    """Relative brightness of every wall point (same shape as the thickness map).

    occluder_r > 0: the post and the LED body (a cylinder of this radius up to the LED height) block the
    light that would go to wall points below the LED. up_only: the LED only lights the upper hemisphere
    (a tealight), with a small residual from its diffusing dome. disc_r > 0: a flat round puck whose top
    face (radius disc_r, at height led_z) emits Lambertian light; it needs no occluder because nothing
    is emitted downward."""
    alpha = float(f["alpha"]) if alpha is None else alpha
    inner, n, t = wall_points(f)
    if disc_r > 0:
        # sample the disc: centre, then rings of points
        samples = [(0.0, 0.0)]
        for rad, cnt in ((0.5, 8), (1.0, 12)):
            samples += [(rad * disc_r * np.cos(2 * np.pi * k / cnt), rad * disc_r * np.sin(2 * np.pi * k / cnt))
                        for k in range(cnt)]
        total = np.zeros(t.shape)
        for sx, sy in samples:
            d = inner - np.array([sx, sy, led_z])
            dist = np.linalg.norm(d, axis=-1)
            dhat = d / dist[..., None]
            cos_i = np.clip(np.sum(n * dhat, axis=-1), 0.05, 1.0)
            w_emit = np.clip(dhat[..., 2], 0.0, 1.0)             # Lambertian about the disc normal (+z)
            total += w_emit * cos_i / dist ** 2 * np.exp(-alpha * t / cos_i)
        return total / len(samples)
    d = inner - np.array([0.0, 0.0, led_z])           # LED -> wall
    dist = np.linalg.norm(d, axis=-1)
    dhat = d / dist[..., None]
    cos_i = np.clip(np.sum(n * dhat, axis=-1), 0.05, 1.0)       # light travels roughly along the outward normal
    if up_only:
        w_emit = np.clip(dhat[..., 2], 0, 1) + 0.12              # a tealight lights the upper hemisphere
    else:
        w_emit = 0.5 * (1 + dhat[..., 2])                        # angle from the LED's +z axis
    b = w_emit * cos_i / dist ** 2 * np.exp(-alpha * t / cos_i)
    if occluder_r > 0:
        # ray LED -> wall point: height of the ray where it crosses the radius of the occluder
        rw = np.hypot(inner[..., 0], inner[..., 1])
        z_at = led_z + (inner[..., 2] - led_z) * (occluder_r / rw)
        b = np.where((inner[..., 2] < led_z) & (z_at < led_z), 0.0, b)
    return b


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("field")
    ap.add_argument("--led-z", type=float, default=65.0)
    ap.add_argument("--sweep", action="store_true")
    ap.add_argument("--occluder-r", type=float, default=0.0, help="radius of the post / LED body that shades the wall below the LED")
    ap.add_argument("--up-only", action="store_true", help="LED lights the upper hemisphere only (tealight)")
    ap.add_argument("--disc-r", type=float, default=0.0, help="radius of a flat round LED puck (mm), e.g. 29.5")
    a = ap.parse_args()
    f = np.load(a.field)
    t, v = f["t"], f["v"]
    veins = t <= 1.0 + 1e-6
    rows = np.repeat(v[:, None], t.shape[1], 1)
    lo, hi = (rows > 20) & (rows < 45) & veins, (rows > 85) & (rows < 108) & veins
    heights = [10, 15, 20, 25, 30, 35, 40, 50, 65] if a.sweep else [a.led_z]
    print(f"{'LED height':>10} {'bottom veins':>13} {'top veins':>10} {'top/bottom':>11}   (relative units)")
    for z in heights:
        b = brightness(f, z, occluder_r=a.occluder_r, up_only=a.up_only, disc_r=a.disc_r)
        bl, bh = b[lo].mean(), b[hi].mean()
        print(f"{z:8.0f}mm {bl*1e4:13.2f} {bh*1e4:10.2f} {bh/bl:11.2f}")


if __name__ == "__main__":
    main()
