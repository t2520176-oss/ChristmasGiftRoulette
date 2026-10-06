#!/usr/bin/env python3
"""Join OpenSCAD parts into one printable STL with Blender's boolean union.

OpenSCAD 2021.01 (CGAL backend) takes many minutes to union two dense wings
with the hub, so export the parts separately and let Blender's Manifold solver
do it in seconds:

    openscad -D 'part="hub"'   -o hub.stl   samara_twister.scad
    openscad -D 'part="blade"' -o blade.stl samara_twister.scad
    blender -b -P blender_assemble.py -- hub.stl blade.stl 2 spinner.stl

The same file also runs with the `bpy` pip module:
    python blender_assemble.py hub.stl blade.stl 2 spinner.stl

Arguments: hub.stl blade.stl <number of wings> <output.stl>
The wing count must equal `blades` in the .scad file.
"""
import math
import sys

import bpy


def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def import_stl(path):
    before = set(bpy.data.objects)
    bpy.ops.wm.stl_import(filepath=path)
    new = [o for o in bpy.data.objects if o not in before]
    return new[0]


def main(hub_path, blade_path, count, out_path):
    clear_scene()
    hub = import_stl(hub_path)
    blade = import_stl(blade_path)

    # make one rotated copy of the wing per blade (OpenSCAD: rotate z by 360/n * k)
    wings = []
    for k in range(count):
        w = blade.copy()
        w.data = blade.data.copy()
        bpy.context.collection.objects.link(w)
        w.rotation_euler[2] = math.radians(360.0 / count * k)
        wings.append(w)
    bpy.data.objects.remove(blade)

    bpy.context.view_layer.objects.active = hub
    for w in wings:
        mod = hub.modifiers.new(name="union", type='BOOLEAN')
        mod.operation = 'UNION'
        mod.operand_type = 'OBJECT'
        mod.object = w
        mod.solver = 'MANIFOLD'
        bpy.ops.object.modifier_apply(modifier=mod.name)

    for w in wings:
        bpy.data.objects.remove(w)

    bpy.ops.object.select_all(action='DESELECT')
    hub.select_set(True)
    bpy.ops.wm.stl_export(filepath=out_path, export_selected_objects=True)
    print(f"wrote {out_path}: {len(hub.data.polygons)} faces")


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    if len(argv) != 4:
        sys.exit(__doc__)
    main(argv[0], argv[1], int(argv[2]), argv[3])
