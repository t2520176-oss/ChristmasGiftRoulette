#!/usr/bin/env python3
"""
Hidden Doodle Lantern - turns a drawing into the WALL THICKNESS of a cylinder.

    python3 lantern.py [drawing.png] [--outdir ../out]

* ink  (dark pen)               -> thick wall (3.0 mm)  : blocks the light
* paper (white)                 -> thin skin  (0.8 mm)  : glows
* small enclosed paper blobs    -> real holes (stars, windows): sharp bright dots
Outside surface is a perfectly smooth cylinder: by day it is just a white tube,
the drawing only appears when a light is switched on inside.

Printing rule enforced here: standing upright, the wall may only get thicker towards the
inside by <= `SLOPE` mm per mm of height (45 deg), so NO supports are needed.
Through-holes are limited to <= HOLE_MAX_MM so that their roofs can be bridged.

The mesh is built directly (columnar cells in cylinder coordinates), watertight by
construction and verified (every edge shared by exactly two triangles, consistent orientation).
"""
import argparse, io, os, struct, sys, zipfile
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

# ---------------- design parameters (mm) ----------------
R_OUT = 42.5          # outer radius  -> 85 mm diameter
H = 100.0             # height (rounded to whole cells)
CELL = 0.8            # grid size in theta and z (2 x nozzle width)
DR = 0.8              # radial step of the wall thickness; DR == CELL -> a one-step ramp is exactly 45 deg
T_INK = 3.2           # thick wall (4 steps)
T_SKIN = 0.8          # translucent skin (1 step = 4 layers of 0.2)
FRAME_BOT = 5.0       # solid ring at the bottom (adhesion) and top (lid seat)
FRAME_TOP = 5.0
SLOPE = DR / CELL     # max inward growth of the wall per mm of height  (= 1.0 -> 45 deg)
HOLE_MIN_MM2 = 4.0    # smaller paper blobs stay glow
HOLE_MAX_MM = 8.0     # holes up to this size (bridgeable roof)
LID_CLEAR = 0.35      # radial clearance of the lid plug

NCOL = int(round(2 * np.pi * R_OUT / CELL))
NROW = int(round(H / CELL))
H = NROW * CELL


