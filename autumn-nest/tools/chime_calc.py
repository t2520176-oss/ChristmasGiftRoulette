#!/usr/bin/env python3
"""Cut sheet and sanity checks for the Maple Chime (entry #6).

    python chime_calc.py [--od 12] [--wall 1] [--E 69] [--rho 2700]
                         [--notes C5,D5,E5,G5,A5] [--strike 0.42] [--pend 170]
                         [--gap 7] [--striker-g 6] [--sail-g 8] [--sail-cm2 43]

1. Tube length for each note from free-free beam theory (Euler-Bernoulli) and
   from a Timoshenko finite-element model (adds shear + rotary inertia, which
   lowers the pitch of short, fat tubes a little).
2. Validation: for a very slender tube (4 m) the shear/rotary corrections vanish, so the
   FE model must reproduce the textbook coefficient (beta*L)^2 = 22.373.
3. Wind estimate: steady wind speed at which the striker is pushed sideways by
   the rest gap (an order-of-magnitude estimate, NOT a simulation).
"""
import argparse
import math

import numpy as np
from scipy.linalg import eigh
from scipy.optimize import brentq

NOTE_SEMITONE = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
NODE_FRAC = 0.2241  # free-free fundamental: nodes at 22.41 % of the length from each end


def note_freq(name):
    letter, octave = name[0].upper(), int(name[-1])
    sharp = 1 if "#" in name else 0
    midi = 12 * (octave + 1) + NOTE_SEMITONE[letter] + sharp
    return 440.0 * 2 ** ((midi - 69) / 12)


def section(od, wall):
    d = od - 2 * wall
    area = math.pi / 4 * (od**2 - d**2) * 1e-6
    inertia = math.pi / 64 * (od**4 - d**4) * 1e-12
    return area, inertia


def eb_length(f, od, wall, E, rho):
    """Euler-Bernoulli free-free fundamental: f = (22.373 / 2 pi) * sqrt(EI / rho A) / L^2."""
    area, inertia = section(od, wall)
    k = 22.3733 / (2 * math.pi) * math.sqrt(E * inertia / (rho * area))
    return math.sqrt(k / f)


def fe_freq(L, od, wall, E, rho, n=400, nu=0.33, rotary=True, kappa=None):
    """Fundamental bending frequency of a free-free tube, Timoshenko beam FE (linear elements,
    reduced-integration shear to avoid locking)."""
    area, inertia = section(od, wall)
    G = E / (2 * (1 + nu))
    if kappa is None:
        kappa = 2 * (1 + nu) / (4 + 3 * nu)  # thin-walled circular tube
    ks = kappa * G * area
    le = L / n
    K = np.zeros((2 * (n + 1), 2 * (n + 1)))
    M = np.zeros_like(K)
    Kb = E * inertia / le * np.array([[0, 0, 0, 0], [0, 1, 0, -1], [0, 0, 0, 0], [0, -1, 0, 1]])
    B = np.array([-1 / le, -0.5, 1 / le, -0.5])
    Ks = ks * le * np.outer(B, B)
    mt = rho * area * le / 6 * np.array([[2, 1], [1, 2]])
    mr = rho * inertia * le / 6 * (1.0 if rotary else 1e-6) * np.array([[2, 1], [1, 2]])  # ~0: keeps M positive definite
    Me = np.zeros((4, 4))
    Me[np.ix_([0, 2], [0, 2])] = mt
    Me[np.ix_([1, 3], [1, 3])] = mr
    for e in range(n):
        idx = slice(2 * e, 2 * e + 4)
        K[idx, idx] += Kb + Ks
        M[idx, idx] += Me
    w2 = eigh(K, M, eigvals_only=True, subset_by_index=[0, 6])
    w2 = w2[w2 > 1e-3 * np.max(w2)]  # drop the two rigid-body modes
    return math.sqrt(w2[0]) / (2 * math.pi)


