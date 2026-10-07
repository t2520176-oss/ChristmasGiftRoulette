#!/usr/bin/env python3
"""Bark-textured outer pot of the self-watering planter (entry #8).

    python bark_shell.py shell.stl [--render preview.png --pot pot.stl --stick dipstick.stl]
    blender -b -P bark_shell.py -- shell.stl ...

The shell is a watertight cup: a smooth cylindrical cavity (the water reservoir) and an outside with
vertical bark furrows. The relief is made of stretched, ridged value noise sampled on a cylinder, so it
wraps around without a seam, runs long in the vertical direction (furrows) and changes slowly with
height, which keeps every surface within the 45 degree overhang limit. It only ever adds material
(0 .. --relief mm) on top of the smooth wall, so the wall is never thinner than --wall.
The bottom and the top rim stay smooth (the pot hangs from the rim) and a root flare widens the base.
An overflow hole at --overflow-z sets the highest water level; Blender cuts it with a boolean.
"""
import argparse
import math
import os
import sys

import numpy as np


# ---------------------------------------------------------------------------------------------
# noise
# ---------------------------------------------------------------------------------------------
def _hash(ix, iy, iz, seed):
    x = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144665) & 0xFFFFFFFF
    x = ((x ^ (x >> 13)) * 1274126177) & 0xFFFFFFFF
    x = x ^ (x >> 16)
    return (x & 0xFFFF) / 65535.0


def vnoise(x, y, z, seed):
    """3D value noise in [0, 1]."""
    xi, yi, zi = np.floor(x).astype(np.int64), np.floor(y).astype(np.int64), np.floor(z).astype(np.int64)
    fx, fy, fz = x - xi, y - yi, z - zi
    sx, sy, sz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (sx if dx else 1 - sx) * (sy if dy else 1 - sy) * (sz if dz else 1 - sz)
                out = out + w * _hash(xi + dx, yi + dy, zi + dz, seed)
    return out


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def bark_relief(theta, z, r_ref, seed, width=8.0, length=70.0):
    """Relief 0..1: furrows are 0, ridges 1. theta: (1, N), z: (M+1, 1)."""
    u, v = r_ref * np.cos(theta) / width, r_ref * np.sin(theta) / width
    w = z / length
    total = 0.0
    for freq, amp, s in ((1.0, 0.58, 0), (2.3, 0.28, 1), (5.2, 0.14, 2)):
        n = vnoise(u * freq, v * freq, w * freq * 0.7, seed + s)
        total = total + amp * np.abs(2 * n - 1)          # |2n-1| is 0 along the creases: furrows
    total = total ** 0.75
    lo, hi = np.percentile(total, 3), np.percentile(total, 97)
    h = np.clip((total - lo) / (hi - lo), 0, 1)
    return smoothstep(0.0, 0.55, h) ** 1.3             # flat-topped ridges, narrow deep furrows


# ---------------------------------------------------------------------------------------------
# mesh
# ---------------------------------------------------------------------------------------------
def build_shell(ri=47.0, wall=3.2, height=100.0, floor=3.0, relief=2.4, flare=6.0, seed=3, n=480, m=170):
    """Vertices, faces (CCW seen from outside) and the relief value per outer vertex."""
    theta = (2 * np.pi * np.arange(n) / n)[None, :]
    z = (np.arange(m + 1) * height / m)[:, None]
    h01 = bark_relief(theta, z, ri + wall, seed)
    fade = smoothstep(2.0, 10.0, z) * (1 - smoothstep(height - 10.0, height - 4.0, z))
    r_out = ri + wall + flare * np.exp(-z / 12.0) * (1 - smoothstep(0, 40, z) * 0.0)
    radius = r_out + relief * fade * h01
    outer = np.stack([radius * np.cos(theta), radius * np.sin(theta), np.broadcast_to(z, radius.shape)], -1)
    outer = outer.reshape(-1, 3)

    th = theta[0]
    top_in = np.stack([ri * np.cos(th), ri * np.sin(th), np.full(n, height)], -1)
    bot_in = np.stack([ri * np.cos(th), ri * np.sin(th), np.full(n, floor)], -1)
    c_floor = np.array([[0, 0, floor]])
    c_bottom = np.array([[0, 0, 0.0]])
    verts = np.concatenate([outer, top_in, bot_in, c_floor, c_bottom])
    n_out = (m + 1) * n
    i_top, i_bot = n_out, n_out + n
    i_cf, i_cb = n_out + 2 * n, n_out + 2 * n + 1

    idx = lambda j, i: j * n + (i % n)
    f = []
    for j in range(m):                                            # outside, normal outward
        for i in range(n):
            f.append((idx(j, i), idx(j, i + 1), idx(j + 1, i + 1), idx(j + 1, i)))
    for i in range(n):
        f.append((idx(m, i), idx(m, i + 1), i_top + (i + 1) % n, i_top + i))            # rim, normal +z
        f.append((i_bot + i, i_bot + (i + 1) % n, i_top + (i + 1) % n, i_top + i)[::-1])  # cavity wall, normal to axis
        f.append((i_cf, i_bot + i, i_bot + (i + 1) % n))                                 # cavity floor, normal +z
        f.append((i_cb, idx(0, i + 1), idx(0, i)))                                       # bottom, normal -z
    return verts, f, (h01 * fade).reshape(-1)


