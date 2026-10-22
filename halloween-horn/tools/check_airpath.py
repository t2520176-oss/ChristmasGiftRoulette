#!/usr/bin/env python3
"""Does the sound get from the phone to the grin - through the glued joint, and only there?

  python3 check_airpath.py --stl ../stl/assembled/body__shell.stl ../stl/assembled/face__shell.stl --scad ../openscad/halloween_horn.scad [--phone 71.5,7.65,146.7]

The two printed pieces are put together (assembled coordinates, an ideal glued joint) and voxelised (0.6 mm).  The phone is put into the
slot and the whole slot above the phone's bottom edge is treated as sealed (a phone that fills its slot, or a strip of foam tape),
then the air is flooded from the chamber under the phone:  it must come out of the GRIN, and nowhere else.  The cross-section of the
air channel is reported along the way (throat -> grin), and so is the leak that remains when the phone does NOT fill the slot.
Exit code 1 when the chamber is closed, or when it leaks somewhere else than through the grin.
"""
import argparse
import re
import sys

import numpy as np
import trimesh
from scipy import ndimage


def scad_num(text, name):
    m = re.search(r"\b%s\s*=\s*([-0-9.]+)" % name, text)
    if not m:
        raise SystemExit("cannot find %s in the .scad" % name)
    return float(m.group(1))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--stl", nargs="+", required=True, help="the printed pieces in ASSEMBLED coordinates")
    ap.add_argument("--scad", required=True)
    ap.add_argument("--phone", default="71.5,7.65,146.7", help="width, thickness, height of the phone (mm)")
    ap.add_argument("--pitch", type=float, default=0.6)
    a = ap.parse_args()
    txt = open(a.scad).read()
    n = lambda name: scad_num(txt, name)
    y_s, hz0, phone_t, phone_w, slot_d = n("Y_S"), n("HZ0"), n("phone_t"), n("phone_w"), n("slot_d")
    yc, yd, shaft_w = n("YC"), n("YD"), n("SHAFT_W")
    z_top = n("ZC") + n("TOPF") * n("RZU")
    z_sf = z_top - slot_d
    y_h0 = y_s + phone_t / 2
    pw, pt, ph = [float(v) for v in a.phone.split(",")]
    mesh = trimesh.util.concatenate([trimesh.load(p, force="mesh") for p in a.stl])
    pitch = a.pitch
    vox = mesh.voxelized(pitch).fill()
    solid, origin = vox.matrix.copy(), np.asarray(vox.translation) - pitch / 2     # origin = corner of voxel (0, 0, 0)
    pad = 6
    solid = np.pad(solid, pad)
    origin = origin - pad * pitch
    vol_err = abs(solid.sum() * pitch ** 3 - sum(trimesh.load(p, force="mesh").volume for p in a.stl)) / mesh.volume
    print("voxelised: %.0f cm3 (%.1f %% off the meshes), top of the pumpkin z = %.1f, slot floor z = %.1f" % (solid.sum() * pitch ** 3 / 1000, 100 * vol_err, z_top, z_sf))

    def idx(x, y, z):
        return tuple(int(round((v - o) / pitch - 0.5)) for v, o in zip((x, y, z), origin))

    def box_mask(x0, x1, y0, y1, z0, z1):
        m = np.zeros(solid.shape, bool)
        i0, j0, k0 = idx(x0, y0, z0)
        i1, j1, k1 = idx(x1, y1, z1)
        m[max(i0, 0):i1 + 1, max(j0, 0):j1 + 1, max(k0, 0):k1 + 1] = True
        return m

    seed = idx(0, y_s, hz0 + 3)
    if solid[seed]:
        sys.exit("the seed in the chamber is solid - wrong parameters?")
    big = box_mask(-phone_w / 2 - 1, phone_w / 2 + 1, y_s - phone_t / 2 - 1, y_s + phone_t / 2 + 1, z_sf + 0.01, z_top + 60)

    def flood(extra_solid):
        air = ~(solid | extra_solid)
        lab, _ = ndimage.label(air)
        return lab == lab[seed]

    def touches_outside(c):
        return bool(c[:pad].any() or c[-pad:].any() or c[:, :pad].any() or c[:, -pad:].any() or c[:, :, :pad].any() or c[:, :, -pad:].any())

    # 1) slot sealed: the chamber must lead to the outside through the grin ...
    comp = flood(big)
    reaches = touches_outside(comp)
    print("slot sealed, air from the chamber reaches the outside: %s" % ("YES" if reaches else "NO - the chamber is closed!"))
    ok = reaches
    # ... and ONLY through the grin: close the grin with a plug and nothing may get out any more
    plug = box_mask(-80, 80, yd + 1.5, yd + 3.5, -5, z_top + 5)
    leak = touches_outside(flood(big | plug))
    print("grin plugged: the chamber is %s" % ("NOT closed - air leaks somewhere else!" if leak else "closed (no other leak, also not through the joint)"))
    if leak:
        ok = False
    # the channel: cross-section of the air along y
    print("cross-section of the air channel (mm2), phone side -> grin:")
    rows = []
    for yy, label in ((y_h0 + 2.5, "throat"), (y_h0 + 15, "horn"), (yc - 2, "horn end"), (yc + 4, "funnel"), (yd + 1, "grin")):
        j = int(round((yy - origin[1]) / pitch - 0.5))
        c = comp[:, j, :]
        # only the air that is inside the pumpkin counts (not the room around it): the connected part that does not touch the outside is
        # not available, so count the voxels that have solid on both sides along x within the body outline
        row = c.copy()
        sl = solid[:, j, :]
        # keep the voxels that have solid somewhere to their left and right in the same row
        left = np.maximum.accumulate(sl, axis=0)
        right = np.maximum.accumulate(sl[::-1], axis=0)[::-1]
        inside = left & right & row
        inside[:, idx(0, 0, z_top - 1)[2]:] = False             # the ears above the flat top do not count
        rows.append((yy, label, int(inside.sum() * pitch * pitch)))
    print("   " + "   ".join("%s y=%+.0f: %d" % (l, yy, ar) for yy, l, ar in rows))
    thr = rows[0][2]
    mouth = rows[-1][2]
    print("   throat %d mm2  ->  grin %d mm2  (flare %.1f : 1)" % (thr, mouth, mouth / max(thr, 1)))
    # 2) the real leak around the phone
    gap = (big & ~solid & ~box_mask(-pw / 2, pw / 2, y_s - pt / 2, y_s + pt / 2, z_sf, z_top + 60))
    zlev = int(round((z_top - 3 - origin[2]) / pitch - 0.5))
    print("gap around the phone at the top of the slot: %.0f mm2 (phone %.1f x %.2f in a slot %.0f x %.0f)  - stuff foam tape around the phone, or use phone_t = %.1f"
          % (gap[:, :, zlev].sum() * pitch * pitch, pw, pt, phone_w, phone_t, pt + 1.5))
    print("RESULT:", "ok" if ok else "PROBLEM")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
