#!/usr/bin/env python3
"""Pack several STL parts onto one print bed.

    print_layout.py --bed 256 256 --gap 10 --margin 5 --out plate  canopy.stl sail.stl striker.stl:2 jig.stl

Writes plate.stl (one merged mesh), plate.3mf (one object per part, for Bambu Studio / Orca / Prusa)
and plate.png (top-down diagram). A part may be followed by ":N" for N copies, and may be given a label
as LABEL=file.stl (labels starting with two digits, like 07_owl, are coloured by that number).
With --split, parts that do not fit are moved to the next bed (plate_1, plate_2, ...) instead of failing.
With --group COLOR@PART[,PART...] (repeatable) the parts are laid out colour by colour: every colour gets its own
bed (or several, if it needs them), written as <out>_<colour>[_N].*, so each bed can be printed with one filament.
With --tries N, N different placement orders are tried (the first is largest-first, the others random) and the
one that leaves the least area unplaced is used; a tight set of parts sometimes fits only in a lucky order.

Parts must already be in print orientation (z up, lowest point on the bed). The packer works on the
real silhouette of every part (not its bounding box), tries several rotations and scans the bed for
the lowest-left spot that keeps `--gap` mm between silhouettes, so small parts tuck into the empty
corners of big ones. `--gap` should be at least twice the brim width.
Exit code 1 if a part does not fit (without --split).
"""
import argparse
import math
import os
import sys

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import trimesh
from shapely import affinity
from shapely.geometry import Polygon, box
from shapely.ops import unary_union
from shapely.prepared import prep


def footprint(mesh):
    """Silhouette of the mesh on the bed (union of the projected triangles, holes filled)."""
    tris = mesh.triangles[:, :, :2]
    keep = np.abs(mesh.face_normals[:, 2]) > 1e-6
    polys = [Polygon(t) for t, k in zip(tris, keep) if k and Polygon(t).area > 1e-9]
    u = unary_union(polys)
    parts = [Polygon(p.exterior) for p in (u.geoms if hasattr(u, "geoms") else [u])]
    return unary_union(parts).simplify(0.2)


def place(poly, placed, bed, margin, gap, step, angles):
    """Best (angle, dx, dy) for `poly` among the free spots, or None."""
    bw, bh = bed
    best = None
    obstacles = unary_union(placed).buffer(gap) if placed else None
    pobs = prep(obstacles) if obstacles is not None else None
    for ang in angles:
        r = affinity.rotate(poly, ang, origin="centroid")
        x0, y0, x1, y1 = r.bounds
        w, h = x1 - x0, y1 - y0
        if w > bw - 2 * margin or h > bh - 2 * margin:
            continue
        xs = np.arange(margin, bw - margin - w + 1e-6, step)
        ys = np.arange(margin, bh - margin - h + 1e-6, step)
        for y in ys:
            for x in xs:
                c = affinity.translate(r, x - x0, y - y0)
                if pobs is None or not pobs.intersects(c):
                    top = y + h
                    score = (round(top, 1), x + w)
                    if best is None or score < best[0]:
                        best = (score, ang, x - x0, y - y0)
                    break  # lowest-left spot for this row found; try higher rows only if needed
            else:
                continue
            break
    return best


def pack(items, a, angles):
    """Place as many items as possible on one bed. Returns (results, leftover)."""
    placed, results, leftover = [], [], []
    for name, m, fp in items:
        best = place(fp, placed, a.bed, a.margin, a.gap, a.step, angles)
        if best is None:
            leftover.append((name, m, fp))
            continue
        _, ang, dx, dy = best
        c = affinity.translate(affinity.rotate(fp, ang, origin="centroid"), dx, dy)
        cx, cy = fp.centroid.x, fp.centroid.y
        T = trimesh.transformations
        mm = m.copy()
        mm.apply_translation([-cx, -cy, 0])
        mm.apply_transform(T.rotation_matrix(math.radians(ang), [0, 0, 1]))
        mm.apply_translation([cx, cy, 0])
        mm.apply_translation([dx, dy, 0])
        placed.append(c)
        results.append((name, mm, c, ang))
    return results, leftover


