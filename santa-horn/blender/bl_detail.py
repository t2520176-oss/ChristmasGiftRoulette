"""Surface details of the Santa (Blender / bmesh / numpy): curls, rope, knit stitches, popcorn."""
import math

import bmesh
import bpy
import numpy as np
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

from bl_lib import ico_bm, mesh_np


def unit(v):
    v = np.asarray(v, float)
    return v / (np.linalg.norm(v, axis=-1, keepdims=True) + 1e-12)


def tangent_frame(n, ref=(0, 0, 1)):
    """two unit vectors u, v perpendicular to n (u points 'up' along the reference direction as far as possible)"""
    n = unit(n)
    r = np.asarray(ref, float)
    if abs(float(n @ r)) > 0.95:
        r = np.array([0, 1.0, 0])
    u = unit(r - (r @ n) * n)
    v = np.cross(n, u)
    return u, v


# ---- tubes -----------------------------------------------------------------------------------------------------------
def tube_into(bm, pts, radii, nv=10):
    """sweep a circle (radius per point) along a polyline; both ends are closed (the radius should end small)"""
    pts = np.asarray(pts, float)
    radii = np.asarray(radii, float)
    n = len(pts)
    T = unit(np.gradient(pts, axis=0))
    # parallel transport frames
    N = np.zeros_like(pts)
    a = np.array([0, 0, 1.0]) if abs(T[0, 2]) < 0.9 else np.array([1.0, 0, 0])
    N[0] = unit(np.cross(T[0], a))
    for i in range(1, n):
        axis = np.cross(T[i - 1], T[i])
        s = np.linalg.norm(axis)
        if s < 1e-9:
            N[i] = N[i - 1]
        else:
            axis = axis / s
            ang = math.atan2(s, float(T[i - 1] @ T[i]))
            k = N[i - 1]
            N[i] = k * math.cos(ang) + np.cross(axis, k) * math.sin(ang) + axis * (axis @ k) * (1 - math.cos(ang))
    B = np.cross(T, N)
    rings = []
    for i in range(n):
        ring = []
        for j in range(nv):
            t = 2 * math.pi * j / nv
            p = pts[i] + radii[i] * (math.cos(t) * N[i] + math.sin(t) * B[i])
            ring.append(bm.verts.new(p.tolist()))
        rings.append(ring)
    for i in range(n - 1):
        for j in range(nv):
            bm.faces.new([rings[i][j], rings[i][(j + 1) % nv], rings[i + 1][(j + 1) % nv], rings[i + 1][j]])
    for end, rev in ((0, True), (n - 1, False)):
        c = bm.verts.new(pts[end].tolist())
        for j in range(nv):
            f = [rings[end][j], rings[end][(j + 1) % nv], c]
            bm.faces.new(f[::-1] if rev else f)


# ---- sampling the surface --------------------------------------------------------------------------------------------
def sample_surface(v, t, n, mask_fn, rng, oversample=40):
    """n_samples random surface points (area weighted) with their normals, filtered by mask_fn(p, normal) -> bool array"""
    a, b, c = v[t[:, 0]], v[t[:, 1]], v[t[:, 2]]
    nr = np.cross(b - a, c - a)
    area = 0.5 * np.linalg.norm(nr, axis=1)
    nr = unit(nr)
    prob = area / area.sum()
    idx = rng.choice(len(t), size=n * oversample, p=prob)
    r1, r2 = rng.random(len(idx)), rng.random(len(idx))
    flip = r1 + r2 > 1
    r1[flip], r2[flip] = 1 - r1[flip], 1 - r2[flip]
    p = a[idx] + (b[idx] - a[idx]) * r1[:, None] + (c[idx] - a[idx]) * r2[:, None]
    nn = nr[idx]
    keep = mask_fn(p, nn)
    return p[keep], nn[keep]


