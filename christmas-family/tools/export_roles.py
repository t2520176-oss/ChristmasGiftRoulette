#!/usr/bin/env python3
"""Export every (piece, role) of the family ornament as a binary STL.

  python3 export_roles.py --out ../stl/print     --layout print      --family PIXELFORM
  python3 export_roles.py --out ../stl/assembled --layout assembled

Files are named  <piece>__<role>.stl .  Pieces/roles come from the SCAD manifest, so the exporter
follows any change of `members` / `names`.
"""
import argparse
import concurrent.futures as cf
import json
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
SCAD = os.path.normpath(os.path.join(HERE, "..", "openscad", "family_ornament.scad"))


def scad_defs(a):
    d = ["-D", f'family="{a.family}"', "-D", f"year={a.year}", "-D", f"fn={a.fn}",
         "-D", f'font_serif="{a.font_serif}"', "-D", f'font_text="{a.font_text}"', "-D", f"tol={a.tol}",
         "-D", "members=" + json.dumps(a.members), "-D", "names=" + json.dumps(a.names)]
    return d


def manifest(a):
    import tempfile
    tmp = os.path.join(tempfile.gettempdir(), "family_manifest.stl")
    p = subprocess.run(["openscad", "-o", tmp, SCAD, "-D", 'part="manifest"'] + scad_defs(a), capture_output=True, text=True)
    out = p.stdout + p.stderr
    items = []
    for m in re.finditer(r'ECHO: PIECE = "([^"]+)", ROLES = \[([^\]]*)\]', out):
        roles = re.findall(r'"([^"]+)"', m.group(2))
        items.append((m.group(1), roles))
    return items


def run(piece, role, a):
    tmp = os.path.join(a.out, f"_{piece}__{role}.stl")
    cmd = ["openscad", "-o", tmp, SCAD, "-D", f'part="{piece}"', "-D", f'role="{role}"', "-D", f'layout="{a.layout}"'] + scad_defs(a)
    t0 = time.time()
    p = subprocess.run(cmd, capture_output=True, text=True)
    dt = time.time() - t0
    if not os.path.exists(tmp) or os.path.getsize(tmp) < 300:
        if os.path.exists(tmp):
            os.remove(tmp)
        return piece, role, None, dt, (p.stderr.strip().splitlines() or [""])[-1]
    import trimesh
    m = trimesh.load(tmp, force="mesh")
    final = os.path.join(a.out, f"{piece}__{role}.stl")
    m.export(final)
    os.remove(tmp)
    return piece, role, final, dt, ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--layout", default="print", choices=["print", "assembled"])
    ap.add_argument("--family", default="PIXELFORM")
    ap.add_argument("--year", type=int, default=2026)
    ap.add_argument("--members", nargs="+", default=["boy", "snowman", "girl", "dog", "cat"])
    ap.add_argument("--names", nargs="+", default=["Ethan", "Lily", "Hazel", "Buddy", "Mochi"])
    ap.add_argument("--fn", type=int, default=36)
    ap.add_argument("--tol", type=float, default=0.2)
    ap.add_argument("--font-serif", default="Liberation Serif:style=Bold")
    ap.add_argument("--font-text", default="Liberation Serif:style=Regular")
    ap.add_argument("--only", default="", help="comma separated piece names")
    ap.add_argument("--jobs", type=int, default=4)
    a = ap.parse_args()
    a.font_serif, a.font_text = a.font_serif, a.font_text
    os.makedirs(a.out, exist_ok=True)
    items = manifest(a)
    if not items:
        sys.exit("manifest is empty - OpenSCAD failed")
    only = set(a.only.split(",")) if a.only else None
    jobs = [(p, r) for p, roles in items for r in roles if not only or p in only]
    json.dump([{"piece": p, "roles": r} for p, r in items], open(os.path.join(a.out, "manifest.json"), "w"), indent=1)
    json.dump({"family": a.family, "year": a.year, "members": a.members, "names": a.names, "layout": a.layout},
              open(os.path.join(a.out, "config.json"), "w"), indent=1)
    print(f"{len(jobs)} (piece, role) exports, layout={a.layout}")
    bad = 0
    with cf.ThreadPoolExecutor(a.jobs) as ex:
        futs = [ex.submit(run, p, r, a) for p, r in jobs]
        for f in cf.as_completed(futs):
            piece, role, path, dt, msg = f.result()
            if not path:
                bad += 1
            print(f"{piece:9s}{role:9s} {'OK  ' if path else 'FAIL'} {dt:5.1f}s {msg}")
            sys.stdout.flush()
    print("failed/empty:", bad)


if __name__ == "__main__":
    main()
