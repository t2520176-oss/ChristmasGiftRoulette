#!/usr/bin/env python3
"""Bill of materials + filament-pin cut list, computed from the same numbers as scad/walker.scad."""
import itertools, re, sys

def num(name, text):
    m = re.search(rf"^{name}\s*=\s*([-0-9.]+)\s*;", text, re.M)
    return float(m.group(1))

src = open("../scad/walker.scad").read()
tl, gap, plate_t, W_in = (num(n, src) for n in ("tl", "lay_gap", "plate_t", "W_in"))
pitch = tl + gap
y_in = W_in / 2; y_out = y_in + plate_t; lay0 = y_out + gap
L = dict(crank=0, coupler=1, rocker=2, elink=2, foot=3)
SET = 4
def ly(j): return lay0 + j * pitch            # inner face of layer j

HEAD = 1.0                                     # extra pin length per end that is melted into a head
joints = {                                     # joint: (kind, layers touched)
    "A  crank-coupler": ("moving", (L["crank"], L["coupler"])),
    "B  coupler-rocker": ("moving", (L["coupler"], L["rocker"])),
    "C  coupler-foot": ("moving", (L["coupler"], L["foot"])),
    "E  foot-elink": ("moving", (L["foot"], L["elink"])),
    "O4 rocker-plate": ("ground", (L["rocker"],)),
    "O6 elink-plate": ("ground", (L["elink"],)),
}
rows = []
for base, who, count in ((0, "front+rear legs", 4), (SET, "mid legs", 2)):
    for jn, (kind, ls) in joints.items():
        lo = min(ls) + base; hi = max(ls) + base
        if kind == "moving":
            stack = ly(hi) + tl - ly(lo)
        else:
            stack = plate_t + (ly(hi) + tl - y_out)          # through the plate and out to the link face
        rows.append((who, jn, count, stack + 2 * HEAD))
print(f"layer pitch {pitch:.1f} mm, link thickness {tl} mm, plate {plate_t} mm, inner width {W_in} mm\n")
print("| leg set | joint | pins | cut length [mm] |\n|---|---|---|---|")
tot = 0
for who, jn, cnt, ln in rows:
    print(f"| {who} | {jn} | {cnt} | {ln:.1f} |"); tot += cnt * ln
idler = W_in + 2 * plate_t + 2 * HEAD
print(f"| idler axles | I1, I2 | 2 | {idler:.1f} |")
tot += 2 * idler
print(f"\ntotal 1.75 mm filament for hinge pins: {tot/1000:.2f} m  (+ 6 set pins of 9 mm)")
for nm, base in (("end shaft x2", 0), ("mid shaft x1", SET)):
    print(f"{nm}: length {2*(ly(base)+tl+0.8):.1f} mm")
print("set pins (hub -> shaft), 1.75 mm filament, 9 mm each: x6")
