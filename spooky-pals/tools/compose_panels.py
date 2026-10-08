#!/usr/bin/env python3
"""Compose the 'build your own' option panel (pets / costumes / bases / name / colours) from the three
Blender panel renders.  Pure Pillow, no fonts besides DejaVu.

  python3 compose_panels.py <tmp dir with panel_pets.png panel_costumes.png panel_bases.png> <out.png>
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

CREAM = (252, 240, 222)
ORANGE = (237, 134, 54)
BROWN = (74, 44, 28)
FONTS = ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]


def font(size, bold=True):
    for f in FONTS if bold else FONTS[::-1]:
        if os.path.exists(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default()


def rounded(draw, box, r, fill):
    draw.rounded_rectangle(box, r, fill=fill)


def fit(img, w, h):
    img = img.copy()
    img.thumbnail((w, h), Image.LANCZOS)
    return img


def main(tmp, out):
    pets = Image.open(os.path.join(tmp, "panel_pets.png")).convert("RGB")
    cost = Image.open(os.path.join(tmp, "panel_costumes.png")).convert("RGB")
    base = Image.open(os.path.join(tmp, "panel_bases.png")).convert("RGB")
    W, H = 2400, 1700
    im = Image.new("RGB", (W, H), CREAM)
    d = ImageDraw.Draw(im)
    d.text((W // 2, 70), "SPOOKY PALS  -  build your own", font=font(78), fill=BROWN, anchor="mm")
    d.text((W // 2, 140), "6 pets  x  6 costumes  x  3 bases  =  108 combinations, one parametric OpenSCAD file",
           font=font(36, False), fill=(120, 80, 50), anchor="mm")
    # three big boxes
    boxes = [("1.  Choose a pet", pets, (40, 190, 810, 1010)), ("2.  Choose a costume", cost, (830, 190, 1600, 1010))]
    for title, img, (x0, y0, x1, y1) in boxes:
        rounded(d, (x0, y0, x1, y1), 36, (255, 249, 238))
        d.rounded_rectangle((x0, y0, x1, y0 + 70), 36, fill=(250, 221, 190))
        d.text((x0 + 30, y0 + 35), title, font=font(40), fill=BROWN, anchor="lm")
        f = fit(img, x1 - x0 - 30, y1 - y0 - 100)
        im.paste(f, (x0 + (x1 - x0 - f.width) // 2, y0 + 85))
    # bases
    x0, y0, x1, y1 = 1620, 190, 2360, 1010
    rounded(d, (x0, y0, x1, y1), 36, (255, 249, 238))
    d.rounded_rectangle((x0, y0, x1, y0 + 70), 36, fill=(250, 221, 190))
    d.text((x0 + 30, y0 + 35), "3.  Choose a base", font=font(40), fill=BROWN, anchor="lm")
    f = base.rotate(0)
    f = fit(f, x1 - x0 - 30, 330)
    im.paste(f, (x0 + 15, y0 + 100))
    for i, t in enumerate(["Pumpkin-patch stump", "Haunted crypt", "Witch cauldron"]):
        d.text((x0 + 40, y0 + 470 + i * 60), f"{i + 1}  {t}", font=font(34, False), fill=BROWN, anchor="lm")
    d.text((x0 + 40, y0 + 690), "all three share the same name board,\nwedge ramp and pet socket", font=font(30, False),
           fill=(130, 90, 60), anchor="lm")
    # name box
    x0, y0, x1, y1 = 40, 1040, 1600, 1660
    rounded(d, (x0, y0, x1, y1), 36, (255, 249, 238))
    d.rounded_rectangle((x0, y0, x1, y0 + 70), 36, fill=(250, 221, 190))
    d.text((x0 + 30, y0 + 35), "4.  Add a name   (30 degree ramp, no supports)", font=font(40), fill=BROWN, anchor="lm")
    # demonstrate with a text plate drawn in pillow: wood board + letters
    bx0, by0, bx1, by1 = x0 + 120, y0 + 140, x1 - 120, y0 + 420
    d.rounded_rectangle((bx0, by0, bx1, by1), 40, fill=(122, 82, 52))
    d.rounded_rectangle((bx0 + 24, by0 + 24, bx1 - 24, by1 - 24), 30, outline=(243, 226, 192), width=10)
    d.text(((bx0 + bx1) // 2, (by0 + by1) // 2), "YOUR NAME", font=font(150), fill=(243, 226, 192), anchor="mm")
    d.text((x0 + 80, y0 + 520), "name = \"MOCHI\";   raise_text = true;   // or engraved",
           font=font(36, False), fill=(110, 70, 40), anchor="lm")
    # colours
    x0, y0, x1, y1 = 1620, 1040, 2360, 1660
    rounded(d, (x0, y0, x1, y1), 36, (255, 249, 238))
    d.rounded_rectangle((x0, y0, x1, y0 + 70), 36, fill=(250, 221, 190))
    d.text((x0 + 30, y0 + 35), "5.  Colours  &  multicolor mode", font=font(40), fill=BROWN, anchor="lm")
    cols = [(237, 134, 54), (126, 63, 160), (35, 31, 38), (245, 241, 234), (107, 74, 51), (192, 36, 46), (79, 138, 59), (242, 194, 48)]
    for i, c in enumerate(cols):
        cx, cy = x0 + 110 + (i % 4) * 150, y0 + 190 + (i // 4) * 150
        d.ellipse((cx - 55, cy - 55, cx + 55, cy + 55), fill=c, outline=(90, 60, 40), width=4)
    d.text((x0 + 40, y0 + 470), "multicolor = true  splits every set into\n11 colour parts: fur, markings, eyes, shine,\nhat, trim, cape, charm, base, decor, name",
           font=font(30, False), fill=BROWN, anchor="lm")
    im.save(out)
    print("wrote", out)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
