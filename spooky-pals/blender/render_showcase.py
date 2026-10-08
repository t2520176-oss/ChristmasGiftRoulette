"""Spooky Pals - presentation renderer (Blender 4.x, Cycles, headless).

blender -b -P render_showcase.py -- config.json

config.json
{
  "output": "/path/out.png", "resolution": [2000, 1200], "samples": 96,
  "camera": {"location": [x, y, z] (mm), "target": [x, y, z] (mm), "lens": 70, "fstop": 4.0},
  "scene": "table" | "plate" | "studio",
  "items": [ {"dir": "/path/to/stl_set", "pos": [x, y, z], "rot": deg, "offsets": {"hat": [0,0,60]},
              "colors": {"pet": "#C98B4A", "hat": "#35283F", ...}} , ... ],
  "labels": [ {"text": "HAT", "pos": [x, y, z]} ],
  "props": true
}
Every STL in `dir` is named after its part (pet.stl, hat.stl, cape.stl, ...). Units in the config are mm.
"""
import json
import math
import os
import random
import sys

import bpy
from mathutils import Vector

SCALE = 0.001  # 1 mm -> 1 mm = 0.001 Blender units (metres)

argv = sys.argv[sys.argv.index("--") + 1:]
cfg = json.load(open(argv[0]))


def lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(lin(int(h[i:i + 2], 16)) for i in (0, 2, 4))


def set_in(node, names, value):
    for n in names:
        if n in node.inputs:
            node.inputs[n].default_value = value
            return True
    return False


