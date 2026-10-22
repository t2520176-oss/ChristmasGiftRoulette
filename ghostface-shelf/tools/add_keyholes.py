#!/usr/bin/env python3
"""Cut wall-hanging keyholes into the BACK of a mesh (a Bambu Studio / PrusaSlicer 3MF project or an STL).

  python3 add_keyholes.py --in project.3mf --out project_keyholes.3mf [--stl out.stl] [--holes="-26,-18;26,-18"] [--search]

Coordinates are the ones of the mesh inside the 3MF (the shelf of "Meshy_AI_Ghostface_Corner_Shelf": x = width, y = depth,
z = up; the flat BACK is the plane y = min(y), the mask looks towards +y).  The shelf is printed lying on its back, so the
keyholes open towards the bed.

One keyhole (u, v = centre of the round part, the slot goes UP from it, because the piece is lowered onto the screws):

      back face (wall side)          pocket behind the lip                 roof
      +----------------+             +------------------+                tent, 40 deg: nothing flat to bridge
      |  O  round hole |  lip 2.4    |  stadium 9.4 wide |  4.0 deep      (a printer without support needs this)
      |  |  narrow slot|  mm         |                   |
      +----------------+             +-------------------+

  head of the screw  <= 9 mm  (M4 pan/round head, #8) - it enters through the round hole and sits in the pocket behind the lip
  shank of the screw <= 4.4 mm - it slides up the narrow slot (4.8 mm)
The pocket is 12 mm deep in total (lip 2.4 + pocket 4.0 + tent 5.6), so the material behind must be at least 15 mm thick:
use --search to let the script find such places (it tests, with the real mesh, that a cutter 3 mm bigger still stays inside).
"""
import argparse
import io
import re
import sys
import zipfile

import numpy as np
import trimesh
import manifold3d as m3d
from shapely.geometry import LineString, Point


def to_m(v, f):
    return m3d.Manifold(m3d.Mesh(vert_properties=np.asarray(v, dtype=np.float32), tri_verts=np.asarray(f, dtype=np.uint32)))


def from_m(man):
    mesh = man.to_mesh()
    return np.asarray(mesh.vert_properties, dtype=np.float64)[:, :3], np.asarray(mesh.tri_verts, dtype=np.int64)


def extrude(poly, y0, y1):
    """polygon in the (u, v) = (x, z) plane, extruded from y0 to y1 (towards +y)"""
    m = trimesh.creation.extrude_polygon(poly, y1 - y0)         # polygon in x, y; extruded along +z
    m.apply_transform(np.array([[1, 0, 0, 0], [0, 0, 1, y0], [0, 1, 0, 0], [0, 0, 0, 1]], dtype=float))   # (x, y, z) -> (x, z + y0, y)
    if m.volume < 0:
        m.invert()
    return m


def cone_ring(u, v, y, r, h, n=40):
    a = np.linspace(0, 2 * np.pi, n, endpoint=False)
    ring = np.stack([u + r * np.cos(a), np.full(n, y), v + r * np.sin(a)], 1)
    return ring, np.array([[u, y + h, v]])


def keyhole(u, v, yb, p):
    """the solid to subtract for one keyhole; yb = y of the back face"""
    r_head, w_slot, sl = p["head"] / 2, p["slot"], p["slot_len"]
    lip, pocket = p["lip"], p["pocket"]
    open_poly = Point(u, v).buffer(r_head, 32).union(LineString([(u, v), (u, v + sl)]).buffer(w_slot / 2, 16))
    pocket_poly = LineString([(u, v), (u, v + sl)]).buffer(r_head, 32)
    parts = [extrude(open_poly, yb - 1.0, yb + lip + 0.05), extrude(pocket_poly, yb + lip, yb + lip + pocket)]
    y_roof = yb + lip + pocket - 0.02
    h = r_head / p["roof_slope"]
    r1, a1 = cone_ring(u, v, y_roof, r_head, h)
    r2, a2 = cone_ring(u, v + sl, y_roof, r_head, h)
    parts.append(trimesh.convex.convex_hull(np.vstack([r1, a1, r2, a2])))
    man = None
    for t in parts:
        x = to_m(t.vertices, t.faces)
        man = x if man is None else man + x
    return man


def read_3mf(path):
    z = zipfile.ZipFile(path)
    names = z.namelist()
    obj = [n for n in names if n.startswith("3D/Objects/") and n.endswith(".model")][0]
    txt = z.read(obj).decode("utf-8")
    v = np.array(re.findall(r'<vertex x="([-\d.eE+]+)" y="([-\d.eE+]+)" z="([-\d.eE+]+)"', txt), dtype=np.float64)
    f = np.array(re.findall(r'<triangle v1="(\d+)" v2="(\d+)" v3="(\d+)"', txt), dtype=np.int64)
    return z, obj, txt, v, f


