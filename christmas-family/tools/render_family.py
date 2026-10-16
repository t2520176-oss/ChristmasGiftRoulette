#!/usr/bin/env python3
"""Presentation renders of the family ornament (Blender / Cycles, headless).

  python3 render_family.py --assembled ../stl/assembled --beds ../beds --out ../renders [--quality draft|final] [--only hero,exploded,bed1]

  hero.png         assembled ornament on a wooden table, warm Christmas bokeh
  hero_close.png   low close-up of the figures and name tags
  exploded.png     every piece lifted out of its slot
  bed1..N.png      top-down view of each print bed (colours as they are loaded into the AMS / MMU)
"""
import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402

BLENDER_SCRIPT = os.path.join(HERE, "..", "blender", "render_showcase.py")
XMAS_BOKEH = ["#FF4A3A", "#FF9A2E", "#FFD27A", "#3DDC84", "#FFF2D0", "#FF7A18"]


def colors_for(files, members, overrides=None):
    out = {}
    for stem in files:
        piece, role = stem.split("__")[:2]
        out[stem] = fp.COLOR_HEX[fp.role_color(piece, role, members, overrides)]
    return out


def common(cfg_extra, quality):
    q = {"draft": dict(samples=24, resolution=[1000, 600]), "final": dict(samples=110, resolution=[1800, 1080])}[quality]
    cfg = dict(q)
    cfg.update(cfg_extra)
    return cfg


