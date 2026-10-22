"""Quick clay preview of the Santa parts (Blender / Cycles, headless).

blender -b -P preview.py -- <stl_dir> <out.png> [view] [samples] [WxH]

Every STL in <stl_dir> is named  <role>__<part>.stl  (role = red|white|peach|black|gold|phone);
view = front34 | front | side | back | top | cut
"""
import math
import os
import sys

import bpy
from mathutils import Vector

argv = sys.argv[sys.argv.index("--") + 1:]
sdir, out = argv[0], argv[1]
view = argv[2] if len(argv) > 2 else "front34"
samples = int(argv[3]) if len(argv) > 3 else 24
res = [int(v) for v in argv[4].split("x")] if len(argv) > 4 else [900, 700]

COL = {"red": "#C8102E", "white": "#F3EFE6", "peach": "#F2B79A", "black": "#1A1A1E", "gold": "#E0B030", "phone": "#222833", "cav": "#3A78FF"}


def lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def rgb(h):
    h = h.lstrip("#")
    return tuple(lin(int(h[i:i + 2], 16)) for i in (0, 2, 4))


bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = "CYCLES"
sc.cycles.samples = samples
sc.cycles.use_denoising = False
sc.render.resolution_x, sc.render.resolution_y = res
sc.render.film_transparent = False
sc.view_settings.view_transform = "Filmic" if "Filmic" in bpy.types.ColorManagedViewSettings.bl_rna.properties["view_transform"].enum_items.keys() else "Standard"

for f in sorted(os.listdir(sdir)):
    if not f.endswith(".stl"):
        continue
    _p = f[:-4].split("__")
    role = _p[1] if _p[0] == "santa" and len(_p) > 1 else _p[0]
    if role not in COL:
        continue
    bpy.ops.wm.stl_import(filepath=os.path.join(sdir, f))
    ob = bpy.context.selected_objects[0]
    bpy.context.view_layer.objects.active = ob
    bpy.ops.object.shade_smooth()
    m = bpy.data.materials.new(f)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*rgb(COL[role]), 1)
    b.inputs["Roughness"].default_value = 0.55
    if role == "white" or role == "peach":
        b.inputs["Subsurface Weight"].default_value = 0.15
    ob.data.materials.append(m)

# floor + world
bpy.ops.mesh.primitive_plane_add(size=1200, location=(0, 0, -0.01))
fl = bpy.context.object
fm = bpy.data.materials.new("floor")
fm.use_nodes = True
fm.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*rgb("#8A5C38"), 1)
fm.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value = 0.6
fl.data.materials.append(fm)
w = bpy.data.worlds.new("w")
sc.world = w
w.use_nodes = True
w.node_tree.nodes["Background"].inputs["Color"].default_value = (*rgb("#6A4A38"), 1)
w.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.9


def light(loc, target, power, size=120, color=(1, 1, 1)):
    d = bpy.data.lights.new("l", "AREA")
    d.energy = power
    d.size = size
    d.color = color
    o = bpy.data.objects.new("l", d)
    sc.collection.objects.link(o)
    o.location = loc
    dirv = Vector(target) - Vector(loc)
    o.rotation_euler = dirv.to_track_quat("-Z", "Y").to_euler()


# the model faces +y.  cameras:
views = {
    "front34": ((190, 330, 150), (0, 8, 40), 60),
    "front": ((0, 420, 60), (0, 0, 42), 62),
    "side": ((420, 0, 60), (0, 0, 42), 62),
    "back": ((-110, -380, 110), (0, 0, 42), 60),
    "top": ((0, 80, 420), (0, 0, 30), 55),
    "low": ((110, 330, 40), (0, 0, 40), 60),
}
loc, tgt, lens = views.get(view, views["front34"])
cam = bpy.data.cameras.new("c")
cam.lens = lens
cam.sensor_width = 36
co = bpy.data.objects.new("c", cam)
sc.collection.objects.link(co)
co.location = loc
co.rotation_euler = (Vector(tgt) - Vector(loc)).to_track_quat("-Z", "Y").to_euler()
sc.camera = co
light((250, 300, 380), (0, 0, 40), 2.4e6, 220, (1.0, 0.92, 0.82))
light((-300, 260, 250), (0, 0, 50), 1.1e6, 200, (0.8, 0.85, 1.0))
light((0, -300, 200), (0, 0, 50), 0.8e6, 200)
sc.render.filepath = out
bpy.ops.render.render(write_still=True)
print("RENDERED", out)
