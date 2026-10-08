#!/usr/bin/env python3
"""Build the contest gallery: export the hero sets with OpenSCAD, then render them with Blender.

  python3 make_showcase.py export  [--fn 40] [--only LUNA,LILY]   # OpenSCAD -> stl/gallery/<NAME>/
  python3 make_showcase.py panels  [--fn 40]                        # option panels (pets / costumes / bases)
  python3 make_showcase.py render  [--shots lineup,grid,hero,explode,plate,panels] [--samples 96]
"""
import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
GAL = os.path.join(ROOT, "stl", "gallery")
RND = os.path.join(ROOT, "renders")
TMP = os.environ.get("SHOWCASE_TMP", "/tmp/spooky_showcase")
BLENDER_SCRIPT = os.path.join(ROOT, "blender", "render_showcase.py")
PY = sys.executable

# name, pet, costume, base
HEROES = [
    ("LUNA",    "dog",      "vampire", "crypt"),
    ("LILY",    "cat",      "ghost",   "stump"),
    ("MOCHI",   "hamster",  "pumpkin", "stump"),
    ("PIPPIN",  "rabbit",   "wizard",  "cauldron"),
    ("CHARLIE", "bird",     "devil",   "crypt"),
    ("ACORN",   "squirrel", "witch",   "cauldron"),
]

FUR = {   # fur, markings
    "dog": ("#33333D", "#F6EEDD"), "cat": ("#A3A3B1", "#F6EEDD"), "hamster": ("#E3A857", "#FFF3E0"),
    "rabbit": ("#D6BFA0", "#FFFFFF"), "bird": ("#F4D24B", "#F6A03A"), "squirrel": ("#B8683A", "#F2E3CC"),
}
HAT = {"vampire": ("#2B2230", "#B3202A"), "witch": ("#35283F", "#8E4FC0"), "ghost": ("#F4F1EA", "#2B1B17"),
       "pumpkin": ("#E8731A", "#4F8A3B"), "devil": ("#C0242E", "#C0242E"), "wizard": ("#2E2A7A", "#F2C230")}
CAPE = {"vampire": "#8E1226", "witch": "#7B3FA0", "ghost": "#F4F1EA", "pumpkin": "#E8731A", "devil": "#8C1B24",
        "wizard": "#3B3AA0"}
CHARM = {"vampire": "#D4263B", "witch": "#F08A2C", "ghost": "#E8731A", "pumpkin": "#4F8A3B", "devil": "#1E1A22",
         "wizard": "#F2C230"}
BASE = {"stump": ("#6B4A33", "#E8731A"), "crypt": ("#8D8A96", "#BDB9C6"), "cauldron": ("#2F2B36", "#7ED036")}
TEXT = "#F3E2C0"


def colors_for(pet, costume, base):
    fur, marks = FUR[pet]
    hat, trim = HAT.get(costume, ("#888888", "#888888"))
    b, deco = BASE[base]
    return {"pet": fur, "marks": marks, "face": "#241512", "shine": "#FFFFFF", "hat": hat, "trim": trim,
            "cape": CAPE.get(costume, "#888888"), "charm": CHARM.get(costume, "#888888"),
            "base": b, "deco": deco, "text": TEXT}


def export_set(outdir, pet, costume, base, name, fn, parts=None, layout="assembled"):
    cmd = [PY, os.path.join(HERE, "export_parts.py"), "--pet", pet, "--costume", costume, "--base", base,
           "--name", name, "--out", outdir, "--layout", layout, "--fn", str(fn), "--multicolor"]
    if parts:
        cmd += ["--parts", parts]
    subprocess.run(cmd, check=True)


def do_export(args):
    only = set(args.only.split(",")) if args.only else None
    for name, pet, costume, base in HEROES:
        if only and name not in only:
            continue
        print(f"== {name}: {pet} / {costume} / {base}")
        export_set(os.path.join(GAL, name), pet, costume, base, name, args.fn)


def do_panels(args):
    # Option panels: pets only, costumes on the dog, bases only
    for pet in FUR:
        export_set(os.path.join(TMP, "opt_pets", pet), pet, "none", "stump", "X", args.fn,
                   parts="pet,face,shine,marks")
    for costume in HAT:
        export_set(os.path.join(TMP, "opt_cost", costume), "dog", costume, "stump", "X", args.fn,
                   parts="hat,trim,cape,charm")
    for base in BASE:
        export_set(os.path.join(TMP, "opt_base", base), "dog", "none", base, "YOUR NAME", args.fn,
                   parts="base,deco,text")


def props_dir():
    return os.path.join(TMP, "props")


def scatter_props(seed, x0, x1, y0, y1, n_leaf=8, n_candy=5, z=-14):
    import random
    rnd = random.Random(seed)
    out = []
    for _ in range(n_leaf):
        out.append({"stl": os.path.join(props_dir(), "leaf.stl"), "pos": [rnd.uniform(x0, x1), rnd.uniform(y0, y1), z],
                    "rot": rnd.uniform(0, 360), "scale": rnd.uniform(0.9, 1.5),
                    "color": rnd.choice(["#D9531E", "#E8851C", "#B8341A", "#E9B02C"])})
    for _ in range(n_candy):
        out.append({"stl": os.path.join(props_dir(), "candy.stl"), "pos": [rnd.uniform(x0, x1), rnd.uniform(y0, y1), z],
                    "rot": rnd.uniform(0, 360), "scale": 1.0, "color": rnd.choice(["#F6B21C", "#F4F0E6", "#F07A1A"])})
    return out