def poisson_sizes(p, nrm, sizes, rng, gap=0.92, max_n=400):
    """dart throwing with decreasing sizes: every accepted point (position, normal, size) keeps `gap` * (size + size of the others) away from the others"""
    acc_p, acc_n, acc_s = [], [], []
    order = rng.permutation(len(p))
    p, nrm = p[order], nrm[order]
    for s in sizes:
        m = 0
        for i in range(len(p)):
            if len(acc_p) >= max_n:
                break
            if acc_p:
                d = np.linalg.norm(np.asarray(acc_p) - p[i], axis=1)
                if np.any(d < gap * (np.asarray(acc_s) + s)):
                    continue
            acc_p.append(p[i])
            acc_n.append(nrm[i])
            acc_s.append(s)
            m += 1
    return np.asarray(acc_p), np.asarray(acc_n), np.asarray(acc_s)


# ---- curls ------------------------------------------------------------------------------------------------------------
def curl_into(bm, p, n, rho, rng, turns=1.45, tube=0.36, lift=0.45, nseg=44, nv=9):
    """a flat spiral roll (a tube that coils in the tangent plane at p, partly sunk into the surface)"""
    u, v = tangent_frame(n, ref=rng.normal(size=3))
    hand = 1.0 if rng.random() < 0.5 else -1.0
    t = np.linspace(0, 1, nseg)
    th = rng.uniform(0, 2 * math.pi) + hand * turns * 2 * math.pi * t
    rr = rho * (1.0 - 0.80 * t ** 0.9)
    r0 = rho * tube
    radii = r0 * (1.0 - 0.55 * t ** 1.3)
    radii[-1] *= 0.25
    pts = p[None, :] + n[None, :] * (lift * radii[:, None]) + rr[:, None] * (np.cos(th)[:, None] * u[None, :] + np.sin(th)[:, None] * v[None, :])
    tube_into(bm, pts, radii, nv)


# ---- rope (the brim) --------------------------------------------------------------------------------------------------
def rope_into(bm, path_fn, n_path=360, strands=3, twists=16.0, off=2.4, tube=3.5, core=4.4, nv=10):
    """a rope of `strands` tubes twisted round a core along a closed path_fn(s) -> (point, tangent_hint) with s in [0, 1)"""
    P = np.array([path_fn(i / n_path) for i in range(n_path + 1)])
    T = unit(np.gradient(P, axis=0))
    up = np.array([0, 0, 1.0])
    Nn = unit(np.cross(T, up))               # outwards/inwards of the ring (horizontal normal)
    Bb = np.cross(T, Nn)
    # core
    tube_into_closed(bm, P[:-1], np.full(n_path, core), nv)
    for k in range(strands):
        ph = 2 * math.pi * k / strands
        ang = ph + 2 * math.pi * twists * np.linspace(0, 1, n_path + 1)
        Q = P + off * (np.cos(ang)[:, None] * Nn + np.sin(ang)[:, None] * Bb)
        tube_into_closed(bm, Q[:-1], np.full(n_path, tube), nv)


def tube_into_closed(bm, pts, radii, nv=10):
    """a tube along a CLOSED polyline (torus-like)"""
    pts = np.asarray(pts, float)
    n = len(pts)
    T = unit(np.roll(pts, -1, axis=0) - np.roll(pts, 1, axis=0))
    N = np.zeros_like(pts)
    a = np.array([0, 0, 1.0])
    N[0] = unit(np.cross(T[0], a))
    for i in range(1, n):
        axis = np.cross(T[i - 1], T[i])
        s = np.linalg.norm(axis)
        if s < 1e-9:
            N[i] = N[i - 1]
        else:
            axis = axis / s
            ang = math.atan2(s, float(T[i - 1] @ T[i]))
            k = N[i - 1]
            N[i] = k * math.cos(ang) + np.cross(axis, k) * math.sin(ang) + axis * (axis @ k) * (1 - math.cos(ang))
    B = np.cross(T, N)
    rings = []
    for i in range(n):
        rings.append([bm.verts.new((pts[i] + radii[i] * (math.cos(2 * math.pi * j / nv) * N[i] + math.sin(2 * math.pi * j / nv) * B[i])).tolist()) for j in range(nv)])
    for i in range(n):
        for j in range(nv):
            bm.faces.new([rings[i][j], rings[i][(j + 1) % nv], rings[(i + 1) % n][(j + 1) % nv], rings[(i + 1) % n][j]])


