#!/usr/bin/env python3
"""Printability check per PIECE (all colour regions of a piece together, print orientation).

  python3 check_pieces.py ../stl/print [--islands 5] [--md ../docs/PRINT_REPORT.md]
Uses the slicer-like layer test of the Spooky Pals tool (copied here so that the folder is self-contained).
"""
import argparse
import json
import math
import os
import sys

import numpy as np
import trimesh
from shapely.geometry import MultiPolygon, Polygon


def face_overhang(mesh, bed_tol=0.05):
    n = mesh.face_normals
    a = mesh.area_faces
    zmin = mesh.bounds[0][2]
    cz = mesh.triangles_center[:, 2]
    off_bed = cz > zmin + bed_tol
    down = n[:, 2] < 0
    ang = np.degrees(np.arcsin(np.clip(-n[:, 2], 0, 1)))
    return {f"over{t}": float(a[down & off_bed & (ang > t)].sum()) for t in (45, 60)}


def layer_test(meshes, layer=0.2, angle=45.0, keep=None):
    """Layer test over several shells (colour regions) of one piece: sections are cut per shell and then united."""
    from shapely.ops import unary_union
    zmin = min(m.bounds[0][2] for m in meshes)
    zmax = max(m.bounds[1][2] for m in meshes)
    heights = np.arange(zmin + layer * 0.5, zmax, layer)
    reach = layer * math.tan(math.radians(angle))
    per = [m.section_multiplane(plane_origin=[0, 0, 0], plane_normal=[0, 0, 1], heights=heights) for m in meshes]
    prev, total, worst, wz = None, 0.0, 0.0, 0.0
    for li, z in enumerate(heights):
        parts = []
        for secs in per:
            sec = secs[li]
            if sec is None:
                continue
            polys = [Polygon(e[:, :2]) for e in sec.discrete if len(e) >= 3]
            polys = [p for p in polys if p.is_valid and p.area > 1e-6]
            cur = None
            for p in sorted(polys, key=lambda q: -q.area):
                cur = p if cur is None else cur.symmetric_difference(p)
            if cur is not None:
                parts.append(cur)
        if not parts:
            prev = None
            continue
        cur = unary_union(parts)
        if prev is not None:
            free = cur.difference(prev.buffer(reach))
            a = free.area
            if a > 0:
                total += a * layer
                if a > worst:
                    worst, wz = a, float(z - zmin)
                if keep is not None and a > 1.0:
                    geoms = list(free.geoms) if isinstance(free, MultiPolygon) else [free]
                    g = max(geoms, key=lambda q: q.area)
                    keep.append((round(a, 1), round(float(z - zmin), 1), round(g.centroid.x, 1), round(g.centroid.y, 1)))
        prev = cur
    return total, worst, wz


def overlaps(meshes, names):
    """Interference volume (mm3) between the colour regions of one piece (they must not overlap)."""
    try:
        import manifold3d as m3d
    except Exception:
        return []
    def mf(m):
        return m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(m.vertices, dtype=np.float32), tri_verts=np.asarray(m.faces, dtype=np.uint32)))
    ms = [mf(m) for m in meshes]
    out = []
    for i in range(len(ms)):
        for j in range(i + 1, len(ms)):
            v = (ms[i] ^ ms[j]).volume()
            if v > 0.05:
                out.append((names[i], names[j], round(v, 2)))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("print_dir")
    ap.add_argument("--islands", type=int, default=0)
    ap.add_argument("--md", default="")
    a = ap.parse_args()
    man = json.load(open(os.path.join(a.print_dir, "manifest.json")))
    rows = []
    for it in man:
        ms = [trimesh.load(os.path.join(a.print_dir, f"{it['piece']}__{r}.stl"), force="mesh") for r in it["roles"]
              if os.path.exists(os.path.join(a.print_dir, f"{it['piece']}__{r}.stl"))]
        closed = all(m.is_watertight and m.is_winding_consistent for m in ms)
        vol = sum(m.volume for m in ms) / 1000.0
        names = [r for r in it["roles"] if os.path.exists(os.path.join(a.print_dir, f"{it['piece']}__{r}.stl"))]
        allm = trimesh.util.concatenate(ms)
        zlow = allm.bounds[0][2]
        for m in ms:
            m.apply_translation([0, 0, -zlow])
        allm.apply_translation([0, 0, -zlow])
        keep = [] if a.islands else None
        tot, worst, wz = layer_test(ms, keep=keep)
        ov = overlaps(ms, names)
        if ov:
            print("     OVERLAPPING colour regions (mm3):", ov)
        oh = face_overhang(allm)
        size = " x ".join(f"{v:.0f}" for v in allm.extents)
        rows.append((it["piece"], size, vol, closed, oh["over45"], tot, worst, wz, 100 * tot / 1000.0 / max(vol, 1e-9)))
        print(f"{it['piece']:9s} {size:14s} {vol:7.1f} cm3 closed={closed}  over45 {oh['over45']:7.1f} mm2  "
              f"layer-test {tot:7.2f} mm3 ({rows[-1][8]:.3f}%)  worst {worst:6.1f} mm2 @ z={wz:.1f}")
        if keep:
            keep.sort(reverse=True)
            print("     biggest:", keep[:a.islands])
        sys.stdout.flush()
    if a.md:
        lines = ["# Print report (generated by tools/check_pieces.py)", "",
                 "Every piece is analysed in its print orientation (all colour regions together).",
                 "`layer test` = material that, at 0.2 mm layers and a 45 deg overhang limit, hangs beyond the layer below",
                 "(what a slicer would have to support or bridge).", "",
                 "| piece | size mm | volume cm3 | closed | faces >45 deg mm2 | layer test mm3 | % of piece | worst layer mm2 @ z |",
                 "|---|---|---:|:-:|---:|---:|---:|---|"]
        for p, size, vol, closed, o45, tot, worst, wz, pct in rows:
            lines.append(f"| {p} | {size} | {vol:.1f} | {'yes' if closed else 'NO'} | {o45:.0f} | {tot:.1f} | {pct:.2f} | {worst:.1f} @ {wz:.1f} |")
        os.makedirs(os.path.dirname(os.path.abspath(a.md)), exist_ok=True)
        open(a.md, "w").write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
