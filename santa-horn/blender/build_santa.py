"""Santa sound amplifier - the Blender build (headless, Blender 4.x).

BPY_SITE=<dir with numpy> blender -b -P build_santa.py -- <out_dir> [voxel_mm] [detail=1]

Clay blobs (metaballs), tubes (spiral curls, twisted rope), instanced stitches, voxel remesh.
Writes   <out_dir>/parts/<role>__<name>.stl   (role = red | white | peach | black | cav)   and   <out_dir>/params.json
tools/assemble_santa.py turns the parts into the four printable colour regions (priority, sound path, self-supporting skirts).
"""
import json
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bl_lib import *  # noqa: E402,F401,F403
from bl_lib import _select_only  # noqa: E402
from bl_cavity import box_bm, cavities, roof_slope_deg, _loft  # noqa: E402
from bl_detail import *  # noqa: E402,F401,F403
from santa_params import P  # noqa: E402

args = sys.argv[sys.argv.index("--") + 1:]
out = args[0]
VOX = float(args[1]) if len(args) > 1 else 0.45
DETAIL = int(args[2]) if len(args) > 2 else 1
reset()
T0 = time.time()
parts = []
rng = np.random.default_rng(11)
POM_C, POM_R = (-45.0, 2.0, 69.0), 12.5


def keep(ob, name, vox=VOX, smooth=0):
    """remesh to a watertight, evenly meshed shell and register it for export"""
    ob.name = name
    remesh(ob, vox, smooth)
    parts.append(ob)
    print("  %-22s %s  (%.0fs)" % (name, stats(ob).split(": ")[1], time.time() - T0), flush=True)
    return ob


def bm_obj(bm, name):
    ob = obj_from_bm(bm, name)
    bm.free()
    return ob


# =====================================================================================================================
#  RED: body + hat dome + the collar behind the hat that carries the phone, with knitted stitches on the hat
# =====================================================================================================================
red = Blobs("red", 0.9)
red.ell((0, -4, 26), (46, 40, 28))                       # body
red.ell((0, -2, 73), (37, 34, 22), rot=(12, 0, 0))       # dome of the hat, leaning back
for x in (-28, -14, 0, 14, 28):
    red.ell((x, -21, 60), (16, 21, 24))                  # the collar behind the hat (flat across: the 74 mm slot is cut into it)
red_base = red.mesh()
red_base.name = "red_base"
if DETAIL:
    bvh = bvh_of(red_base)
    arms = knit_arms((0, -10, 56), bvh, 0, 0, row_h=3.0, col_w=3.5, rng=rng, psi_range=(6, 104), mask=lambda h, n: h[2] > 68)
    print("  knit: %d arms" % len(arms), flush=True)
    bm = bmesh.new()
    for c, ax, nr, semi in arms:
        ellipsoid_into(bm, c, ax, nr, semi, subdiv=2)
    stitches = bm_obj(bm, "stitches")
    keep(join([red_base, stitches], "red__body"), "red__body", vox=0.4)
    remove(red_base, stitches)
else:
    keep(red_base, "red__body")

# =====================================================================================================================
#  WHITE: beard with moustache and side burns (curls), the bell, the brim (rope), the pompom, the brows
# =====================================================================================================================
bd = Blobs("beard", 0.8)
bd.ell((0, 16, 22), (44, 33, 24))                        # beard
bd.ell((-16, 33, 46), (14, 10, 8), rot=(0, 0, 25))       # moustache
bd.ell((16, 33, 46), (14, 10, 8), rot=(0, 0, -25))
bd.ell((-38, 14, 45), (10, 13, 16))                      # side burns
bd.ell((38, 14, 45), (10, 13, 16))
beard_base = bd.mesh()
beard_base.name = "beard_base"
if DETAIL:
    v, t = mesh_np(beard_base)

    def beard_mask(p, n):
        x, y, z = p[:, 0], p[:, 1], p[:, 2]
        r_axis = np.hypot(x, z - P["mouth_z"])
        near_bell = (y > P["throat_y"] - 4) & (r_axis < P["mouth_R"] + P["lip_minor"] * 2 + 4.0)
        near_pom = np.linalg.norm(p - np.array(POM_C), axis=1) < POM_R + 6.0
        return (z < 62) & (n[:, 1] > -0.25) & ~near_bell & ~near_pom & (n[:, 2] < 0.85) & (p[:, 1] > -8)

    cand, cnrm = sample_surface(v, t, 60000, beard_mask, rng, oversample=1)
    bm = bmesh.new()
    # 1. soft lumps (the cloud): balls sunk into the surface
    lp, ln, ls = poisson_sizes(cand, cnrm, [7.5, 6.5, 5.5, 4.5], rng, gap=0.72, max_n=500)
    for p_, n_, s_ in zip(lp, ln, ls):
        ball_into(bm, p_ - n_ * 0.35 * s_, s_ * 1.0, 3)
    # 2. spiral curls on top
    pts, nrm, siz = poisson_sizes(cand, cnrm, [9.0, 8.0, 7.0, 6.0, 5.0, 4.2, 3.6], rng, gap=0.85, max_n=700)
    print("  beard: %d lumps, %d curls" % (len(lp), len(pts)), flush=True)
    for p_, n_, s_ in zip(pts, nrm, siz):
        curl_into(bm, p_, n_, s_, rng, tube=0.34)
    curls = bm_obj(bm, "curls")
    beard_o = join([beard_base, curls], "white__beard")
    keep(beard_o, "white__beard", vox=0.42)
    remove(beard_base, curls)
