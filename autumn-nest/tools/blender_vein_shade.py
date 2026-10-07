#!/usr/bin/env python3
"""Build the lithophane shade mesh from a thickness map (vein_field.py) and render it lit and unlit.

    python blender_vein_shade.py field.npz shade.stl [--lit lit.png] [--unlit unlit.png]
                                 [--base base.stl --base-h 12] [--samples 96]
    blender -b -P blender_vein_shade.py -- field.npz shade.stl --lit lit.png

The shade is a surface of revolution (profile from vein_field.profile). Its outer surface is smooth;
the inner surface is pushed inward along the surface normal by the wall thickness T(u, v), so the veins
are where the wall is thin. The renders show the lit lamp with the emission of every point taken from
the simple LED model in lamp_light.py (a flat round module, 60 mm like the Bambu Lab LED Lamp Kit, lit face at
34 mm; Beer-Lambert transmission with an ASSUMED absorption of white PLA, plus a little diffuse scatter):
a preview, not a measurement.
The unlit shade is plain white plastic.
"""
import argparse
import math
import os
import sys

import bpy
import numpy as np
from mathutils import Vector

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vein_field import profile  # noqa: E402
from lamp_light import brightness  # noqa: E402


def build_arrays(f, led_z=34.0, occluder_r=0.0, scatter=0.10, uniform=False, disc_r=30.0):
    u, v, t = f["u"], f["v"], f["t"]
    h, r0, rmax, rtop = float(f["height"]), float(f["r_bottom"]), float(f["r_max"]), float(f["r_top"])
    nv1, nu = t.shape
    theta = 2 * np.pi * np.arange(nu) / nu
    r, dr = profile(v, h, r0, rmax, rtop)
    c, s = np.cos(theta)[None, :], np.sin(theta)[None, :]
    outer = np.stack([r[:, None] * c, r[:, None] * s, np.repeat(v[:, None], nu, 1)], -1)   # (nv1, nu, 3)
    n = np.stack([np.broadcast_to(c, (nv1, nu)), np.broadcast_to(s, (nv1, nu)),
                  -np.repeat(dr[:, None], nu, 1)], -1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True)
    inner = outer - t[..., None] * n
    verts = np.concatenate([outer.reshape(-1, 3), inner.reshape(-1, 3)])
    nvert = nv1 * nu
    idx = lambda j, i: j * nu + (i % nu)
    faces = []
    for j in range(nv1 - 1):
        for i in range(nu):
            a, b, c2, d = idx(j, i), idx(j, i + 1), idx(j + 1, i + 1), idx(j + 1, i)
            faces.append((a, b, c2, d))                                   # outer, normal outward
            faces.append((nvert + d, nvert + c2, nvert + b, nvert + a))   # inner, normal toward the axis
    for i in range(nu):                                                   # bottom rim (normal -z)
        faces.append((nvert + idx(0, i), nvert + idx(0, i + 1), idx(0, i + 1), idx(0, i)))
    top = nv1 - 1
    for i in range(nu):                                                   # top rim (normal +z)
        faces.append((idx(top, i), idx(top, i + 1), nvert + idx(top, i + 1), nvert + idx(top, i)))
    if uniform:
        glow = np.exp(-float(f["alpha"]) * t)
    else:
        b = brightness(f, led_z, occluder_r=occluder_r, up_only=disc_r <= 0, disc_r=disc_r)
        b = b + scatter * b.mean()                          # light scattered inside the shade reaches the dark part
        glow = b / np.median(b[(t <= 1.0 + 1e-6) & (b > 0)])   # veins about 1.0
    glow = glow.reshape(-1)
    return verts, faces, np.concatenate([glow, glow]), t


def make_mesh(verts, faces, glow, name="shade"):
    me = bpy.data.meshes.new(name)
    me.from_pydata(verts.tolist(), [], faces)
    me.update()
    a = me.attributes.new("glow", "FLOAT", "POINT")
    a.data.foreach_set("value", glow.astype(np.float32))
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    for p in me.polygons:
        p.use_smooth = True
    return ob


def lit_material(strength=3.0):
    m = bpy.data.materials.new("lit")
    m.use_nodes = True
    nt = m.node_tree
    nt.nodes.clear()
    attr = nt.nodes.new("ShaderNodeAttribute")
    attr.attribute_name = "glow"
    mul = nt.nodes.new("ShaderNodeMath")
    mul.operation = "MULTIPLY"
    mul.inputs[1].default_value = strength
    emi = nt.nodes.new("ShaderNodeEmission")
    emi.inputs["Color"].default_value = (1.0, 0.76, 0.42, 1)      # warm white LED, about 2700 K
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    nt.links.new(attr.outputs["Fac"], mul.inputs[0])
    nt.links.new(mul.outputs[0], emi.inputs["Strength"])
    nt.links.new(emi.outputs[0], out.inputs[0])
    return m


