#!/usr/bin/env python3
"""Slice every bed with PrusaSlicer (CLI) and report time, filament per slot, tool changes and - with the
slicer's own auto-support (45 deg threshold) switched on - how much support material it would add.

  python3 slice_check.py --beds ../beds --ini ../slicer/prusaslicer_4color_256.ini --json ../beds/slice_summary.json
"""
import argparse
import json
import os
import re
import subprocess
import tempfile

import numpy as np
import trimesh


def slice_bed(ini, model, support, out):
    extra = ["--support-material", "--support-material-auto", "--support-material-threshold", "45",
             "--support-material-extruder", "0", "--support-material-interface-extruder", "0"] if support else []
    p = subprocess.run(["prusa-slicer", "--export-gcode", "--dont-arrange", "--load", ini, *extra, "--output", out, model], capture_output=True, text=True)
    if not os.path.exists(out):
        raise SystemExit(f"slicing failed: {p.stdout[-400:]} {p.stderr[-400:]}")
    typ, e_by, tools, prim = None, {}, set(), False
    tw = [1e9, -1e9, 1e9, -1e9]
    for line in open(out):
        if "CP PRIMING START" in line:
            prim = True
        elif "CP PRIMING END" in line:
            prim = False
        if line.startswith(";TYPE:"):
            typ = line[6:].strip()
        elif typ == "Wipe tower" and not prim:
            m = re.match(r"G1 X([\d.]+) Y([\d.]+).* E[\d.]", line)
            if m:
                x, y = float(m.group(1)), float(m.group(2))
                tw = [min(tw[0], x), max(tw[1], x), min(tw[2], y), max(tw[3], y)]
        if re.match(r"T\d\b", line):
            tools.add(line.strip())
        elif line.startswith("G1") and " E" in line and typ:
            m = re.search(r" E(-?[\d.]+)", line)
            if m and float(m.group(1)) > 0:
                e_by[typ] = e_by.get(typ, 0.0) + float(m.group(1))
    txt = open(out).read()
    t = re.search(r"; estimated printing time \(normal mode\) = (.*)", txt)
    f = re.search(r"; filament used \[g\] = (.*)", txt)
    return {"tower_xy": [round(v, 1) for v in tw] if tw[0] < 1e8 else None, "time": t.group(1) if t else "?", "filament_g": [float(x) for x in f.group(1).split(",")] if f else [],
            "tools": sorted(tools), "extrusion_mm": {k: round(v) for k, v in e_by.items()}}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--beds", required=True)
    ap.add_argument("--ini", required=True)
    ap.add_argument("--json", default="")
    a = ap.parse_args()
    plan = json.load(open(os.path.join(a.beds, "plan.json")))
    res = {}
    with tempfile.TemporaryDirectory() as tmp:
        for b in plan["beds"]:
            n = b["bed"]
            model = os.path.join(a.beds, f"bed{n}.3mf")
            r0 = slice_bed(a.ini, model, False, os.path.join(tmp, f"b{n}.gcode"))
            for _ in range(3):                      # PrusaSlicer's time estimator now and then returns garbage (days / negative)
                if re.match(r"^\d{1,2}[dhms]", r0["time"]) and not r0["time"].startswith("-"):
                    break
                r0 = slice_bed(a.ini, model, False, os.path.join(tmp, f"b{n}.gcode"))
            r1 = slice_bed(a.ini, model, True, os.path.join(tmp, f"b{n}s.gcode"))
            sup = {k: v for k, v in r1["extrusion_mm"].items() if k.startswith("Support")}
            r0["auto_support_extrusion_mm"] = sup
            # wipe tower vs the pieces (XY boxes, from the planned positions)
            info = json.load(open(os.path.join(a.beds, f"bed{n}.json")))
            gap = 1e9
            tx0, tx1, ty0, ty1 = r0["tower_xy"] or (1e9, -1e9, 1e9, -1e9)
            for v in info["volumes"]:
                lo, hi = trimesh.load(os.path.join(a.beds, v["file"]), force="mesh").bounds
                dx = max(lo[0] - tx1, tx0 - hi[0], 0.0)
                dy = max(lo[1] - ty1, ty0 - hi[1], 0.0)
                gap = min(gap, float(np.hypot(dx, dy)))
            r0["tower_gap_to_pieces_mm"] = round(gap, 1) if r0["tower_xy"] else None
            r0["tower_inside_bed"] = bool(tx0 >= 0 and ty0 >= 0 and tx1 <= plan["bed"][0] and ty1 <= plan["bed"][1]) if r0["tower_xy"] else None
            r0["tower_extrusion_mm"] = r0["extrusion_mm"].get("Wipe tower", 0)
            res[str(n)] = r0
            print(f"bed {n}: " + (f"tower x {tx0}-{tx1} y {ty0}-{ty1} (gap to pieces {r0['tower_gap_to_pieces_mm']} mm, inside bed: {r0['tower_inside_bed']})" if r0["tower_xy"] else "no colour change - no wipe tower"))
            print(f"bed {n}: {r0['time']}  filament g {r0['filament_g']}  tools {r0['tools']}  auto-support (extruded mm) {sup or 'none'}")
    if a.json:
        json.dump(res, open(a.json, "w"), indent=1)


if __name__ == "__main__":
    main()