# ---------------------------------------------------------------------------------------------
# Blender part: boolean for the overflow hole, export, render
# ---------------------------------------------------------------------------------------------
def blender_part(args, verts, faces, hval):
    import bpy
    from mathutils import Vector

    bpy.ops.wm.read_factory_settings(use_empty=True)
    me = bpy.data.meshes.new("shell")
    tri = []
    for f in faces:
        if len(f) == 4:
            tri += [(f[0], f[1], f[2]), (f[0], f[2], f[3])]
        else:
            tri.append(tuple(f))
    me.from_pydata(verts.tolist(), [], tri)
    me.update()
    attr = me.attributes.new("relief", "FLOAT", "POINT")
    vals = np.zeros(len(verts), dtype=np.float32)
    vals[:len(hval)] = hval
    attr.data.foreach_set("value", vals)
    shell = bpy.data.objects.new("shell", me)
    bpy.context.scene.collection.objects.link(shell)

    # overflow hole through the front wall (-y): a horizontal cylinder
    ri, wall = args.ri, args.wall
    length = 24.0
    bpy.ops.mesh.primitive_cylinder_add(vertices=48, radius=args.overflow_d / 2, depth=length,
                                        location=(0, -(ri - 6 + length / 2), args.overflow_z),
                                        rotation=(math.radians(90), 0, 0))
    cutter = bpy.context.object
    mod = shell.modifiers.new("hole", "BOOLEAN")
    mod.operation = "DIFFERENCE"
    mod.object = cutter
    mod.solver = "MANIFOLD"
    bpy.context.view_layer.objects.active = shell
    bpy.ops.object.modifier_apply(modifier=mod.name)
    bpy.data.objects.remove(cutter)
    for p in shell.data.polygons:
        p.use_smooth = True

    bpy.ops.object.select_all(action="DESELECT")
    shell.select_set(True)
    bpy.ops.wm.stl_export(filepath=args.out, export_selected_objects=True)
    print(f"wrote {args.out}: {len(shell.data.polygons)} faces")

    if args.render:
        render(args, shell)


def material(name, rgb, rough=0.5):
    import bpy
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*rgb, 1)
    b.inputs["Roughness"].default_value = rough
    return m


def bark_material():
    import bpy
    m = bpy.data.materials.new("bark")
    m.use_nodes = True
    nt = m.node_tree
    b = nt.nodes["Principled BSDF"]
    b.inputs["Roughness"].default_value = 0.85
    attr = nt.nodes.new("ShaderNodeAttribute")
    attr.attribute_name = "relief"
    ramp = nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].position = 0.15
    ramp.color_ramp.elements[0].color = (0.035, 0.022, 0.015, 1)     # dark furrows
    ramp.color_ramp.elements[1].position = 0.95
    ramp.color_ramp.elements[1].color = (0.30, 0.19, 0.11, 1)        # lighter ridges
    nt.links.new(attr.outputs["Fac"], ramp.inputs["Fac"])
    nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    return m