def plain_material(name, rgb, rough=0.5):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*rgb, 1)
    b.inputs["Roughness"].default_value = rough
    return m


def render(shade_ob, base_path, base_h, out, lit, samples):
    scene = bpy.context.scene
    shade_ob.location.z = base_h
    objs = [shade_ob]
    if base_path:
        before = set(bpy.data.objects)
        bpy.ops.wm.stl_import(filepath=base_path)
        base = [o for o in bpy.data.objects if o not in before][0]
        base.data.materials.append(plain_material("base", (0.16, 0.08, 0.04) if lit else (0.35, 0.2, 0.1)))
        objs.append(base)
    shade_ob.data.materials.clear()
    shade_ob.data.materials.append(lit_material() if lit else plain_material("pla", (0.93, 0.9, 0.84), 0.55))

    pts = [o.matrix_world @ Vector(c) for o in objs for c in o.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    centre = (lo + hi) / 2
    ext = max(hi - lo)

    bpy.ops.mesh.primitive_plane_add(size=2000, location=(centre.x, centre.y, lo.z))
    bpy.context.object.data.materials.append(
        plain_material("floor", (0.03, 0.025, 0.02) if lit else (0.9, 0.88, 0.84), 0.8))
    scene.world = bpy.data.worlds.new("w")
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (0.012, 0.012, 0.018, 1) if lit else (0.95, 0.94, 0.92, 1)
    bg.inputs["Strength"].default_value = 1.0 if lit else 0.6

    if not lit:
        for loc, e in (((-160, -180, 190), 45000), ((200, 80, 90), 14000)):
            d = bpy.data.lights.new("a", "AREA")
            d.energy, d.size = e, 160
            o = bpy.data.objects.new("a", d)
            o.location = centre + Vector(loc)
            scene.collection.objects.link(o)
            o.rotation_euler = (-Vector(loc)).to_track_quat("-Z", "Y").to_euler()

    az, el, dist = math.radians(30), math.radians(9), ext * 3.1
    cam_d = bpy.data.cameras.new("c")
    cam_d.lens, cam_d.clip_start, cam_d.clip_end = 70, 1, dist * 4
    cam = bpy.data.objects.new("c", cam_d)
    cam.location = (centre.x + dist * math.sin(az) * math.cos(el), centre.y - dist * math.cos(az) * math.cos(el),
                    centre.z + dist * math.sin(el))
    scene.collection.objects.link(cam)
    cam.rotation_euler = (centre - cam.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    scene.view_settings.view_transform = "Standard"
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = samples
    scene.cycles.use_denoising = False
    scene.cycles.max_bounces = 4
    scene.render.resolution_x, scene.render.resolution_y = 900, 1100
    scene.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print("wrote", out)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    ap = argparse.ArgumentParser()
    ap.add_argument("field")
    ap.add_argument("stl")
    ap.add_argument("--lit")
    ap.add_argument("--unlit")
    ap.add_argument("--base")
    ap.add_argument("--base-h", type=float, default=12.0)
    ap.add_argument("--samples", type=int, default=96)
    ap.add_argument("--led-z", type=float, default=34.0, help="height of the lit face above the shade bottom (mm)")
    ap.add_argument("--disc-r", type=float, default=30.0, help="radius of the round LED puck (mm); 0 = point LED")
    ap.add_argument("--occluder-r", type=float, default=0.0, help="point LED only: radius of the post that shades the wall below")
    ap.add_argument("--uniform", action="store_true", help="old preview: every point lit the same way")
    a = ap.parse_args(argv)

    bpy.ops.wm.read_factory_settings(use_empty=True)
    f = np.load(a.field)
    verts, faces, glow, t = build_arrays(f, a.led_z, a.occluder_r, uniform=a.uniform, disc_r=a.disc_r)
    ob = make_mesh(verts, faces, glow)
    bpy.ops.object.select_all(action="DESELECT")
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob
    bpy.ops.wm.stl_export(filepath=a.stl, export_selected_objects=True)
    print(f"wrote {a.stl}: {len(verts)} vertices, {len(faces)} faces, wall {t.min():.2f}..{t.max():.2f} mm")
    for out, lit in ((a.lit, True), (a.unlit, False)):
        if out:
            # fresh scene for each render, keeping the mesh
            bpy.ops.wm.read_factory_settings(use_empty=True)
            ob = make_mesh(verts, faces, glow)
            render(ob, a.base, a.base_h, out, lit, a.samples)


if __name__ == "__main__":
    main()
