#!/usr/bin/env python3
"""Studio render of a spinner STL with Blender Cycles (CPU, no GPU needed).

    blender -b -P blender_render.py -- spinner.stl preview.png [azimuth_deg] [samples]
    python blender_render.py spinner.stl preview.png

The model is split into its loose parts: the biggest one (the rotor) gets the
autumn-orange material, the others (the shaft) get dark wood.
"""
import math
import sys

import bpy
from mathutils import Vector


def material(name, rgb, rough=0.45):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (*rgb, 1)
    bsdf.inputs["Roughness"].default_value = rough
    return m


def main(stl, out, azimuth=35.0, samples=48):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene

    bpy.ops.wm.stl_import(filepath=stl)
    obj = bpy.context.selected_objects[0]
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.separate(type='LOOSE')
    bpy.ops.object.mode_set(mode='OBJECT')
    parts = [o for o in scene.objects if o.type == 'MESH']
    parts.sort(key=lambda o: -len(o.data.polygons))
    orange = material("rotor", (0.80, 0.26, 0.04))
    wood = material("shaft", (0.18, 0.08, 0.03))
    for i, o in enumerate(parts):
        o.data.materials.append(orange if i == 0 else wood)
        bpy.context.view_layer.objects.active = o
        for p in o.data.polygons:
            p.use_smooth = False

    # frame the model
    pts = [o.matrix_world @ Vector(c) for o in parts for c in o.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    centre = (lo + hi) / 2
    height = hi.z - lo.z

    # floor and soft light-grey world
    bpy.ops.mesh.primitive_plane_add(size=1000, location=(centre.x, centre.y, lo.z))
    floor = bpy.context.object
    floor.data.materials.append(material("floor", (0.92, 0.9, 0.86), 0.9))
    scene.world = bpy.data.worlds.new("w")
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (0.95, 0.94, 0.92, 1)
    bg.inputs["Strength"].default_value = 0.6

    def add_light(kind, loc, energy, size):
        data = bpy.data.lights.new(kind, kind)
        data.energy = energy
        if kind == 'AREA':
            data.size = size
        o = bpy.data.objects.new(kind, data)
        o.location = loc
        scene.collection.objects.link(o)
        d = centre - Vector(loc)
        o.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()

    add_light('AREA', (centre.x - 160, centre.y - 180, centre.z + 190), 45000, 160)
    add_light('AREA', (centre.x + 200, centre.y + 80, centre.z + 90), 14000, 120)

    az = math.radians(azimuth)
    dist = height * 3.1
    cam_data = bpy.data.cameras.new("cam")
    cam_data.lens = 70
    cam = bpy.data.objects.new("cam", cam_data)
    cam.location = (centre.x + dist * math.sin(az), centre.y - dist * math.cos(az),
                    centre.z + height * 0.30)
    scene.collection.objects.link(cam)
    cam.rotation_euler = (centre - cam.location).to_track_quat('-Z', 'Y').to_euler()
    scene.camera = cam

    scene.view_settings.view_transform = 'Standard'
    scene.render.engine = 'CYCLES'
    scene.cycles.device = 'CPU'
    scene.cycles.samples = int(samples)
    scene.cycles.use_denoising = False
    scene.render.resolution_x = 900
    scene.render.resolution_y = 1100
    scene.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print("wrote", out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    if len(argv) < 2:
        sys.exit(__doc__)
    main(argv[0], argv[1], *(float(a) for a in argv[2:4]))