else:
    keep(beard_base, "white__beard")

# the bell: an oblique cone (shell round the funnel) + the lip (torus)
ns, na = 14, 72
y0, y1 = P["throat_y"] - 6, P["mouth_y"]
rings = []
for y in [y0 + (y1 - y0) * i / (ns - 1) for i in range(ns)]:
    s = (y - P["throat_y"]) / (P["mouth_y"] - P["throat_y"])
    zc = P["throat_z"] + s * (P["mouth_z"] - P["throat_z"])
    r = P["throat_r"] + s * (P["mouth_R"] - P["throat_r"]) + 5.5
    rings.append([(r * math.cos(2 * math.pi * k / na), y, zc + r * math.sin(2 * math.pi * k / na)) for k in range(na)])
bell = obj_from_bm(_loft(rings), "bell")
lip_R = P["mouth_R"] + P["lip_minor"]
lip = obj_from_bm(torus_bm((0, P["mouth_y"], P["mouth_z"]), lip_R, P["lip_minor"], axis=(0, 1, 0), nu=120, nv=28), "lip")
keep(join([bell, lip], "white__bell"), "white__bell", vox=0.4)
remove(bell, lip)

# the brim: a twisted rope round the hat, tilted (lower in front)
if DETAIL:
    def ring_path(s):
        a = 2 * math.pi * s
        return np.array([40 * math.cos(a), 2 + 40 * math.sin(a), 78.5 - 4 * math.sin(a)])

    bm = bmesh.new()
    rope_into(bm, ring_path, n_path=300, strands=3, twists=16, off=2.9, tube=3.8, core=5.0, nv=10)
    keep(bm_obj(bm, "brim"), "white__brim", vox=0.4)
else:
    bm = Blobs("brim", 0.7)
    for k in range(44):
        a = 2 * math.pi * k / 44
        bm.ball((40 * math.cos(a), 2 + 40 * math.sin(a), 78.5 - 4 * math.sin(a)), 5.6)
    keep(bm.mesh(), "white__brim")

# the pompom: a popcorn ball
if DETAIL:
    bm = bmesh.new()
    popcorn_into(bm, POM_C, POM_R, 4.8, 36, rng)
    keep(bm_obj(bm, "pompom"), "white__pompom", vox=0.4)
else:
    pp = Blobs("pompom", 0.6)
    pp.ball(POM_C, POM_R)
    keep(pp.mesh(), "white__pompom")

# =====================================================================================================================
#  PEACH: face, nose, cheeks, buckle     BLACK: eyes, boots, belt
# =====================================================================================================================
pe = Blobs("peach", 0.7)
pe.ell((0, 18, 58), (30, 26, 22))
face = pe.mesh()
face.name = "face_tmp"
bvh_face = bvh_of(face)


def on_face(x, z, lift=0.0):
    """point of the face surface in front of (x, z) and its normal (ray from the front)"""
    hit, nrm, _, _ = bvh_face.ray_cast(Vector((x, 120, z)), Vector((0, -1, 0)), 300.0)
    return np.array(hit) + np.array(nrm) * lift, np.array(nrm)


keep(face, "peach__face", vox=0.5)

