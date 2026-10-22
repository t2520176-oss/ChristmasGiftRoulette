#!/usr/bin/env python3
"""Presentation renders of the SANTA SOUND AMPLIFIER (Blender / Cycles, headless).

  python3 render_scene.py --assembled ../stl/assembled --beds ../beds --scad ../openscad/santa_horn.scad --out ../renders [--quality draft|final] [--only hero,glow]

  hero.png      Santa on a table, phone in the slot (day)
  night.png     lights out: a candle-glow inside the grin, spooky background
  face.png      close-up of the face / grin
  side.png      from behind: the cradle ("ears") of the phone slot, the stem and the leaf
  section.png   cut open: the path of the sound from the phone to the grin
  top.png       from above: the phone slot with its ears, the stem, the leaf and the vines
  bed1.png      top-down print layout of bed 1 (body + face), bed2.png = the garnish (see compose_beds.py for the legend)
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile

import numpy as np
import trimesh


HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402

BLENDER_SCRIPT = os.path.join(HERE, "..", "blender", "render_showcase.py")
BOKEH = ["#FFD27A", "#FF5A4A", "#FFE9A8", "#6CD08A", "#FFB347"]


def colors_for(stems, overrides):
    return {s: fp.COLOR_HEX[fp.role_color(s.split("__")[0], s.split("__")[1], None, overrides)] for s in stems}


def scad_num(scad, name):
    import re
    return float(re.search(r"\b%s\s*=\s*([-0-9.]+)" % name, open(scad).read()).group(1))


def grin_y(scad):
    """y of the front of the beard where the mouth starts (x = 0), and of the back wall of the mouth"""
    n = lambda k: scad_num(scad, k)
    zp, bz = n("MZC") + n("MZH"), n("BD_ZC")
    bprof = n("BD_R") * (np.sqrt(max(0, 1 - ((bz - zp) / n("BD_ZL")) ** 2)) if zp <= bz else (max(0, 1 - ((zp - bz) / n("BD_ZU")) ** n("BD_PU"))) ** (1 / n("BD_PU")))
    yf = n("BD_Y") + n("BD_RY") * bprof
    return yf, yf - n("ROOM")


def make_phone(scad, d):
    os.makedirs(d, exist_ok=True)
    for r in ("body", "screen"):
        subprocess.run(["openscad", "-o", os.path.join(d, f"phone__{r}.stl"), scad, "-D", 'part="phone"', "-D", f'role="{r}"'], capture_output=True, check=True)


def cut_open(src, dst, names):
    """the half x >= 0 of the given STLs (capped), so that the inside can be seen"""
    os.makedirs(dst, exist_ok=True)
    for n in names:
        m = trimesh.load(os.path.join(src, n + ".stl"), force="mesh")
        m = m.slice_plane([0, 0, 0], [1, 0, 0], cap=True)
        m.export(os.path.join(dst, n + ".stl"))


def sound_arrows(scad, path):
    """arrows along the sound path (chamber -> throat -> mouth), one STL"""
    y_s, hz0, slot_d = scad_num(scad, "Y_S"), scad_num(scad, "HZ0"), scad_num(scad, "slot_d")
    phone_t = scad_num(scad, "phone_t")
    z_sf = scad_num(scad, "RZT") - slot_d
    y_h0 = y_s + phone_t / 2
    yf, yb = grin_y(scad)
    pts = np.array([[2, y_s, z_sf - 3], [2, y_s, hz0 + 7], [2, y_h0 + 4, hz0 + 7], [2, yb - 4, hz0 + 8], [2, yf - 6, hz0 + 14], [2, yf + 34, hz0 + 24]], float)
    parts = []
    for a, b in zip(pts[:-1], pts[1:]):
        parts.append(trimesh.creation.cylinder(radius=1.5, segment=[a, b], sections=16))
        parts.append(trimesh.creation.uv_sphere(radius=1.5, count=[12, 12]).apply_translation(b))
    d = pts[-1] - pts[-2]
    d = d / np.linalg.norm(d)
    cone = trimesh.creation.cone(radius=5.5, height=13, sections=24)
    z = np.array([0, 0, 1.0])
    v = np.cross(z, d)
    c = float(z @ d)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    R = np.eye(3) + K + K @ K * (1 / (1 + c))
    T = np.eye(4)
    T[:3, :3] = R
    T[:3, 3] = pts[-1]
    cone.apply_transform(T)
    parts.append(cone)
    trimesh.util.concatenate(parts).export(path)


def make_ui(scad, d):
    """what the phone shows (a music player): a grey disc with a note, a progress bar, three buttons - flat parts just in front of the screen"""
    os.makedirs(d, exist_ok=True)
    y_s, slot_d = scad_num(scad, "Y_S"), scad_num(scad, "slot_d")
    z_sf = scad_num(scad, "RZT") - slot_d
    y = y_s + 3.95 + 0.12
    z0 = z_sf + 0.5
    R = trimesh.transformations.rotation_matrix(np.pi / 2, [1, 0, 0])

    def cyl(r, h, x, z, sections=48):
        m = trimesh.creation.cylinder(radius=r, height=h, sections=sections)
        m.apply_transform(R)
        m.apply_translation([x, y, z0 + z])
        return m

    def box(w, h, x, z, t=0.25):
        m = trimesh.creation.box(extents=[w, t, h])
        m.apply_translation([x, y, z0 + z])
        return m
    disc = cyl(17, 0.25, 0, 112)
    disc.export(os.path.join(d, "ui_disc.stl"))
    note = [cyl(3.3, 0.5, 3.2, 106.5, 24), box(1.3, 15, 0.55, 112.2, 0.5), box(5.5, 2.0, -1.9, 119.3, 0.5)]
    bar = [box(46, 1.2, 0, 88, 0.5), cyl(2.1, 0.6, -6, 88, 20)]
    btn = [cyl(4.6, 0.5, 0, 74, 32), box(10, 1.2, -19, 74, 0.5), box(10, 1.2, 19, 74, 0.5)]
    trimesh.util.concatenate(note + bar + btn).export(os.path.join(d, "ui_marks.stl"))
    trimesh.creation.box(extents=[0.2, 0.2, 0.2]).export(os.path.join(d, "glow.stl"))          # a dummy that carries the candle light


def run_blender(cfg, path):
    with open(path, "w") as f:
        json.dump(cfg, f, indent=1)
    r = subprocess.run(["blender", "-b", "-P", BLENDER_SCRIPT, "--", path], capture_output=True, text=True)
    if "RENDERED" not in r.stdout:
        print(r.stdout[-1500:], r.stderr[-1500:])
        raise SystemExit("blender failed")
    print("   ->", cfg["output"])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--assembled", required=True)
    ap.add_argument("--beds", required=True)
    ap.add_argument("--scad", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--quality", default="draft", choices=["draft", "final"])
    ap.add_argument("--only", default="")
    a = ap.parse_args()
    only = {s for s in a.only.split(",") if s}
    plan = json.load(open(os.path.join(a.beds, "plan.json")))
    fp.COLOR_HEX.update(plan.get("hex", {}))
    overrides = plan.get("overrides") or None
    stems = sorted(f[:-4] for f in os.listdir(a.assembled) if f.endswith(".stl"))
    cols = colors_for(stems, overrides)
    tmp = tempfile.mkdtemp(prefix="santa_render_")
    phone_dir = os.path.join(tmp, "phone")
    make_phone(a.scad, phone_dir)
    pc = {"phone__body": fp.COLOR_HEX["BLACK"], "phone__screen": fp.COLOR_HEX["BLUE"]}
    both = os.path.join(tmp, "both")
    os.makedirs(both)
    for d_ in (a.assembled, phone_dir):
        for f in os.listdir(d_):
            if f.endswith(".stl"):
                shutil.copy(os.path.join(d_, f), os.path.join(both, f))
    allcols = dict(cols, **pc)
    os.makedirs(a.out, exist_ok=True)
    cfgdir = os.path.join(a.out, "_cfg")
    os.makedirs(cfgdir, exist_ok=True)

    q = {"draft": dict(samples=24, resolution=[650, 850]), "final": dict(samples=110, resolution=[1300, 1700])}[a.quality]
    wide = {"draft": [900, 650], "final": [1800, 1300]}[a.quality]
    base = dict(q, scene="studio", floor_z=-0.5, floor_color="#7A5232", world_color="#4A2E1E", world_strength=0.6, exposure=-0.1,
                default_roughness=0.5, default_sss=0.15, sss={"santa__red": 0.15, "santa__white": 0.45, "santa__peach": 0.3}, props=False, balls=[],
                lights={"key": [300, -380, 300], "key_target": [0, 0, 60], "key_power": 6.0,
                        "rim": [-320, 260, 260], "rim_target": [0, 0, 70], "rim_power": 4.0,
                        "fill": [-250, -300, 120], "fill_target": [0, 0, 50], "fill_power": 1.2},
                light_colors={"key": (1.0, 0.82, 0.62), "rim": (0.62, 0.55, 1.0), "fill": (1.0, 0.9, 0.85)})
    ui_dir = os.path.join(tmp, "ui")
    make_ui(a.scad, ui_dir)
    glass = {"stl": os.path.join(phone_dir, "phone__screen.stl"), "pos": [0, 0, 0], "rot": 180, "color": "#0B0D13", "emit": 0.0}
    disc = {"stl": os.path.join(ui_dir, "ui_disc.stl"), "pos": [0, 0, 0], "rot": 180, "color": "#3A3D49", "emit": 0.35}
    marks = {"stl": os.path.join(ui_dir, "ui_marks.stl"), "pos": [0, 0, 0], "rot": 180, "color": "#F1F3F8", "emit": 1.6}
    screen = [glass, disc, marks]
    yf, yb = grin_y(a.scad)
    candle = {"stl": os.path.join(ui_dir, "glow.stl"), "pos": [0, -(yb + 6), 0], "rot": 0, "color": "#FFB347", "emit": 0.0,
              "light": {"power": 1.6, "color": "#FFA23A", "z": 24}}
    item_both = {"dir": both, "pos": [0, 0, 0], "rot": 180, "colors": allcols, "skip": ["phone__screen"]}
    
    jobs = {}
    jobs["hero"] = dict(base, output=os.path.join(a.out, "hero.png"), floor_color="#8A5C38", world_color="#5A3A26", world_strength=0.8, exposure=0.1,
                        props=True, bokeh=28, bokeh_colors=BOKEH, bokeh_strength=0.5,
                        items=[item_both], extras=screen,
                        camera={"location": [250, -470, 170], "target": [0, 0, 88], "lens": 60, "fstop": 22.0})
    jobs["night"] = dict(base, output=os.path.join(a.out, "night.png"), world_color="#0B0605", world_strength=0.2, floor_color="#2A1A10", exposure=0.35,
                         props=True, bokeh=46, bokeh_colors=BOKEH, bokeh_strength=0.9, sss={"santa__red": 0.3, "santa__white": 0.6, "santa__peach": 0.5},
                         items=[item_both], extras=screen + [candle],
                         lights={"key": [250, -330, 150], "key_target": [0, 0, 30], "key_power": 1.0,
                                 "rim": [-320, 260, 260], "rim_target": [0, 0, 70], "rim_power": 3.2,
                                 "fill": [-250, -300, 100], "fill_target": [0, 0, 40], "fill_power": 0.25},
                         light_colors={"key": (1.0, 0.55, 0.2), "rim": (0.5, 0.45, 1.0), "fill": (1.0, 0.6, 0.4)},
                         camera={"location": [250, -470, 170], "target": [0, 0, 88], "lens": 60, "fstop": 16.0})
    jobs["face"] = dict(base, output=os.path.join(a.out, "face.png"), resolution=wide, exposure=-0.15,
                        items=[item_both], extras=screen,
                        camera={"location": [60, -330, 80], "target": [0, 0, 44], "lens": 62, "fstop": 14.0, "focus": [0, -55, 40]})
    jobs["top"] = dict(base, output=os.path.join(a.out, "top.png"), resolution=wide, exposure=-0.3,
                       items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 180, "colors": cols}],
                       camera={"location": [10, -150, 340], "target": [0, 5, 62], "lens": 55, "fstop": 18.0})
    jobs["side"] = dict(base, output=os.path.join(a.out, "side.png"), resolution=wide, exposure=-0.1,
                        items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 180, "colors": cols}],
                        camera={"location": [-290, 340, 240], "target": [0, 0, 52], "lens": 55, "fstop": 20.0})
    cut = os.path.join(tmp, "cut")
    cut_open(both, cut, ["santa__red", "santa__white", "santa__peach", "santa__black", "phone__body"])
    arrows = os.path.join(tmp, "arrows.stl")
    sound_arrows(a.scad, arrows)
    jobs["section"] = dict(base, output=os.path.join(a.out, "section.png"), resolution=wide, floor_color="#8A5C38", world_color="#5A3A26", world_strength=0.8, exposure=0.1,
                           items=[{"dir": cut, "pos": [0, 0, 0], "rot": 180, "colors": allcols}],
                           extras=[{"stl": arrows, "pos": [0, 0, 0], "rot": 180, "color": "#FFD34A", "emit": 6.0}],
                           lights={"key": [450, -250, 300], "key_target": [0, 0, 50], "key_power": 6.5,
                                   "rim": [-300, 300, 250], "rim_target": [0, 0, 60], "rim_power": 3.0,
                                   "fill": [450, 150, 150], "fill_target": [0, 0, 50], "fill_power": 1.6},
                           camera={"location": [480, -40, 125], "target": [0, 0, 60], "lens": 70, "fstop": 24.0})

    bw, bh = plan["bed"]
    tow = plan.get("tower") or [45, 70]
    for b in plan["beds"]:
        info = json.load(open(os.path.join(a.beds, f"bed{b['bed']}.json")))
        bcols = {os.path.basename(v["file"])[:-4]: v["hex"] for v in info["volumes"]}
        cfg = dict(base, scene="plate", floor_z=-3.5, output=os.path.join(a.out, f"_bed{b['bed']}_raw.png"),
                   bed_plate={"size": [bw, bh], "color": "#4A4F5C", "tower_color": "#6A7184",
                              "tower": [bw - 4 - tow[0], bh - 4 - tow[1], tow[0], tow[1]] if len(b["colors"]) > 1 else None},
                   items=[{"dir": os.path.join(a.beds, f"bed{b['bed']}"), "pos": [0, 0, 0], "rot": 0, "colors": bcols}],
                   camera={"location": [bw / 2, bh / 2 - 22, 700], "target": [bw / 2, bh / 2, 0], "lens": 50, "ortho_scale": 292},
                   lights={"key": [-120, -160, 420], "key_target": [bw / 2, bh / 2, 0], "key_power": 3.5,
                           "rim": [300, 320, 260], "rim_target": [bw / 2, bh / 2, 0], "rim_power": 1.0,
                           "fill": [0, -300, 200], "fill_target": [bw / 2, bh / 2, 0], "fill_power": 1.5},
                   light_colors={"key": (1, 0.97, 0.92), "rim": (0.85, 0.9, 1), "fill": (1, 1, 1)}, view_transform="Standard",
                   world_strength=0.85, world_color="#B9BCC8", exposure=0.0, sss={})
        cfg["resolution"] = [1400, 1400] if a.quality == "final" else [700, 700]
        cfg["samples"] = 48 if a.quality == "final" else 16
        jobs[f"bed{b['bed']}"] = cfg

    for name, cfg in jobs.items():
        if only and name not in only:
            continue
        print("render", name)
        run_blender(cfg, os.path.join(cfgdir, f"{name}.json"))
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
