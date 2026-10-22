"""The sound path of the Santa as closed meshes (Blender bmesh): phone slot, chamber, tunnel, funnel (the bell)."""
import math

import bmesh
import bpy
import numpy as np
from mathutils import Vector

from bl_lib import obj_from_bm


def _loft(rings, cap=True):
    """closed mesh from rings (list of lists of (x, y, z)); all rings have the same number of points, the first and the last are capped"""
    bm = bmesh.new()
    vr = [[bm.verts.new(p) for p in ring] for ring in rings]
    n = len(vr[0])
    for i in range(len(vr) - 1):
        for j in range(n):
            bm.faces.new([vr[i][j], vr[i][(j + 1) % n], vr[i + 1][(j + 1) % n], vr[i + 1][j]])
    if cap:
        bm.faces.new(vr[0][::-1])
        bm.faces.new(vr[-1])
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bmesh.ops.triangulate(bm, faces=bm.faces)
    return bm


def box_bm(x0, x1, y0, y1, z0, z1):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = (x0 if v.co.x < 0 else x1, y0 if v.co.y < 0 else y1, z0 if v.co.z < 0 else z1)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return bm


def teardrop_ring(zc, r, theta_deg, n=40):
    """cross section of the tunnel: a circle with a pointed top whose sides lean theta from the vertical; points (x, z) from the apex clockwise"""
    th = math.radians(theta_deg)
    h = r / math.sin(th)                                   # the apex above the centre of the circle (tangent lines from the apex)
    pts = [(0.0, zc + h)]
    a_r, a_end = th, -math.pi - th                         # right tangent point -> over the bottom -> left tangent point (angles from +x)
    for i in range(n + 1):
        a = a_r + (a_end - a_r) * i / n
        pts.append((r * math.cos(a), zc + r * math.sin(a)))
    return pts


def cavities(P, name="cav"):
    """returns the list of cavity objects: slot, chamber, tunnel, funnel"""
    objs = []
    ys0, ys1 = P["Y_S"] - P["phone_t"] / 2, P["Y_S"] + P["phone_t"] / 2
    objs.append(obj_from_bm(box_bm(-P["phone_w"] / 2, P["phone_w"] / 2, ys0, ys1, P["Z_SF"], P["Z_TOP"] + 60), name + "_slot"))
    objs.append(obj_from_bm(box_bm(-P["CH_W"] / 2, P["CH_W"] / 2, ys0, ys1, P["HZ0"], P["Z_SF"] + 1.0), name + "_chamber"))
    # tunnel: the teardrop section extruded along y from inside the chamber to the throat of the funnel
    pts = teardrop_ring(P["tun_zc"], P["tun_r"], P["tun_theta"])
    y_a, y_b = ys1 - 1.0, P["throat_y"] + 0.6
    rings = [[(x, y_a, z) for x, z in pts], [(x, y_b, z) for x, z in pts]]
    objs.append(obj_from_bm(_loft(rings), name + "_tunnel"))
    # funnel: circles whose centre and radius change linearly from the throat to the mouth (an oblique cone), prolonged 4 mm beyond the mouth
    ns, na = 24, 72
    ys = np.linspace(P["throat_y"], P["mouth_y"] + 4.0, ns)
    rings = []
    for y in ys:
        s = (y - P["throat_y"]) / (P["mouth_y"] - P["throat_y"])
        zc = P["throat_z"] + s * (P["mouth_z"] - P["throat_z"])
        r = P["throat_r"] + s * (P["mouth_R"] - P["throat_r"])
        rings.append([(r * math.cos(2 * math.pi * k / na), float(y), zc + r * math.sin(2 * math.pi * k / na)) for k in range(na)])
    objs.append(obj_from_bm(_loft(rings), name + "_funnel"))
    return objs


def roof_slope_deg(P):
    """slope (from the horizontal) of the top generator of the funnel"""
    top_m, top_t = P["mouth_z"] + P["mouth_R"], P["throat_z"] + P["throat_r"]
    return math.degrees(math.atan2(top_m - top_t, P["mouth_y"] - P["throat_y"]))
