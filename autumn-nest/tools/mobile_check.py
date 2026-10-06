#!/usr/bin/env python3
"""Measure and verify the Forest Balance Mobile (entry #7).

    python mobile_check.py ../07-forest-mobile/forest_mobile.scad [--apply] [--twist] [--keep DIR]

1. Exports every ornament with OpenSCAD and measures its mass from the STL volume
   (PLA 1.24 g/cm3). With --apply the measured masses are written into the .scad defaults,
   so the computed pivot positions use real numbers (run it twice if the numbers changed).
2. Exports the assembled mobile piece by piece (world coordinates) and checks, on the meshes
   and independently of the formulas in the .scad file:
   - balance: the centre of mass of every sub-mobile lies below its pivot ring
     (distance along the arm, and the tilt this would cause)
   - joints: every ring clears the hook wire it hangs on
   - swing: sibling sub-mobiles can rotate freely around their hang points without touching
   - sensitivity: tilt caused by a +/-5 % mass error of one piece
   - with --twist: how far each piece can swing (yaw) on its hook before the ring jams
Exit code 1 if a check fails.
"""
import argparse
import math
import os
import re
import subprocess
import sys
import tempfile

import numpy as np
import trimesh

DENSITY = 1.24e-3  # g/mm3
ORN = ["owl", "leaf", "acorn", "mushroom", "pinecone"]
ARMS = ["arm1", "arm2", "arm3", "arm4"]
# tree: arm -> (left child, right child)
TREE = {"arm1": ("arm2", "arm3"), "arm2": ("leaf", "acorn"),
        "arm3": ("owl", "arm4"), "arm4": ("mushroom", "pinecone")}
PIV_NAME = {"arm1": "A1", "arm2": "A2", "arm3": "A3", "arm4": "A4"}
HANG_DROP = 17.0 + 4 + 9 + 5.5  # |hy| = d_h + r_h + y_c; refreshed from the scad below


