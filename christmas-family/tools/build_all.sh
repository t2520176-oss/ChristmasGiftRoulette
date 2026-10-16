#!/usr/bin/env bash
# One command from the OpenSCAD model to print beds, checks and renders.
#
#   tools/build_all.sh                                   # 8-colour edition: PIXELFORM 2026 -> ./stl ./beds ./renders
#   tools/build_all.sh --family SMITH --year 2027 --members boy girl dog cat --names Ethan Lily Buddy Mochi
#
#   # 4-colour edition (brown / ivory / green / red), everything under ./four_color
#   SCAD=openscad/family_ornament_4color.scad PALETTE=palettes/four_color.json PIECE_GROUPS=palettes/groups_4color.json \
#   OUT=four_color PRINT_LAYOUT=local tools/build_all.sh
#
# Environment: SKIP_EXPORT=1 (reuse the STLs) · SKIP_RENDER=1 (no Blender) · QUALITY=draft|final · PYTHON=... · SLICE=0 (skip PrusaSlicer) · CHECKS=0
# Needs: openscad, python3 with trimesh numpy shapely rtree manifold3d pillow, Blender 4.x (renders), PrusaSlicer (slice check)
set -euo pipefail
cd "$(dirname "$0")/.."
PY=${PYTHON:-python3}
SCAD=${SCAD:-openscad/family_ornament.scad}
OUT=${OUT:-.}
PRINT_LAYOUT=${PRINT_LAYOUT:-print}
EXTRA=("$@")
PAL=(); [ -n "${PALETTE:-}" ] && PAL=(--palette "$PALETTE")
GRP=(); [ -n "${PIECE_GROUPS:-}" ] && GRP=(--groups "$PIECE_GROUPS")   # (GROUPS is a reserved bash variable)
mkdir -p "$OUT/docs"
if [ "${SKIP_EXPORT:-0}" != "1" ]; then
  $PY tools/export_roles.py --scad "$SCAD" --out "$OUT/stl/print" --layout "$PRINT_LAYOUT" "${EXTRA[@]}"
  $PY tools/export_roles.py --scad "$SCAD" --out "$OUT/stl/assembled" --layout assembled "${EXTRA[@]}"
fi
rm -rf "$OUT/beds" && $PY tools/plan_beds.py --print-dir "$OUT/stl/print" --out "$OUT/beds" "${PAL[@]}" "${GRP[@]}"
if [ "${CHECKS:-1}" = "1" ]; then
  $PY tools/check_pieces.py "$OUT/stl/print" --islands 5 --md "$OUT/docs/PRINT_REPORT.md"
  { $PY tools/check_fit.py "$OUT/stl/assembled" --min-mm3 0.05;
    $PY tools/check_assembly.py "$OUT/stl/assembled" || true;
    $PY tools/check_sky.py "$OUT/stl/assembled" || true; } | tee "$OUT/docs/ASSEMBLY_REPORT.txt"
fi
if [ "${SLICE:-1}" = "1" ] && command -v prusa-slicer >/dev/null; then
  $PY tools/slice_check.py --beds "$OUT/beds" --ini slicer/prusaslicer_4color_256.ini --json "$OUT/beds/slice_summary.json"
fi
$PY tools/write_plan_md.py --beds "$OUT/beds" --print-dir "$OUT/stl/print"
if [ "${SKIP_RENDER:-0}" != "1" ]; then
  $PY tools/render_family.py --assembled "$OUT/stl/assembled" --beds "$OUT/beds" --out "$OUT/renders" --quality "${QUALITY:-final}" ${EXPOSURE:+--exposure "$EXPOSURE"}
  $PY tools/compose_beds.py --beds "$OUT/beds" --renders "$OUT/renders" --slice "$OUT/beds/slice_summary.json"
fi
