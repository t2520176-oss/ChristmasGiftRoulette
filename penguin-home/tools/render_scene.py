#!/usr/bin/env python3
"""Presentation renders of PENGUIN HOME (Blender / Cycles, headless).

  python3 render_scene.py --assembled ../stl/assembled --beds ../beds --scad ../openscad/penguin_home.scad --out ../renders [--quality draft|final] [--only hero,glow]

  hero.png      closed igloo, daylight blue scene
  glow.png      lid ajar, tea-light burning inside (dusk)
  top.png       lid off, looking down onto the tea-light pocket
  exploded.png  every piece lifted out of its place
  closeup.png   the entrance and the sign
  bed1/2.png    top-down print layouts (see compose_beds.py for the legend)
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
BOKEH = ["#FFD27A", "#FFF2D0", "#9CD3FF", "#FF9A2E", "#FFFFFF"]
HINGE = [50.2, 0.0, 29.0]            # right-hand edge of the lid (R cos 30 deg, Z_CUT)


def colors_for(stems, overrides):
    return {s: fp.COLOR_HEX[fp.role_color(s.split("__")[0], s.split("__")[1], None, overrides)] for s in stems}


def make_props(scad, tmp):
    d = os.path.join(tmp, "props")
    os.makedirs(d, exist_ok=True)
    for r in ("cup", "flame"):
        subprocess.run(["openscad", "-o", os.path.join(d, f"tealight__{r}.stl"), scad, "-D", 'part="tealight"', "-D", f'role="{r}"', "-D", 'layout="assembled"'],
                       capture_output=True, check=True)
    return d


def lid_ajar(src, tmp, angle=32.0, lift=3.0):
    """copy of the assembled set with the lid hinged open on its right-hand edge"""
    d = os.path.join(tmp, "ajar")
    shutil.copytree(src, d)
    H = np.array(HINGE)
    T = (trimesh.transformations.translation_matrix(H) @ trimesh.transformations.rotation_matrix(np.radians(angle), [0, 1, 0])
         @ trimesh.transformations.translation_matrix(-H))
    for f in os.listdir(d):
        if f.startswith("lid__") and f.endswith(".stl"):
            m = trimesh.load(os.path.join(d, f), force="mesh")
            m.apply_transform(T)
            m.apply_translation([0, 0, lift])
            m.export(os.path.join(d, f))
    return d


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
    tmp = tempfile.mkdtemp(prefix="penguin_render_")
    props = make_props(a.scad, tmp)
    ajar = lid_ajar(a.assembled, tmp)
    os.makedirs(a.out, exist_ok=True)
    cfgdir = os.path.join(a.out, "_cfg")
    os.makedirs(cfgdir, exist_ok=True)

    q = {"draft": dict(samples=24, resolution=[1000, 640]), "final": dict(samples=100, resolution=[1800, 1150])}[a.quality]
    white_parts = [s for s in stems if fp.role_color(s.split("__")[0], s.split("__")[1], None, overrides) == "WHITE"]
    base = dict(q, scene="studio", floor_z=-8, floor_color="#46668F", world_color="#22395A", world_strength=0.30, exposure=-0.5,
                default_roughness=0.5, default_sss=0.05, sss={s: 0.35 for s in white_parts}, props=False, balls=[],
                lights={"key": [200, -300, 380], "key_target": [0, -10, 30], "key_power": 4.5,
                        "rim": [-300, 260, 260], "rim_target": [0, 0, 30], "rim_power": 3,
                        "fill": [-200, -300, 120], "fill_target": [0, 0, 20], "fill_power": 1},
                light_colors={"key": (1.0, 0.95, 0.88), "rim": (0.7, 0.8, 1.0), "fill": (1, 1, 1)})
    props_item = {"dir": props, "pos": [0, 0, 0], "rot": 0, "colors": {"tealight__cup": "#E8ECF2", "tealight__flame": "#FFC060"}, "skip": ["tealight__flame"]}
    flame = {"stl": os.path.join(props, "tealight__flame.stl"), "pos": [0, 0, 0], "color": "#FFC060", "emit": 14.0,
             "light": {"power": 0.45, "color": "#FFB45A", "z": 12.5}}

    jobs = {}
    jobs["hero"] = dict(base, output=os.path.join(a.out, "hero.png"),
                        items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}],
                        camera={"location": [140, -300, 150], "target": [-4, -14, 24], "lens": 50, "fstop": 24.0})
    jobs["glow"] = dict(base, output=os.path.join(a.out, "glow.png"), world_color="#0B1428", world_strength=0.22, floor_color="#2C4468", exposure=-0.15,
                        props=True, bokeh=36, bokeh_colors=BOKEH, bokeh_strength=0.8,
                        items=[{"dir": ajar, "pos": [0, 0, 0], "rot": 0, "colors": cols}, props_item], extras=[flame],
                        lights={"key": [250, -300, 300], "key_target": [0, 0, 20], "key_power": 1.6,
                                "rim": [-300, 260, 260], "rim_target": [0, 0, 30], "rim_power": 2.0,
                                "fill": [-200, -300, 120], "fill_target": [0, 0, 20], "fill_power": 0.5},
                        light_colors={"key": (1.0, 0.82, 0.62), "rim": (0.55, 0.7, 1.0), "fill": (0.8, 0.9, 1.0)},
                        camera={"location": [190, -300, 190], "target": [0, -10, 22], "lens": 52, "fstop": 16.0})
    jobs["top"] = dict(base, output=os.path.join(a.out, "top.png"), exposure=-1.0,
                       items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols, "skip": ["lid__ice", "lid__flake"]}, props_item], extras=[flame],
                       camera={"location": [0, -150, 330], "target": [0, -8, 12], "lens": 50, "fstop": 12.0})
    offs = {}
    for s in stems:
        p = s.split("__")[0]
        offs[s] = {"lid": [0, 0, 78], "body": [0, 0, 22], "trim": [0, -46, 22], "sign": [0, 0, 26], "tree0": [0, 0, 26], "tree1": [0, 0, 26]}.get(p, [0, 0, 0])
    jobs["exploded"] = dict(base, output=os.path.join(a.out, "exploded.png"), floor_color="#7E96B5", world_color="#C9D6E6", world_strength=0.5,
                            items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols, "offsets": offs}],
                            camera={"location": [-130, -400, 250], "target": [0, -10, 40], "lens": 52, "fstop": 10.0})
    jobs["closeup"] = dict(base, output=os.path.join(a.out, "closeup.png"), exposure=-0.9,
                           items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}],
                           camera={"location": [10, -300, 70], "target": [-14, -70, 26], "lens": 50, "fstop": 14.0, "focus": [-14, -78, 22]})

    bw, bh = plan["bed"]
    tow = plan.get("tower") or [45, 70]
    for b in plan["beds"]:
        info = json.load(open(os.path.join(a.beds, f"bed{b['bed']}.json")))
        bcols = {os.path.basename(v["file"])[:-4]: v["hex"] for v in info["volumes"]}
        cfg = dict(base, scene="plate", floor_z=-3.5, output=os.path.join(a.out, f"_bed{b['bed']}_raw.png"),
                   bed_plate={"size": [bw, bh], "color": "#4A4F5C", "tower": [bw - 4 - tow[0], bh - 4 - tow[1], tow[0], tow[1]], "tower_color": "#6A7184"},
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
