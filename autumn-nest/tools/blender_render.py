#!/usr/bin/env python3
"""Studio render of STL files with Blender Cycles (CPU, no GPU needed).

    blender -b -P blender_render.py -- INPUT preview.png [azimuth_deg] [samples] [floor|nofloor] [elevation_deg]
    python blender_render.py INPUT preview.png

INPUT is either one STL (it is split into loose parts: the biggest gets the
autumn-orange material, the others dark wood) or several "file.stl:material"
entries joined by commas, e.g. "canopy.stl:orange,tubes.stl:metal,cords.stl:cord".
Materials: orange, wood, metal, red, cream, cord.
"""
import math
import sys

import bpy
from mathutils import Vector


def import_stl(path):
    """Import an STL and return the new object (selection is not reliable across imports)."""
    before = set(bpy.data.objects)
    bpy.ops.wm.stl_import(filepath=path)
    return [o for o in bpy.data.objects if o not in before][0]


def material(name, rgb, rough=0.45, metallic=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (*rgb, 1)
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = metallic
    return m


MATERIALS = {
    "orange": ((0.80, 0.26, 0.04), 0.45, 0.0),
    "wood": ((0.18, 0.08, 0.03), 0.5, 0.0),
    "metal": ((0.80, 0.80, 0.82), 0.28, 1.0),
    "red": ((0.55, 0.08, 0.03), 0.5, 0.0),
    "cream": ((0.90, 0.84, 0.68), 0.55, 0.0),
    "cord": ((0.05, 0.05, 0.05), 0.8, 0.0),
}


def main(inputs, out, azimuth=35.0, samples=48, floor=True, elevation=None):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene

    parts = []
    if ":" in inputs.replace("\\", "/").split("/")[-1] or "," in inputs:
        for item in inputs.split(","):
            path, mat = item.rsplit(":", 1)
            o = import_stl(path)
            o.data.materials.append(bpy.data.materials.get(mat) or material(mat, *MATERIALS[mat]))
            parts.append(o)
    else:
        obj = import_stl(inputs)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.mode_set(mode='EDIT')
        bpy.ops.mesh.separate(type='LOOSE')
        bpy.ops.object.mode_set(mode='OBJECT')
        parts = [o for o in scene.objects if o.type == 'MESH']
        parts.sort(key=lambda o: -len(o.data.polygons))
        orange = material("rotor", *MATERIALS["orange"])
        wood = material("shaft", *MATERIALS["wood"])
        for i, o in enumerate(parts):
            o.data.materials.append(orange if i == 0 else wood)
    for o in parts:
        for p in o.data.polygons:
            p.use_smooth = False

    # frame the model
    pts = [o.matrix_world @ Vector(c) for o in parts for c in o.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    centre = (lo + hi) / 2
    height = hi.z - lo.z

    # floor (optional) and soft light-grey world
    if floor:
        bpy.ops.mesh.primitive_plane_add(size=1000, location=(centre.x, centre.y, lo.z))
        bpy.context.object.data.materials.append(material("floor", (0.92, 0.9, 0.86), 0.9))
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
    extent = max(hi.x - lo.x, hi.y - lo.y, hi.z - lo.z)   # flat parts are wide, not tall
    dist = extent * 3.1
    el = math.radians(elevation) if elevation is not None else math.atan2(extent * 0.30, dist)
    cam_data = bpy.data.cameras.new("cam")
    cam_data.lens = 70
    cam_data.clip_start = 1
    cam_data.clip_end = dist * 4   # tall models sit far from the camera
    cam = bpy.data.objects.new("cam", cam_data)
    cam.location = (centre.x + dist * math.sin(az) * math.cos(el), centre.y - dist * math.cos(az) * math.cos(el),
                    centre.z + dist * math.sin(el))
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
    nums = [float(a) for a in argv[2:4]]
    main(argv[0], argv[1], *nums, floor=(len(argv) < 5 or argv[4] != "nofloor"),
         elevation=float(argv[5]) if len(argv) > 5 else None)
