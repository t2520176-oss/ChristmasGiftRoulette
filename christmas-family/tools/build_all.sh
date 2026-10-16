#!/usr/bin/env bash
# One command from the OpenSCAD model to print beds + renders.
#   tools/build_all.sh                       # PIXELFORM family, 2026, the five default members
#   tools/build_all.sh --family SMITH --year 2027 --names Anna Ben Cleo   (members/names must match in count)
# Needs: openscad, python3 with trimesh numpy shapely (check) + pillow (legend), blender (renders).
set -euo pipefail
cd "$(dirname "$0")/.."
PY=${PYTHON:-python3}
EXTRA=("$@")
$PY tools/export_roles.py --out stl/print --layout print "${EXTRA[@]}"
$PY tools/export_roles.py --out stl/assembled --layout assembled "${EXTRA[@]}"
rm -rf beds && $PY tools/plan_beds.py --print-dir stl/print --out beds
if [ "${SKIP_RENDER:-0}" != "1" ]; then
  $PY tools/render_family.py --assembled stl/assembled --beds beds --out renders --quality "${QUALITY:-final}"
  $PY tools/compose_beds.py --beds beds --renders renders ${SLICE_SUMMARY:+--slice "$SLICE_SUMMARY"}
fi
