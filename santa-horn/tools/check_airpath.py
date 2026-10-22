#!/usr/bin/env python3
"""Does the sound get from the phone to the bell - and only there?

  python3 check_airpath.py --stl ../stl/assembled/santa__*.stl --params ../build/params.json [--phone 71.5,7.65,146.7]

The figure is voxelised (0.6 mm).  The phone is put into the slot and the whole slot above the phone's bottom edge is treated as sealed
(a phone that fills its slot, or a strip of foam tape), then the air is flooded from the chamber under the phone:  it must come out of the
BELL, and nowhere else.  The cross section of the air channel is reported along the way (chamber -> tunnel -> funnel -> mouth), and so is
the leak that remains when the phone does NOT fill the slot.   Exit code 1 when the chamber is closed or leaks somewhere else.
"""
import argparse
import json
import sys

import numpy as np
import trimesh
from scipy import ndimage


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--stl", nargs="+", required=True, help="the printed regions in assembled coordinates")
    ap.add_argument("--params", required=True)
    ap.add_argument("--phone", default="71.5,7.65,146.7", help="width, thickness, height of the phone (mm)")
    ap.add_argument("--pitch", type=float, default=0.6)
    a = ap.parse_args()
    P = json.load(open(a.params))
    pw, pt, ph = [float(v) for v in a.phone.split(",")]
    mesh = trimesh.util.concatenate([trimesh.load(p, force="mesh") for p in a.stl])
    pitch = a.pitch
    vox = mesh.voxelized(pitch).fill()
    solid, origin = vox.matrix.copy(), np.asarray(vox.translation) - pitch / 2     # origin = corner of voxel (0, 0, 0)
    pad = 6
    solid = np.pad(solid, pad)
    origin = origin - pad * pitch
    vol_err = abs(solid.sum() * pitch ** 3 - sum(trimesh.load(p, force="mesh").volume for p in a.stl)) / mesh.volume
    z_sf, z_top, y_s = P["Z_SF"], P["Z_TOP"], P["Y_S"]
    print("voxelised: %.0f cm3 (%.1f %% off the meshes), top of the collar z = %.1f, slot floor z = %.1f" % (solid.sum() * pitch ** 3 / 1000, 100 * vol_err, z_top, z_sf))

    def idx(x, y, z):
        return tuple(int(round((v - o) / pitch - 0.5)) for v, o in zip((x, y, z), origin))

    def box_mask(x0, x1, y0, y1, z0, z1):
        m = np.zeros(solid.shape, bool)
        i0, j0, k0 = idx(x0, y0, z0)
        i1, j1, k1 = idx(x1, y1, z1)
        m[max(i0, 0):i1 + 1, max(j0, 0):j1 + 1, max(k0, 0):k1 + 1] = True
        return m

    seed = idx(0, y_s, P["HZ0"] + 3)
    if solid[seed]:
        sys.exit("the seed in the chamber is solid - wrong parameters?")
    big = box_mask(-P["phone_w"] / 2 - 1, P["phone_w"] / 2 + 1, y_s - P["phone_t"] / 2 - 1, y_s + P["phone_t"] / 2 + 1, z_sf + 0.01, z_top + 60)

    def flood(extra_solid):
        air = ~(solid | extra_solid)
        lab, _ = ndimage.label(air)
        return lab == lab[seed]

    def touches_outside(c):
        return bool(c[:pad].any() or c[-pad:].any() or c[:, :pad].any() or c[:, -pad:].any() or c[:, :, :pad].any() or c[:, :, -pad:].any())

    comp = flood(big)
    reaches = touches_outside(comp)
    print("slot sealed, air from the chamber reaches the outside: %s" % ("YES" if reaches else "NO - the chamber is closed!"))
    ok = reaches
    # ... and ONLY through the bell: close the tunnel with a plug and nothing may get out any more
    y_plug = (P["throat_y"] + (y_s + P["phone_t"] / 2)) / 2
    plug = box_mask(-80, 80, y_plug - 1, y_plug + 1, -5, z_top + 5)
    leak = touches_outside(flood(big | plug))
    print("tunnel plugged: the chamber is %s" % ("NOT closed - air leaks somewhere else!" if leak else "closed (no other leak)"))
    if leak:
        ok = False
    print("cross section of the air channel (mm2), phone side -> bell:")
    rows = []
    stations = [("chamber", y_s - 1.0), ("tunnel", y_plug), ("throat", P["throat_y"] - 1.0), ("funnel mid", (P["throat_y"] + P["mouth_y"]) / 2), ("mouth", P["mouth_y"] - 0.6)]
    for label, yy in stations:
        j = int(round((yy - origin[1]) / pitch - 0.5))
        c = comp[:, j, :]
        sl = solid[:, j, :]
        left = np.maximum.accumulate(sl, axis=0)                  # only the air that is inside the figure counts
        right = np.maximum.accumulate(sl[::-1], axis=0)[::-1]
        inside = left & right & c
        inside[:, idx(0, 0, z_top - 1)[2]:] = False
        rows.append((label, int(inside.sum() * pitch * pitch)))
    print("   " + "   ".join("%s: %d" % r for r in rows))
    thr = dict(rows)["tunnel"]
    mouth = dict(rows)["mouth"]
    print("   tunnel %d mm2  ->  mouth %d mm2  (flare %.1f : 1)" % (thr, mouth, mouth / max(thr, 1)))
    gap = (big & ~solid & ~box_mask(-pw / 2, pw / 2, y_s - pt / 2, y_s + pt / 2, z_sf, z_top + 60))
    zlev = int(round((z_top - 3 - origin[2]) / pitch - 0.5))
    print("gap around the phone at the top of the slot: %.0f mm2 (phone %.1f x %.2f in a slot %.0f x %.0f)  - stuff foam tape around the phone, or use phone_t = %.1f"
          % (gap[:, :, zlev].sum() * pitch * pitch, pw, pt, P["phone_w"], P["phone_t"], pt + 1.5))
    print("RESULT:", "ok" if ok else "PROBLEM")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
