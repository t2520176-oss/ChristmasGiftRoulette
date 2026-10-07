#!/usr/bin/env python3
"""Mass / centre-of-mass estimate and stability margin for Sketch Walker (x measured from the mid crank axle)."""
import re, sys, json, glob
import numpy as np
from shapely.geometry import Polygon
import linkage6 as S, linkage_opt as L

import subprocess, tempfile, os
SC = tempfile.mkdtemp(prefix="walker_parts_")
for _p in ("crank_0", "coupler", "rocker", "foot", "elink", "gear", "idler", "pinion", "plate", "deck", "shaft_end", "shaft_mid", "body"):
    subprocess.run(["openscad", "-D", f'PART="{_p}"', "-o", f"{SC}/{_p}.stl", "../scad/walker.scad"], capture_output=True, check=True)
RHO = 1.25                                    # g/cm3 PETG/PLA
def vol(p):
    v = np.array([[float(x) for x in m.groups()] for m in re.finditer(r"vertex (\S+) (\S+) (\S+)", open(p).read())]).reshape(-1, 3, 3)
    return abs(np.sum(np.einsum('ij,ij->i', v[:, 0], np.cross(v[:, 1], v[:, 2]))) / 6) / 1000.0   # cm3
V = {n: vol(f"{SC}/{n}.stl") for n in ("crank_0", "coupler", "rocker", "foot", "elink", "gear", "idler", "pinion", "plate", "deck", "shaft_end", "shaft_mid", "body")}
src = open("../scad/walker.scad").read()
def _scad_vars(text):
    env = {}
    text = re.sub(r"//.*", "", text)
    for name, expr in re.findall(r"\b([A-Za-z_]\w*)\s*=\s*([^;=]+);", text):
        try: env[name] = eval(expr, {}, dict(env))
        except Exception: pass
    return env
_ENV = _scad_vars(src)
def num(n): return float(_ENV[n])
body_x0 = num("body_x0"); rear_end = num("rear_end"); batt_x0 = num("batt_x0"); batt_l = num("batt_l")
bw = float(re.search(r"body_w\s*=\s*([0-9.]+)", open("../scad/body_outline.scad").read()).group(1))
fill = dict(links=0.95, gear=0.75, shaft=1.0, plate=0.55, deck=0.5, body=0.30)
m = {}
m["links x6 legs (avg)"] = (6 * RHO * fill["links"] * (V["crank_0"] + V["coupler"] + V["rocker"] + V["foot"] + V["elink"]), None)
# leg link centroid over the cycle
c = json.load(open("candidates6_refined.json"))[1]; a = 9.0
g = S.sixbar(c["params"]); n = len(g["A"])
vols = dict(crank=V["crank_0"], coupler=V["coupler"], rocker=V["rocker"], foot=V["foot"], elink=V["elink"])
def com_leg(k):
    A, B, C, E, P = (g[q][k] * a for q in ("A", "B", "C", "E", "P"))
    O4, O6 = g["O4"] * a, g["O6"] * a
    cen = {"crank": (np.zeros(2) + A) / 2, "coupler": (A + B + C) / 3, "rocker": (O4 + B) / 2, "foot": (C + E + P * 2) / 4, "elink": (E + O6) / 2}
    tot = sum(vols.values())
    return sum(vols[k_] * cen[k_] for k_ in vols) / tot
legx = {"F": 60.0, "M": 0.0, "R": -60.0}
legcom = []
for k in range(0, n, 10):
    pts = []
    for nm, x0 in legx.items():
        for side in (0, 1):
            kk = (k + (360 if (nm == "M") ^ (side == 1) else 0)) % n
            pts.append(com_leg(kk) + np.array([x0, 0]))
    legcom.append(np.mean(pts, axis=0))
legcom = np.array(legcom)
lx_mean, lz_mean = legcom[:, 0].mean(), legcom[:, 1].mean()
items = [
    ("legs (6)", 6 * RHO * fill["links"] * sum(vols.values()), lx_mean, lz_mean),
    ("plates (2)", 2 * RHO * fill["plate"] * V["plate"], (-144 + 74) / 2, 3),
    ("deck", RHO * fill["deck"] * V["deck"], (rear_end + 66) / 2, _ENV["deck_z"] - 4),
    ("gears 3+2", 5 * RHO * fill["gear"] * V["gear"], 0.0, 0.0),
    ("shafts 3", RHO * (2 * V["shaft_end"] + V["shaft_mid"]), 0.0, 0.0),
    ("pinion", RHO * V["pinion"], 0.0, 22.5),
    ("pins", 1.8, 0.0, 0.0),
    ("N20 motor", 11.0, 0.0, 22.5),
    ("battery 2xAAA + holder", 38.0, batt_x0 + batt_l / 2, 20.0),
    ("wires/switch", 3.0, -60.0, 20.0),
    ("body (creature)", RHO * fill["body"] * V["body"], body_x0 + bw / 2, _ENV["deck_z"] + _ENV["deck_t"] + 40),
]
M = sum(i[1] for i in items)
cx = sum(i[1] * i[2] for i in items) / M; cz = sum(i[1] * i[3] for i in items) / M
for nme, mass, x, z in items:
    print(f"{nme:24s} {mass:6.1f} g   x={x:7.1f}")
print(f"\nTOTAL {M:.0f} g   COM x = {cx:.1f} mm   COM z = {cz:.1f} mm above crank axle (ground is 49.5 mm below)")
print("stable COM window (design/stability.py, real foot tracks): x in [-68, -23] mm, best -45")
# what body_x0 would centre the COM at -45?
others = M - items[-1][1]; ox = (cx * M - items[-1][1] * items[-1][2]) / others
need = (-45 * M - others * ox) / items[-1][1]
print(f"body centre should sit at x = {need:.1f} mm  ->  body_x0 = {need - bw/2:.1f} mm (currently {body_x0})")
