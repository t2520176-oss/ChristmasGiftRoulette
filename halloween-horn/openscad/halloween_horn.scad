/*
 * HALLOWEEN PUMPKIN SOUND AMPLIFIER  "Smiley"  -  a passive horn for a smartphone          (OpenSCAD 2021.01+, millimetres)
 * ----------------------------------------------------------------------------------------------------------------
 * Built on the structure of "Fixed Frog Acoustic Horn" (reference/frog_horn_original.scad):
 *     phone slot  ->  sound chamber under the phone  ->  throat  ->  horn  ->  mouth
 * The frog became a smiling jack-o'-lantern: the horn opens into its big grin, the eyes are happy arches, and it wears a
 * stem, a leaf, curly vines and two little pumpkins.
 *
 *   how the phone is used:  it stands in the slot behind the stem (screen towards the grin); its bottom speaker fires into
 *   the chamber below it; the horn leads the sound forward and widens up to the mouth.  No electronics.
 *
 * TWO PRINTED PIECES (so that a smile with a flat roof needs no support):
 *   body : everything behind the plane y = YC - phone slot, chamber, throat, collar ("ears"), side pumpkins.
 *          Printed standing, flat bottom.  The throat is an A-frame (pointed 38.7-degree roof).
 *   face : the front cap of the pumpkin - eyes, nose, grin, feet.  Printed LYING ON ITS BACK (face up): the cut plane y = YC is
 *          the flat bottom, the dome shrinks all the way up, and the grin / eyes / nose are holes that only get wider upwards.
 *   The two are glued at y = YC (a flat joint: the big pentagon hole of the horn shows how to line them up).  No pegs or
 *   sockets: even tiny ones made PrusaSlicer add support.
 *
 *   garnish : stem + leaf + curly vines, ONE flat green piece that is glued into a shallow recess on the flat top of the body.
 *          (Printing it separately keeps bed 1 one-colour: no colour changes, no wipe tower.)
 *
 *  Pieces / roles (every role is ONE filament colour, see tools/family_palette.py):
 *     body    :  shell (orange)
 *     face    :  shell (orange)
 *     garnish :  green
 *     phone:  render-only prop (not printed)
 *  part   = "body" | "face" | "garnish" | "all" | "phone" | "manifest"        layout = "assembled" | "local" (local = print orientation)
 */

part   = "all";
layout = "assembled";
role   = "";

/* [Phone]  (the three numbers of the frog code) */
phone_w = 78;        // width of the slot   (iPhone 13: 71.5 mm + case)
phone_t = 14;        // thickness of the slot (7.65 mm + case; keep it snug)
slot_d  = 41;        // how deep the phone stands in the pumpkin (below its flat top)

/* [Pumpkin] */
Rx    = 62;          // radius at the widest point (x)
RYF   = 0.88;        // y radius = RYF * x radius
ZC    = 22;          // height of the widest point
RZL   = 45;          // lower semi-axis (the egg is cut off at z = 0: the lean there is about 38 deg)
RZU   = 62;          // upper semi-axis
TOPF  = 0.85;        // the top is cut off flat at this fraction of RZU
PU    = 3;           // squareness of the upper half (3 = flat shoulders)
NR    = 10;          // ribs
RIB   = 0.055;       // depth of the ribs (fraction of the radius)

/* [Sound path] */
Y_S     = -14;       // centre of the phone slot (y)
HZ0     = 10;        // floor of chamber, horn and grin
HH1     = 5;         // height of the vertical walls of the A-frame throat
W_T     = 28;        // width of the throat where it leaves the chamber
W_J     = 44;        // width of the horn at the joint plane
SHAFT_W = 52;        // width of the chamber under the phone (covers the speaker side and the microphone side)
SLH     = 0.80;      // lean (tan) of the horn roof: 38.7 deg from the vertical
YC      = 26;        // joint plane body | face
YD      = 34;        // from here on the grin has its full shape

/* [Grin]  (x / z of the outline in the front view) */
D_X  = 40;           // half width
D_ZB = 10;           // floor
D_ZT = 44;           // roof (flat)
D_AF = 25;           // half length of the flat floor
D_RB = 24;           // height of the rounded lower corners
D_RT = 8;            // radius of the upper corners

/* [Fit and quality] */
fn = 64;
// unused here, accepted so that tools/export_roles.py can pass the same -D set as for the other models
family = "HALLOWEEN"; year = 2026; members = []; names = []; font_serif = ""; font_text = "";

