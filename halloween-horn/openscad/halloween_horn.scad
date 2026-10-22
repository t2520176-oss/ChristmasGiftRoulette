/*
 * HALLOWEEN PUMPKIN SOUND AMPLIFIER  "Smiley"  -  a passive horn for a smartphone          (OpenSCAD 2021.01+, millimetres)
 * ----------------------------------------------------------------------------------------------------------------
 * Built on the structure of "Fixed Frog Acoustic Horn" (reference/frog_horn_original.scad):
 *     phone slot  ->  sound chamber under the phone  ->  throat  ->  horn  ->  mouth
 * The frog became a smiling jack-o'-lantern: the horn opens into its big grin, the eyes are happy arches, and it wears a
 * stem, a leaf, curly vines, two little pumpkins, two feet and a cradle ("ears") that holds the phone.
 *
 *   how the phone is used:  it stands in the slot behind the stem (screen towards the grin); its bottom speaker fires into
 *   the chamber below it; the horn leads the sound forward and widens up to the mouth.  No electronics.
 *
 * ONE PIECE, printed the way it stands (flat bottom), without support:
 *   - the lower part of the pumpkin is an egg cut off at z = 0 (lean 38 deg), the ribs are vertical
 *   - the chamber has no ceiling (it is the lower end of the phone slot); the throat is an A-frame (pointed 38.7-degree roof)
 *   - the GRIN has a flat top edge at its front, but its roof is a sheet that FALLS towards the back at 40 deg from the vertical
 *     (a megaphone; it follows the curve of the front), so each layer grows forward out of the back wall and reaches out only 0.17 mm
 *   - the eyes are "countersunk" (the pocket gets smaller towards its floor at 40 deg), the nose is a triangle with the apex up
 *   - the feet and the side pumpkins are little eggs cut off at z = 0, the stem leans <= 25 deg, the leaf and the vines lie flat
 *
 *  Roles (every role is ONE filament colour, see tools/family_palette.py):
 *     pumpkin :  shell (orange),  green (stem + leaf + vines)      - ONE object; give both roles the same filament for a one-colour print
 *     phone   :  render-only prop (not printed)
 *  part   = "pumpkin" | "all" | "phone" | "manifest"
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
HZ0     = 6;         // floor of chamber, horn and grin
HH1     = 3;         // height of the vertical walls of the A-frame throat
W_T     = 28;        // width of the throat where it leaves the chamber
W_F     = 34;        // the horn widens quickly to this width at y = Y_F (a steep ridge: a nearly flat ridge needs support), then runs straight
Y_F     = -1;
Y_E     = 44;        // and ends inside the grin
SHAFT_W = 52;        // width of the chamber under the phone (covers the speaker side and the microphone side)
SLH     = 0.80;      // lean (tan) of the horn roof: 38.7 deg from the vertical

/* [Grin]  (x / z of the outline in the front view) */
D_X  = 40;           // half width
D_ZB = 6;            // floor
D_ZT = 46;           // the flat top edge of the grin (at the front)
D_AF = 25;           // half length of the flat floor
D_RB = 24;           // height of the rounded lower corners
RK   = 1.2;          // the roof of the grin FALLS RK mm per mm towards the back (a megaphone: 40 deg from the vertical, 45 would be the limit)
ROOM = 12;           // depth of the grin: its back wall is ROOM mm behind the front (and follows its curve)

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
// the same with the ribs: the front of the real pumpkin at (x, z) (the angle a of that point is found by iteration)
function rr_(a, z) = pprof(z, Rx, ZC, RZL, RZU, PU) * (1 + RIB * prib(z, ZC, Z_TOP) * cos(NR * a));
function ang_it(x, z, a, i) = i == 0 ? a : ang_it(x, z, acos(max(-1, min(1, x / rr_(a, z)))), i - 1);
function ysurf_r(x, z) = let(a = ang_it(x, z, acos(max(-1, min(1, x / pprof(z, Rx, ZC, RZL, RZU, PU)))), 6)) RYF * rr_(a, z) * sin(a);


// the little pumpkins at the sides (body) and the feet (face): same shape, smaller
MINI_X = 68;  MINI_Y = 6;  MINI_R = 15.5;  MINI_ZC = 9;  MINI_ZT = MINI_ZC + 0.93 * 17;
module mini() pump(MINI_R, 1, MINI_ZC, 18, 17, 2.2, MINI_ZT, 8, 0.07, 72);
module mini_stem(s) translate([s * MINI_X, MINI_Y, MINI_ZT - 0.5]) for (i = [0 : 3])
    hull() for (t = [i / 4, (i + 1) / 4])
        translate([s * 3.2 * t * t, -1.2 * t, 8 * t]) cylinder(r = 2.9 - 1.1 * t, h = 0.05, $fn = 20);