def lantern(pos, rot=0, scale=1.0):
    return {"stl": os.path.join(props_dir(), "lantern.stl"), "pos": pos, "rot": rot, "scale": scale,
            "color": "#E8731A", "emit": 0.0, "light": {"power": 1.6 * scale, "color": "#FF8A1E", "z": 20}}


def write_cfg(path, cfg):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(cfg, fh, indent=1)
    return path


def blender(cfg_path):
    subprocess.run(["blender", "-b", "-P", BLENDER_SCRIPT, "--", cfg_path], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)


def item(name, pet, costume, base, pos, rot=0, offsets=None):
    return {"dir": os.path.join(GAL, name), "pos": pos, "rot": rot, "colors": colors_for(pet, costume, base),
            "offsets": offsets or {}}


def do_render(args):
    shots = set(args.shots.split(","))
    s = args.samples
    # ---- wide line-up (6 sets in a row)
    if "lineup" in shots:
        its = [item(n, p, c, b, [(i - 2.5) * 92, 0, 0], rot=0) for i, (n, p, c, b) in enumerate(HEROES)]
        cfg = {"output": os.path.join(RND, "01_lineup.png"), "resolution": [2600, 1000], "samples": s,
               "scene": "table", "props": True, "bokeh": 46, "items": its,
               "extras": [lantern([-300, 150, -14], 12, 1.25), lantern([310, 140, -14], -20, 1.1)] +
                         scatter_props(11, -300, 300, -120, -60),
               "camera": {"location": [0, -1150, 330], "target": [0, 0, 52], "lens": 100, "fstop": 5.6},
               "lights": {"key": [300, -500, 520], "key_power": 220, "key_target": [0, 0, 40],
                          "rim": [-500, 320, 300], "rim_power": 160, "fill": [-380, -650, 160], "fill_power": 40}}
        blender(write_cfg(os.path.join(TMP, "cfg_lineup.json"), cfg))
    # ---- 2x3 grid
    if "grid" in shots:
        its = []
        for i, (n, p, c, b) in enumerate(HEROES):
            row, col = divmod(i, 3)
            its.append(item(n, p, c, b, [(col - 1) * 100 + (row * 30 - 15), row * -105, 0], rot=(col - 1) * -10))
        cfg = {"output": os.path.join(RND, "02_group.png"), "resolution": [2000, 1500], "samples": s,
               "scene": "table", "props": True, "bokeh": 40, "items": its,
               "extras": [lantern([-215, 40, -14], 15, 1.2), lantern([225, 20, -14], -30, 1.0)] +
                         scatter_props(5, -230, 230, -250, -170, 10, 6),
               "camera": {"location": [0, -820, 400], "target": [0, -50, 45], "lens": 85, "fstop": 5.0},
               "lights": {"key": [260, -520, 620], "key_power": 200, "key_target": [0, -40, 40],
                          "rim": [-520, 280, 320], "rim_power": 150, "fill": [-380, -700, 180], "fill_power": 40}}
        blender(write_cfg(os.path.join(TMP, "cfg_group.json"), cfg))
    # ---- hero close-ups
    if "hero" in shots:
        only = set(args.only.split(",")) if args.only else None
        for i, (n, p, c, b) in enumerate(HEROES):
            if only and n not in only:
                continue
            cfg = {"output": os.path.join(RND, f"hero_{n.lower()}.png"), "resolution": [1400, 1400], "samples": s,
                   "scene": "table", "props": True, "bokeh": 30, "seed": 3 + i,
                   "items": [item(n, p, c, b, [0, 0, 0], rot=-12)],
                   "extras": [lantern([-95, 85, -14], 10, 1.0)] + scatter_props(20 + i, -90, 80, -130, -55, 6, 4),
                   "camera": {"location": [150, -330, 150], "target": [0, 0, 44], "lens": 85, "fstop": 3.2},
                   "lights": {"key": [220, -300, 300], "key_power": 120, "key_target": [0, 0, 40],
                              "rim": [-300, 220, 220], "rim_power": 80, "fill": [-220, -380, 120], "fill_power": 25}}
            blender(write_cfg(os.path.join(TMP, f"cfg_hero_{n}.json"), cfg))
    # ---- exploded parts view
    if "explode" in shots:
        n, p, c, b = HEROES[5]
        offs = {"hat": [0, 0, 62], "trim": [0, 0, 62], "cape": [0, 70, 6], "charm": [0, -60, 6],
                "face": [0, -42, 22], "shine": [0, -42, 22], "marks": [0, 0, 0], "pet": [0, 0, 10],
                "base": [0, 0, -34], "deco": [0, 0, -22], "text": [0, 0, -22]}
        labels = [{"text": t, "pos": pos} for t, pos in [("HAT", [-40, -30, 118]), ("PET", [-72, -30, 60]),
                  ("CAPE", [62, -30, 70]), ("CHARM", [-40, -85, 30]), ("BASE", [-80, -30, -30])]]
        cfg = {"output": os.path.join(RND, "03_exploded.png"), "resolution": [2000, 1600], "samples": s,
               "scene": "studio", "world_color": "#1A0F18", "floor_color": "#2A2230", "props": False,
               "items": [item(n, p, c, b, [0, 0, 0], rot=0, offsets=offs)], "labels": [],
               "camera": {"location": [260, -520, 260], "target": [0, 10, 40], "lens": 70, "fstop": 8.0},
               "lights": {"key": [300, -400, 480], "key_power": 150, "key_target": [0, 0, 40],
                          "rim": [-400, 300, 300], "rim_power": 120, "fill": [-300, -500, 150], "fill_power": 40}}
        blender(write_cfg(os.path.join(TMP, "cfg_explode.json"), cfg))
    # ---- option panels
    if "panels" in shots:
        render_panels(s)