def best_pack(items, a, angles):
    """Try several placement orders; keep the one that leaves the least area unplaced."""
    rng = np.random.default_rng(a.seed)
    best = None
    for t in range(max(1, a.tries)):
        order = list(items)
        if t > 0:
            head = order[:2]                      # the two biggest stay first
            tail = order[2:]
            rng.shuffle(tail)
            order = head + tail
        results, leftover = pack(order, a, angles)
        left_area = sum(fp.area for _, _, fp in leftover)
        print(f"  try {t + 1}/{a.tries}: {len(results)} placed, {len(leftover)} left ({left_area:.0f} mm2)", flush=True)
        if best is None or left_area < best[0]:
            best = (left_area, results, leftover)
        if not leftover:
            break
    return best[1], best[2]


def finish(results, a, out, color=None):
    """Centre the group, verify it, write STL / 3MF / PNG. Returns True if every check passed."""
    ok = True
    lo = np.min([r[1].bounds[0][:2] for r in results], axis=0)
    hi = np.max([r[1].bounds[1][:2] for r in results], axis=0)
    off = (np.array(a.bed) - (lo + hi)) / 2
    results = [(n, m.copy().apply_translation([off[0], off[1], 0]),
                affinity.translate(c, off[0], off[1]), ang) for n, m, c, ang in results]

    print(f"{out}: bed {a.bed[0]:g} x {a.bed[1]:g} mm, gap {a.gap:g}, margin {a.margin:g}")
    for name, mm, c, ang in results:
        lo, hi = mm.bounds
        tol = 0.05   # mm; parts placed exactly on the margin line differ by rounding
        inside = lo[0] >= a.margin - tol and lo[1] >= a.margin - tol and \
            hi[0] <= a.bed[0] - a.margin + tol and hi[1] <= a.bed[1] - a.margin + tol
        print(f"  {name:16s} rot {ang:5.1f} deg  x {lo[0]:6.1f}..{hi[0]:6.1f}  y {lo[1]:6.1f}..{hi[1]:6.1f}  "
              f"h {hi[2]:5.1f}  {'ok' if inside else 'OUTSIDE BED'}")
        ok &= inside
    worst = min((results[i][2].distance(results[j][2]) for i in range(len(results))
                 for j in range(i + 1, len(results))), default=float("inf"))
    print(f"  smallest distance between parts: {worst:.1f} mm  ({'ok' if worst >= a.gap - 0.05 else 'TOO CLOSE'})")
    ok &= worst >= a.gap - 0.05
    used = sum(c.area for _, _, c, _ in results) / (a.bed[0] * a.bed[1]) * 100
    print(f"  {len(results)} parts, silhouettes cover {used:.0f} % of the bed, tallest {max(r[1].bounds[1][2] for r in results):.0f} mm")

    if not a.no_stl:
        trimesh.util.concatenate([r[1] for r in results]).export(out + ".stl")
    scene = trimesh.Scene()
    for name, mm, _, _ in results:
        scene.add_geometry(mm, node_name=name, geom_name=name)
    try:
        scene.export(out + ".3mf")
    except Exception as e:  # 3MF needs lxml
        print("  (3mf not written:", e, ")")

    fig, ax = plt.subplots(figsize=(7.5, 7.5 * a.bed[1] / a.bed[0]), dpi=130)
    ax.add_patch(plt.Rectangle((0, 0), *a.bed, fc="#d9d9d9" if color else "#f4f1ea", ec="#333", lw=1.5))
    ax.add_patch(plt.Rectangle((a.margin, a.margin), a.bed[0] - 2 * a.margin, a.bed[1] - 2 * a.margin,
                               fc="none", ec="#bbb", lw=0.8, ls="--"))
    colors = ["#d9622b", "#b83a1e", "#7a4a21", "#4c7a5a", "#5b6fa6", "#a65b8a"]
    named = {"white": "#ffffff", "cream": "#e9d9b0", "orange": "#d9622b", "red": "#b83a1e", "brown": "#7a4a21",
             "wood": "#7a4a21", "green": "#4c7a5a", "blue": "#5b6fa6", "gray": "#9a9a9a", "grey": "#9a9a9a",
             "black": "#2b2b2b"}
    for i, (name, mm, c, ang) in enumerate(results):
        if color:
            col = named.get(color.lower(), colors[i % len(colors)])
        else:
            col = colors[int(name[:2]) % len(colors)] if name[:2].isdigit() else colors[i % len(colors)]
        txt = "#222222" if color and color.lower() in ("white", "cream") else "white"
        for g in (c.geoms if hasattr(c, "geoms") else [c]):
            x, y = g.exterior.xy
            ax.fill(x, y, fc=col, ec="#222", lw=0.8, alpha=0.9)
        lo, hi = mm.bounds
        small = c.area < 700
        ax.text(c.centroid.x, c.centroid.y, f"{name}\n{hi[0]-lo[0]:.0f}x{hi[1]-lo[1]:.0f}x{hi[2]:.0f}",
                ha="center", va="center", fontsize=4.5 if small else 6.5, color=txt, weight="bold")
    ax.set_xlim(-4, a.bed[0] + 4)
    ax.set_ylim(-4, a.bed[1] + 4)
    ax.set_aspect("equal")
    ax.set_title(f"{os.path.basename(out)}" + (f"  [{color} filament]" if color else "") +
                 f": bed {a.bed[0]:g} x {a.bed[1]:g} mm, spacing {a.gap:g} mm, {len(results)} parts", fontsize=9)
    ax.set_xlabel("mm")
    fig.tight_layout()
    fig.savefig(out + ".png")
    plt.close(fig)
    print(f"  wrote {out}" + ("" if a.no_stl else ".stl") + f".3mf, {out}.png")
    return ok


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("parts", nargs="*", help="STL files: [LABEL=]FILE[:COUNT]")
    ap.add_argument("--group", action="append", default=[], metavar="COLOR@PART[,PART...]",
                    help="parts printed in one colour; one bed (or more) per colour")
    ap.add_argument("--bed", type=float, nargs=2, default=[256, 256], metavar=("W", "D"))
    ap.add_argument("--gap", type=float, default=10.0, help="minimum distance between silhouettes (mm)")
    ap.add_argument("--margin", type=float, default=5.0, help="distance to the bed edge (mm)")
    ap.add_argument("--step", type=float, default=2.0, help="search grid (mm)")
    ap.add_argument("--angles", type=float, default=15.0, help="rotation step (deg)")
    ap.add_argument("--split", action="store_true", help="put parts that do not fit on further beds")
    ap.add_argument("--tries", type=int, default=1, help="number of placement orders to try")
    ap.add_argument("--no-stl", action="store_true", help="do not write the merged STL (the 3MF has the same parts)")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--out", default="plate")
    a = ap.parse_args()

    def load_items(specs):
        out = []
        for spec in specs:
            label = None
            if "=" in spec:
                label, spec = spec.split("=", 1)
            if ":" in os.path.basename(spec):
                path, n = spec.rsplit(":", 1)
            else:
                path, n = spec, "1"
            m = trimesh.load(path, force="mesh")
            m.apply_translation([0, 0, -m.bounds[0][2]])
            fp = footprint(m)
            base = label or os.path.splitext(os.path.basename(path))[0]
            for i in range(int(n)):
                out.append((base + (f"_{i+1}" if int(n) > 1 else ""), m, fp))
        return out

    if a.group:
        groups = []
        for g in a.group:
            color, _, rest = g.partition("@")
            groups.append((color, load_items([x for x in rest.split(",") if x])))
        angles = np.arange(0, 180, a.angles)
        ok, total = True, 0
        for color, its in groups:
            its.sort(key=lambda t: -t[2].area)
            remaining, k = its, 0
            while remaining:
                results, leftover = best_pack(remaining, a, angles)
                if not results:
                    for name, _, _ in remaining:
                        print(f"  DOES NOT FIT on any bed {a.bed[0]:g} x {a.bed[1]:g} mm: {name}")
                    sys.exit(1)
                k += 1
                total += 1
                out = f"{a.out}_{color}" if (k == 1 and not leftover) else f"{a.out}_{color}_{k}"
                ok &= finish(results, a, out, color)
                remaining = leftover
        print(f"{total} bed(s) in total")
        sys.exit(0 if ok else 1)

    if not a.parts:
        ap.error("give STL files or at least one --group")
    items = load_items(a.parts)

    items.sort(key=lambda t: -t[2].area)
    angles = np.arange(0, 180, a.angles)
    ok, plate_no, remaining = True, 0, items
    while remaining:
        results, leftover = best_pack(remaining, a, angles)
        if not results:
            for name, _, _ in remaining:
                print(f"  DOES NOT FIT on any bed {a.bed[0]:g} x {a.bed[1]:g} mm: {name}")
            sys.exit(1)
        plate_no += 1
        out = a.out if (not a.split and not leftover) else f"{a.out}_{plate_no}"
        ok &= finish(results, a, out)
        if leftover and not a.split:
            for name, _, _ in leftover:
                print(f"  DOES NOT FIT: {name} on {a.bed[0]:g} x {a.bed[1]:g} mm (use --split for more beds)")
            sys.exit(1)
        remaining = leftover
    if a.split:
        print(f"{plate_no} bed(s) in total")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
