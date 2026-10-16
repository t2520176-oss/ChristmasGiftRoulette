#!/usr/bin/env python3
"""Plan the print beds of the family ornament.

  * every piece (all colours of one piece must stay together) is assigned to a bed
  * a bed uses at most --max-colors filaments (AMS / MMU: 4).  When a piece needs a colour that does
    not fit any more, it goes to the next bed.
  * pieces are packed flat on the bed (rotation by 90 deg allowed); a corner is kept free for the
    prime / wipe tower
  * output per bed: placed STLs (one per piece and colour), a 3MF in which every colour region is a
    separate volume with its extruder number, a json description (for the Blender previews)

  python3 plan_beds.py --print-dir ../stl/print --out ../beds
"""
import argparse
import itertools
import json
import os
import sys
import zipfile

import numpy as np
import trimesh

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402


# ----------------------------------------------------------------------------------------
class Piece:
    def __init__(self, name, roles, members, overrides):
        self.name = name
        self.vols = []                         # (role, color, mesh)
        for role, path in roles:
            m = trimesh.load(path, force="mesh")
            self.vols.append((role, fp.role_color(name, role, members, overrides), m))
        self.colors = {c for _, c, _ in self.vols}
        allv = np.vstack([m.vertices for _, _, m in self.vols])
        self.min = allv.min(0)
        self.max = allv.max(0)
        self.w = float(self.max[0] - self.min[0])
        self.h = float(self.max[1] - self.min[1])
        self.volume = sum(m.volume for _, _, m in self.vols)

    @property
    def area(self):
        return self.w * self.h


class MaxRects:
    """MaxRects free-rectangle packer (best short side fit), rotation allowed."""

    def __init__(self, w, h, blocked=()):
        self.free = [(0.0, 0.0, w, h)]
        for b in blocked:
            self._cut(b)

    @staticmethod
    def _overlap(a, b):
        return not (a[0] + a[2] <= b[0] or b[0] + b[2] <= a[0] or a[1] + a[3] <= b[1] or b[1] + b[3] <= a[1])

    def _cut(self, r):
        out = []
        for f in self.free:
            if not self._overlap(f, r):
                out.append(f)
                continue
            fx, fy, fw, fh = f
            rx, ry, rw, rh = r
            if rx > fx:
                out.append((fx, fy, rx - fx, fh))
            if rx + rw < fx + fw:
                out.append((rx + rw, fy, fx + fw - rx - rw, fh))
            if ry > fy:
                out.append((fx, fy, fw, ry - fy))
            if ry + rh < fy + fh:
                out.append((fx, ry + rh, fw, fy + fh - ry - rh))
        # drop rectangles contained in another one
        keep = []
        for i, a in enumerate(out):
            if a[2] < 1e-6 or a[3] < 1e-6:
                continue
            if any(j != i and b[0] <= a[0] + 1e-9 and b[1] <= a[1] + 1e-9 and b[0] + b[2] >= a[0] + a[2] - 1e-9
                   and b[1] + b[3] >= a[1] + a[3] - 1e-9 and (b != a or j < i) for j, b in enumerate(out)):
                continue
            keep.append(a)
        self.free = keep

    def place(self, w, h):
        best = None
        for rot, (pw, ph) in ((0, (w, h)), (1, (h, w))):
            for f in self.free:
                if pw <= f[2] + 1e-9 and ph <= f[3] + 1e-9:
                    score = (min(f[2] - pw, f[3] - ph), max(f[2] - pw, f[3] - ph))
                    if best is None or score < best[0]:
                        best = (score, f[0], f[1], pw, ph, rot)
        if best is None:
            return None
        _, x, y, pw, ph, rot = best
        self._cut((x, y, pw, ph))
        return x, y, rot, pw, ph


def try_pack(pieces, bed, margin, gap, tower):
    """Greedy packing; returns list of (piece, x, y, rot) for the pieces that fit."""
    bw, bh = bed
    usable = (bw - 2 * margin, bh - 2 * margin)
    blocked = []
    if tower[0] > 0 and tower[1] > 0:               # prime tower keep-out: back right corner
        blocked.append((usable[0] - tower[0], usable[1] - tower[1], tower[0], tower[1]))
    best = None
    orders = [sorted(pieces, key=lambda p: -p.area), sorted(pieces, key=lambda p: -max(p.w, p.h)),
              sorted(pieces, key=lambda p: -p.h)]
    for order in orders:
        mr = MaxRects(usable[0], usable[1], blocked)
        placed = []
        for p in order:
            r = mr.place(p.w + gap, p.h + gap)
            if r:
                x, y, rot, pw, ph = r
                placed.append((p, x + margin + gap / 2, y + margin + gap / 2, rot))
        score = sum(p.area for p, *_ in placed)
        if best is None or score > best[0]:
            best = (score, placed)
    return best[1]


