#!/usr/bin/env python3
"""Export every part of one Spooky Pals set as (binary) STL using OpenSCAD.

  python3 export_parts.py --pet dog --costume witch --base stump --name LUNA \
          --out ../stl/dog_witch_stump --layout print --fn 40

Parts exported: pet, hat, cape, charm, base   (+ face, shine, trim, deco, text with --multicolor)
All parts are written in their *print orientation* (--layout print) or at their
assembled position (--layout assembled, used for renders and fit checks).
"""
import argparse
import concurrent.futures as cf
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
SCAD = os.path.normpath(os.path.join(HERE, "..", "openscad", "spooky_pals.scad"))


def run_part(part, args, outdir):
    out_ascii = os.path.join(outdir, f"_{part}.stl")
    cmd = [
        "openscad", "-o", out_ascii, SCAD,
        "-D", f'part="{part}"', "-D", f'pet="{args.pet}"', "-D", f'costume="{args.costume}"',
        "-D", f'base_style="{args.base}"', "-D", f'name="{args.name}"', "-D", f'layout="{args.layout}"',
        "-D", f"fn={args.fn}", "-D", f"multicolor={'true' if args.multicolor else 'false'}",
        "-D", f'font="{args.font}"', "-D", f"tol={args.tol}",
    ]
    t0 = time.time()
    p = subprocess.run(cmd, capture_output=True, text=True)
    dt = time.time() - t0
    if p.returncode != 0 or not os.path.exists(out_ascii):
        return part, None, dt, p.stderr[-600:]
    # empty geometry => OpenSCAD still writes a tiny file; drop it
    if os.path.getsize(out_ascii) < 400:
        os.remove(out_ascii)
        return part, None, dt, "empty"
    import trimesh
    m = trimesh.load(out_ascii, force="mesh")
    final = os.path.join(outdir, f"{part}.stl")
    m.export(final)
    os.remove(out_ascii)
    return part, final, dt, ("WARNING" in p.stderr and p.stderr[-300:]) or ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pet", default="dog")
    ap.add_argument("--costume", default="witch")
    ap.add_argument("--base", default="stump")
    ap.add_argument("--name", default="LUNA")
    ap.add_argument("--out", required=True)
    ap.add_argument("--layout", default="print", choices=["print", "assembled"])
    ap.add_argument("--fn", type=int, default=40)
    ap.add_argument("--multicolor", action="store_true")
    ap.add_argument("--parts", default="")
    ap.add_argument("--font", default="DejaVu Sans:style=Bold")
    ap.add_argument("--tol", type=float, default=0.15)
    ap.add_argument("--jobs", type=int, default=4)
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    parts = args.parts.split(",") if args.parts else ["pet", "base"] + (
        [] if args.costume == "none" else ["hat", "cape", "charm"])
    if args.multicolor:
        parts += ["marks", "face", "shine", "deco", "text"] + ([] if args.costume == "none" else ["trim"])
    with cf.ThreadPoolExecutor(args.jobs) as ex:
        futs = [ex.submit(run_part, p, args, args.out) for p in parts]
        for f in cf.as_completed(futs):
            part, path, dt, msg = f.result()
            print(f"{part:6s} {'OK ' if path else 'FAIL'} {dt:5.1f}s  {os.path.basename(path) if path else ''} {msg}")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
