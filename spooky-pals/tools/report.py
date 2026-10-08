#!/usr/bin/env python3
"""Build docs/PRINT_REPORT.md from the print-orientation STL sets.

  python3 report.py <dir containing one folder per set (NAME/pet.stl, hat.stl, ...)> [--out ../docs/PRINT_REPORT.md]

For every part it records geometry sanity (watertight / single body), the area of faces that
overhang steeper than 45 deg, and the 'layer test' volume: material a slicer would have to
bridge or support at 0.2 mm layers with a 45 deg overhang limit.
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import printcheck  # noqa: E402

PARTS = ["pet", "hat", "cape", "charm", "base"]
DENSITY = 1.24          # PLA g/cm3


def est_mass(volume_cm3, surface_mm2, infill=0.15, wall_mm=1.2):
    """Rough filament mass: shell (walls + skins) plus sparse infill."""
    shell = min(volume_cm3, surface_mm2 * wall_mm / 1000.0)
    return DENSITY * (shell + max(0.0, volume_cm3 - shell) * infill)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("sets")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(__file__), "..", "docs", "PRINT_REPORT.md"))
    ap.add_argument("--json", default="")
    ap.add_argument("--meta", default="", help="json {NAME: 'pet / costume / base'}")
    a = ap.parse_args()
    meta = json.load(open(a.meta)) if a.meta else {}

    rows, data = [], {}
    tot = dict(parts=0, clean=0, vol=0.0, uns=0.0, mass=0.0)
    for name in sorted(os.listdir(a.sets)):
        d = os.path.join(a.sets, name)
        if not os.path.isdir(d):
            continue
        for part in PARTS:
            f = os.path.join(d, part + ".stl")
            if not os.path.exists(f):
                continue
            r = printcheck.analyze(f)
            data[f"{name}/{part}"] = r
            vol = r["volume_cm3"] or 0.0
            mass = est_mass(vol, r["surface_mm2"])
            pct = (r["unsupported_mm3"] / 1000.0) / vol * 100 if vol else 0
            verdict = "no supports" if pct < 0.12 else ("tiny overhang (no supports needed)" if pct < 0.3 else "check")
            rows.append((name, meta.get(name, ""), part, r, mass, pct, verdict))
            tot["parts"] += 1
            tot["clean"] += 1 if pct < 0.3 else 0
            tot["vol"] += vol
            tot["uns"] += r["unsupported_mm3"]
            tot["mass"] += mass
            print(f"{name:8s}{part:6s} vol {vol:6.2f} cm3  unsupported {r['unsupported_mm3']:6.2f} mm3 ({pct:.3f}%)  {verdict}")
            sys.stdout.flush()

    lines = ["# Print report (automatically generated)", "",
             "Every STL is analysed in its **print orientation** by `tools/printcheck.py`:",
             "", "* *watertight / bodies* - the mesh is closed and a single shell (no slicer repair needed)",
             "* *over 45 deg* - area of downward facing faces steeper than 45 deg (flat bed contact is ignored)",
             "* *layer test* - the volume of material that, at 0.2 mm layers, sticks out more than 0.2 mm beyond the layer "
             "below (= what a slicer would have to bridge or support). `% of part` relates it to the part volume.",
             "* *est. PLA* - rough filament use (1.2 mm shell + 15 % infill), for planning only", "",
             "| set | combo | part | size (mm) | volume cm3 | closed | over 45 deg mm2 | layer test mm3 | % of part | est. PLA g | verdict |",
             "|---|---|---|---|---:|:-:|---:|---:|---:|---:|---|"]
    for name, combo, part, r, mass, pct, verdict in rows:
        lines.append(f"| {name} | {combo} | {part} | {' x '.join(str(int(round(v))) for v in r['size_mm'])} | "
                     f"{r['volume_cm3']} | {'yes' if r['watertight'] and r['bodies'] == 1 else 'NO'} | {r['over45_mm2']} | "
                     f"{r['unsupported_mm3']} | {pct:.3f} | {mass:.0f} | {verdict} |")
    lines += ["", f"**{tot['clean']} of {tot['parts']} parts** need no support structures. "
              f"All {tot['parts']} parts together contain {tot['uns']:.0f} mm3 of 'questionable' material "
              f"({tot['uns'] / 1000.0 / max(tot['vol'], 1e-9) * 100:.2f} % of {tot['vol']:.0f} cm3 of plastic).", ""]
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    with open(a.out, "w") as fh:
        fh.write("\n".join(lines) + "\n")
    if a.json:
        json.dump(data, open(a.json, "w"), indent=1)
    print("wrote", a.out)


if __name__ == "__main__":
    main()