def plan(pieces, bed, max_colors, margin, gap, tower):
    remaining = list(pieces)
    beds = []
    while remaining:
        colors = sorted({c for p in remaining for c in p.colors}, key=fp.ORDER.index)
        n = min(max_colors, len(colors))
        best = None
        for subset in itertools.combinations(colors, n):
            sub = set(subset)
            elig = [p for p in remaining if p.colors <= sub]
            if not elig:
                continue
            placed = try_pack(elig, bed, margin, gap, tower)
            score = (sum(p.area for p, *_ in placed), len(placed))
            if best is None or score > best[0]:
                best = (score, subset, placed)
        if best is None or not best[2]:
            bad = [p.name for p in remaining if len(p.colors) > max_colors]
            sys.exit(f"cannot place {[p.name for p in remaining]}  (pieces with more than {max_colors} colours: {bad})")
        placed = best[2]
        used = sorted({c for p, *_ in placed for c in p.colors}, key=fp.ORDER.index)
        beds.append({"colors": used, "placed": placed})
        done = {p.name for p, *_ in placed}
        remaining = [p for p in remaining if p.name not in done]
    return beds


def balance(beds, bed, max_colors, margin, gap, tower):
    """Move pieces from fuller beds to emptier ones (never adding a 5th colour, never overfilling a bed) so
    that the print times of the beds are closer together.  Volume is used as the proxy for print time."""
    def vol(b):
        return sum(p.volume for p, *_ in b["placed"])

    def sq(bs):
        return sum(vol(b) ** 2 for b in bs)

    for _ in range(40):
        moved = False
        for src in sorted(beds, key=vol, reverse=True):
            for p in sorted((q for q, *_ in src["placed"]), key=lambda q: -q.volume):
                for dst in beds:
                    if dst is src:
                        continue
                    cols = set(dst["colors"]) | p.colors
                    if len(cols) > max_colors:
                        continue
                    new_dst = [q for q, *_ in dst["placed"]] + [p]
                    placed = try_pack(new_dst, bed, margin, gap, tower)
                    if len(placed) != len(new_dst):
                        continue
                    old = sq(beds)
                    src_left = [t for t in src["placed"] if t[0] is not p]
                    trial = [dict(src, placed=src_left), dict(dst, placed=placed)] + [b for b in beds if b is not src and b is not dst]
                    if sq(trial) < old - 1e-6:
                        src["placed"] = try_pack([t[0] for t in src_left], bed, margin, gap, tower) if src_left else []
                        dst["placed"] = placed
                        for b in (src, dst):
                            b["colors"] = sorted({c for q, *_ in b["placed"] for c in q.colors}, key=fp.ORDER.index)
                        moved = True
                        break
                if moved:
                    break
            if moved:
                break
        if not moved:
            break
    return [b for b in beds if b["placed"]]


# ----------------------------------------------------------------------------------------
def transform_piece(p, x, y, rot):
    """Return [(role, color, mesh)] placed with the lower-left corner of its footprint at (x, y)."""
    out = []
    ang = np.pi / 2 if rot else 0.0
    R = trimesh.transformations.rotation_matrix(ang, [0, 0, 1])
    corners = np.array([[p.min[0], p.min[1], 0], [p.max[0], p.min[1], 0], [p.min[0], p.max[1], 0], [p.max[0], p.max[1], 0]])
    rc = (R[:3, :3] @ corners.T).T
    shift = np.array([x - rc[:, 0].min(), y - rc[:, 1].min(), 0.0])
    for role, color, m in p.vols:
        mm = m.copy()
        mm.apply_transform(R)
        mm.apply_translation(shift)
        out.append((role, color, mm))
    return out


