#!/usr/bin/env python3
"""Printability check per PIECE (all colour regions of a piece together, print orientation).

  python3 check_pieces.py ../stl/print [--islands 5] [--md ../docs/PRINT_REPORT.md] [--solid ice,ring,belt] [--wall lid.ice]
Uses the slicer-like layer test of the Spooky Pals tool (copied here so that the folder is self-contained).

  --solid ROLES   (role or piece.role) every colour region of these roles must be ONE solid (a loose sliver - e.g. an outer skin that a groove cut
                  free - is reported as a FRAGMENT)
  --wall P.ROLE   no wall thinner than 1.4 mm in these piece.role regions (morphological opening of the layers, every 0.5 mm of
                  height, lowest --wall-zmax mm (6); not for regions with a sharp ridge, whose tip is thin by design)
Exit code 1 when a FRAGMENT or a thin wall is found.
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


def cross_sections(mesh, heights):
    """list (one per height) of the closed outlines (N x 2 arrays) of the cut - done with manifold3d, which is not
    confused by a cutting plane that runs a hair from a vertex (trimesh dropped a whole outline now and then)"""
    try:
        import manifold3d as m3d
        man = m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(mesh.vertices, dtype=np.float32), tri_verts=np.asarray(mesh.faces, dtype=np.uint32)))
        if man.status() != m3d.Error.NoError:
            raise ValueError("not a manifold")
        return [[np.asarray(p, dtype=float) for p in man.slice(float(z)).to_polygons()] for z in heights]
    except Exception:
        out = []
        for sec in mesh.section_multiplane(plane_origin=[0, 0, 0], plane_normal=[0, 0, 1], heights=heights):
            out.append([] if sec is None else [e[:, :2] for e in sec.discrete])
        return out


def layer_test(meshes, layer=0.2, angle=45.0, keep=None):
    """Layer test over several shells (colour regions) of one piece: sections are cut per shell and then united."""
    from shapely.ops import unary_union
    zmin = min(m.bounds[0][2] for m in meshes)
    zmax = max(m.bounds[1][2] for m in meshes)
    heights = np.arange(zmin + layer * 0.5, zmax, layer)
    reach = layer * math.tan(math.radians(angle))
    per = [cross_sections(m, heights) for m in meshes]
    prev, total, worst, wz = None, 0.0, 0.0, 0.0
    for li, z in enumerate(heights):
        parts = []
        for secs in per:
            polys = [Polygon(e[:, :2]) for e in secs[li] if len(e) >= 3]
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


def fragments(mesh, min_mm3=1000.0):
    """volumes (mm3) of the loose pieces of a region that are smaller than min_mm3 (while the region has a bigger part)"""
    parts = mesh.split(only_watertight=False)
    vols = sorted(abs(float(p.volume)) for p in parts)
    return [round(v, 1) for v in vols[:-1] if v < min_mm3] if len(vols) > 1 else []


def thin_walls(mesh, r=0.7, step=0.5, zmax=6.0):
    """largest area (mm2) of material in one slice that is thinner than 2r (it vanishes when the slice is eroded by r),
    looked for in the lowest zmax mm of the region (the lid seat; the crystal inlay on top has thin slivers by design)"""
    from shapely.ops import unary_union
    heights = np.arange(mesh.bounds[0][2] + 0.1, min(mesh.bounds[1][2], mesh.bounds[0][2] + zmax), step)
    worst, wz = 0.0, 0.0
    for z, sec in zip(heights, cross_sections(mesh, heights)):
        polys = [Polygon(e[:, :2]) for e in sec if len(e) >= 3]
        polys = [q for q in polys if q.is_valid and q.area > 1e-6]
        cur = None
        for q in sorted(polys, key=lambda t: -t.area):
            cur = q if cur is None else cur.symmetric_difference(q)
        if cur is None:
            continue
        lost = cur.area - cur.buffer(-r).buffer(r).area
        if lost > worst:
            worst, wz = lost, float(z - mesh.bounds[0][2])
    return worst, wz


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
    ap.add_argument("--solid", default="")
    ap.add_argument("--wall", default="")
    ap.add_argument("--wall-zmax", type=float, default=6.0, help="how many mm above the lowest point of a region the wall test looks")
    a = ap.parse_args()
    problems = []
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
        for r, m in zip(names, ms):
            if r in a.solid.split(",") or f"{it['piece']}.{r}" in a.solid.split(","):
                fr = fragments(m)
                if fr:
                    problems.append(f"{it['piece']}.{r}: FRAGMENT(s) {fr} mm3 loose from the main body")
            if f"{it['piece']}.{r}" in a.wall.split(","):
                lost, lz = thin_walls(m, zmax=a.wall_zmax)
                if lost > 15.0:
                    problems.append(f"{it['piece']}.{r}: wall thinner than 1.4 mm ({lost:.0f} mm2 in one slice, z = {lz:.1f})")
                print(f"     wall test {it['piece']}.{r}: thinnest-wall area {lost:.1f} mm2 @ z={lz:.1f}")
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
    for t in problems:
        print("PROBLEM:", t)
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
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
