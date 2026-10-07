#!/bin/bash
# Colour-coded assembly previews (software renderer, no GPU).  Usage: make_previews.sh [THETA]
set -e
cd "$(dirname "$0")/../scad"
TH=${1:-40}
T=$(mktemp -d)
SCADIR=$(pwd)
render_group() {   # name, scad statement, extra -D
  n=$1; stmt=$2; shift 2
  printf 'include <%s/walker.scad>\nPART="none";\n%s\n' "$SCADIR" "$stmt" > "$T/$n.scad"
  openscad -D "THETA=$TH" -D '$fn=24' "$@" -o "$T/$n.stl" "$T/$n.scad" > "$T/$n.log" 2>&1
}
for w in crank coupler rocker foot elink; do
  render_group "$w" 'legs_asm();' -D "LINK_FILTER=\"$w\"" &
done
for g in plates deck shafts gears pinion motor battery; do
  render_group "$g" "${g}_asm();" &
done
wait
openscad -D 'PART="body"' -D '$fn=24' -o "$T/body.stl" walker.scad > "$T/body.log" 2>&1
python3 - "$T" "$(pwd)/../out" <<'PY'
import sys, re
import numpy as np
sys.path.insert(0, "../design")
from render_stl import load_stl, render
T, OUT = sys.argv[1], sys.argv[2]
src = open("walker.scad").read()
env = {}
for name, expr in re.findall(r"\b([A-Za-z_]\w*)\s*=\s*([^;=]+);", re.sub(r"//.*", "", src)):
    try: env[name] = eval(expr, {}, dict(env))
    except Exception: pass
cols = {"plates": (170, 176, 186), "deck": (140, 146, 158), "shafts": (80, 80, 84), "gears": (205, 133, 63), "pinion": (112, 128, 144),
        "motor": (30, 30, 34), "battery": (46, 125, 50), "crank": (230, 90, 70), "coupler": (235, 190, 40), "rocker": (50, 160, 235),
        "foot": (60, 160, 100), "elink": (190, 100, 210), "body": (242, 227, 161)}
tris, tc = [], []
for g, c in cols.items():
    try: t = load_stl(f"{T}/{g}.stl")
    except Exception as e: print("skip", g, e); continue
    if g == "body":      # place the creature on the deck exactly like body_placed()
        bx0 = env["body_x0"]; bt = env["body_t"]; top = env["deck_z"] + env["deck_t"]
        # body_placed(): translate([x0, bt/2, top]) rotate([90,0,0])  (x,y,z)->(x,-z,y)
        t = np.stack([t[..., 0] + bx0, -t[..., 2] + bt / 2, t[..., 1] + top], axis=-1)
    tris.append(t); tc += [c] * len(t)
tri = np.concatenate(tris); tc = np.array(tc, float)
for name, az, el, W in (("preview_iso", 38, 20, 1500), ("preview_side", -90, 0, 1500), ("preview_top", -90, 89.9, 1500), ("preview_front", 0, 4, 900)):
    render(tri, az, el, W, int(W * 0.64), tri_colors=tc).save(f"{OUT}/{name}.png"); print("wrote", name)
PY
rm -rf "$T"