def load_drawing(path, pic_w_mm, pic_h_mm, fit="cover"):
    """Return a bool ink mask (rows top->bottom) of size (pic_rows, NCOL)."""
    rows = int(round(pic_h_mm / CELL))
    img = Image.open(path).convert("L")
    w, h = img.size
    tw, th = NCOL, rows
    if fit == "cover":
        s = max(tw / w, th / h)
        img = img.resize((max(1, int(w * s + .5)), max(1, int(h * s + .5))), Image.LANCZOS)
        x0 = (img.width - tw) // 2; y0 = (img.height - th) // 2
        img = img.crop((x0, y0, x0 + tw, y0 + th))
    else:   # contain: pad with paper
        s = min(tw / w, th / h)
        im2 = img.resize((max(1, int(w * s + .5)), max(1, int(h * s + .5))), Image.LANCZOS)
        canvas = Image.new("L", (tw, th), 255)
        canvas.paste(im2, ((tw - im2.width) // 2, (th - im2.height) // 2))
        img = canvas
    a = np.asarray(img, float)
    # Otsu threshold
    hist, _ = np.histogram(a, bins=256, range=(0, 256)); p = hist / hist.sum()
    om = np.cumsum(p); mu = np.cumsum(p * np.arange(256)); mt = mu[-1]
    sb = (mt * om - mu) ** 2 / (om * (1 - om) + 1e-12); thr = np.argmax(sb)
    return a <= thr


def build_thickness(ink, holes=True):
    """ink: (pic_rows, NCOL) rows top->bottom.  Returns T[row_from_bottom, col] in mm (0 = hole)."""
    pic = ink[::-1].copy()                               # bottom -> top
    pic_rows = pic.shape[0]
    # wrap-around aware hole detection: tile 3x horizontally
    paper = ~pic
    tiled = np.tile(paper, (1, 3))
    lab, n = ndi.label(tiled)                            # 4-connectivity
    mid = lab[:, NCOL:2 * NCOL]
    hole = np.zeros_like(pic)
    if holes and n:
        objs = ndi.find_objects(lab)
        for idx, sl in enumerate(objs, 1):
            ys, xs = sl
            if ys.start == 0 or ys.stop == pic_rows:     # touches top/bottom of picture -> open sky, not a hole
                continue
            if xs.start == 0 or xs.stop == tiled.shape[1]:
                continue
            area = (lab[sl] == idx).sum() * CELL * CELL
            hh, ww = (ys.stop - ys.start) * CELL, (xs.stop - xs.start) * CELL
            if area >= HOLE_MIN_MM2 and max(hh, ww) <= HOLE_MAX_MM and min(hh, ww) >= 1.6:
                hole |= (mid == idx)
    T = np.where(pic, T_INK, T_SKIN).astype(float)
    T[hole] = 0.0
    fb, ft = int(round(FRAME_BOT / CELL)), int(round(FRAME_TOP / CELL))
    full = np.full((NROW, NCOL), T_INK)
    full[fb:NROW - ft] = T[:NROW - fb - ft] if pic_rows >= NROW - fb - ft else np.pad(T, ((0, NROW - fb - ft - pic_rows), (0, 0)), constant_values=T_INK)
    return full


def limit_overhang(T):
    """Wall may grow inward by at most one radial step (DR) per row (CELL): 45 deg. Holes (T=0) are bridged and exempt."""
    T = T.copy()
    for k in range(1, NROW):
        prev = T[k - 1]
        ref = np.where(prev > 0, prev, T_INK)            # above a hole: no ramp (bridging)
        T[k] = np.where(T[k] > 0, np.minimum(T[k], ref + DR), 0)
    return T


def fix_level_pinches(T):
    """Per radial layer, two filled cells touching only diagonally make a non-manifold edge. Lower one of them by one
    step (removing material only), re-apply the 45-degree rule, repeat until no such contact remains."""
    for _ in range(200):
        lev = np.rint(T / DR).astype(int)
        changed = False
        for n in range(1, NLAY + 1):                                        # layer n-1 filled <=> lev >= n
            f = lev >= n
            fw = np.concatenate([f, f[:, :1]], axis=1)                      # wrap in theta
            a = fw[:-1, :-1] & fw[1:, 1:] & ~fw[:-1, 1:] & ~fw[1:, :-1]     # diagonal  / contact
            b = ~fw[:-1, :-1] & ~fw[1:, 1:] & fw[:-1, 1:] & fw[1:, :-1]     # diagonal  \ contact
            for mask, cell in ((a, (0, 0)), (b, (0, 1))):
                ks, js = np.where(mask)
                for k, j in zip(ks, js):
                    kk, jj = k + cell[0], (j + cell[1]) % NCOL
                    if lev[kk, jj] >= n and n - 1 >= 1:                     # never remove the outer skin layer here
                        lev[kk, jj] = n - 1; changed = True
        T = limit_overhang(lev * DR)
        if not changed:
            return T
    return T


def clean_pinches(T):
    """Remove diagonal-only contacts of empty cells (non-manifold corner lines)."""
    for _ in range(10):
        solid = T > 0
        s = solid
        a = (~s[:-1, :-1]) & (~s[1:, 1:]) & s[:-1, 1:] & s[1:, :-1]
        b = s[:-1, :-1] & s[1:, 1:] & (~s[:-1, 1:]) & (~s[1:, :-1])
        bad = a | b
        # also wrap-around columns
        sw = np.concatenate([s, s[:, :1]], axis=1)
        a2 = (~sw[:-1, :-1]) & (~sw[1:, 1:]) & sw[:-1, 1:] & sw[1:, :-1]
        b2 = sw[:-1, :-1] & sw[1:, 1:] & (~sw[:-1, 1:]) & (~sw[1:, :-1])
        bad2 = (a2 | b2)[:, :NCOL]
        if not bad.any() and not bad2.any():
            return T
        ks, js = np.where(bad)
        for k, j in zip(ks, js):
            T[k, j] = T[k, j] if T[k, j] > 0 else T_SKIN
            T[k + 1, j + 1] = T[k + 1, j + 1] if T[k + 1, j + 1] > 0 else T_SKIN
        ks, js = np.where(bad2)
        for k, j in zip(ks, js):
            for (kk, jj) in ((k, j), (k + 1, (j + 1) % NCOL), (k, (j + 1) % NCOL), (k + 1, j)):
                if T[kk, jj] == 0: T[kk, jj] = T_SKIN
    return T


# ------------------------------- mesh -------------------------------------------
NLAY = int(round(T_INK / DR))                      # radial layers (layer 0 = outermost)


def build_mesh(T):
    """Boundary faces of the (layer, row, col) voxel set.  Every vertex sits on the regular lattice
    (radius index, theta index, z index), so neighbouring faces share their edges exactly: watertight by construction."""
    lev = np.rint(T / DR).astype(int)                                        # filled radial layers per cell
    F3 = np.arange(NLAY)[:, None, None] < lev[None]                          # [layer, row, col]
    ids, exp = [], []                                                        # quads (4 lattice ids), expected normal codes

    def vid(rq, i, k):
        return (rq * NCOL + (i % NCOL)) * (NROW + 1) + k

    def push(q, code):
        ids.append(q); exp.append(np.full(len(q), code))

    # outer skin (+r) and inner surface (-r)
    k_, i_ = np.where(F3[0])
    push(np.stack([vid(NLAY, i_, k_), vid(NLAY, i_ + 1, k_), vid(NLAY, i_ + 1, k_ + 1), vid(NLAY, i_, k_ + 1)], 1), 1)
    nxt = np.concatenate([F3[1:], np.zeros((1,) + F3.shape[1:], bool)])
    n, k_, i_ = np.where(F3 & ~nxt)
    r_ = NLAY - n - 1
    push(np.stack([vid(r_, i_, k_), vid(r_, i_ + 1, k_), vid(r_, i_ + 1, k_ + 1), vid(r_, i_, k_ + 1)], 1), 2)
    # theta faces
    right = np.roll(F3, -1, axis=2)
    for m, code in ((F3 & ~right, 3), (~F3 & right, 4)):                    # 3: normal +tangent, 4: -tangent
        n, k_, i_ = np.where(m); a, b = NLAY - n - 1, NLAY - n
        push(np.stack([vid(a, i_ + 1, k_), vid(b, i_ + 1, k_), vid(b, i_ + 1, k_ + 1), vid(a, i_ + 1, k_ + 1)], 1), code)
    # z faces; pad one empty row below and above
    Fp = np.concatenate([np.zeros((NLAY, 1, NCOL), bool), F3, np.zeros((NLAY, 1, NCOL), bool)], axis=1)
    lower, upper = Fp[:, :-1], Fp[:, 1:]                                      # boundary p sits at z index p
    for m, code in ((lower & ~upper, 5), (~lower & upper, 6)):               # 5: +z, 6: -z
        n, p, i_ = np.where(m); a, b = NLAY - n - 1, NLAY - n
        push(np.stack([vid(a, i_, p), vid(b, i_, p), vid(b, i_ + 1, p), vid(a, i_ + 1, p)], 1), code)
    Q = np.concatenate(ids); C = np.concatenate(exp)
    uniq, inv = np.unique(Q.ravel(), return_inverse=True)
    Qi = inv.reshape(-1, 4)
    # coordinates of the unique lattice vertices
    k = uniq % (NROW + 1); rest = uniq // (NROW + 1); i = rest % NCOL; rq = rest // NCOL
    r = R_OUT - DR * (NLAY - rq); th = 2 * np.pi * i / NCOL; z = CELL * k
    V = np.stack([r * np.cos(th), r * np.sin(th), z], 1)
    # orient every quad so that its normal points along the expected direction
    P = V[Qi]
    nn = np.cross(P[:, 1] - P[:, 0], P[:, 2] - P[:, 0]) + np.cross(P[:, 2] - P[:, 0], P[:, 3] - P[:, 0])
    cen = P.mean(1); ang = np.arctan2(cen[:, 1], cen[:, 0])
    radial = np.stack([np.cos(ang), np.sin(ang), 0 * ang], 1); tang = np.stack([-np.sin(ang), np.cos(ang), 0 * ang], 1)
    want = np.zeros_like(nn)
    for code, vec in ((1, radial), (2, -radial), (3, tang), (4, -tang)):
        w = C == code; want[w] = vec[w]
    want[C == 5] = [0, 0, 1]; want[C == 6] = [0, 0, -1]
    flip = np.einsum("ij,ij->i", nn, want) < 0
    Qi[flip] = Qi[flip][:, ::-1]
    F = np.concatenate([Qi[:, [0, 1, 2]], Qi[:, [0, 2, 3]]])
    return V, F.astype(np.int64)


def validate(V, F):
    """Every undirected edge in exactly two triangles, every directed edge once (consistent orientation)."""
    e = np.concatenate([F[:, [0, 1]], F[:, [1, 2]], F[:, [2, 0]]])
    key = e[:, 0] * len(V) + e[:, 1]
    rkey = e[:, 1] * len(V) + e[:, 0]
    ks = np.sort(key)
    dup_dir = (ks[1:] == ks[:-1]).sum()                  # same directed edge twice -> bad
    s = np.sort(key); idx = np.searchsorted(s, rkey)
    idx[idx >= len(s)] = len(s) - 1
    missing = (s[idx] != rkey).sum()                     # directed edge without its opposite
    vol = np.einsum("ij,ij->i", V[F[:, 0]], np.cross(V[F[:, 1]], V[F[:, 2]])).sum() / 6
    return dict(directed_dups=int(dup_dir), unmatched=int(missing), volume_cm3=vol / 1000, tris=len(F), verts=len(V))


def write_stl(path, V, F):
    t = V[F]
    n = np.cross(t[:, 1] - t[:, 0], t[:, 2] - t[:, 0]); n /= np.maximum(np.linalg.norm(n, axis=1, keepdims=True), 1e-12)
    rec = np.zeros(len(F), dtype=np.dtype([("n", "<3f4"), ("v", "<9f4"), ("a", "<u2")]))
    rec["n"] = n; rec["v"] = t.reshape(-1, 9)
    with open(path, "wb") as f:
        f.write(b"Hidden Doodle Lantern".ljust(80, b" ")); f.write(struct.pack("<I", len(F))); f.write(rec.tobytes())


def write_3mf(path, V, F, name="HiddenDoodleLantern"):
    vx = "".join(f'<vertex x="{x:.4f}" y="{y:.4f}" z="{zz:.4f}"/>' for x, y, zz in V)
    tr = "".join(f'<triangle v1="{a}" v2="{b}" v3="{c}"/>' for a, b, c in F)
    model = ('<?xml version="1.0" encoding="UTF-8"?><model unit="millimeter" xml:lang="en-US" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02">'
             f'<resources><object id="1" type="model" name="{name}"><mesh><vertices>{vx}</vertices><triangles>{tr}</triangles></mesh></object></resources>'
             '<build><item objectid="1"/></build></model>')
    ct = ('<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
          '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
          '<Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/></Types>')
    rels = ('<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            '<Relationship Target="/3D/3dmodel.model" Id="rel0" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/></Relationships>')
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", ct); z.writestr("_rels/.rels", rels); z.writestr("3D/3dmodel.model", model)


# ------------------------------- placeholder drawing ---------------------------
def placeholder_drawing(path, seed=3):
    """A stand-in panorama (2670 x 906 px = 10 px/mm): star band, glowing sky, dark skyline with window holes, doodle creatures."""
    rng = np.random.default_rng(seed)
    W, Hh = int(2 * np.pi * R_OUT * 10), int((H - FRAME_BOT - FRAME_TOP) * 10)
    img = Image.new("L", (W, Hh), 255); d = ImageDraw.Draw(img)
    band = int(Hh * 0.22)
    d.rectangle([0, 0, W, band], fill=0)                                         # night band (dark) ...
    for _ in range(70):                                                           # ... with star holes
        x, y = rng.integers(20, W - 20), rng.integers(30, band - 30); r = int(rng.integers(13, 20))
        d.ellipse([x - r, y - r, x + r, y + r], fill=255)
    ground = int(Hh * 0.90)
    x = 0
    while x < W:                                                                  # skyline
        bw = int(rng.integers(90, 230)); bh = int(rng.integers(int(Hh * .22), int(Hh * .55)))
        d.rectangle([x, ground - bh, x + bw, ground], fill=0)
        for wy in range(ground - bh + 40, ground - 50, 95):
            for wx in range(x + 28, x + bw - 50, 70):
                if rng.random() < 0.55:
                    d.rectangle([wx, wy, wx + 36, wy + 56], fill=255)             # window = hole
        x += bw + int(rng.integers(8, 40))
    d.rectangle([0, ground, W, Hh], fill=0)                                       # street
    for cx in np.linspace(180, W - 180, 7):                                       # doodle creatures walking on the roofs of the street
        cy = ground - 38
        d.ellipse([cx - 62, cy - 70, cx + 62, cy + 10], fill=255, outline=0, width=0)
        d.polygon([(cx - 50, cy - 48), (cx - 30, cy - 105), (cx - 8, cy - 58)], fill=255)
        d.polygon([(cx + 50, cy - 48), (cx + 30, cy - 105), (cx + 8, cy - 58)], fill=255)
        d.ellipse([cx - 28, cy - 44, cx - 14, cy - 28], fill=0); d.ellipse([cx + 14, cy - 44, cx + 28, cy - 28], fill=0)
    img.save(path)
    return path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("drawing", nargs="?", default=None, help="your drawing (photo/scan, dark pen on white). Omit for the placeholder.")
    ap.add_argument("--outdir", default=os.path.join(os.path.dirname(__file__), "..", "out"))
    ap.add_argument("--fit", choices=["cover", "contain"], default="cover")
    ap.add_argument("--no-holes", action="store_true")
    ap.add_argument("--name", default="lantern")
    ap.add_argument("--skin-steps", type=int, choices=[1, 2], default=1, help="glow wall: 1 = 0.8 mm (bright), 2 = 1.6 mm (softer, stronger; use if your filament is very translucent)")
    a = ap.parse_args()
    os.makedirs(a.outdir, exist_ok=True)
    global T_SKIN
    T_SKIN = DR * a.skin_steps
    src = a.drawing or placeholder_drawing(os.path.join(a.outdir, "placeholder_drawing.png"))
    pic_h = H - FRAME_BOT - FRAME_TOP
    ink = load_drawing(src, 2 * np.pi * R_OUT, pic_h, a.fit)
    T0 = build_thickness(ink, holes=not a.no_holes)
    T = fix_level_pinches(limit_overhang(clean_pinches(T0)))
    np.save(os.path.join(a.outdir, f"{a.name}_thickness.npy"), T)
    V, F = build_mesh(T)
    info = validate(V, F)
    print("mesh:", info)
    # overhang audit on the final field
    both = (T[1:] > 0) & (T[:-1] > 0)                                            # holes are bridged -> exempt
    grow = np.where(both, np.diff(T, axis=0) / CELL, 0)
    print(f"max inward growth per mm height: {grow.max():.2f} (limit {SLOPE})  -> overhang angle {np.degrees(np.arctan(grow.max())):.1f} deg")
    assert grow.max() <= SLOPE + 1e-6, "overhang rule violated"
    print(f"wall: min non-hole {T[T>0].min():.2f} mm, holes {(T==0).sum()*CELL*CELL:.0f} mm2, ink share {(T>=T_INK-1e-9).mean()*100:.0f}%")
    write_stl(os.path.join(a.outdir, f"{a.name}.stl"), V, F)
    write_3mf(os.path.join(a.outdir, f"{a.name}.3mf"), V, F)
    print("wrote", a.name + ".stl/.3mf", "volume %.1f cm3" % info["volume_cm3"], "(solid; slicer infill/walls decide the real mass)")
    if info["directed_dups"] or info["unmatched"]:
        print("WARNING: mesh is not watertight"); sys.exit(2)


if __name__ == "__main__":
    main()