# ---- popcorn ball -----------------------------------------------------------------------------------------------------
def ball_into(bm, c, r, subdiv=2):
    bmesh.ops.create_icosphere(bm, subdivisions=subdiv, radius=float(r), matrix=Matrix.Translation(Vector(tuple(float(v) for v in c))))


def popcorn_into(bm, c, R, r_small, n_small, rng, jitter=0.12):
    """a ball of radius R covered with small lumps (fibonacci sphere)"""
    c = np.asarray(c, float)
    ball_into(bm, c, R * 0.9, 3)
    for d in fibonacci(n_small):
        d = unit(d + rng.normal(scale=jitter, size=3))
        ball_into(bm, c + d * R * 0.86, r_small * rng.uniform(0.85, 1.15), 2)


def fibonacci(n):
    i = np.arange(n) + 0.5
    phi = np.arccos(1 - 2 * i / n)
    th = math.pi * (1 + 5 ** 0.5) * i
    return np.stack([np.cos(th) * np.sin(phi), np.sin(th) * np.sin(phi), np.cos(phi)], 1)


# ---- knit stitches ----------------------------------------------------------------------------------------------------
def knit_arms(center_ref, bvh, dz0, dz1, row_h, col_w, rng, arm=(2.2, 0.75, 0.85), spread=0.95, tilt_deg=32, emb=0.12, max_arms=6000, psi_range=(10, 88), mask=None):
    """V-shaped stitches on the surface hit by rays from center_ref (a spherical grid of rows and columns).
    returns a list of (centre, long axis, normal, semi axes) for the little ellipsoids that make the stitches"""
    C = Vector(center_ref)
    out = []
    R = 52.0                                                 # typical radius: row spacing in degrees
    psis = np.arange(psi_range[0], psi_range[1], math.degrees(row_h / R))
    for ri, psi in enumerate(psis):
        ps = math.radians(psi)
        ncol = max(6, int(round(2 * math.pi * R * math.sin(ps) / col_w)))
        for j in range(ncol):
            ph = 2 * math.pi * (j + 0.5 * (ri % 2)) / ncol
            d = Vector((math.sin(ps) * math.cos(ph), math.sin(ps) * math.sin(ph), math.cos(ps)))
            hit, nrm, _, _ = bvh.ray_cast(C, d, 200.0)
            if hit is None:
                continue
            if mask is not None and not mask(np.array(hit), np.array(nrm)):
                continue
            nrm = np.array(nrm)
            # 'north' = towards the apex of the dome (along -psi): the tangent of the surface that points up
            up = np.array([0, 0, 1.0])
            north = unit(up - (up @ nrm) * nrm)
            east = np.cross(north, nrm)
            for sgn in (-1, 1):
                a = math.radians(tilt_deg) * sgn
                axis = unit(north * math.cos(a) + east * math.sin(a))
                c = np.array(hit) + east * (sgn * spread * col_w * 0.26) + north * (0.18 * row_h) - nrm * emb
                out.append((c, axis, nrm, arm))
            if len(out) >= max_arms:
                return out
    return out


def ellipsoid_into(bm, c, axis, nrm, semi, subdiv=2):
    """an ellipsoid with the long semi axis along `axis`, the short one along the normal"""
    ax = unit(axis)
    nn = unit(nrm - (nrm @ ax) * ax)
    side = np.cross(ax, nn)
    M = np.eye(4)
    M[:3, 0], M[:3, 1], M[:3, 2] = ax * semi[0], side * semi[1], nn * semi[2]
    M[:3, 3] = c
    bmesh.ops.create_icosphere(bm, subdivisions=subdiv, radius=1.0, matrix=Matrix(M.tolist()))


def bvh_of(ob):
    v, t = mesh_np(ob)
    return BVHTree.FromPolygons([tuple(x) for x in v.tolist()], [tuple(x) for x in t.tolist()])
