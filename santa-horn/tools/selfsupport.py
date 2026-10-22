#!/usr/bin/env python3
"""Self-supporting closure of a layered solid: add the material that every overhang needs so that no layer hangs further out than a slicer bridges.

    close_overhangs(L, C, step=1.0)       L: uint8 (nz, H, W) colour labels per layer (0 = empty), changed IN PLACE
                                          C: bool  (nz, H, W) cavities that must stay empty (sound path, phone slot)

The raster pitch is chosen by the caller so that ONE pixel = dz / tan(angle): a layer may stick out one pixel beyond the layer below
(angle = the flattest slope from the horizontal that is allowed, e.g. 56 degrees; PrusaSlicer's auto support starts at 45).

The solid of every layer is carried as a signed distance field psi (> 0 inside, in pixels, float - never thresholded between layers, so
no noise accumulates).  Going down, layer k keeps its own solid S_k and everything the layer above brings with it, moved one pixel TOWARDS
the nearest solid of layer k:

        psi_k(p) = max( psi_S_k(p),  psi_{k+1}(p + step * a(p)) ),       a(p) = direction away from S_k  (smoothed gradient of the distance to S_k)

A ledge, bead or curl gets a skirt that runs down and inwards at the allowed slope until it lands on the body, a floating rim leans down
towards the body like a ramp, a bridge is filled up; where the layer above reaches no further than a pixel beyond S_k nothing is added.  What is
added takes the colour of the layer above.  Cavities (the sound path, the phone slot) are never filled.
"""
import time

import numpy as np
from scipy import ndimage


def close_overhangs(L, C, step=1.0, log=True, sigma=1.5):
    nz, H, W = L.shape
    t0 = time.time()
    added = 0
    rr_all, cc_all = np.mgrid[0:H, 0:W].astype(np.float32)
    psi_prev = None
    for k in range(nz - 1, -1, -1):
        S = L[k] > 0
        d_out = ndimage.distance_transform_edt(~S).astype(np.float32)
        d_in = ndimage.distance_transform_edt(S).astype(np.float32)
        psi_s = d_in - d_out + 0.0                                    # > 0 inside S (about 0.5 px inside at the boundary pixel)
        psi_s[S] = d_in[S] - 0.5
        psi_s[~S] = -d_out[~S] + 0.5
        if psi_prev is None or not (psi_prev > 0).any():
            psi_prev = psi_s
            continue
        zone = ndimage.binary_dilation(psi_prev > 0, iterations=3) | S
        # direction away from the solid
        Dg = ndimage.gaussian_filter(d_out, sigma)
        gr, gc = np.gradient(Dg)
        gn = np.hypot(gr, gc)
        gn[gn < 1e-6] = 1.0
        ar, ac = gr / gn, gc / gn
        ur = np.clip(rr_all + ar * step, 0, H - 1)
        uc = np.clip(cc_all + ac * step, 0, W - 1)
        adv = np.full((H, W), -1.0, np.float32)
        rows, cols = np.nonzero(zone)
        adv[rows, cols] = ndimage.map_coordinates(psi_prev, [ur[rows, cols], uc[rows, cols]], order=1, mode="nearest")
        psi = np.maximum(psi_s, adv)
        psi[C[k]] = np.minimum(psi[C[k]], -0.5)                       # the cavity stays empty
        new = (psi > 0) & ~S
        if new.any():
            # colour of the new pixels: that of the layer above where p + a lands (look in the 3 x 3 neighbourhood, take the strongest label)
            lab_up = ndimage.maximum_filter(L[k + 1] if k + 1 < nz else np.zeros((H, W), np.uint8), size=3)
            r2, c2 = np.nonzero(new)
            lab = lab_up[np.clip(np.rint(ur[r2, c2]).astype(int), 0, H - 1), np.clip(np.rint(uc[r2, c2]).astype(int), 0, W - 1)]
            lab[lab == 0] = lab_up[r2[lab == 0], c2[lab == 0]]
            keep = lab > 0
            L[k][r2[keep], c2[keep]] = lab[keep]
            added += int(keep.sum())
        psi_prev = psi
        if log and k % 100 == 0:
            print("   closure layer %d/%d  (%.0fs)" % (k, nz, time.time() - t0), flush=True)
    return added