def render_panels(s):
    def opt_item(d, cols, pos, rot=0, skip=None):
        it = {"dir": d, "pos": pos, "rot": rot, "colors": cols}
        if skip:
            it["skip"] = skip
        return it
    # pets
    its = []
    for i, pet in enumerate(FUR):
        row, col = divmod(i, 3)
        fur, marks = FUR[pet]
        its.append(opt_item(os.path.join(TMP, "opt_pets", pet),
                            {"pet": fur, "marks": marks, "face": "#241512", "shine": "#FFFFFF"},
                            [(col - 1) * 70, row * -75, 0], rot=-8))
    common = {"scene": "studio", "world_color": "#2A1A22", "floor_color": "#F3E6D3", "world_strength": 1.2,
              "samples": s, "resolution": [1200, 1000], "props": False}
    cfg = dict(common, output=os.path.join(TMP, "panel_pets.png"), items=its,
               camera={"location": [0, -520, 330], "target": [0, -40, 28], "lens": 85, "fstop": 11},
               lights={"key": [250, -400, 500], "key_power": 140, "key_target": [0, -40, 30],
                       "rim": [-300, 200, 300], "rim_power": 60, "fill": [-300, -450, 200], "fill_power": 60})
    blender(write_cfg(os.path.join(TMP, "cfg_panel_pets.json"), cfg))
    # costumes on the dog: dog parts + each costume
    its = []
    dog_fur, dog_marks = FUR["dog"]
    for i, costume in enumerate(HAT):
        row, col = divmod(i, 3)
        pos = [(col - 1) * 70, row * -75, 0]
        its.append(opt_item(os.path.join(TMP, "opt_pets", "dog"),
                            {"pet": dog_fur, "marks": dog_marks, "face": "#241512", "shine": "#FFFFFF"}, pos, rot=-8))
        hat, trim = HAT[costume]
        its.append(opt_item(os.path.join(TMP, "opt_cost", costume),
                            {"hat": hat, "trim": trim, "cape": CAPE[costume], "charm": CHARM[costume]}, pos, rot=-8))
    cfg = dict(common, output=os.path.join(TMP, "panel_costumes.png"), items=its,
               camera={"location": [0, -560, 340], "target": [0, -40, 36], "lens": 85, "fstop": 11},
               lights={"key": [250, -400, 500], "key_power": 140, "key_target": [0, -40, 30],
                       "rim": [-300, 200, 300], "rim_power": 60, "fill": [-300, -450, 200], "fill_power": 60})
    blender(write_cfg(os.path.join(TMP, "cfg_panel_costumes.json"), cfg))
    # bases
    its = []
    for i, base in enumerate(BASE):
        b, deco = BASE[base]
        its.append(opt_item(os.path.join(TMP, "opt_base", base), {"base": b, "deco": deco, "text": TEXT},
                            [(i - 1) * 92, 0, 0], rot=-10))
    cfg = dict(common, output=os.path.join(TMP, "panel_bases.png"), resolution=[1500, 700], items=its,
               camera={"location": [0, -420, 260], "target": [0, -4, -2], "lens": 85, "fstop": 11},
               lights={"key": [250, -400, 500], "key_power": 140, "key_target": [0, 0, 0],
                       "rim": [-300, 200, 300], "rim_power": 60, "fill": [-300, -450, 200], "fill_power": 60})
    blender(write_cfg(os.path.join(TMP, "cfg_panel_bases.json"), cfg))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["export", "panels", "render"])
    ap.add_argument("--fn", type=int, default=40)
    ap.add_argument("--only", default="")
    ap.add_argument("--shots", default="lineup,grid,hero,explode,panels")
    ap.add_argument("--samples", type=int, default=96)
    a = ap.parse_args()
    os.makedirs(TMP, exist_ok=True)
    {"export": do_export, "panels": do_panels, "render": do_render}[a.cmd](a)


if __name__ == "__main__":
    main()
