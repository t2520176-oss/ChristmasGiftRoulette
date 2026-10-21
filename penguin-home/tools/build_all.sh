#!/usr/bin/env bash
# One command from the OpenSCAD model to print beds, checks and renders.
#   tools/build_all.sh                                   # everything (renders at "final" quality)
#   tools/build_all.sh --scad-args ...                   # (not needed: edit sign_line1 / sign_line2 in the scad, or use -D via SIGN1/SIGN2)
# Environment: SKIP_EXPORT=1 reuse STLs · SKIP_RENDER=1 no Blender · QUALITY=draft|final · SLICE=0 · CHECKS=0 · PYTHON=...
#              SIGN1="PIXELFORM" SIGN2="PENGUINS"   change the text of the sign
set -euo pipefail
cd "$(dirname "$0")/.."
PY=${PYTHON:-python3}
SCAD=openscad/penguin_home.scad
OUT=${OUT:-.}
# the sign text is a plain OpenSCAD variable: pass it through a tiny wrapper when it is changed
if [ -n "${SIGN1:-}${SIGN2:-}" ]; then
  W=$(mktemp -d)/penguin_home_custom.scad
  { echo "sign_line1 = \"${SIGN1:-PENGUIN}\"; sign_line2 = \"${SIGN2:-HOME}\";"; sed -e '/^sign_line1 =/d' -e '/^sign_line2 =/d' "$SCAD"; } > "$W"
  SCAD="$W"
fi
mkdir -p "$OUT/docs"
if [ "${SKIP_EXPORT:-0}" != "1" ]; then
  $PY tools/export_roles.py --scad "$SCAD" --out "$OUT/stl/print" --layout local --members x --names x
  $PY tools/export_roles.py --scad "$SCAD" --out "$OUT/stl/assembled" --layout assembled --members x --names x
fi
rm -rf "$OUT/beds" && $PY tools/plan_beds.py --print-dir "$OUT/stl/print" --out "$OUT/beds" --groups palettes/groups.json
if [ "${CHECKS:-1}" = "1" ]; then
  $PY tools/check_pieces.py "$OUT/stl/print" --islands 5 --solid ice,ring,belt --wall lid.ice --md "$OUT/docs/PRINT_REPORT.md"
  { $PY tools/check_fit.py "$OUT/stl/assembled" --min-mm3 0.05;
    $PY tools/check_assembly.py "$OUT/stl/assembled" --top 0 --after "body:lid,trim" || true; } | tee "$OUT/docs/ASSEMBLY_REPORT.txt"
fi
if [ "${SLICE:-1}" = "1" ] && command -v prusa-slicer >/dev/null; then
  $PY tools/slice_check.py --beds "$OUT/beds" --ini slicer/prusaslicer_4color_256.ini --json "$OUT/beds/slice_summary.json"
fi
$PY tools/write_plan_md.py --beds "$OUT/beds" --print-dir "$OUT/stl/print"
if [ "${SKIP_RENDER:-0}" != "1" ]; then
  $PY tools/render_scene.py --assembled "$OUT/stl/assembled" --beds "$OUT/beds" --scad "$SCAD" --out "$OUT/renders" --quality "${QUALITY:-final}"
  $PY tools/compose_beds.py --beds "$OUT/beds" --renders "$OUT/renders" --slice "$OUT/beds/slice_summary.json"
fi
