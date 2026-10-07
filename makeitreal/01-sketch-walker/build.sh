#!/bin/bash
# Build every printable job + calibration coupon.  Needs OpenSCAD >= 2021.01, python3 (numpy, pillow).
set -e
cd "$(dirname "$0")/scad"
mkdir -p ../out
for p in print_links print_feet print_drive print_plates print_deck print_body; do
  echo "== $p"; openscad -D "PART=\"$p\"" -o "../out/$p.stl" walker.scad 2>&1 | grep -E "ERROR|WARNING|Simple" || true
done
openscad -o ../out/00_calibration_coupon.stl tolerance_coupon.scad 2>&1 | grep -E "ERROR|WARNING|Simple" || true
python3 ../design/check_stl.py ../out/*.stl
