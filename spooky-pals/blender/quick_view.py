"""Fast design-review render (Workbench engine) of one or more STL files.

blender -b -P quick_view.py -- out.png  a.stl[@x,y,z[:#RRGGBB]]  b.stl ...  [--views front,three,back,top]
Each STL may carry an offset and a colour:  name.stl@10,0,0:#d9a25b
"""
import math
import sys

import bpy
from mathutils import Vector

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
out = argv[0]
items = [a for a in argv[1:] if not a.startswith("--")]
views = ["three"]
res = (1400, 1000)
for i, a in enumerate(argv):
    if a == "--views":
        views = argv[i + 1].split(",")
    if a == "--res":
        w, h = argv[i + 1].split("x")
        res = (int(w), int(h))

bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = "BLENDER_WORKBENCH"
sc.display.shading.light = "STUDIO"
sc.display.shading.color_type = "MATERIAL"
sc.display.shading.show_cavity = True
sc.display.shading.cavity_type = "BOTH"
sc.display.shading.show_shadows = True
sc.display.shading.shadow_intensity = 0.35
sc.display.shading.studio_light = "studio.sl"
sc.render.resolution_x, sc.render.resolution_y = res
sc.render.film_transparent = False
sc.world = bpy.data.worlds.new("w")
sc.world.color = (0.93, 0.92, 0.9)

objs = []
for it in items:
    path, color, off = it, "#d9a25b", (0, 0, 0)
    if ":" in path and path.rsplit(":", 1)[1].startswith("#"):
        path, color = path.rsplit(":", 1)
    if "@" in path:
        path, o = path.split("@")
        off = tuple(float(v) for v in o.split(","))
    bpy.ops.wm.stl_import(filepath=path)
    ob = bpy.context.selected_objects[0]
    ob.location = off
    bpy.ops.object.shade_smooth()
    me = ob.data
    if hasattr(me, "use_auto_smooth"):
        me.use_auto_smooth = True
    mat = bpy.data.materials.new("m")
    c = color.lstrip("#")
    rgb = tuple(int(c[i:i + 2], 16) / 255 for i in (0, 2, 4))
    mat.diffuse_color = (*rgb, 1)
    ob.data.materials.append(mat)
    objs.append(ob)

# bounds
mn = Vector((1e9, 1e9, 1e9))
mx = Vector((-1e9, -1e9, -1e9))
for ob in objs:
    for v in ob.bound_box:
        w = ob.matrix_world @ Vector(v)
        mn = Vector((min(mn[i], w[i]) for i in range(3)))
        mx = Vector((max(mx[i], w[i]) for i in range(3)))
ctr = (mn + mx) / 2
size = max((mx - mn)[0], (mx - mn)[2] * 1.6, 40)

cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
sc.collection.objects.link(cam)
sc.camera = cam
cam.data.type = "ORTHO"
cam.data.ortho_scale = size * 1.25
cam.data.clip_end = 5000

dirs = {  # (azimuth deg from -Y towards +X, elevation deg)
    "front": (0, 8), "three": (32, 22), "side": (90, 8), "back": (180, 12), "top": (0, 80),
    "three2": (-35, 22),
}
for v in views:
    az, el = dirs[v]
    d = size * 3
    cam.location = ctr + Vector((math.sin(math.radians(az)) * math.cos(math.radians(el)) * d,
                                 -math.cos(math.radians(az)) * math.cos(math.radians(el)) * d,
                                 math.sin(math.radians(el)) * d))
    direction = ctr - cam.location
    cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    sc.render.filepath = out if len(views) == 1 else out.replace(".png", f"_{v}.png")
    bpy.ops.render.render(write_still=True)
print("rendered", views)
