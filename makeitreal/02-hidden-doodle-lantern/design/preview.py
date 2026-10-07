#!/usr/bin/env python3
"""Predicted look of the lantern from its wall-thickness field: unlit (day) and lit (night).  Estimate, not a photo.
Light model: white PLA attenuates exp(-T/TAU); LED at height Z_LED on the axis, wall intensity ~ R / d^3."""
import os, sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
import lantern as L

TAU = 0.9            # mm, light attenuation length of white PLA (rough)
Z_LED = 52.0         # mm: tealight (15 mm) on a 45 mm riser
OUT = os.path.join(os.path.dirname(__file__), "..", "out")


def brightness(T):
    B = np.exp(-T / TAU)
    B[T == 0] = 1.6                                   # open holes: the LED itself shines through
    z = (np.arange(L.NROW) + .5) * L.CELL
    d = np.sqrt((L.R_OUT - L.T_INK) ** 2 + (z - Z_LED) ** 2)
    prof = (L.R_OUT - L.T_INK) / d ** 3; prof /= prof.max()
    return B * prof[:, None]


def lit_view(T, rot_deg=0.0, W=900):
    B = brightness(T)[::-1]                           # top -> bottom
    nrow, ncol = B.shape
    Hh = int(W * (L.H / (2 * L.R_OUT)))
    xs = (np.arange(W) + .5) / W * 2 - 1
    ok = np.abs(xs) < 1
    phi = np.arcsin(np.clip(xs, -1, 1))                                       # angle from the view axis
    cols = ((np.degrees(phi) + rot_deg) % 360) / 360 * ncol
    rows = (np.arange(Hh) + .5) / Hh * nrow
    img = np.zeros((Hh, W, 3))
    yy, cc = np.meshgrid(rows, cols, indexing="ij")
    val = ndi.map_coordinates(B, [yy, cc], order=1, mode="wrap")
    shade = (0.45 + 0.55 * np.cos(phi))[None, :]
    warm = np.array([1.0, 0.72, 0.40]); white = np.array([1.0, 0.95, 0.80])
    base = np.clip(val * shade, 0, 3)[..., None]
    col = np.where(base > 1.0, white, warm)
    img = base * col
    img[:, ~ok] = 0
    glow = ndi.gaussian_filter(img, sigma=(W / 60, W / 60, 0))
    img = np.clip(1 - np.exp(-(img * 1.7 + glow * 1.3)), 0, 1)                # soft tone map + bloom
    img[:, ~ok] = 0.02
    bg = np.full((Hh, W, 3), 0.03)
    bg[:, ~ok] = bg[:, ~ok] + 0.5 * glow[:, ~ok]
    out = np.where(ok[None, :, None], img + 0.02, np.clip(bg, 0, 1))
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8))


def day_view(W=900):
    Hh = int(W * (L.H / (2 * L.R_OUT)))
    xs = (np.arange(W) + .5) / W * 2 - 1; ok = np.abs(xs) < 1
    phi = np.arcsin(np.clip(xs, -1, 1))
    sh = 0.62 + 0.38 * np.cos(phi + 0.45)                                      # lit from the upper left
    img = np.zeros((Hh, W, 3)); img[:] = (0.93, 0.93, 0.94)
    img = img * sh[None, :, None]
    img[:, ~ok] = (0.80, 0.82, 0.85)
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))


def unrolled(T, W=1400):
    B = brightness(T)[::-1]
    img = np.clip(1 - np.exp(-B * 1.8), 0, 1)[..., None] * np.array([1.0, 0.8, 0.5])
    img[(T[::-1] == 0)] = (1, 0.96, 0.85)
    im = Image.fromarray((img * 255).astype(np.uint8)).resize((W, int(W * L.NROW / L.NCOL)), Image.NEAREST)
    return im


if __name__ == "__main__":
    name = sys.argv[1] if len(sys.argv) > 1 else "lantern"
    T = np.load(os.path.join(OUT, f"{name}_thickness.npy"))
    lit0, lit1 = lit_view(T, 0), lit_view(T, 120)
    day = day_view(); un = unrolled(T)
    W = lit0.width * 3 + 40; Hh = lit0.height + un.height + 60
    sheet = Image.new("RGB", (max(W, un.width), Hh), (20, 20, 24))
    sheet.paste(day, (10, 10)); sheet.paste(lit0, (lit0.width + 20, 10)); sheet.paste(lit1, (2 * lit0.width + 30, 10))
    sheet.paste(un, (10, lit0.height + 30))
    sheet.save(os.path.join(OUT, "preview_lantern.png"))
    lit0.save(os.path.join(OUT, "preview_lit.png")); day.save(os.path.join(OUT, "preview_day.png"))
    print("wrote previews", sheet.size)