# nose
nz_p, _ = on_face(0, 54.5)
bm = bmesh.new()
ball_into(bm, nz_p + np.array([0, 0.6, 0]), 6.3, 4)
keep(bm_obj(bm, "nose"), "peach__feat_nose", vox=0.4)
# cheeks
bm = bmesh.new()
for sx in (-1, 1):
    c_, n_ = on_face(sx * 22, 49.0)
    ball_into(bm, c_ - n_ * 0.8, 7.6, 4)
keep(bm_obj(bm, "cheeks"), "peach__feat_cheeks", vox=0.45)

# belt (a band round the body, black) and its buckle (peach) at the back
body_c = (0, -4, 26)
bb = ico_bm(body_c, (46 + 1.6, 40 + 1.6, 28 + 1.6), subdiv=6)
belt_src = obj_from_bm(bb, "belt_src")
bb.free()
slab = obj_from_bm(box_bm(-60, 60, -60, 60, 12.0, 19.5), "belt_slab")
_select_only(belt_src)
mod = belt_src.modifiers.new("b", "BOOLEAN")
mod.operation = "INTERSECT"
mod.object = slab
mod.solver = "EXACT"
bpy.ops.object.modifier_apply(modifier=mod.name)
remove(slab)
# a band, not a disc: take the body out again (the belt is a 2.2 mm thick shell round the red body)
core_bm = ico_bm(body_c, (46 - 0.6, 40 - 0.6, 28 - 0.6), subdiv=6)
core = obj_from_bm(core_bm, "belt_core")
core_bm.free()
_select_only(belt_src)
mod = belt_src.modifiers.new("b2", "BOOLEAN")
mod.operation = "DIFFERENCE"
mod.object = core
mod.solver = "EXACT"
bpy.ops.object.modifier_apply(modifier=mod.name)
remove(core)
keep(belt_src, "black__belt", vox=0.4)

y_back = body_c[1] - (40 + 1.6) * math.sqrt(1 - ((16 - 26) / 29.6) ** 2)
outer = obj_from_bm(box_bm(-8.5, 8.5, y_back - 1.4, y_back + 2.0, 10.5, 21.5), "bk_o")
inner = obj_from_bm(box_bm(-5.4, 5.4, y_back - 3, y_back + 4, 13.2, 18.8), "bk_i")
_select_only(outer)
mod = outer.modifiers.new("b", "BOOLEAN")
mod.operation = "DIFFERENCE"
mod.object = inner
mod.solver = "EXACT"
bpy.ops.object.modifier_apply(modifier=mod.name)
remove(inner)
keep(outer, "peach__feat_buckle", vox=0.4)

# eyes: black beads, brows: white fluffy arcs
bm = bmesh.new()
for sx in (-1, 1):
    c_, n_ = on_face(sx * 12.5, 59.5)
    ball_into(bm, c_ + n_ * 1.0, 4.2, 3)
keep(bm_obj(bm, "eyes"), "black__eyes", vox=0.35)

bm = bmesh.new()
for sx in (-1, 1):
    pts_b, rad_b = [], []
    for t_ in np.linspace(-1, 1, 14):
        x_ = sx * (14.0 + 5.4 * t_)
        z_ = 65.2 + 1.3 * (1 - t_ ** 2) + 1.2 * t_
        c_, n_ = on_face(x_, z_)
        pts_b.append(c_ + n_ * 0.9)
        rad_b.append(2.2 * (1 - 0.55 * abs(t_) ** 2))
    tube_into(bm, pts_b, rad_b, nv=10)
keep(bm_obj(bm, "brows"), "white__brows", vox=0.35)

# boots
bk = Blobs("boots", 0.7)
bk.ell((-35, 30, 12), (16, 20, 14), rot=(0, 0, -10))
bk.ell((35, 30, 12), (16, 20, 14), rot=(0, 0, 10))
keep(bk.mesh(), "black__boots", vox=0.5)

# =====================================================================================================================
#  the sound path
# =====================================================================================================================
cav = cavities(P)
print("roof slope of the funnel: %.1f deg" % roof_slope_deg(P))

os.makedirs(os.path.join(out, "parts"), exist_ok=True)
for ob in parts:
    export_stl(ob, os.path.join(out, "parts", ob.name + ".stl"))
for ob in cav:
    export_stl(ob, os.path.join(out, "parts", "cav__" + ob.name.replace("cav_", "") + ".stl"))
json.dump(P, open(os.path.join(out, "params.json"), "w"), indent=1)
print("BUILD DONE %.0fs" % (time.time() - T0))
