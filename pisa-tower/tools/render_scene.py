#!/usr/bin/env python3
"""Presentation renders of the LEANING TOWER OF PISA lamp (Blender / Cycles, headless).

  python3 render_scene.py --assembled ../stl/assembled --beds ../beds --scad ../openscad/pisa_tower.scad --out ../renders [--quality draft|final] [--only hero,glow]

  hero.png      day: the lamp on a table, soft daylight
  night.png     the tea-light burning inside, light pouring out of the arches
  exploded.png  base, tower, belfry and flag lifted apart (the tea-light in its pocket)
  detail.png    the arcades and cornices, close up
  garden.png    the base: lawn, steps, fence and cypresses
  bed1/2.png    top-down print layouts (see compose_beds.py for the legend)
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile


HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402

BLENDER_SCRIPT = os.path.join(HERE, "..", "blender", "render_showcase.py")
BOKEH = ["#FFD27A", "#FFF2D0", "#9CD3FF", "#FF9A2E", "#FFFFFF"]


def colors_for(stems, overrides):
    return {s: fp.COLOR_HEX[fp.role_color(s.split("__")[0], s.split("__")[1], None, overrides)] for s in stems}


def make_props(scad, tmp):
    d = os.path.join(tmp, "props")
    os.makedirs(d, exist_ok=True)
    for r in ("cup", "flame"):
        subprocess.run(["openscad", "-o", os.path.join(d, f"tealight__{r}.stl"), scad, "-D", 'part="tealight"', "-D", f'role="{r}"', "-D", 'layout="assembled"'],
                       capture_output=True, check=True)
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
    tmp = tempfile.mkdtemp(prefix="pisa_render_")
    props = make_props(a.scad, tmp)
    os.makedirs(a.out, exist_ok=True)
    cfgdir = os.path.join(a.out, "_cfg")
    os.makedirs(cfgdir, exist_ok=True)

    portrait = [1300, 1700]
    q = {"draft": dict(samples=24, resolution=[650, 850]), "final": dict(samples=110, resolution=portrait)}[a.quality]
    stone = [s for s in stems if s.split("__")[1] in ("stone", "white")]
    base = dict(q, scene="studio", floor_z=-0.5, floor_color="#B9A88E", world_color="#9DB9D9", world_strength=0.55, exposure=-0.3,
                default_roughness=0.55, default_sss=0.05, sss={s: 0.25 for s in stone}, props=False, balls=[],
                lights={"key": [260, -330, 330], "key_target": [10, 0, 70], "key_power": 5.0,
                        "rim": [-300, 240, 260], "rim_target": [0, 0, 80], "rim_power": 3.0,
                        "fill": [-220, -300, 120], "fill_target": [0, 0, 60], "fill_power": 1.2},
                light_colors={"key": (1.0, 0.94, 0.84), "rim": (0.75, 0.85, 1.0), "fill": (1, 1, 1)})
    props_item = {"dir": props, "pos": [0, 0, 0], "rot": 0, "colors": {"tealight__cup": "#E8ECF2", "tealight__flame": "#FFC060"}, "skip": ["tealight__flame"]}
    FLAME_Z = 18.8 - 13.3 + 14
    flame = {"stl": os.path.join(props, "tealight__flame.stl"), "pos": [0, 0, 0], "color": "#FFC060", "emit": 16.0,
             "light": {"power": 1.6, "color": "#FFB45A", "z": FLAME_Z}}

    jobs = {}
    jobs["hero"] = dict(base, output=os.path.join(a.out, "hero.png"),
                        items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}],
                        camera={"location": [250, -430, 150], "target": [6, 0, 76], "lens": 85, "fstop": 24.0})
    jobs["night"] = dict(base, output=os.path.join(a.out, "night.png"), world_color="#0A1226", world_strength=0.25, floor_color="#3A2E26", exposure=0.1,
                         props=True, bokeh=40, bokeh_colors=BOKEH, bokeh_strength=0.7,
                         sss={s: 0.6 for s in stone},
                         items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}, props_item], extras=[flame],
                         lights={"key": [250, -330, 260], "key_target": [0, 0, 70], "key_power": 0.8,
                                 "rim": [-300, 240, 260], "rim_target": [0, 0, 80], "rim_power": 1.5,
                                 "fill": [-220, -300, 120], "fill_target": [0, 0, 60], "fill_power": 0.3},
                         light_colors={"key": (1.0, 0.8, 0.6), "rim": (0.55, 0.7, 1.0), "fill": (0.8, 0.9, 1.0)},
                         camera={"location": [250, -430, 150], "target": [6, 0, 76], "lens": 85, "fstop": 18.0})
    offs = {}
    for s in stems:
        p = s.split("__")[0]
        offs[s] = {"tower": [0, 0, 34], "top": [0, 0, 66], "flag": [28, 0, 88]}.get(p, [0, 0, 0])
    jobs["exploded"] = dict(base, output=os.path.join(a.out, "exploded.png"), floor_color="#D2D7DE", world_color="#E4EAF2", world_strength=0.7,
                            props=False, items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols, "offsets": offs}, props_item],
                            camera={"location": [315, -545, 190], "target": [8, 0, 116], "lens": 85, "fstop": 26.0})
    jobs["garden"] = dict(base, output=os.path.join(a.out, "garden.png"), exposure=-0.3,
                          items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}],
                          camera={"location": [150, -250, 85], "target": [-4, -8, 24], "lens": 70, "fstop": 12.0, "focus": [0, -30, 22]})
    jobs["detail"] = dict(base, output=os.path.join(a.out, "detail.png"), exposure=-0.4,
                          items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}],
                          camera={"location": [120, -230, 96], "target": [8, -12, 70], "lens": 100, "fstop": 10.0, "focus": [6, -22, 70]})

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
