#!/usr/bin/env python3
"""Does the sound get from the phone to the grin - through the glued joint, and only there?

  python3 check_airpath.py --stl ../stl/assembled/pumpkin__shell.stl --scad ../openscad/halloween_horn.scad [--phone 71.5,7.65,146.7]

The pumpkin is voxelised (0.6 mm).  The phone is put into the
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
    ap.add_argument("--stl", nargs="+", required=True, help="the printed piece(s) in assembled coordinates")
    ap.add_argument("--scad", required=True)
    ap.add_argument("--phone", default="71.5,7.65,146.7", help="width, thickness, height of the phone (mm)")
    ap.add_argument("--pitch", type=float, default=0.6)
    a = ap.parse_args()
    txt = open(a.scad).read()
    n = lambda name: scad_num(txt, name)
    y_s, hz0, phone_t, phone_w, slot_d = n("Y_S"), n("HZ0"), n("phone_t"), n("phone_w"), n("slot_d")
    shaft_w, y_f, y_e, room, d_zt = n("SHAFT_W"), n("Y_F"), n("Y_E"), n("ROOM"), n("D_ZT")
    rx, ryf, zc, rzl, rzu, pu = n("Rx"), n("RYF"), n("ZC"), n("RZL"), n("RZU"), n("PU")
    zp = d_zt
    prof = rx * (np.sqrt(max(0, 1 - ((zc - zp) / rzl) ** 2)) if zp <= zc else (max(0, 1 - ((zp - zc) / rzu) ** pu)) ** (1 / pu))
    y_front = ryf * prof                                             # the front of the grin (x = 0), at its top edge
    y_back = y_front - room                                          # the back wall of the grin (x = 0)
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
    y_plug = (y_f + y_back) / 2
    plug = box_mask(-80, 80, y_plug - 1, y_plug + 1, -5, z_top + 5)               # a plate across the horn
    leak = touches_outside(flood(big | plug))
    print("horn plugged: the chamber is %s" % ("NOT closed - air leaks somewhere else!" if leak else "closed (no other leak)"))
    if leak:
        ok = False
    # the channel: cross-section of the air along y
    print("cross-section of the air channel (mm2), phone side -> grin:")
    rows = []
    for yy, label in ((y_h0 + 2.5, "throat"), (y_h0 + 12, "horn"), (y_back - 3, "horn end")):
        j = int(round((yy - origin[1]) / pitch - 0.5))
        c = comp[:, j, :]
        sl = solid[:, j, :]
        # only the air that is inside the pumpkin counts (not the room around it): the voxels that have solid somewhere to their left and right in the same row
        left = np.maximum.accumulate(sl, axis=0)
        right = np.maximum.accumulate(sl[::-1], axis=0)[::-1]
        inside = left & right & c
        inside[:, idx(0, 0, z_top - 1)[2]:] = False             # the ears above the flat top do not count
        rows.append((yy, label, int(inside.sum() * pitch * pitch)))
    # the grin is open to the front: its size is the window seen from the front (the air of the grin projected along y)
    j0, j1 = int(round((y_back - 2 - origin[1]) / pitch - 0.5)), int(round((y_front + 6 - origin[1]) / pitch - 0.5))
    k_top = idx(0, 0, z_top - 1)[2]
    win = comp[:, j0:j1, :k_top].any(axis=1)
    win[:idx(-45, 0, 0)[0]] = False
    win[idx(45, 0, 0)[0]:] = False
    rows.append((y_front, "grin window (front view)", int(win.sum() * pitch * pitch)))
    print("   " + "   ".join("%s: %d" % (l, ar) for yy, l, ar in rows))
    thr, mouth = rows[0][2], rows[-1][2]
    print("   throat %d mm2  ->  grin window %d mm2  (flare %.1f : 1)" % (thr, mouth, mouth / max(thr, 1)))
    # 2) the real leak around the phone
    gap = (big & ~solid & ~box_mask(-pw / 2, pw / 2, y_s - pt / 2, y_s + pt / 2, z_sf, z_top + 60))
    zlev = int(round((z_top - 3 - origin[2]) / pitch - 0.5))
    print("gap around the phone at the top of the slot: %.0f mm2 (phone %.1f x %.2f in a slot %.0f x %.0f)  - stuff foam tape around the phone, or use phone_t = %.1f"
          % (gap[:, :, zlev].sum() * pitch * pitch, pw, pt, phone_w, phone_t, pt + 1.5))
    print("RESULT:", "ok" if ok else "PROBLEM")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