def write_3mf(src, dst, obj, txt, v, f):
    head = txt[:txt.index("<mesh>")]
    tail = txt[txt.index("</mesh>") + len("</mesh>"):]
    verts = "\n".join('     <vertex x="%.6f" y="%.6f" z="%.6f"/>' % tuple(p) for p in v)
    tris = "\n".join('     <triangle v1="%d" v2="%d" v3="%d"/>' % tuple(t) for t in f)
    new = head + "<mesh>\n    <vertices>\n" + verts + "\n    </vertices>\n    <triangles>\n" + tris + "\n    </triangles>\n   </mesh>" + tail
    zin = zipfile.ZipFile(src)
    with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == obj:
                data = new.encode("utf-8")
            elif item.filename == "Metadata/model_settings.config":
                s = data.decode("utf-8")
                s = re.sub(r'face_count="\d+"', 'face_count="%d"' % len(f), s)
                data = s.encode("utf-8")
            zout.writestr(item, data)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="src", required=True)
    ap.add_argument("--out", required=True, help="3MF to write")
    ap.add_argument("--stl", default="", help="also write an STL, standing the way the shelf is printed (back on the bed, centred)")
    ap.add_argument("--holes", default="", help='"u,v;u,v" centres of the round parts (mm, mesh coordinates)')
    ap.add_argument("--search", action="store_true", help="look for the best places (two keyholes side by side)")
    ap.add_argument("--head", type=float, default=9.4)
    ap.add_argument("--slot", type=float, default=4.8)
    ap.add_argument("--slot-len", type=float, default=8.0)
    ap.add_argument("--lip", type=float, default=2.4)
    ap.add_argument("--pocket", type=float, default=4.0)
    ap.add_argument("--roof-slope", type=float, default=0.84, help="tan of the lean of the tent roof (0.84 = 40 deg)")
    ap.add_argument("--margin", type=float, default=3.0, help="wall that must stay around every keyhole (mm)")
    a = ap.parse_args()
    p = dict(head=a.head, slot=a.slot, slot_len=a.slot_len, lip=a.lip, pocket=a.pocket, roof_slope=a.roof_slope)

    _, obj, txt, v, f = read_3mf(a.src)
    yb = float(v[:, 1].min())
    solid = to_m(v, f)
    depth = a.lip + a.pocket + (a.head / 2) / a.roof_slope
    print("mesh: %d vertices, %d triangles, back face y = %.3f, keyhole depth %.1f mm" % (len(v), len(f), yb, depth))
    inside = m3d.Manifold.cube([400, 400, 400], True).translate([0, yb + 0.8 + 200, 0])    # everything more than 0.8 mm in front of the back face (it is not perfectly flat)

    def fits(u, vv):
        """the keyhole, made `margin` bigger in every direction, must stay inside the solid"""
        k = keyhole(u, vv, yb, p)
        bb = k.bounding_box()
        c = [(bb[i] + bb[i + 3]) / 2 for i in range(3)]
        size = [bb[i + 3] - bb[i] for i in range(3)]
        big = k.translate([-c[0], -c[1], -c[2]]).scale([(size[i] + 2 * a.margin) / size[i] for i in range(3)]).translate(c)
        stray = (big ^ inside) - solid                       # part of the enlarged keyhole that is in front of the back and outside the solid
        return stray.volume() < 0.5

    if a.search:
        best = None
        for vv in np.arange(-6.0, -52.0, -2.0):
            for u in np.arange(14.0, 40.0, 2.0):
                if fits(-u, vv) and fits(u, vv):
                    best = (u, vv)
                    break
            if best:
                break
        if not best:
            sys.exit("no place found")
        print("best place: two keyholes at u = +-%.0f, v = %.0f" % best)
        holes = [(-best[0], best[1]), (best[0], best[1])]
    else:
        holes = [tuple(float(x) for x in h.split(",")) for h in a.holes.split(";") if h]
    cut = solid
    for (u, vv) in holes:
        ok = fits(u, vv)
        print("  keyhole at u=%.1f v=%.1f : %s" % (u, vv, "ok, >= %.0f mm of material around it" % a.margin if ok else "TOO CLOSE TO ANOTHER SURFACE"))
        cut = cut - keyhole(u, vv, yb, p)
    nv, nf = from_m(cut)
    out = trimesh.Trimesh(nv, nf, process=False)
    print("result: %d triangles, watertight %s, volume %.1f cm3 (was %.1f), bodies %d" % (len(nf), out.is_watertight, out.volume / 1000, solid.volume() / 1000,
                                                                                        len(out.split(only_watertight=False))))
    write_3mf(a.src, a.out, obj, txt, nv, nf)
    print("wrote", a.out)
    if a.stl:
        p3 = np.stack([nv[:, 0], -nv[:, 2], nv[:, 1]], 1)               # (x, y, z) -> (x, -z, y): the back face (y = min) lies on the bed
        p3[:, 2] -= p3[:, 2].min()
        p3[:, :2] -= (p3[:, :2].min(0) + p3[:, :2].max(0)) / 2
        trimesh.Trimesh(p3, nf, process=False).export(a.stl)
        print("wrote", a.stl, "(printing orientation: back face down)")


if __name__ == "__main__":
    main()
