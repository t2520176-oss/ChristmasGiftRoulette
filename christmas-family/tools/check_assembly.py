#!/usr/bin/env python3
"""Can the ASSEMBLED ornament really be put together?   (complements check_fit.py)

  python3 check_assembly.py ../stl/assembled [--top 27]

For every piece (all colour regions united):
  * seated    - how deep the lowest point sits below the top of the snow (tabs inside their sockets)
  * clearance - smallest gap to the base (mm) and how many surface samples are within 0.5 mm of it
  * holds     - the pieces it touches (gap < 0.35 mm); a piece that touches nothing would hang in the air
  * lift      - slide the piece straight up (+z) in small steps: the first height at which it hits another piece.
                If every piece can be lifted out freely, the pieces can be inserted from above in ANY order.
Exit code 1 when a problem is found.
"""
import argparse
import itertools
import os
import re
import sys

import manifold3d as m3d
import numpy as np
import trimesh


def to_m(mesh):
    return m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(mesh.vertices, dtype=np.float32), tri_verts=np.asarray(mesh.faces, dtype=np.uint32)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--top", type=float, default=27.0, help="z of the top of the snow")
    ap.add_argument("--glued", default="tag,plaque,holly", help="pieces that are glued on (no tab): only the contact is checked")
    a = ap.parse_args()
    glued = tuple(a.glued.split(","))

    groups = {}
    for f in sorted(os.listdir(a.dir)):
        if f.endswith(".stl"):
            groups.setdefault(f.split("__")[0], []).append(trimesh.load(os.path.join(a.dir, f), force="mesh"))
    meshes = {p: trimesh.util.concatenate(ms) for p, ms in groups.items()}
    mani = {p: to_m(m) for p, m in meshes.items()}
    base = meshes["base"]
    pq = trimesh.proximity.ProximityQuery(base)
    rng = np.random.default_rng(1)
    problems = []

    print(f"{'piece':9s} {'seated':>7s} {'gap to base':>12s} {'touching':28s} lift")
    for p in sorted(meshes, key=lambda s: (re.sub(r"\d+", "", s), s)):
        if p == "base":
            continue
        m = meshes[p]
        pts, _ = trimesh.sample.sample_surface(m, 4000, seed=1)
        d = pq.on_surface(pts)[1]
        low = m.bounds[0][2]
        seated = a.top - low
        # contact partners
        touch = []
        for q, mq in meshes.items():
            if q == p:
                continue
            if np.any(m.bounds[1] < mq.bounds[0] - 0.4) or np.any(mq.bounds[1] < m.bounds[0] - 0.4):
                continue
            dq = trimesh.proximity.ProximityQuery(mq).on_surface(pts)[1]          # unsigned distance
            if dq.min() < 0.35:
                touch.append(q)
        # lift test
        steps = list(np.arange(0.5, 12.01, 0.5)) + list(np.arange(16, 160, 4))
        blocked = None
        others = [q for q in meshes if q != p]
        for dz in steps:
            mv = mani[p].translate([0, 0, float(dz)])
            lo, hi = m.bounds[0] + [0, 0, dz], m.bounds[1] + [0, 0, dz]
            hit = [q for q in others
                   if not (np.any(hi < meshes[q].bounds[0]) or np.any(meshes[q].bounds[1] < lo))   # bounding boxes apart
                   and (mv ^ mani[q]).volume() > 0.05]
            if hit:
                blocked = (dz, hit)
                break
        lift = "free" if not blocked else f"BLOCKED at +{blocked[0]:.1f} mm by {','.join(blocked[1])}"
        tab = p.startswith(glued) is False
        if not tab and blocked:
            # glued-on pieces may also be slid in from the front (towards +y)
            fb = None
            for d in np.arange(1.0, 60.1, 1.5):
                mv = mani[p].translate([0, -float(d), 0])
                lo, hi = m.bounds[0] - [0, d, 0], m.bounds[1] - [0, d, 0]
                hit = [q for q in others
                       if not (np.any(hi < meshes[q].bounds[0]) or np.any(meshes[q].bounds[1] < lo))
                       and (mv ^ mani[q]).volume() > 0.05]
                if hit:
                    fb = (d, hit)
                    break
            if fb is None:
                lift = "glue-on: slides in from the front"
                blocked = None
            else:
                lift = f"glue-on: from above {lift[8:]}; from the front blocked by {','.join(fb[1])}"
        print(f"{p:9s} {seated:6.1f}  {d.min():10.2f}  {','.join(touch)[:28]:28s} {lift}")
        if not touch:
            problems.append(f"{p}: touches nothing")
        if blocked:
            problems.append(f"{p}: {lift}")
        if tab and p.startswith(("tree", "house", "fig", "lantern", "sky")):
            if seated < TAB_MIN:
                problems.append(f"{p}: tab only {seated:.1f} mm deep (< {TAB_MIN} mm)")
    print()
    print("PROBLEMS:" if problems else "no problems found")
    for t in problems:
        print("  -", t)
    sys.exit(1 if problems else 0)


TAB_MIN = 6.0

if __name__ == "__main__":
    main()
