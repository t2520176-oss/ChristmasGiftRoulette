"""Macro forms of the Santa (clay blobs) - proportions only, to be compared with the photo.

BPY_SITE=<dir with numpy> blender -b -P build_macro.py -- <out_dir>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bl_lib import *  # noqa: E402,F401,F403

out = sys.argv[sys.argv.index("--") + 1]
reset()
parts = []

# ---- RED: body + hat dome + the collar behind the hat that carries the phone -----------------------------------------
red = Blobs("red", 0.9)
red.ell((0, -6, 26), (46, 42, 28))                       # body
red.ell((0, -4, 66), (37, 34, 22), rot=(12, 0, 0))       # dome of the hat, leaning back
for x in (-28, -14, 0, 14, 28):
    red.ell((x, -27, 62), (16, 22, 24))                  # the collar behind the hat
r = red.mesh(); r.name = "red__body"; parts.append(r)

# ---- WHITE: beard (with moustache and side burns) ---------------------------------------------------------------------
bd = Blobs("beard", 0.8)
bd.ell((0, 14, 24), (46, 34, 24))                        # beard
bd.ell((-16, 33, 47), (14, 10, 8), rot=(0, 0, 25))       # moustache
bd.ell((16, 33, 47), (14, 10, 8), rot=(0, 0, -25))
bd.ell((-38, 14, 44), (10, 13, 15))                      # side burns
bd.ell((38, 14, 44), (10, 13, 15))
w = bd.mesh(); w.name = "white__beard"; parts.append(w)

# brim: a rope of balls around the hat, tilted (lower in front)
bm = Blobs("brim", 0.7)
for k in range(44):
    a = 2 * math.pi * k / 44
    bm.ball((40 * math.cos(a), 2 + 40 * math.sin(a), 74 - 4 * math.sin(a)), 5.6)
br = bm.mesh(); br.name = "white__brim"; parts.append(br)

pp = Blobs("pompom", 0.6)
pp.ball((38, 6, 66), 11.0)
pm = pp.mesh(); pm.name = "white__pompom"; parts.append(pm)

# the lip of the bell: a torus, axis along y
lip = obj_from_bm(torus_bm((0, 50, 24), 24.0, 3.2, axis=(0, 1, 0)), "white__lip"); parts.append(lip)

# ---- PEACH: face, nose, cheeks ----------------------------------------------------------------------------------------
pe = Blobs("peach", 0.7)
pe.ell((0, 16, 56), (30, 26, 22))
pe.ball((0, 43, 52), 6.0)
pe.ball((-22, 37, 47), 6.5)
pe.ball((22, 37, 47), 6.5)
p = pe.mesh(); p.name = "peach__face"; parts.append(p)

# ---- BLACK: eyes + boots ---------------------------------------------------------------------------------------------
bk = Blobs("black", 0.7)
bk.ball((-12.5, 40.2, 57.5), 3.6)
bk.ball((12.5, 40.2, 57.5), 3.6)
bk.ell((-35, 30, 11), (15, 19, 13), rot=(0, 0, -10))
bk.ell((35, 30, 11), (15, 19, 13), rot=(0, 0, 10))
b = bk.mesh(); b.name = "black__boots"; parts.append(b)

for ob in parts:
    print(stats(ob))
    export_stl(ob, os.path.join(out, ob.name + ".stl"))
print("MACRO DONE")