FOOT_X = 36;  FOOT_Y = 38;                  // embedded deep enough that the dome carries them (a ball that hangs out of the dome needs support)
module foot() pump(10.5, 1, 4.5, 9, 8.2, 2.2, 4.5 + 0.93 * 8.2, 6, 0.07, 48);

// ---------------------------------------------------------------------------------------------
//  the sound path
// ---------------------------------------------------------------------------------------------
module slot() translate([-phone_w / 2, Y_S - phone_t / 2, Z_SF]) cube([phone_w, phone_t, 200]);
module shaft() translate([-SHAFT_W / 2, Y_S - phone_t / 2, HZ0]) cube([SHAFT_W, phone_t, Z_SF - HZ0 + 1]);        // the chamber: the slot continues down
function sec_a(w) = [[-w / 2, HZ0], [w / 2, HZ0], [w / 2, HZ0 + HH1], [0, HZ0 + HH1 + w / 2 / SLH], [-w / 2, HZ0 + HH1]];
module slab(y, w) translate([0, y, 0]) rotate([90, 0, 0]) linear_extrude(0.02, center = true) polygon(sec_a(w));
module horn_body() { hull() { slab(Y_H0, W_T); slab(Y_F, W_F); } hull() { slab(Y_F, W_F); slab(Y_E, W_F); } }
module prism_front(y0, y1) translate([0, y0, 0]) mirror([0, 1, 0]) rotate([90, 0, 0]) linear_extrude(y1 - y0) children();   // 2D (x, z) -> solid from y0 towards +y

// the grin: a "bowl" (flat floor, big rounded lower corners, vertical sides) under a sloping roof sheet.
// The roof passes through the flat line z = D_ZT on the front surface and falls RK per mm towards the back; the back wall is ROOM mm behind the front.
// (falling towards the back matters: every layer of the roof then grows forward out of the solid back wall; a roof that rises towards the back starts as a free
//  line in mid-air at the front edge and needs support)
function arc(cx, cz, rx, rz, a0, a1, n = 14) = [for (i = [0 : n]) let(a = a0 + (a1 - a0) * i / n) [cx + rx * cos(a), cz + rz * sin(a)]];
function bowl() = concat(
    arc(D_AF, D_ZB + D_RB, D_X - D_AF, D_RB, -90, 0),
    [[D_X, 120], [-D_X, 120]],
    arc(-D_AF, D_ZB + D_RB, D_X - D_AF, D_RB, 180, 270));
function ys44(x) = ysurf_r(x, D_ZT);                 // with the ribs: the top edge of the grin is a straight line
RX_N = 64;
module roof_solid() {
    n = RX_N;
    P = [for (i = [0 : n]) let(x = -D_X - 2 + (2 * D_X + 4) * i / n, yb = ysurf(x, D_ZT) - ROOM, yf = ys44(x) + 16)
        each [[x, yb, -1], [x, yf, -1], [x, yf, D_ZT + RK * 16], [x, yb, D_ZT - RK * ROOM]]];
    polyhedron(points = P, faces = concat(
        [for (i = [0 : n - 1]) for (k = [0 : 3]) each [
            [i * 4 + k, (i + 1) * 4 + k, (i + 1) * 4 + (k + 1) % 4],
            [i * 4 + k, (i + 1) * 4 + (k + 1) % 4, i * 4 + (k + 1) % 4]]],
        [[0, 1, 2], [0, 2, 3]],
        [[n * 4, n * 4 + 3, n * 4 + 2], [n * 4, n * 4 + 2, n * 4 + 1]]));
}
module grin() intersection() { roof_solid(); prism_front(0, 120) polygon(bowl()); }

