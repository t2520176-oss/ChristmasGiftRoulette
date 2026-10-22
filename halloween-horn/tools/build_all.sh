#!/usr/bin/env bash
# One command from the OpenSCAD model to the print bed, the checks and the renders.
#   tools/build_all.sh
# Environment: SKIP_EXPORT=1 reuse STLs · SKIP_RENDER=1 no Blender · QUALITY=draft|final · SLICE=0 · CHECKS=0 · PYTHON=...
set -euo pipefail
cd "$(dirname "$0")/.."
PY=${PYTHON:-python3}
SCAD=openscad/halloween_horn.scad
OUT=${OUT:-.}
mkdir -p "$OUT/docs"
if [ "${SKIP_EXPORT:-0}" != "1" ]; then
  $PY tools/export_roles.py --scad "$SCAD" --out "$OUT/stl/print" --layout local --members x --names x
  $PY tools/export_roles.py --scad "$SCAD" --out "$OUT/stl/assembled" --layout assembled --members x --names x
fi
rm -rf "$OUT/beds" && $PY tools/plan_beds.py --print-dir "$OUT/stl/print" --out "$OUT/beds" --groups palettes/groups.json
if [ "${CHECKS:-1}" = "1" ]; then
  $PY tools/check_pieces.py "$OUT/stl/print" --islands 5 --solid body.shell,face.shell,garnish.green --md "$OUT/docs/PRINT_REPORT.md"
  $PY tools/check_airpath.py --stl "$OUT/stl/assembled/body__shell.stl" "$OUT/stl/assembled/face__shell.stl" --scad "$SCAD" | tee "$OUT/docs/AIRPATH_REPORT.txt"
fi
if [ "${SLICE:-1}" = "1" ] && command -v prusa-slicer >/dev/null; then
  $PY tools/slice_check.py --beds "$OUT/beds" --ini slicer/prusaslicer_4color_256.ini --json "$OUT/beds/slice_summary.json"
fi
$PY tools/write_plan_md.py --beds "$OUT/beds" --print-dir "$OUT/stl/print"
if [ "${SKIP_RENDER:-0}" != "1" ]; then
  $PY tools/render_scene.py --assembled "$OUT/stl/assembled" --beds "$OUT/beds" --scad "$SCAD" --out "$OUT/renders" --quality "${QUALITY:-final}"
  $PY tools/compose_beds.py --beds "$OUT/beds" --renders "$OUT/renders" --slice "$OUT/beds/slice_summary.json"
fi
