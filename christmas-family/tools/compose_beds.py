#!/usr/bin/env python3
"""Add a legend (filament slots, pieces) to the raw top-down bed renders:  bedN.png

  python3 compose_beds.py --beds ../beds --renders ../renders [--slice slice_summary.json]
"""
import argparse
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
NICE = {"BLUE": "Blue", "YELLOW": "Yellow", "WHITE": "White", "BROWN": "Brown", "GREEN": "Green", "RED": "Red",
        "BLACK": "Black", "PINK": "Pink"}


def font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--beds", required=True)
    ap.add_argument("--renders", required=True)
    ap.add_argument("--slice", default="", help="json {bed: {time, filament_g:[..]}} to print under the legend")
    a = ap.parse_args()
    plan = json.load(open(os.path.join(a.beds, "plan.json")))
    fp.COLOR_HEX.update(plan.get("hex", {}))
    sl = json.load(open(a.slice)) if a.slice and os.path.exists(a.slice) else {}
    for b in plan["beds"]:
        n = b["bed"]
        raw = Image.open(os.path.join(a.renders, f"_bed{n}_raw.png")).convert("RGB")
        W, H = raw.size
        crop = raw.crop((int(W * 0.04), int(H * 0.04), int(W * 0.96), int(H * 0.96)))
        s = crop.size[0] / 700.0
        side = int(300 * s)
        out = Image.new("RGB", (crop.size[0] + side, crop.size[1]), "#15171C")
        out.paste(crop, (0, 0))
        d = ImageDraw.Draw(out)
        x0 = crop.size[0] + int(20 * s)
        y = int(22 * s)
        d.text((x0, y), f"BED {n} / {len(plan['beds'])}", font=font(BOLD, int(30 * s)), fill="#FFFFFF")
        y += int(44 * s)
        d.text((x0, y), "256 x 256 mm  -  max 4 colours", font=font(FONT, int(14 * s)), fill="#9AA0AE")
        y += int(36 * s)
        d.text((x0, y), "FILAMENT SLOTS (AMS / MMU)", font=font(BOLD, int(13 * s)), fill="#C9CEDA")
        y += int(26 * s)
        for i, c in enumerate(b["colors"], 1):
            d.rounded_rectangle((x0, y, x0 + int(40 * s), y + int(32 * s)), radius=int(6 * s), fill=fp.COLOR_HEX[c], outline="#FFFFFF", width=2)
            d.text((x0 + int(52 * s), y + int(2 * s)), f"{i}  {NICE[c]}", font=font(BOLD, int(18 * s)), fill="#FFFFFF")
            y += int(42 * s)
        y += int(8 * s)
        d.text((x0, y), "PIECES ON THIS BED", font=font(BOLD, int(13 * s)), fill="#C9CEDA")
        y += int(24 * s)
        names = ", ".join(b["pieces"])
        # simple word wrap
        line, f = "", font(FONT, int(14 * s))
        for w in names.split(", "):
            t = (line + ", " if line else "") + w
            if d.textlength(t, font=f) > side - int(36 * s):
                d.text((x0, y), line + ",", font=f, fill="#E8EAF0")
                y += int(20 * s)
                line = w
            else:
                line = t
        d.text((x0, y), line, font=f, fill="#E8EAF0")
        y += int(34 * s)
        d.text((x0, y), f"Footprint {b['footprint_pct']} % of the bed", font=font(FONT, int(14 * s)), fill="#9AA0AE")
        y += int(24 * s)
        if str(n) in sl:
            d.text((x0, y), f"Est. time {sl[str(n)]['time']} (generic profile)", font=font(FONT, int(14 * s)), fill="#9AA0AE")
            y += int(22 * s)
            d.text((x0, y), "Filament (g, per slot): " + ", ".join(f"{float(v):.0f}" for v in sl[str(n)]["filament_g"]), font=font(FONT, int(14 * s)), fill="#9AA0AE")
            y += int(22 * s)
        d.text((x0, out.size[1] - int(50 * s)), "pale box = prime / wipe tower (keep free)", font=font(FONT, int(12 * s)), fill="#7C8294")
        d.text((x0, out.size[1] - int(30 * s)), "PIXELFORM family ornament", font=font(FONT, int(12 * s)), fill="#7C8294")
        out.save(os.path.join(a.renders, f"bed{n}.png"))
        print("   ->", os.path.join(a.renders, f"bed{n}.png"))


if __name__ == "__main__":
    main()