def write_3mf(path, volumes, slots, bed, name, tower, margin):
    """PrusaSlicer-style 3MF: one object, one volume per (piece, colour), extruder = slot of the colour."""
    verts, tris, ranges = [], [], []
    vo = 0
    t0 = 0
    for vname, color, m in volumes:
        verts.append(m.vertices)
        tris.append(m.faces + vo)
        ranges.append((vname, color, t0, t0 + len(m.faces) - 1))
        vo += len(m.vertices)
        t0 += len(m.faces)
    V = np.vstack(verts)
    T = np.vstack(tris)
    # object origin = centre of the footprint; the build item carries the translation, so slicers that re-centre
    # the object around its origin (PrusaSlicer, Bambu Studio, OrcaSlicer do) keep every piece where it was planned
    cx, cy = (V[:, 0].min() + V[:, 0].max()) / 2, (V[:, 1].min() + V[:, 1].max()) / 2
    V = V - np.array([cx, cy, 0.0])
    ns = "http://schemas.microsoft.com/3dmanufacturing/core/2015/02"
    model = ['<?xml version="1.0" encoding="UTF-8"?>',
             f'<model unit="millimeter" xml:lang="en-US" xmlns="{ns}" xmlns:slic3rpe="http://schemas.slic3r.org/3mf/2017/06">',
             ' <metadata name="slic3rpe:Version3mf">1</metadata>', ' <resources>', '  <object id="1" type="model">', '   <mesh>',
             '    <vertices>']
    model += [f'     <vertex x="{x:.4f}" y="{y:.4f}" z="{z:.4f}"/>' for x, y, z in V]
    model += ['    </vertices>', '    <triangles>']
    model += [f'     <triangle v1="{a}" v2="{b}" v3="{c}"/>' for a, b, c in T]
    model += ['    </triangles>', '   </mesh>', '  </object>', ' </resources>',
              ' <build>', f'  <item objectid="1" transform="1 0 0 0 1 0 0 0 1 {cx:.4f} {cy:.4f} 0" printable="1"/>', ' </build>', '</model>']
    cfg = ['<?xml version="1.0" encoding="UTF-8"?>', '<config>', ' <object id="1" instances_count="1">',
           f'  <metadata type="object" key="name" value="{name}"/>']
    for vname, color, a, b in ranges:
        cfg += [f'  <volume firstid="{a}" lastid="{b}">',
                f'   <metadata type="volume" key="name" value="{vname} [{color}]"/>',
                '   <metadata type="volume" key="volume_type" value="ModelPart"/>',
                '   <metadata type="volume" key="matrix" value="1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1"/>',
                f'   <metadata type="volume" key="extruder" value="{slots.index(color) + 1}"/>',
                '   <mesh edges_fixed="0" degenerate_facets="0" facets_removed="0" facets_reversed="0" backwards_edges="0"/>',
                '  </volume>']
    cfg += [' </object>', '</config>']
    hexes = ";".join(f'"{fp.COLOR_HEX[c]}"' for c in slots)
    n = len(slots)
    twid = tower[0] - 5.0                                    # wipe tower width (keep-out minus a 5 mm brim/clearance)
    tx = bed[0] - margin - tower[0] + 2.5                     # left edge of the tower
    ty = bed[1] - margin - tower[1]                           # front edge of the tower
    ini = ["; generated by plan_beds.py  (PrusaSlicer / OrcaSlicer / Bambu Studio can read this project)",
           f"; extruder_colour = {hexes}", f"; filament_colour = {';'.join(fp.COLOR_HEX[c] for c in slots)}",
           f"; bed_shape = 0x0,{bed[0]}x0,{bed[0]}x{bed[1]},0x{bed[1]}",
           f"; nozzle_diameter = {','.join(['0.4'] * n)}", "; wipe_tower = 1", f"; wipe_tower_x = {tx:.1f}",
           f"; wipe_tower_y = {ty:.1f}", f"; wipe_tower_width = {twid:.0f}", "; single_extruder_multi_material = 1"]
    ct = ('<?xml version="1.0" encoding="UTF-8"?>\n<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\n'
          ' <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\n'
          ' <Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>\n</Types>\n')
    rels = ('<?xml version="1.0" encoding="UTF-8"?>\n<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\n'
            ' <Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>\n'
            '</Relationships>\n')
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", ct)
        z.writestr("_rels/.rels", rels)
        z.writestr("3D/3dmodel.model", "\n".join(model))
        z.writestr("Metadata/Slic3r_PE_model.config", "\n".join(cfg))
        z.writestr("Metadata/Slic3r_PE.config", "\n".join(ini) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--print-dir", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--bed", type=float, nargs=2, default=[256, 256])
    ap.add_argument("--max-colors", type=int, default=4)
    ap.add_argument("--margin", type=float, default=4.0)
    ap.add_argument("--gap", type=float, default=5.0)
    ap.add_argument("--tower", type=float, nargs=2, default=[45.0, 70.0], metavar=("W", "D"),
                    help="width and depth of the back-right area kept free for the prime / wipe tower (0 0 = none)")
    ap.add_argument("--palette", default="", help="json {'piece-kind.role': 'COLOUR'} overrides")
    ap.add_argument("--groups", default="", help="json list of piece-name lists = one bed each (the rest is planned automatically)")
    ap.add_argument("--balance", action="store_true", help="even out the print time of the beds (default: fullest bed first, keeps pairs/groups together)")
    a = ap.parse_args()

    cfg = json.load(open(os.path.join(a.print_dir, "config.json")))
    manifest = json.load(open(os.path.join(a.print_dir, "manifest.json")))
    overrides = fp.load_overrides(a.palette)
    pieces = []
    for it in manifest:
        roles = [(r, os.path.join(a.print_dir, f"{it['piece']}__{r}.stl")) for r in it["roles"]]
        roles = [(r, p) for r, p in roles if os.path.exists(p)]
        pieces.append(Piece(it["piece"], roles, cfg["members"], overrides))
    beds = []
    if a.groups:                                      # hand-made beds: [["base", "sky", ...], ["fig0", ...]]
        by_name = {p.name: p for p in pieces}
        for gi, names in enumerate(json.load(open(a.groups)), 1):
            ps = [by_name.pop(n) for n in names]
            cols = sorted({c for p in ps for c in p.colors}, key=fp.ORDER.index)
            if len(cols) > a.max_colors:
                sys.exit(f"group {gi} needs {len(cols)} colours {cols} (max {a.max_colors})")
            placed = try_pack(ps, a.bed, a.margin, a.gap, a.tower)
            if len(placed) != len(ps):
                sys.exit(f"group {gi}: only {len(placed)} of {len(ps)} pieces fit on the bed")
            beds.append({"colors": cols, "placed": placed})
        pieces = list(by_name.values())               # pieces not named in any group are planned automatically
    if pieces:
        beds += plan(pieces, a.bed, a.max_colors, a.margin, a.gap, a.tower)
    if a.balance:
        beds = balance(beds, a.bed, a.max_colors, a.margin, a.gap, a.tower)

    os.makedirs(a.out, exist_ok=True)
    summary = []
    for bi, b in enumerate(beds, 1):
        slots = b["colors"]
        bdir = os.path.join(a.out, f"bed{bi}")
        os.makedirs(bdir, exist_ok=True)
        vols, desc = [], []
        for p, x, y, rot in b["placed"]:
            for role, color, m in transform_piece(p, x, y, rot):
                vols.append((f"{p.name}.{role}", color, m))
                fn = f"{p.name}__{role}__{color}.stl"
                m.export(os.path.join(bdir, fn))
                desc.append({"file": f"bed{bi}/{fn}", "piece": p.name, "role": role, "color": color,
                             "hex": fp.COLOR_HEX[color], "slot": slots.index(color) + 1})
        name = f"{cfg['family']} bed {bi}"
        write_3mf(os.path.join(a.out, f"bed{bi}.3mf"), vols, slots, a.bed, name, a.tower, a.margin)
        area = sum(p.area for p, *_ in b["placed"])
        mass = {c: 0.0 for c in slots}
        for _, c, m in vols:
            mass[c] += m.volume / 1000.0
        info = {"bed": bi, "colors": slots, "slots": {str(i + 1): c for i, c in enumerate(slots)},
                "pieces": sorted(p.name for p, *_ in b["placed"]), "footprint_pct": round(100 * area / (a.bed[0] * a.bed[1]), 1),
                "solid_volume_cm3": {c: round(v, 1) for c, v in mass.items()}, "volumes": desc}
        json.dump(info, open(os.path.join(a.out, f"bed{bi}.json"), "w"), indent=1)
        summary.append(info)
        print(f"bed {bi}: colours {slots}  pieces {len(info['pieces'])}  footprint {info['footprint_pct']}%")
        print("   ", ", ".join(info["pieces"]))
    used = sorted({c for s in summary for c in s["colors"]}, key=fp.ORDER.index)
    json.dump({"bed": a.bed, "max_colors": a.max_colors, "tower": a.tower, "hex": {c: fp.COLOR_HEX[c] for c in used},
               "overrides": overrides or {}, "beds": [{k: v for k, v in s.items() if k != 'volumes'} for s in summary]},
              open(os.path.join(a.out, "plan.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