def run_scad(scad, defs, out):
    cmd = ["openscad"] + [x for k, v in defs.items() for x in ("-D", f'{k}="{v}"' if isinstance(v, str) else f"{k}={v}")]
    r = subprocess.run(cmd + ["-o", out, scad], capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit(r.stderr)
    return r.stderr


def load(path):
    m = trimesh.load(path, force="mesh")
    return m


def parent_contains(parent, child, n=15000):
    """True if the child's surface enters the parent's volume (a collision)."""
    pts = child.sample(n)
    return bool(parent.contains(pts).any())


def subtree(name):
    if name in TREE:
        l, r = TREE[name]
        return [name] + subtree(l) + subtree(r)
    return [name]


def scad_number(text, name):
    m = re.search(rf"^{name}\s*=\s*([-0-9.]+)\s*;", text, re.M)
    return float(m.group(1)) if m else None


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("scad")
    ap.add_argument("--apply", action="store_true", help="write measured ornament masses into the .scad")
    ap.add_argument("--twist", action="store_true", help="also measure how far each joint can twist (slow)")
    ap.add_argument("--keep", help="keep the exported STL files in this directory")
    a = ap.parse_args()
    scad = os.path.abspath(a.scad)
    tmp = a.keep or tempfile.mkdtemp(prefix="mobile_")
    os.makedirs(tmp, exist_ok=True)
    text = open(scad).read()
    ok = True

    # 1. ornament masses ------------------------------------------------------------
    print("ornament masses (STL volume x PLA density):")
    measured = {}
    for o in ORN:
        p = os.path.join(tmp, f"{o}.stl")
        run_scad(scad, {"part": o}, p)
        mesh = load(p)
        v = mesh.volume
        nb = len([b for b in mesh.split(only_watertight=False) if abs(b.volume) > 1.0])
        if nb != 1:
            print(f"  FAIL: {o} is {nb} separate bodies (a piece would fall apart while printing)")
            ok = False
        measured[o] = v * DENSITY
        cur = scad_number(text, f"m_{o}")
        flag = ""
        if cur is not None and abs(cur - measured[o]) / measured[o] > 0.01:
            flag = f"  <-- scad default {cur:.2f} g differs by {100*(cur/measured[o]-1):+.1f} %"
            ok = False
        print(f"  {o:10s} {measured[o]:5.2f} g   ({v:7.0f} mm3){flag}")
    if a.apply:
        for o in ORN:
            text = re.sub(rf"^(m_{o}\s*=\s*)[-0-9.]+(\s*;)", rf"\g<1>{measured[o]:.2f}\g<2>", text, flags=re.M)
        open(scad, "w").write(text)
        print("  -> written into", scad, "(run again to verify the pivots with the new masses)")
        return

    for k in ARMS:
        p = os.path.join(tmp, f"{k}.stl")
        run_scad(scad, {"part": k}, p)
        nb = len([b for b in load(p).split(only_watertight=False) if abs(b.volume) > 1.0])
        if nb != 1:
            print(f"  FAIL: {k} is {nb} separate bodies")
            ok = False

    # 2. assembled export -----------------------------------------------------------
    piv = {}
    stderr = ""
    meshes = {}
    for name in ARMS + ORN:
        p = os.path.join(tmp, f"asm_{name}.stl")
        stderr = run_scad(scad, {"part": "asm", "asm_part": name}, p)
        meshes[name] = load(p)
    for line in stderr.splitlines():
        m = re.search(r'PIV\|(\w+)\|([-0-9.e]+)\|([-0-9.e]+)\|([-0-9.e]+)\|([-0-9.]+)', line)
        if m:
            piv[m.group(1)] = (np.array([float(m.group(i)) for i in (2, 3, 4)]), float(m.group(5)))
    pv = lambda n: piv[PIV_NAME.get(n, n)]

    mass = {n: abs(m.volume) * DENSITY for n, m in meshes.items()}
    cg = {n: m.center_mass for n, m in meshes.items()}
    total = sum(mass.values())
    allpts = np.vstack([m.vertices for m in meshes.values()])
    size = allpts.max(0) - allpts.min(0)
    print(f"\nassembled mobile: total mass {total:.1f} g, size {size[0]:.0f} x {size[1]:.0f} x {size[2]:.0f} mm")

    # 3. balance --------------------------------------------------------------------
    print("\nbalance (centre of mass of everything an arm carries vs its pivot ring):")
    hang_drop = None
    for arm in ARMS:
        names = subtree(arm)
        m = np.array([mass[n] for n in names])
        c = (np.array([cg[n] for n in names]) * m[:, None]).sum(0) / m.sum()
        P, yaw = pv(arm)
        u = np.array([math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), 0.0])
        e_along = float(np.dot(c - P, u))
        e_side = float(np.dot(c - P, [-u[1], u[0], 0.0]))
        drop = P[2] - c[2]
        tilt = math.degrees(math.atan2(abs(e_along), drop))
        good = abs(e_along) <= 0.5
        ok &= good
        print(f"  {arm}: carries {m.sum():5.1f} g, CG {e_along:+5.2f} mm along the arm, {e_side:+5.2f} mm sideways, "
              f"{drop:5.1f} mm below the pivot -> tilt {tilt:4.1f} deg  {'ok' if good else 'NOT BALANCED'}")

    # 4. joints ---------------------------------------------------------------------
    print("\njoint clearance (ring hole around the hook wire, parent <-> child):")
    for parent, (l, r) in TREE.items():
        for child in (l, r):
            pts = meshes[child].sample(30000)
            _, d, _ = trimesh.proximity.closest_point(meshes[parent], pts)
            good = d.min() > 0.3
            ok &= good
            print(f"  {parent} <-> {child:9s} {d.min():4.2f} mm  {'ok' if good else 'COLLIDES'}")

    # 5. swing clearance --------------------------------------------------------------
    print("\nswing clearance between siblings (each sub-mobile may rotate about its hang axis):")

    def radius(name):
        P, _ = pv(name)
        pts = np.vstack([meshes[n].vertices for n in subtree(name)])
        return float(np.max(np.hypot(pts[:, 0] - P[0], pts[:, 1] - P[1])))

    for parent, (l, r) in TREE.items():
        sep = float(np.linalg.norm(pv(l)[0][:2] - pv(r)[0][:2]))
        rl, rr = radius(l), radius(r)
        margin = sep - rl - rr
        good = margin >= 3.0
        ok &= good
        print(f"  {parent}: hang points {sep:5.1f} mm apart, swing radii {rl:4.1f} + {rr:4.1f} -> free {margin:+5.1f} mm  "
              f"{'ok' if good else 'TOO TIGHT'}")

    # 6. sensitivity ------------------------------------------------------------------
    print("\nsensitivity: tilt of an arm when one piece it carries is 5 % heavier (conservative lever = hang drop):")
    for arm in ARMS:
        names = subtree(arm)
        P, yaw = pv(arm)
        u = np.array([math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), 0.0])
        m0 = np.array([mass[n] for n in names])
        c0 = np.array([cg[n] for n in names])
        worst = 0.0
        for i in range(len(names)):
            m1 = m0.copy()
            m1[i] *= 1.05
            c1 = (c0 * m1[:, None]).sum(0) / m1.sum()
            worst = max(worst, abs(float(np.dot(c1 - P, u))))
        drop = P[2] - (c0 * m0[:, None]).sum(0)[2] / m0.sum()
        print(f"  {arm}: worst case {worst:4.2f} mm off the pivot -> tilt about {math.degrees(math.atan2(worst, drop)):3.1f} deg")

    print("\nsensitivity to infill: all arms printed 10 % lighter than the ornaments (e.g. other wall/infill settings):")
    for arm in ARMS:
        names = subtree(arm)
        P, yaw = pv(arm)
        u = np.array([math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), 0.0])
        m1 = np.array([mass[n] * (0.9 if n in ARMS else 1.0) for n in names])
        c0 = np.array([cg[n] for n in names])
        c1 = (c0 * m1[:, None]).sum(0) / m1.sum()
        e = abs(float(np.dot(c1 - P, u)))
        drop = P[2] - c1[2]
        print(f"  {arm}: {e:4.2f} mm off the pivot -> tilt about {math.degrees(math.atan2(e, drop)):3.1f} deg")

    if a.twist:
        print("\ntwist range of every joint (child yaw about the vertical axis):")
        print("  a yaw is possible if the ring can sit somewhere on the wire without touching it;")
        print("  the ring re-seats itself (its sag changes) as it turns, so several sags are tried")

        def hit(parent, child, sag, yaw):
            c = meshes[child].copy()
            c.apply_translation([0, 0, -sag])
            P, _ = pv(child)
            c.apply_transform(trimesh.transformations.rotation_matrix(math.radians(yaw), [0, 0, 1], point=P))
            return parent_contains(meshes[parent], c, 8000)

        sags = [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5]
        for parent, (l, r) in TREE.items():
            for child in (l, r):
                lim = {}
                for sign in (+1, -1):
                    lim[sign] = 75
                    for ang in range(5, 76, 5):
                        if all(hit(parent, child, sg, sign * ang) for sg in sags):
                            lim[sign] = ang - 5
                            break
                print(f"  {parent} -> {child:9s} can yaw {lim[1]:2d} deg one way and {lim[-1]:2d} deg the other (checked up to 75)")

    print("\nRESULT:", "all checks passed" if ok else "CHECK FAILED")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