def explode_offsets(stems):
    # (dx, dy, dz) in mm, per piece: back-to-front layers so nothing hides anything else
    table = {"sky": (0, 70, 55), "tree": (0, 34, 42), "house": (0, 34, 46), "fig": (0, -16, 52), "tag": (0, -38, 20),
             "plaque": (0, -52, -4), "holly": (0, -30, 24), "lantern": (0, 6, 62), "base": (0, 0, 0)}
    out = {}
    for s in stems:
        piece = s.split("__")[0]
        for k, v in table.items():
            if piece.startswith(k):
                out[s] = list(v)
                break
    return out


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
    ap.add_argument("--out", required=True)
    ap.add_argument("--quality", default="draft", choices=["draft", "final"])
    ap.add_argument("--only", default="")
    ap.add_argument("--palette", default="")
    a = ap.parse_args()
    only = {s for s in a.only.split(",") if s}
    overrides = fp.load_overrides(a.palette)
    cfg0 = json.load(open(os.path.join(a.assembled, "config.json")))
    members = cfg0["members"]
    stems = sorted(f[:-4] for f in os.listdir(a.assembled) if f.endswith(".stl"))
    cols = colors_for(stems, members, overrides)
    os.makedirs(a.out, exist_ok=True)
    tmp = os.path.join(a.out, "_cfg")
    os.makedirs(tmp, exist_ok=True)

    emissive = {s: 2.2 for s in stems if s.endswith("__glass")}
    emissive.update({s: 1.4 for s in stems if s.startswith("house__window")})
    emissive.update({s: 0.35 for s in stems if s.startswith("sky__stars")})
    rough = {s: 0.7 for s in stems if s.endswith("__snow")}
    rough.update({s: 0.25 for s in stems if s.endswith("__glass")})
    rough.update({s: 0.3 for s in stems if s.startswith("tree") and s.endswith("__star")})
    balls = [
        {"pos": [-128, -62, 16], "r": 16, "color": "#C8202F", "metal": 0.8},
        {"pos": [132, -70, 13], "r": 13, "color": "#1F6B3A", "metal": 0.8},
        {"pos": [150, -30, 9], "r": 9, "color": "#F2C230", "metal": 0.9},
    ]
    base = dict(scene="table", floor_z=0, world_color="#0D0A10", world_strength=0.35, default_roughness=0.42, roughness=rough,
                light_colors={"key": (1.0, 0.86, 0.68), "rim": (0.55, 0.7, 1.0), "fill": (1.0, 0.82, 0.62)}, exposure=-0.3,
                emissive=emissive, props=True, bokeh=46, bokeh_colors=XMAS_BOKEH, bokeh_strength=1.0, bump=0.1,
                items=[{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols}],
                lights={"key": [220, -420, 380], "key_target": [0, 0, 55], "key_power": 38,
                        "rim": [-380, 240, 300], "rim_target": [0, 0, 60], "rim_power": 30,
                        "fill": [-260, -420, 120], "fill_target": [0, 0, 45], "fill_power": 7})

    jobs = {}
    jobs["hero"] = common(dict(base, output=os.path.join(a.out, "hero.png"), balls=balls,
                               camera={"location": [95, -560, 215], "target": [0, 0, 56], "lens": 62, "fstop": 6.0}), a.quality)
    jobs["hero_close"] = common(dict(base, output=os.path.join(a.out, "hero_close.png"), balls=balls[:2], exposure=-1.0,
                                     camera={"location": [-40, -250, 70], "target": [-5, -5, 52], "lens": 70, "fstop": 7.0, "focus": [-5, -9, 48]}), a.quality)
    ex = dict(base, output=os.path.join(a.out, "exploded.png"), balls=[], scene="studio", props=False, floor_color="#D9D0C3",
              world_color="#EFE7DA", world_strength=0.9, exposure=-0.2,
              lights={"key": [220, -420, 380], "key_target": [0, 0, 55], "key_power": 22,
                      "rim": [-380, 240, 300], "rim_target": [0, 0, 60], "rim_power": 14,
                      "fill": [-260, -420, 120], "fill_target": [0, 0, 45], "fill_power": 5})
    ex["items"] = [{"dir": a.assembled, "pos": [0, 0, 0], "rot": 0, "colors": cols, "offsets": explode_offsets(stems)}]
    ex["camera"] = {"location": [190, -560, 235], "target": [0, 4, 66], "lens": 58, "fstop": 9.0}
    jobs["exploded"] = common(ex, a.quality)

    plan = json.load(open(os.path.join(a.beds, "plan.json")))
    bw, bh = plan["bed"]
    for b in plan["beds"]:
        info = json.load(open(os.path.join(a.beds, f"bed{b['bed']}.json")))
        bcols = {os.path.basename(v["file"])[:-4]: v["hex"] for v in info["volumes"]}
        tw, td = 45, 70
        cfg = dict(base, scene="plate", floor_z=-3.5, exposure=0.0, output=os.path.join(a.out, f"_bed{b['bed']}_raw.png"), props=False, balls=[],
                   floor_color="#14161B", emissive={}, roughness={k: v for k, v in rough.items()},
                   bed_plate={"size": [bw, bh], "color": "#4A4F5C", "tower": [bw - 4 - tw, bh - 4 - td, tw, td], "tower_color": "#6A7184"},
                   items=[{"dir": os.path.join(a.beds, f"bed{b['bed']}"), "pos": [0, 0, 0], "rot": 0, "colors": bcols}],
                   camera={"location": [bw / 2, bh / 2 - 22, 700], "target": [bw / 2, bh / 2, 0], "lens": 50, "ortho_scale": 292},
                   lights={"key": [-120, -160, 420], "key_target": [bw / 2, bh / 2, 0], "key_power": 3.5,
                           "rim": [300, 320, 260], "rim_target": [bw / 2, bh / 2, 0], "rim_power": 1.0,
                           "fill": [0, -300, 200], "fill_target": [bw / 2, bh / 2, 0], "fill_power": 1.5},
                   light_colors={"key": (1, 0.97, 0.92), "rim": (0.85, 0.9, 1), "fill": (1, 1, 1)},
                   view_transform="Standard", world_strength=0.85, world_color="#B9BCC8")
        cfg["resolution"] = [1400, 1400] if a.quality == "final" else [700, 700]
        cfg["samples"] = 48 if a.quality == "final" else 16
        jobs[f"bed{b['bed']}"] = cfg
    for name, cfg in jobs.items():
        if only and name not in only:
            continue
        print("render", name)
        if name.startswith("bed"):
            cfg = dict(cfg)
        run_blender(cfg, os.path.join(tmp, f"{name}.json"))


if __name__ == "__main__":
    main()
