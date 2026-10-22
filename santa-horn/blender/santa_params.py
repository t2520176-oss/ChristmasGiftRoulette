"""Shared dimensions of the Santa sound amplifier (mm).  x = right, y = front (the beard), z = up, floor z = 0.

The Blender build (build_santa.py) and the Python assembly (tools/assemble_santa.py, check_airpath.py, render_scene.py)
read the same numbers: build_santa.py writes them to  <out>/params.json.
"""
import math

P = dict(
    # ---- phone ----
    phone_w=74.0, phone_t=12.0,           # the slot: 74 x 12 mm (iPhone 13: 71.5 x 7.65 - bare or in a thin case)
    Y_S=-20.0,                            # centre of the slot in y
    Z_TOP=84.0,                           # the top of the collar (the slot opens here)
    slot_d=46.0,                          # depth of the slot
    CH_W=50.0,                            # width of the chamber under the phone
    HZ0=7.0,                             # floor of chamber and tunnel
    # ---- sound path: chamber -> A-frame tunnel -> funnel (the bell) ----
    tun_r=8.0,                            # radius of the round part of the tunnel (16 mm wide)
    tun_zc=15.0,                          # its centre height
    tun_theta=28.0,                       # half angle of the pointed top (degrees from the vertical)
    mouth_y=51.0, mouth_z=22.0, mouth_R=15.5,       # the mouth of the bell (a circle in a plane y = const)
    throat_y=39.5, throat_z=10.0, throat_r=6.0,     # where the funnel ends and the tunnel begins
    lip_minor=3.6,                        # the lip around the mouth is a torus of this tube radius (its inner edge = the mouth)
)
P["Z_SF"] = P["Z_TOP"] - P["slot_d"]      # floor of the slot: the phone's bottom edge rests here
