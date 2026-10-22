"""Helpers for the headless Blender build of the Santa sound amplifier (Blender 4.x).

Units: 1 Blender unit = 1 mm (STL export without scaling).  x = right, y = front (the beard), z = up, the floor is z = 0.
"""
import math
import os
import sys

SITE = os.environ.get("BPY_SITE")
if SITE:
    sys.path.insert(0, SITE)

import bmesh  # noqa: E402
import bpy  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Euler, Matrix, Quaternion, Vector  # noqa: E402

# ---- metaballs ------------------------------------------------------------------------------------------------------
MB_STIFF = 2.0
MB_THR = 0.6
MB_K = math.sqrt(1 - (MB_THR / MB_STIFF) ** (1 / 3))      # a lone element of "radius R" shows a surface at R * K (measured with Blender 4.0)


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def _select_only(ob):
    for o in bpy.context.scene.objects:
        o.select_set(False)
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob


class Blobs:
    """A group of metaball elements = a smooth union ("clay").  semi-axes are the half sizes of the element on its own."""

    def __init__(self, name, res=0.8):
        self.name = name
        self.mb = bpy.data.metaballs.new(name)
        self.ob = bpy.data.objects.new(name, self.mb)
        bpy.context.scene.collection.objects.link(self.ob)
        self.mb.resolution = res
        self.mb.render_resolution = res
        self.mb.threshold = MB_THR
        self.mb.update_method = "UPDATE_ALWAYS"
        self.n = 0

    def ell(self, c, semi, rot=(0, 0, 0), stiff=MB_STIFF, neg=False):
        """ellipsoid with semi axes (a, b, c) mm, centre c, rotation in degrees (XYZ euler)"""
        e = self.mb.elements.new(type="ELLIPSOID")
        e.co = tuple(c)
        R0 = 10.0
        e.radius = R0
        e.size_x, e.size_y, e.size_z = [s / (R0 * MB_K) for s in semi]
        e.stiffness = stiff
        e.use_negative = neg
        e.rotation = Euler([math.radians(a) for a in rot], "XYZ").to_quaternion()
        self.n += 1
        return e

    def ball(self, c, r, stiff=MB_STIFF, neg=False):
        e = self.mb.elements.new(type="BALL")
        e.co = tuple(c)
        e.radius = r / MB_K
        e.stiffness = stiff
        e.use_negative = neg
        self.n += 1
        return e

    def mesh(self):
        _select_only(self.ob)
        bpy.ops.object.convert(target="MESH")
        return bpy.context.view_layer.objects.active


# ---- plain meshes ----------------------------------------------------------------------------------------------------
def obj_from_bm(bm, name):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def ico_bm(c, semi, rot=(0, 0, 0), subdiv=3):
    bm = bmesh.new()
    bmesh.ops.create_icosphere(bm, subdivisions=subdiv, radius=1.0)
    M = Matrix.Translation(Vector(c)) @ Euler([math.radians(a) for a in rot], "XYZ").to_matrix().to_4x4() @ Matrix.Diagonal(Vector((*semi, 1.0)))
    bmesh.ops.transform(bm, matrix=M, verts=bm.verts)
    return bm


def torus_bm(c, R, r, axis=(0, 0, 1), nu=96, nv=24):
    """torus with the major radius R, the minor radius r around the centre c; its axis points along `axis`"""
    bm = bmesh.new()
    verts = []
    for i in range(nu):
        a = 2 * math.pi * i / nu
        ring = []
        for j in range(nv):
            b = 2 * math.pi * j / nv
            ring.append(bm.verts.new(((R + r * math.cos(b)) * math.cos(a), (R + r * math.cos(b)) * math.sin(a), r * math.sin(b))))
        verts.append(ring)
    for i in range(nu):
        for j in range(nv):
            bm.faces.new([verts[i][j], verts[(i + 1) % nu][j], verts[(i + 1) % nu][(j + 1) % nv], verts[i][(j + 1) % nv]])
    ax = Vector(axis).normalized()
    q = Vector((0, 0, 1)).rotation_difference(ax)
    bmesh.ops.transform(bm, matrix=Matrix.Translation(Vector(c)) @ q.to_matrix().to_4x4(), verts=bm.verts)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return bm


def mesh_np(ob):
    """(vertices (n,3), triangles (m,3)) of an object's mesh as numpy arrays"""
    me = ob.data
    me.calc_loop_triangles()
    v = np.empty(len(me.vertices) * 3)
    me.vertices.foreach_get("co", v)
    t = np.empty(len(me.loop_triangles) * 3, dtype=np.int32)
    me.loop_triangles.foreach_get("vertices", t)
    return v.reshape(-1, 3), t.reshape(-1, 3)


def obj_from_np(v, f, name):
    me = bpy.data.meshes.new(name)
    me.from_pydata(v.tolist(), [], f.tolist())
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def join(objs, name):
    """one mesh from many objects (world positions kept)"""
    bm = bmesh.new()
    for o in objs:
        me = o.data
        tmp = bmesh.new()
        tmp.from_mesh(me)
        bmesh.ops.transform(tmp, matrix=o.matrix_world, verts=tmp.verts)
        t = bpy.data.meshes.new("t")
        tmp.to_mesh(t)
        tmp.free()
        bm.from_mesh(t)
        bpy.data.meshes.remove(t)
    ob = obj_from_bm(bm, name)
    bm.free()
    return ob


def remesh(ob, voxel, smooth=0, smooth_fac=0.5):
    """voxel remesh = union of every overlapping shell of the object, then optional Laplacian-ish smoothing"""
    _select_only(ob)
    m = ob.modifiers.new("remesh", "REMESH")
    m.mode = "VOXEL"
    m.voxel_size = voxel
    m.adaptivity = 0.0
    m.use_smooth_shade = True
    bpy.ops.object.modifier_apply(modifier=m.name)
    if smooth:
        s = ob.modifiers.new("smooth", "SMOOTH")
        s.factor = smooth_fac
        s.iterations = smooth
        bpy.ops.object.modifier_apply(modifier=s.name)
    return ob


def export_stl(ob, path):
    _select_only(ob)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bpy.ops.export_mesh.stl(filepath=path, use_selection=True, global_scale=1.0, use_mesh_modifiers=True, ascii=False)


def remove(*obs):
    for o in obs:
        if o and o.name in bpy.data.objects:
            bpy.data.objects.remove(o)


def stats(ob):
    v, t = mesh_np(ob)
    return "%s: %d verts %d tris  bbox %s .. %s" % (ob.name, len(v), len(t), np.round(v.min(0), 1), np.round(v.max(0), 1))