/* [Hidden] */
$fn = fn;
eps = 0.01;
Z_TOP = ZC + TOPF * RZU;                                     // flat top of the pumpkin
Z_SF  = Z_TOP - slot_d;                                      // floor of the slot (the phone's bottom edge rests here)
Y_H0  = Y_S + phone_t / 2 - 1;                               // the throat starts 1 mm inside the chamber

// ---------------------------------------------------------------------------------------------
//  the pumpkin: a surface of revolution with ribs, built as one polyhedron (fast, smooth)
// ---------------------------------------------------------------------------------------------
function pprof(z, R, zc, rzl, rzu, pu) = z <= zc ? R * sqrt(max(0, 1 - pow((zc - z) / rzl, 2))) : R * pow(max(0, 1 - pow((z - zc) / rzu, pu)), 1 / pu);
function prib(z, zc, ztop) = z <= zc ? 0.45 + 0.55 * z / zc : 1 - 0.7 * (z - zc) / (ztop - zc);
module pump(R, ryf, zc, rzl, rzu, pu, ztop, nr, rib, na = 120) {
    zs = concat([for (k = [0 : 8]) k * zc / 8], [for (k = [1 : 20]) zc + k * (ztop - zc) / 20]);
    nz = len(zs);
    polyhedron(
        points = concat(
            [for (k = [0 : nz - 1]) for (j = [0 : na - 1])
                let(a = 360 * j / na, r = pprof(zs[k], R, zc, rzl, rzu, pu) * (1 + rib * prib(zs[k], zc, ztop) * cos(nr * a)))
                [r * cos(a), ryf * r * sin(a), zs[k]]],
            [[0, 0, 0], [0, 0, ztop]]),
        faces = concat(
            [for (k = [0 : nz - 2]) for (j = [0 : na - 1]) [k * na + j, (k + 1) * na + j, (k + 1) * na + (j + 1) % na, k * na + (j + 1) % na]],
            [for (j = [0 : na - 1]) [j, (j + 1) % na, nz * na]],
            [for (j = [0 : na - 1]) [(nz - 1) * na + (j + 1) % na, (nz - 1) * na + j, nz * na + 1]]));
}
module pumpkin_outer() pump(Rx, RYF, ZC, RZL, RZU, PU, Z_TOP, NR, RIB, 120);

// y of the front surface at (x, z) (without the ribs), used to place the carvings
function ysurf(x, z) = RYF * pprof(z, Rx, ZC, RZL, RZU, PU) * sqrt(max(0, 1 - pow(x / pprof(z, Rx, ZC, RZL, RZU, PU), 2)));

module back_space()  translate([-300, -300, -5]) cube([600, 300 + YC, 400]);
module front_space() translate([-300, YC, -5]) cube([600, 300, 400]);

// the little pumpkins at the sides (body) and the feet (face): same shape, smaller
MINI_X = 68;  MINI_Y = 6;  MINI_R = 15.5;  MINI_ZC = 9;  MINI_ZT = MINI_ZC + 0.93 * 17;
module mini() pump(MINI_R, 1, MINI_ZC, 18, 17, 2.2, MINI_ZT, 8, 0.07, 72);
module mini_stem(s) translate([s * MINI_X, MINI_Y, MINI_ZT - 0.5]) for (i = [0 : 3])
    hull() for (t = [i / 4, (i + 1) / 4])
        translate([s * 3.2 * t * t, -1.2 * t, 8 * t]) cylinder(r = 2.9 - 1.1 * t, h = 0.05, $fn = 20);
FOOT_X = 36;  FOOT_Y = 38;                  // embedded deep enough that the dome carries them (a ball that hangs out of the dome needs support)
module foot() pump(10.5, 1, 5, 10, 9.2, 2.2, 5 + 0.93 * 9.2, 6, 0.07, 48);

// ---------------------------------------------------------------------------------------------
//  the sound path
// ---------------------------------------------------------------------------------------------
module slot() translate([-phone_w / 2, Y_S - phone_t / 2, Z_SF]) cube([phone_w, phone_t, 200]);
module shaft() translate([-SHAFT_W / 2, Y_S - phone_t / 2, HZ0]) cube([SHAFT_W, phone_t, Z_SF - HZ0 + 1]);        // the chamber: the slot continues down
function sec_a(w) = [[-w / 2, HZ0], [w / 2, HZ0], [w / 2, HZ0 + HH1], [0, HZ0 + HH1 + w / 2 / SLH], [-w / 2, HZ0 + HH1]];
module slab(y, w) translate([0, y, 0]) rotate([90, 0, 0]) linear_extrude(0.02, center = true) polygon(sec_a(w));
module horn_body() hull() { slab(Y_H0, W_T); slab(YC + eps, W_J); }
module prism_front(y0, y1) translate([0, y0, 0]) mirror([0, 1, 0]) rotate([90, 0, 0]) linear_extrude(y1 - y0) children();   // 2D (x, z) -> solid from y0 towards +y

