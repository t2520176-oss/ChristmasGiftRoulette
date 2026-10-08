#!/usr/bin/env python3
"""Printability check for Spooky Pals STL files (print orientation, Z up).

For every STL it reports
  * watertight / winding / body count / volume / bounding box
  * overhang: area of faces that point downwards steeper than 45 deg
    (faces lying on the bed are ignored)
  * a slicer-like layer test: for each layer, the part of the cross-section that
    sticks out more than `tan(angle) * layer_height` beyond the layer below.
    That area is what a slicer would have to bridge / support.  Islands that
    start in mid-air are counted separately.

Usage:  python3 printcheck.py a.stl b.stl ...  [--layer 0.2] [--json out.json]
        python3 printcheck.py --to-binary in.stl out.stl
"""
import argparse
import json
import math
import sys

import numpy as np
import trimesh
from shapely.geometry import MultiPolygon, Polygon
from shapely.ops import unary_union


def load(path):
    m = trimesh.load(path, force="mesh")
    m.merge_vertices()
    return m


def face_overhang(mesh, bed_tol=0.05):
    """Area (mm2) of down-facing faces steeper than 45 / 60 deg, not on the bed."""
    n = mesh.face_normals
    a = mesh.area_faces
    zmin = mesh.bounds[0][2]
    cz = mesh.triangles_center[:, 2]
    off_bed = cz > zmin + bed_tol
    # angle from vertical: asin(-nz) for downward faces
    down = n[:, 2] < 0
    ang = np.degrees(np.arcsin(np.clip(-n[:, 2], 0, 1)))
    res = {}
    for t in (45, 55, 65):
        sel = down & off_bed & (ang > t)
        res[f"over{t}_mm2"] = float(a[sel].sum())
    flat = down & off_bed & (ang > 85)
    res["flat_ceiling_mm2"] = float(a[flat].sum())
    res["surface_mm2"] = float(a.sum())
    return res


def as_polys(path3d):
    """Polygon list (with holes) of a planar cross-section."""
    try:
        p2, _ = path3d.to_planar(check=False)
        polys = p2.polygons_full
    except Exception:
        return None
    return unary_union([p for p in polys if p.is_valid and p.area > 1e-6]) if polys else None


def layer_test(mesh, layer=0.2, angle=45.0, island_min=0.4, keep=None):
    zmin, zmax = mesh.bounds[0][2], mesh.bounds[1][2]
    heights = np.arange(zmin + layer * 0.5, zmax, layer)
    reach = layer * math.tan(math.radians(angle))
    # slice everything at once
    sections = mesh.section_multiplane(plane_origin=[0, 0, 0], plane_normal=[0, 0, 1],
                                       heights=heights - 0.0)
    prev = None
    uns_total = 0.0
    uns_max = 0.0
    uns_max_z = 0.0
    islands = 0
    island_area = 0.0
    worst = []
    for z, sec in zip(heights, sections):
        if sec is None:
            prev = None
            continue
        # section_multiplane returns paths whose 2D frame is shifted; rebuild in XY directly
        polys = []
        for ent in sec.discrete:
            if len(ent) >= 3:
                p = Polygon(ent[:, :2])
                if p.is_valid and p.area > 1e-6:
                    polys.append(p)
        if not polys:
            prev = None
            continue
        # even-odd fill so holes are holes
        cur = None
        for p in sorted(polys, key=lambda q: -q.area):
            cur = p if cur is None else cur.symmetric_difference(p)
        if prev is not None:
            supported = prev.buffer(reach)
            free = cur.difference(supported)
            a = free.area
            if a > 0:
                uns_total += a * layer
                if a > uns_max:
                    uns_max, uns_max_z = a, float(z - zmin)
                if a > 1.5:
                    worst.append((float(z - zmin), float(a)))
                # islands = free pieces that do not touch the previous layer at all
                geoms = list(free.geoms) if isinstance(free, MultiPolygon) else [free]
                for g in geoms:
                    if g.area >= island_min and not g.buffer(0.01).intersects(prev):
                        islands += 1
                        island_area += g.area
                        if keep is not None:
                            keep.append((float(g.area), float(z - zmin), float(g.centroid.x), float(g.centroid.y)))
        prev = cur
    worst.sort(key=lambda t: -t[1])
    return dict(unsupported_mm3=round(uns_total, 2), max_layer_area_mm2=round(uns_max, 2),
                max_layer_z=round(uns_max_z, 2), floating_islands=islands,
                island_area_mm2=round(island_area, 2), worst_layers=worst[:4])


def analyze(path, layer=0.2, show_islands=0):
    m = load(path)
    info = dict(file=path, watertight=bool(m.is_watertight), winding=bool(m.is_winding_consistent),
                bodies=len(m.split(only_watertight=False)) if len(m.faces) < 400000 else -1,
                volume_cm3=round(float(m.volume) / 1000.0, 2) if m.is_watertight else None,
                size_mm=[round(float(x), 1) for x in m.extents], faces=int(len(m.faces)),
                zmin=round(float(m.bounds[0][2]), 3))
    info.update({k: round(v, 1) for k, v in face_overhang(m).items()})
    keep = [] if show_islands else None
    info.update(layer_test(m, layer=layer, keep=keep))
    if keep is not None:
        keep.sort(reverse=True)
        info["islands_top"] = [tuple(round(v, 1) for v in k) for k in keep[:show_islands]]
    return info


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="*")
    ap.add_argument("--layer", type=float, default=0.2)
    ap.add_argument("--json")
    ap.add_argument("--islands", type=int, default=0, help="list the N largest floating islands (area, z, x, y)")
    ap.add_argument("--to-binary", nargs=2, metavar=("IN", "OUT"))
    a = ap.parse_args()
    if a.to_binary:
        m = load(a.to_binary[0])
        m.export(a.to_binary[1])
        return
    out = []
    for f in a.files:
        r = analyze(f, a.layer, a.islands)
        out.append(r)
        print(f"\n== {f}")
        print(f"   size {r['size_mm']} mm   volume {r['volume_cm3']} cm3   faces {r['faces']}   "
              f"watertight={r['watertight']} winding={r['winding']} bodies={r['bodies']}  zmin={r['zmin']}")
        print(f"   overhang faces >45deg: {r['over45_mm2']} mm2   >55deg: {r['over55_mm2']} mm2   "
              f">65deg: {r['over65_mm2']} mm2   flat ceilings: {r['flat_ceiling_mm2']} mm2   "
              f"(total surface {r['surface_mm2']} mm2)")
        print(f"   layer test @45deg: unsupported {r['unsupported_mm3']} mm3, worst layer "
              f"{r['max_layer_area_mm2']} mm2 at z={r['max_layer_z']}, floating islands {r['floating_islands']} "
              f"({r['island_area_mm2']} mm2)")
        if r.get("islands_top"):
            print("   islands (area mm2, z, x, y):", r["islands_top"])
        if r["worst_layers"]:
            print("   worst layers (z, mm2):", r["worst_layers"])
    if a.json:
        with open(a.json, "w") as fh:
            json.dump(out, fh, indent=1)


if __name__ == "__main__":
    sys.exit(main())