// ---------------------------------------------------------------------------------------------
//  eyes and nose, carved into the front: "countersunk" pockets - the outline shrinks by PK_S towards the floor, which is PK_D deep,
//  so the roof of the pocket leans 45 deg and the pocket needs no support.  The pocket frame is turned to the local slope of the
//  front (about z) and sheared to its lean (in z), so that the floor follows the surface; the frustum starts far in front of the
//  surface, so that no thin skin of the pumpkin is left over the pocket.
// ---------------------------------------------------------------------------------------------
PK_D = 3.6;  PK_S = 3.6;  PK_E = 6;               // depth, shrink of the floor, how far in front the frustum starts
module sheared(k) multmatrix([[1, 0, 0, 0], [0, 1, k, 0], [0, 0, 1, 0], [0, 0, 0, 1]]) children();
module slab2(c, zb) translate([0, c, 0]) rotate([90, 0, 0]) linear_extrude(0.02, center = true) translate([0, zb]) children();
module pocket(x0, zb, h, kind) {
    zm  = zb + h / 2;
    psi = atan((ysurf(x0 + 1, zm) - ysurf(x0 - 1, zm)) / 2);        // slope of the front in x (smooth)
    k   = (ysurf(x0, zb + h) - ysurf(x0, zb)) / h;                   // lean of the front (negative: it leans back)
    tau = PK_S / PK_D;
    translate([x0, ysurf_r(x0, zm), 0]) rotate([0, 0, psi]) sheared(k) translate([0, -k * zm, 0]) {
        hull() {
            slab2(PK_E, zb) offset(delta = tau * PK_E) children();
            slab2(-PK_D, zb) offset(delta = -PK_S) children();
        }
        prism_front(PK_E, PK_E + 30) translate([0, zb]) offset(delta = tau * PK_E) children();
    }
}
function eye_pts(w, h, s) = concat([for (a = [0 : 10 : 180]) [w / 2 * cos(a), h * sin(a)]],
                                    [for (i = [1 : 11]) let(x = -w / 2 + w * i / 12) [x, -s * (1 - pow(x / (w / 2), 2))]]);
module eye2d() polygon(eye_pts(25, 11, 3));
module nose2d() offset(r = 1.8) offset(delta = -1.8) polygon([[-8, 0], [8, 0], [0, 11.5]]);
EYE_X = 25;  EYE_Z = 58;  NOSE_Z = 50;
module face_carve() {
    for (s = [-1, 1]) pocket(s * EYE_X, EYE_Z, 14, "eye") eye2d();
    pocket(0, NOSE_Z, 11.5, "nose") nose2d();
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
//  garnish (green): stem, leaf and curly vines.  Everything lies flat on the top or leans <= 25 deg.  Built with its underside at z = 0.
// ---------------------------------------------------------------------------------------------
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

// ---------------------------------------------------------------------------------------------
//  the pumpkin: ONE piece
// ---------------------------------------------------------------------------------------------
module pumpkin_shell() difference() {
    union() {
        pumpkin_outer();
        for (s = [-1, 1]) { translate([s * MINI_X, MINI_Y, 0]) mini(); mini_stem(s); translate([s * FOOT_X, FOOT_Y, 0]) foot(); }
        collar();
    }
    slot();
    shaft();
    horn_body();
    grin();
    face_carve();
}
module green() translate([0, 0, Z_TOP]) garnish();

// ---- the phone, only for the renders ----------------------------------------------------------
module phone(r) {
    if (r == "body")   translate([0, Y_S, Z_SF + 0.5]) hull() for (x = [-1, 1], z = [0, 1]) translate([x * (71.5 / 2 - 8), 0, 8 + z * (146.7 - 16)]) rotate([90, 0, 0]) cylinder(r = 8, h = 7.65, center = true, $fn = 32);
    if (r == "screen") translate([0, Y_S + 3.9, Z_SF + 0.5]) hull() for (x = [-1, 1], z = [0, 1]) translate([x * (71.5 / 2 - 8.5), 0, 8 + z * (146.7 - 16)]) rotate([90, 0, 0]) cylinder(r = 7.5, h = 0.1, center = true, $fn = 32);
}

// ---------------------------------------------------------------------------------------------
function piece_names() = ["pumpkin"];
function piece_roles(p) = p == "pumpkin" ? ["shell", "green"] : p == "phone" ? ["body", "screen"] : [];
module emit(p, r) {
    if (p == "pumpkin") { if (r == "shell") pumpkin_shell(); if (r == "green") green(); }
    else if (p == "phone") phone(r);
}
C_ORANGE = "#E8751A";  C_GREEN = "#4F8F3A";
function filament(p, r) = p == "phone" ? (r == "screen" ? "#6FA8FF" : "#15171C") : (r == "green" ? C_GREEN : C_ORANGE);
module show(p) { for (r = piece_roles(p)) if (role == "" || role == r) color(filament(p, r)) emit(p, r); }

if (part == "all" || part == "assembled") { show("pumpkin"); }
else if (part == "manifest") { for (p = piece_names()) echo(PIECE = p, ROLES = piece_roles(p)); }
else if (part != "none") show(part);