def make_mat(name, color, rough=0.42, metallic=0.0, emit=0.0, sheen=0.25, coat=0.15, sss=0.05, noise_bump=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    b = nt.nodes["Principled BSDF"]
    rgb = hex_rgb(color)
    b.inputs["Base Color"].default_value = (*rgb, 1)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Metallic"].default_value = metallic
    set_in(b, ["Sheen Weight"], sheen)
    set_in(b, ["Coat Weight"], coat)
    set_in(b, ["Subsurface Weight"], sss)
    set_in(b, ["Subsurface Radius"], (0.6, 0.3, 0.2))
    if emit > 0:
        set_in(b, ["Emission Color"], (*rgb, 1))
        set_in(b, ["Emission Strength"], emit)
    if noise_bump > 0:
        tex = nt.nodes.new("ShaderNodeTexNoise")
        tex.inputs["Scale"].default_value = 900
        tex.inputs["Detail"].default_value = 6
        bump = nt.nodes.new("ShaderNodeBump")
        bump.inputs["Strength"].default_value = noise_bump
        bump.inputs["Distance"].default_value = 0.0005
        nt.links.new(tex.outputs["Fac"], bump.inputs["Height"])
        nt.links.new(bump.outputs["Normal"], b.inputs["Normal"])
    return m


def wood_material():
    m = bpy.data.materials.new("table")
    m.use_nodes = True
    nt = m.node_tree
    b = nt.nodes["Principled BSDF"]
    coord = nt.nodes.new("ShaderNodeTexCoord")
    mapping = nt.nodes.new("ShaderNodeMapping")
    mapping.inputs["Scale"].default_value = (0.35, 26.0, 0.35)
    mapping.inputs["Rotation"].default_value = (0, 0, math.radians(8))
    wave = nt.nodes.new("ShaderNodeTexWave")
    wave.wave_type = "BANDS"
    wave.inputs["Scale"].default_value = 6.0
    wave.inputs["Distortion"].default_value = 3.0
    wave.inputs["Detail"].default_value = 4.0
    wave.inputs["Detail Scale"].default_value = 1.4
    ramp = nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color = (*hex_rgb("#2A180E"), 1)
    ramp.color_ramp.elements[1].color = (*hex_rgb("#6B4128"), 1)
    nt.links.new(coord.outputs["Object"], mapping.inputs["Vector"])
    nt.links.new(mapping.outputs["Vector"], wave.inputs["Vector"])
    nt.links.new(wave.outputs["Fac"], ramp.inputs["Fac"])
    nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    b.inputs["Roughness"].default_value = 0.42
    bump = nt.nodes.new("ShaderNodeBump")
    bump.inputs["Strength"].default_value = 0.15
    nt.links.new(wave.outputs["Fac"], bump.inputs["Height"])
    nt.links.new(bump.outputs["Normal"], b.inputs["Normal"])
    return m


def import_stl(path, mat, loc, rot_deg, offset):
    before = set(bpy.data.objects)
    bpy.ops.wm.stl_import(filepath=path, global_scale=SCALE)
    new = [o for o in bpy.data.objects if o not in before][0]
    bpy.context.view_layer.objects.active = new
    new.select_set(True)
    bpy.ops.object.shade_smooth()
    if hasattr(new.data, "use_auto_smooth"):
        new.data.use_auto_smooth = True
        new.data.auto_smooth_angle = math.radians(38)
    new.data.materials.append(mat)
    # rotate about the item origin, then move
    new.location = Vector(loc) * SCALE
    new.rotation_euler = (0, 0, math.radians(rot_deg))
    # apply rotation of the item around its own origin: rotate the vertices offset
    if offset:
        # offset is given in item space -> rotate it as the item is rotated
        o = Vector(offset)
        o.rotate(__import__("mathutils").Euler((0, 0, math.radians(rot_deg))))
        new.location += o * SCALE
    new.select_set(False)
    return new


def add_area(name, loc, target, size, power, color=(1, 1, 1), shape="DISK"):
    ld = bpy.data.lights.new(name, "AREA")
    ld.energy = power
    ld.size = size
    ld.shape = shape
    ld.color = color
    ob = bpy.data.objects.new(name, ld)
    bpy.context.scene.collection.objects.link(ob)
    ob.location = Vector(loc) * SCALE
    d = Vector(target) * SCALE - ob.location
    ob.rotation_euler = d.to_track_quat("-Z", "Y").to_euler()
    return ob


def build():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    cy = sc.cycles
    cy.samples = cfg.get("samples", 96)
    # the distro build of Blender has no OpenImageDenoise: clean images come from samples + adaptive sampling
    cy.use_denoising = False
    cy.use_adaptive_sampling = True
    cy.adaptive_threshold = cfg.get("noise_threshold", 0.015)
    cy.adaptive_min_samples = 24
    cy.sample_clamp_indirect = 4.0
    cy.sample_clamp_direct = 20.0
    cy.max_bounces = 6
    cy.caustics_reflective = False
    cy.caustics_refractive = False
    cy.device = "CPU"
    sc.render.resolution_x, sc.render.resolution_y = cfg.get("resolution", [2000, 1200])
    sc.render.resolution_percentage = 100
    sc.render.image_settings.file_format = "PNG"
    sc.render.film_transparent = cfg.get("transparent", False)
    sc.view_settings.view_transform = "Filmic" if "Filmic" in [v.identifier for v in bpy.types.ColorManagedViewSettings.bl_rna.properties["view_transform"].enum_items] else "Standard"
    try:
        sc.view_settings.look = "Medium High Contrast"
    except Exception:
        pass
    sc.view_settings.exposure = cfg.get("exposure", 0.0)

    # ---- world
    w = bpy.data.worlds.new("w")
    sc.world = w
    w.use_nodes = True
    bg = w.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (*hex_rgb(cfg.get("world_color", "#140A12")), 1)
    bg.inputs["Strength"].default_value = cfg.get("world_strength", 0.5)

    kind = cfg.get("scene", "table")
    # ---- floor
    # the assembled base reaches 14 mm below the deck (z = 0), so the table top sits at z = -14 mm
    bpy.ops.mesh.primitive_plane_add(size=6.0, location=(0, 0, cfg.get("floor_z", -14) * SCALE))
    floor = bpy.context.active_object
    floor.name = "floor"
    if kind == "table":
        floor.data.materials.append(wood_material())
    elif kind == "plate":
        floor.data.materials.append(make_mat("bed", "#2B2E36", rough=0.55, metallic=0.0, coat=0.0, sheen=0, sss=0))
    else:
        floor.data.materials.append(make_mat("studio", cfg.get("floor_color", "#E9E1D6"), rough=0.6, coat=0.0, sheen=0, sss=0))

    # ---- items
    mats = {}
    default_cols = cfg.get("default_colors", {})
    for i, item in enumerate(cfg["items"]):
        cols = dict(default_cols)
        cols.update(item.get("colors", {}))
        offs = item.get("offsets", {})
        for fn in sorted(os.listdir(item["dir"])):
            if not fn.endswith(".stl"):
                continue
            part = fn[:-4]
            if part in item.get("skip", []):
                continue
            col = cols.get(part, "#CCCCCC")
            key = (i, part)
            rough = {"pet": 0.5, "face": 0.18, "shine": 0.1, "text": 0.4, "base": 0.5, "deco": 0.45}.get(part, 0.42)
            emit = 0.0
            mats[key] = make_mat(f"{part}_{i}", col, rough=rough, emit=emit, noise_bump=cfg.get("bump", 0.12))
            import_stl(os.path.join(item["dir"], fn), mats[key], item.get("pos", [0, 0, 0]), item.get("rot", 0),
                       offs.get(part))

    # ---- extra STL props (jack-o'-lanterns with a light inside, leaves, candy corn ...)
    for k, ex in enumerate(cfg.get("extras", [])):
        m = make_mat(f"extra{k}", ex.get("color", "#E8731A"), rough=0.5, emit=ex.get("emit", 0.0), noise_bump=0.08)
        ob = import_stl(ex["stl"], m, ex.get("pos", [0, 0, 0]), ex.get("rot", 0), None)
        sc_ = ex.get("scale", 1.0)
        ob.scale = tuple(v * sc_ for v in ob.scale)       # the importer already applied the 0.001 mm -> m scale
        if ex.get("light"):
            lp = ex["light"]
            ld = bpy.data.lights.new(f"inner{k}", "POINT")
            ld.energy = lp.get("power", 1.5)
            ld.color = hex_rgb(lp.get("color", "#FF8A1E"))
            ld.shadow_soft_size = 0.004
            lo = bpy.data.objects.new(f"inner{k}", ld)
            sc.collection.objects.link(lo)
            lo.location = (Vector(ex.get("pos", [0, 0, 0])) + Vector([0, 0, lp.get("z", 18) * sc_])) * SCALE
    # ---- optional glowing props (little lanterns / candles) behind the scene
    if cfg.get("props", False):
        random.seed(cfg.get("seed", 7))
        for k in range(cfg.get("bokeh", 34)):
            r = random.uniform(0.004, 0.012)
            x = random.uniform(-0.55, 0.55)
            y = random.uniform(0.28, 0.9)
            z = random.uniform(0.02, 0.38)
            bpy.ops.mesh.primitive_uv_sphere_add(radius=r, location=(x, y, z), segments=24, ring_count=12)
            s = bpy.context.active_object
            col = random.choice(["#FF9A2E", "#FFB34A", "#FF7A18", "#FFD27A", "#B35CFF"])
            m = make_mat(f"glow{k}", col, rough=1.0, emit=random.uniform(2.5, 9.0) * cfg.get("bokeh_strength", 1.0), coat=0, sheen=0, sss=0)
            s.data.materials.append(m)

    # ---- lights
    L = cfg.get("lights", {})
    add_area("key", L.get("key", [260, -380, 420]), L.get("key_target", [0, 0, 40]), 0.25,
             L.get("key_power", 90), color=(1.0, 0.78, 0.55))
    add_area("rim", L.get("rim", [-380, 260, 260]), L.get("rim_target", [0, 0, 60]), 0.2,
             L.get("rim_power", 70), color=(0.62, 0.45, 1.0))
    add_area("fill", L.get("fill", [-300, -420, 140]), L.get("fill_target", [0, 0, 40]), 0.5,
             L.get("fill_power", 18), color=(1.0, 0.7, 0.45))

    # ---- camera
    C = cfg["camera"]
    cd = bpy.data.cameras.new("cam")
    cd.lens = C.get("lens", 70)
    cd.sensor_width = 36
    cam = bpy.data.objects.new("cam", cd)
    sc.collection.objects.link(cam)
    sc.camera = cam
    cam.location = Vector(C["location"]) * SCALE
    tgt = Vector(C["target"]) * SCALE
    cam.rotation_euler = (tgt - cam.location).to_track_quat("-Z", "Y").to_euler()
    if C.get("fstop"):
        cd.dof.use_dof = True
        cd.dof.aperture_fstop = C["fstop"]
        empty = bpy.data.objects.new("focus", None)
        sc.collection.objects.link(empty)
        empty.location = Vector(C.get("focus", C["target"])) * SCALE
        cd.dof.focus_object = empty

    # ---- text labels
    for lab in cfg.get("labels", []):
        tc = bpy.data.curves.new("lab", "FONT")
        tc.body = lab["text"]
        tc.size = lab.get("size", 7) * SCALE
        tc.align_x = "CENTER"
        tc.extrude = 0.0002
        to = bpy.data.objects.new("lab", tc)
        sc.collection.objects.link(to)
        to.location = Vector(lab["pos"]) * SCALE
        to.rotation_euler = (cam.rotation_euler.to_matrix().to_euler())
        to.data.materials.append(make_mat("labmat", lab.get("color", "#FFE9C8"), rough=0.6, emit=3.0, coat=0, sheen=0, sss=0))

    os.makedirs(os.path.dirname(cfg["output"]), exist_ok=True)
    sc.render.filepath = cfg["output"]
    bpy.ops.render.render(write_still=True)
    print("RENDERED", cfg["output"])


build()
