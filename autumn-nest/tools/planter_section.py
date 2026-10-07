#!/usr/bin/env python3
"""Cross-section drawing of the Stump Planter (entry #8), cut from the real STL files.

    python planter_section.py 08-bark-planter section.png [--pot-z 38]

The plane goes through the middle of a filling slot (150 degrees, right side of the picture, with the dipstick)
and through the middle of a tab (330 degrees, left side).
"""
import argparse
import os

import matplotlib
import numpy as np
import trimesh

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
from matplotlib.patches import Polygon as MPoly, Rectangle  # noqa: E402


def cut(mesh, ang_deg, dz=0.0):
    a = np.radians(ang_deg)
    c, s = np.cos(a), np.sin(a)
    sec = mesh.section(plane_origin=[0, 0, 0], plane_normal=[s, -c, 0])
    T = np.array([[c, s, 0, 0], [0, 0, 1, dz], [s, -c, 0, 0], [0, 0, 0, 1]])
    p2d, _ = sec.to_2D(to_2D=T)
    return list(p2d.polygons_full)


def draw(ax, polys, face, edge="#222", lw=0.6, z=2):
    for p in polys:
        ext = np.asarray(p.exterior.coords)
        ax.add_patch(MPoly(ext, closed=True, fc=face, ec=edge, lw=lw, zorder=z))
        for h in p.interiors:
            ax.add_patch(MPoly(np.asarray(h.coords), closed=True, fc="white", ec=edge, lw=lw, zorder=z + 0.1))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("out")
    ap.add_argument("--pot-z", type=float, default=38.0)
    ap.add_argument("--ri", type=float, default=47.0)
    ap.add_argument("--max-water", type=float, default=30.0)
    ap.add_argument("--soil-z", type=float, default=60.0)
    a = ap.parse_args()
    L = lambda n: trimesh.load(os.path.join(a.folder, "stl", n + ".stl"), force="mesh")
    shell, pot, stick = L("shell"), L("pot"), L("dipstick_asm")

    fig, ax = plt.subplots(figsize=(10.5, 6.6), dpi=130)
    ax.set_aspect("equal")
    # -- water and soil first
    ax.add_patch(Rectangle((-a.ri, 3), 2 * a.ri, a.max_water - 3, fc="#9ecbea", ec="none", zorder=1))
    ax.add_patch(Rectangle((-39.6, a.pot_z + 3), 2 * 39.6, a.soil_z - 3, fc="#5a3b25", ec="none", zorder=1))
    # -- parts: one cut plane; +u is the 150 degree side (slot), -u the 330 degree side (tab)
    draw(ax, cut(shell, 150), "#b08258")
    draw(ax, cut(pot, 150, a.pot_z), "#e0742f")
    draw(ax, cut(stick, 150), "#4d9a3c", lw=0.4, z=3)
    # -- wick (a cotton rope through the hole at r = 15 on the right side)
    t = np.linspace(0, 1, 60)
    wx = 15 + 2.2 * np.sin(6 * t * np.pi) * (1 - t) + 1.0
    wz = a.pot_z + 52 - t * (a.pot_z + 52 - 6)
    ax.plot(wx, wz, color="#efe2c0", lw=3.2, solid_capstyle="round", zorder=2.6)
    ax.plot(wx, wz, color="#8c7b4f", lw=0.6, zorder=2.7)
    # -- water level line
    ax.plot([-a.ri, a.ri], [a.max_water] * 2, color="#1b6fa8", lw=1.2, ls=(0, (5, 3)), zorder=4)

    def note(x, z, text, tx, tz, color="#111", ha="left"):
        ax.annotate(text, xy=(x, z), xytext=(tx, tz), fontsize=8.2, color=color, ha=ha, va="center",
                    arrowprops=dict(arrowstyle="-", color=color, lw=0.7), zorder=6)

    note(-a.ri + 6, 16, "reservoir: 187 mL up to the overflow hole", -142, 20, "#1b6fa8")
    note(-30, a.max_water, "highest water level (bottom of the\noverflow hole, 30 mm)", -142, 40, "#1b6fa8")
    note(-20, a.pot_z - 1, "air gap: 8 mm between the highest\nwater and the pot floor", -142, 62, "#444")
    note(a.ri + 1, 12, "outer pot: watertight,\n3.2 mm wall + bark relief", 62, 12)
    note(-48.5, 88, "tab: a 45\u00b0 cone resting on the\nrim edge, centres the pot", -142, 100, "#b34a00")
    note(44.5, 80, "filling slot 5 mm\n(pour here, or drip from a syringe)", 62, 76, "#444")
    note(44.6, 120, "dipstick with MAX / LOW grooves", 62, 122, "#2f6e22")
    note(26, a.pot_z + 1.5, "pot floor: wick holes \u00d86.5 (x3),\ndrain holes \u00d83 (x6)", 62, 44, "#444")
    note(16, 84, "cotton wick\n(rope \u00d84-6 mm)", 22, 66, "#f4e7c3")
    note(-20, a.pot_z + 52, "soil, about 280 mL", -36, a.pot_z + 40, "#f2d6b8")
    ax.set_xlim(-150, 135)
    ax.set_ylim(-6, 140)
    ax.set_xlabel("mm  (left: through a tab, right: through a filling slot)")
    ax.set_ylabel("mm above the floor of the outer pot")
    ax.set_title("Stump Planter - section", fontsize=11)
    ax.grid(alpha=0.15)
    fig.tight_layout()
    fig.savefig(a.out)
    print("wrote", a.out)


if __name__ == "__main__":
    main()