def render(args, shell):
    import bpy
    from mathutils import Vector
    scene = bpy.context.scene
    shell.data.materials.clear()          # the boolean leaves an empty slot 0
    shell.data.materials.append(bark_material())
    objs = [shell]

    def load(path, mat, dz=0.0):
        if not path:
            return None
        before = set(bpy.data.objects)
        bpy.ops.wm.stl_import(filepath=path)
        o = [x for x in bpy.data.objects if x not in before][0]
        o.location.z += dz
        o.data.materials.append(mat)
        objs.append(o)
        return o

    load(args.pot, material("pot", (0.78, 0.30, 0.10), 0.55), args.pot_z)
    load(args.stick, material("stick", (0.20, 0.45, 0.18), 0.5), 0.0)
    if args.soil_z:   # a disc of soil in the pot
        bpy.ops.mesh.primitive_cylinder_add(vertices=64, radius=args.soil_r, depth=2, location=(0, 0, args.soil_z))
        soil = bpy.context.object
        soil.data.materials.append(material("soil", (0.06, 0.035, 0.02), 0.95))
        objs.append(soil)

    pts = [o.matrix_world @ Vector(c) for o in objs for c in o.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    centre, ext = (lo + hi) / 2, max(hi - lo)
    bpy.ops.mesh.primitive_plane_add(size=2000, location=(centre.x, centre.y, lo.z))
    bpy.context.object.data.materials.append(material("floor", (0.90, 0.88, 0.84), 0.9))
    scene.world = bpy.data.worlds.new("w")
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (0.95, 0.94, 0.92, 1)
    bg.inputs["Strength"].default_value = 0.7
    for loc, e in (((-160, -180, 190), 45000), ((200, 80, 90), 14000)):
        d = bpy.data.lights.new("a", "AREA")
        d.energy, d.size = e, 160
        o = bpy.data.objects.new("a", d)
        o.location = centre + Vector(loc)
        scene.collection.objects.link(o)
        o.rotation_euler = (-Vector(loc)).to_track_quat("-Z", "Y").to_euler()
    az, el, dist = math.radians(args.azimuth), math.radians(args.elev), ext * 3.1
    cd = bpy.data.cameras.new("c")
    cd.lens, cd.clip_start, cd.clip_end = 70, 1, dist * 4
    cam = bpy.data.objects.new("c", cd)
    cam.location = (centre.x + dist * math.sin(az) * math.cos(el), centre.y - dist * math.cos(az) * math.cos(el),
                    centre.z + dist * math.sin(el))
    scene.collection.objects.link(cam)
    cam.rotation_euler = (centre - cam.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    scene.view_settings.view_transform = "Standard"
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = args.samples
    scene.cycles.use_denoising = False
    scene.render.resolution_x, scene.render.resolution_y = 900, 1100
    scene.render.filepath = args.render
    bpy.ops.render.render(write_still=True)
    print("wrote", args.render)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("out")
    ap.add_argument("--ri", type=float, default=47.0, help="inner radius of the reservoir (mm)")
    ap.add_argument("--wall", type=float, default=3.2, help="smooth wall thickness (mm)")
    ap.add_argument("--height", type=float, default=100.0)
    ap.add_argument("--floor", type=float, default=3.0)
    ap.add_argument("--relief", type=float, default=3.0, help="bark relief on top of the wall (mm)")
    ap.add_argument("--flare", type=float, default=6.0, help="root flare at the base (mm)")
    ap.add_argument("--seed", type=int, default=3)
    ap.add_argument("--n", type=int, default=480)
    ap.add_argument("--m", type=int, default=170)
    ap.add_argument("--overflow-z", type=float, default=32.5, help="height of the overflow hole centre (mm)")
    ap.add_argument("--overflow-d", type=float, default=5.0)
    ap.add_argument("--render")
    ap.add_argument("--pot")
    ap.add_argument("--pot-z", type=float, default=38.0, help="height of the pot floor above the shell bottom")
    ap.add_argument("--stick")
    ap.add_argument("--soil-z", type=float, default=0.0)
    ap.add_argument("--soil-r", type=float, default=38.0)
    ap.add_argument("--azimuth", type=float, default=30.0)
    ap.add_argument("--elev", type=float, default=14.0, help="camera elevation (degrees)")
    ap.add_argument("--samples", type=int, default=64)
    args = ap.parse_args(argv)
    verts, faces, hval = build_shell(args.ri, args.wall, args.height, args.floor, args.relief, args.flare,
                                     args.seed, args.n, args.m)
    blender_part(args, verts, faces, hval)


if __name__ == "__main__":
    main()
