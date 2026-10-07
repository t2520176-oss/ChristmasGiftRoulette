#!/usr/bin/env python3
"""Cross-checks: constants shared by lantern.py and scad/parts.scad, 3MF well-formedness, lid-fit numbers."""
import re, zipfile, sys, os
import xml.etree.ElementTree as ET
import numpy as np
import lantern as L
here = os.path.dirname(os.path.abspath(__file__))
scad = open(os.path.join(here, "../scad/parts.scad")).read()
def sv(n): return float(re.search(rf"^{n}\s*=\s*([-0-9.]+)\s*;", scad, re.M).group(1))
ok = True
for name, py in (("R_OUT", L.R_OUT), ("T_INK", L.T_INK), ("LID_CLEAR", L.LID_CLEAR)):
    same = abs(sv(name) - py) < 1e-9; ok &= same
    print(f"{name:10s} python {py}  scad {sv(name)}  {'OK' if same else 'MISMATCH'}")
r_in = L.R_OUT - L.T_INK; plug = sv("R_OUT") - sv("T_INK") - sv("LID_CLEAR")
print(f"inner radius {r_in:.2f}  lid plug radius {plug:.2f}  radial clearance {r_in-plug:.2f} mm")
z = zipfile.ZipFile(os.path.join(here, "../out/lantern.3mf"))
root = ET.fromstring(z.read("3D/3dmodel.model"))
ns = {"m": "http://schemas.microsoft.com/3dmanufacturing/core/2015/02"}
nv = len(root.findall(".//m:vertex", ns)); nt = len(root.findall(".//m:triangle", ns))
print("3MF parts:", z.namelist(), " vertices", nv, " triangles", nt)
stl_tris = (os.path.getsize(os.path.join(here, "../out/lantern.stl")) - 84) // 50
print("STL triangles", stl_tris, "-> same as 3MF:", stl_tris == nt); ok &= stl_tris == nt
sys.exit(0 if ok else 1)