// the grin: a flat roof, a flat floor and big rounded lower corners (convex, so the funnel to it is a hull)
function arc(cx, cz, rx, rz, a0, a1, n = 14) = [for (i = [0 : n]) let(a = a0 + (a1 - a0) * i / n) [cx + rx * cos(a), cz + rz * sin(a)]];
function dshape() = concat(
    arc(D_AF, D_ZB + D_RB, D_X - D_AF, D_RB, -90, 0),
    arc(D_X - D_RT, D_ZT - D_RT, D_RT, D_RT, 0, 90),
    arc(-(D_X - D_RT), D_ZT - D_RT, D_RT, D_RT, 90, 180),
    arc(-D_AF, D_ZB + D_RB, D_X - D_AF, D_RB, 180, 270));
// a smooth funnel from the horn (pentagon) to the grin: the pentagon is sampled at the same relative positions along its outline
// as the points of the grin, then both are blended (the end section is exactly the grin, so it fits its prism)
function seglen(p) = [for (i = [0 : len(p) - 1]) norm(p[(i + 1) % len(p)] - p[i])];
function cumsum(v, i = 0, acc = 0) = i >= len(v) ? [] : concat([acc], cumsum(v, i + 1, acc + v[i]));
function fracs(p) = let(L = seglen(p), C = cumsum(L), tot = C[len(C) - 1] + L[len(L) - 1]) [for (c = C) c / tot];
function sample(p, fr) = let(L = seglen(p), C = cumsum(L), tot = C[len(C) - 1] + L[len(L) - 1])
    [for (f = fr) let(s = tot * f, i = len([for (c = C) if (c <= s + 1e-9) 1]) - 1, t = (s - C[i]) / L[i]) p[i] * (1 - t) + p[(i + 1) % len(p)] * t];
function penta(w) = [[0, HZ0], [w / 2, HZ0], [w / 2, HZ0 + HH1], [0, HZ0 + HH1 + w / 2 / SLH], [-w / 2, HZ0 + HH1], [-w / 2, HZ0]];
LOFT_N = 10;
module funnel() {
    b = concat([[0, D_ZB]], dshape());
    m = len(b);
    a = sample(penta(W_J), fracs(b));
    y0 = YC - eps;
    pts = [for (j = [0 : LOFT_N]) let(t = j / LOFT_N, u = sin(90 * t)) for (k = [0 : m - 1])
        [(1 - u) * a[k][0] + u * b[k][0], y0 + (YD - y0) * t, (1 - u) * a[k][1] + u * b[k][1]]];
    polyhedron(
        points = concat(pts, [[0, y0, HZ0 + 12], [0, YD, (D_ZB + D_ZT) / 2]]),
        faces = concat(
            [for (j = [0 : LOFT_N - 1]) for (k = [0 : m - 1]) each [
                [j * m + k, (j + 1) * m + k, (j + 1) * m + (k + 1) % m],
                [j * m + k, (j + 1) * m + (k + 1) % m, j * m + (k + 1) % m]]],
            [for (k = [0 : m - 1]) [(LOFT_N + 1) * m, k, (k + 1) % m]],
            [for (k = [0 : m - 1]) [(LOFT_N + 1) * m + 1, LOFT_N * m + (k + 1) % m, LOFT_N * m + k]]));
}
module cap_cavity() {
    funnel();                                                  // from the end of the horn to the full grin
    prism_front(YD, 140) polygon(dshape());                    // the grin itself (starts exactly where the funnel ends)
}