def timoshenko_length(f, od, wall, E, rho):
    guess = eb_length(f, od, wall, E, rho)
    return brentq(lambda L: fe_freq(L, od, wall, E, rho) - f, guess * 0.9, guess * 1.02, xtol=1e-7)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--od", type=float, default=12.0, help="tube outer diameter (mm)")
    ap.add_argument("--wall", type=float, default=1.0, help="tube wall thickness (mm)")
    ap.add_argument("--E", type=float, default=69.0, help="Young's modulus (GPa), 6063 aluminium ~69")
    ap.add_argument("--rho", type=float, default=2700.0, help="density (kg/m3)")
    ap.add_argument("--notes", default="C5,D5,E5,G5,A5")
    ap.add_argument("--strike", type=float, default=0.42, help="strike point, fraction of length from the top end")
    ap.add_argument("--pend", type=float, default=170.0, help="canopy underside to striker rim (mm)")
    ap.add_argument("--gap", type=float, default=7.0, help="rest gap striker rim - tube (mm)")
    ap.add_argument("--striker-g", type=float, default=6.0)
    ap.add_argument("--sail-g", type=float, default=8.0)
    ap.add_argument("--sail-cm2", type=float, default=43.0, help="sail area facing the wind (cm2)")
    a = ap.parse_args()
    E, rho = a.E * 1e9, a.rho

    # validation: for a very slender tube the Timoshenko corrections vanish -> must match Euler-Bernoulli
    L0 = 4.0
    area, inertia = section(a.od, a.wall)
    f_eb = 22.3733 / (2 * math.pi) * math.sqrt(E * inertia / (rho * area)) / L0**2
    f_fe = fe_freq(L0, a.od, a.wall, E, rho, n=400)
    print(f"validation (slender {L0:g} m tube)  textbook {f_eb:.3f} Hz vs FE {f_fe:.3f} Hz  "
          f"(diff {100*(f_fe/f_eb-1):+.3f} %)")

    print(f"\ntube {a.od:g} x {a.wall:g} mm, E={a.E:g} GPa, rho={a.rho:g}")
    print(f"{'note':>5} {'Hz':>7} {'L_EB mm':>8} {'L_Timo':>8} {'cut at':>7} {'node mm':>8} {'cord drop':>9}")
    rows = []
    for name in a.notes.split(","):
        f = note_freq(name)
        l_eb = eb_length(f, a.od, a.wall, E, rho) * 1000
        l_t = timoshenko_length(f, a.od, a.wall, E, rho) * 1000
        cut = l_t + 2.0  # trimming allowance
        node = NODE_FRAC * l_t
        drop = a.pend - (a.strike - NODE_FRAC) * l_t  # canopy underside -> node hole
        top_clear = a.pend - a.strike * l_t  # canopy underside -> top end of the tube
        rows.append((name, f, l_eb, l_t, cut, node, drop, top_clear))
        print(f"{name:>5} {f:7.1f} {l_eb:8.1f} {l_t:8.1f} {cut:7.1f} {node:8.1f} {drop:9.1f}")
    print("  L_Timo = length for the exact pitch; 'cut at' = L_Timo + 2 mm, then file the end in 0.5 mm steps")
    print("  node = distance from the top end of the tube to the cord hole (both ends are not drilled)")
    print("  cord drop = canopy underside to the cord hole")
    worst = min(r[7] for r in rows)
    print(f"  smallest clearance canopy -> top of a tube: {worst:.0f} mm "
          + ("(OK)" if worst >= 15 else "(TOO SMALL: increase --pend)"))
    lmax = max(r[3] for r in rows)
    bottom = a.pend + (1 - a.strike) * lmax
    print(f"  longest tube bottom is {bottom:.0f} mm below the canopy -> hang the sail >= {bottom + 25:.0f} mm below it")

    # wind estimate: the striker rim has to move `gap` sideways before it touches a tube
    rho_air, cd = 1.2, 1.2
    weight = (a.striker_g + a.sail_g) * 1e-3 * 9.81
    tan_t = a.gap / a.pend
    force = tan_t * weight
    area_m2 = a.sail_cm2 * 1e-4
    u = math.sqrt(2 * force / (rho_air * area_m2 * cd))
    print(f"\nwind estimate (flat plate, Cd {cd}, sail {a.sail_cm2:g} cm2, striker {a.striker_g:g} g + sail {a.sail_g:g} g)")
    print(f"  steady wind needed to push the striker {a.gap:g} mm sideways: {u:.1f} m/s")
    for g in (4, 7, 10):
        uu = math.sqrt(2 * (g / a.pend) * weight / (rho_air * area_m2 * cd))
        print(f"    gap {g:>2} mm -> {uu:.1f} m/s")
    print("  (a breeze of 1.6-3.3 m/s is 'light breeze'; gusts and swinging help, tubes/cords add drag)")


if __name__ == "__main__":
    main()