// ---------------------------------------------------------------------------------------------
//  eyes and nose: carved into the front, the floor follows the slope of the surface (depth 4.5)
// ---------------------------------------------------------------------------------------------
CARVE = 4.5;
module carve(x, zb, h) {
    zm = zb + h / 2;
    k  = (ysurf(x, zb + h) - ysurf(x, zb)) / h;                      // slope of the surface (negative: it leans back)
    y0 = ysurf(x, zm) - CARVE - k * zm;
    translate([x, 0, 0]) multmatrix([[1, 0, 0, 0], [0, 1, k, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
        prism_front(y0, y0 + 40) translate([0, zb]) children();
}
function eye_pts(w, h, s) = concat([for (a = [0 : 10 : 180]) [w / 2 * cos(a), h * sin(a)]],
                                    [for (i = [1 : 11]) let(x = -w / 2 + w * i / 12) [x, -s * (1 - pow(x / (w / 2), 2))]]);
module eye2d() polygon(eye_pts(20, 10, 2.6));
module nose2d() offset(r = 1.5) offset(delta = -1.5) polygon([[-6.5, 0], [6.5, 0], [0, 10]]);
EYE_X = 24;  EYE_Z = 57;  NOSE_Z = 47.5;
module face_carve() {
    for (s = [-1, 1]) carve(s * EYE_X, EYE_Z, 12) eye2d();
    carve(0, NOSE_Z, 10) nose2d();
}

// ---------------------------------------------------------------------------------------------
//  the phone cradle: two "ears" and a back wall rise above the flat top and hold the phone
// ---------------------------------------------------------------------------------------------
CO_H = 24;  CO_T = 6;
Y_BACK = Y_S - phone_t / 2 - CO_T;                 // back of the cradle
Y_EAR  = Y_S + phone_t / 2 + 10;                   // where the ears end in front
module ear_profile() polygon(concat(
    [[Y_BACK, Z_TOP - 30]],
    [for (a = [0 : 6 : 90]) [Y_BACK + (Y_EAR - Y_BACK) * sin(a), Z_TOP + CO_H * cos(a)]],
    [[Y_EAR, Z_TOP - 30]]));
module collar() {
    for (s = [-1, 1]) translate([s * (phone_w / 2 + CO_T / 2), 0, 0]) rotate([90, 0, 90]) translate([0, 0, -CO_T / 2]) linear_extrude(CO_T) ear_profile();
    translate([0, Y_BACK, 0]) rotate([90, 0, 0]) mirror([0, 0, 1]) linear_extrude(CO_T)
        hull() {
            for (s = [-1, 1]) translate([s * (phone_w / 2 + CO_T - 12), Z_TOP + CO_H - 12]) circle(12);
            translate([-(phone_w / 2 + CO_T), Z_TOP - 30]) square([phone_w + 2 * CO_T, 1]);
        }
}

// ---------------------------------------------------------------------------------------------
//  garnish (green): stem, leaf and curly vines - ONE piece with a flat underside.  Everything lies flat or leans <= 25 deg.
//  It is built with its underside at z = 0; on the body it sits in a recess POCKET deep in the flat top.
// ---------------------------------------------------------------------------------------------
POCKET = 0.8;
STEM = [-13, 12];  STEM_H = 21;  STEM_DX = -4;  STEM_DY = -6;
module gear(r, ph = 0) polygon([for (a = [0 : 10 : 350]) let(k = r * (1 + 0.11 * cos(5 * a + ph))) [k * cos(a), k * sin(a)]]);
module stem_disc(t) translate([STEM[0] + STEM_DX * pow(t, 1.7), STEM[1] + STEM_DY * pow(t, 1.7), STEM_H * t])
    linear_extrude(0.05) gear(6.4 - 2.4 * t + 2.8 * pow(1 - t, 6), 60 * t);
// a broad pumpkin leaf: five rounded lobes (centre one longest) around the point where the stalk joins; the sinuses are shallow
function lobe_t(a) = abs(((a + 27.5 + 550) % 55) - 27.5) / 27.5;                       // 0 on a lobe, 1 in a sinus
function leaf_pts(R) = concat([[0, 0]], [for (a = [-112 : 4 : 112])
    let(r = R * (1 - 0.2 * abs(a) / 112) * (0.62 + 0.38 * pow(1 - lobe_t(a), 0.8))) [r * cos(a), r * sin(a)]]);
module leaf2d() offset(r = 1.2) offset(delta = -2.4) offset(r = 1.2) polygon(leaf_pts(20));
module veins2d() {
    for (a = [0, 55, -55, 110, -110]) hull() { circle(0.5, $fn = 8); translate([(a == 0 ? 16 : abs(a) == 55 ? 13 : 8.5) * cos(a), (a == 0 ? 16 : abs(a) == 55 ? 13 : 8.5) * sin(a)]) circle(0.3, $fn = 8); }
}
LEAF = [-4, 9.5, 8];                           // x, y, rotation of the leaf
module leaf() translate([LEAF[0], LEAF[1], 0]) rotate(LEAF[2]) difference() {
    linear_extrude(1.8) leaf2d();
    translate([0, 0, 1.2]) linear_extrude(1) veins2d();
}
function turtle(i, n, p, h, ds, k0, dk) = i >= n ? [p] :
    concat([p], turtle(i + 1, n, [p[0] + ds * cos(h), p[1] + ds * sin(h)], h + (k0 + dk * i) * ds, ds, k0, dk));
VINE_W = 3.2;
VINES = [[[-19, 15], 160, 60, 0.7, 2.0, 0.25], [[9, 8], -22, 56, 0.7, 2.0, 0.26]];      // start, heading, steps, step, curvature, curvature growth
function vine_pts(v) = turtle(0, v[2], v[0], v[1], v[3], v[4], v[5]);
module ribbon2d(pts, w) for (i = [0 : len(pts) - 2]) hull() { translate(pts[i]) circle(w / 2, $fn = 10); translate(pts[i + 1]) circle(w / 2, $fn = 10); }
module vine(v) {
    pts = vine_pts(v);
    linear_extrude(1.1) ribbon2d(pts, VINE_W);
    translate([0, 0, 1.1]) linear_extrude(0.9) offset(delta = -0.5) ribbon2d(pts, VINE_W);
    translate([0, 0, 2.0]) linear_extrude(0.7) offset(delta = -1.0) ribbon2d(pts, VINE_W);
}
module garnish() {
    for (i = [0 : 11]) hull() { stem_disc(i / 12); stem_disc((i + 1) / 12); }
    leaf();
    for (v = VINES) vine(v);
}
// the footprint of the garnish (for the recess in the body)
module garnish_2d() {
    translate([LEAF[0], LEAF[1]]) rotate(LEAF[2]) leaf2d();
    translate(STEM) gear(9.0);
    for (v = VINES) ribbon2d(vine_pts(v), VINE_W);
}
module garnish_pocket() translate([0, 0, Z_TOP - POCKET]) linear_extrude(POCKET + 1) offset(delta = 0.25) garnish_2d();

// ---------------------------------------------------------------------------------------------
//  the two pieces
// ---------------------------------------------------------------------------------------------
module body_shell() difference() {
    union() {
        intersection() { pumpkin_outer(); back_space(); }
        for (s = [-1, 1]) { translate([s * MINI_X, MINI_Y, 0]) mini(); mini_stem(s); }
        collar();
    }
    slot();
    shaft();
    horn_body();
    garnish_pocket();
}
module face_shell() difference() {
    intersection() {
        union() { pumpkin_outer(); for (s = [-1, 1]) translate([s * FOOT_X, FOOT_Y, 0]) foot(); }
        front_space();
    }
    cap_cavity();
    face_carve();
}

// the face is printed on its back: the joint plane is the bed, the front looks up
module place(p) {
    if (layout == "local" && p == "face") translate([0, 80, -YC]) rotate([90, 0, 0]) children();
    else if (layout != "local" && p == "garnish") translate([0, 0, Z_TOP - POCKET]) children();     // assembled: it sits in its recess
    else children();
}

// ---- the phone, only for the renders ----------------------------------------------------------
module phone(r) {
    if (r == "body")   translate([0, Y_S, Z_SF + 0.5]) hull() for (x = [-1, 1], z = [0, 1]) translate([x * (71.5 / 2 - 8), 0, 8 + z * (146.7 - 16)]) rotate([90, 0, 0]) cylinder(r = 8, h = 7.65, center = true, $fn = 32);
    if (r == "screen") translate([0, Y_S + 3.9, Z_SF + 0.5]) hull() for (x = [-1, 1], z = [0, 1]) translate([x * (71.5 / 2 - 8.5), 0, 8 + z * (146.7 - 16)]) rotate([90, 0, 0]) cylinder(r = 7.5, h = 0.1, center = true, $fn = 32);
}

// ---------------------------------------------------------------------------------------------
function piece_names() = ["body", "face", "garnish"];
function piece_roles(p) = p == "body" ? ["shell"] : p == "face" ? ["shell"] : p == "garnish" ? ["green"] : p == "phone" ? ["body", "screen"] : [];
module emit(p, r) {
    if (p == "body") { if (r == "shell") body_shell(); }
    else if (p == "face") { if (r == "shell") face_shell(); }
    else if (p == "garnish") { if (r == "green") garnish(); }
    else if (p == "phone") phone(r);
}
C_ORANGE = "#E8751A";  C_GREEN = "#4F8F3A";
function filament(p, r) = p == "phone" ? (r == "screen" ? "#6FA8FF" : "#15171C") : (r == "green" ? C_GREEN : C_ORANGE);
module show(p) { for (r = piece_roles(p)) if (role == "" || role == r) color(filament(p, r)) place(p) emit(p, r); }

if (part == "all" || part == "assembled") { show("body"); show("face"); show("garnish"); }
else if (part == "manifest") { for (p = piece_names()) echo(PIECE = p, ROLES = piece_roles(p)); }
else if (part != "none") show(part);
