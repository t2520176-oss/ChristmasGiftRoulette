/*
 * PENGUIN HOME  -  a faceted "ice-block" igloo with a lift-off lid, an LED tea-light inside,
 *                  a penguin family and a name sign.            (OpenSCAD 2021.01+, millimetres)
 * ----------------------------------------------------------------------------------------------
 * Designed so that NOTHING needs support material:
 *   - igloo body   printed upright, open at the top; every inner and outer surface leans <= 40 deg,
 *                  the door is a 45-deg "low-poly" arch (no round roof to bridge)
 *   - lid          solid lens printed flat side down (sparse infill carries the dome), 3 keyed pegs
 *   - arch trim    printed flat, snaps onto the tunnel with two diamond pegs
 *   - ground       flat snow plate with sockets for the sign, the trees and the penguins
 *   - sign, trees, penguins   flat-back relief pieces (like the family ornament): they lie on their back,
 *                  domes grow from the bed, a tab plugs them into the ground
 *
 *  Every piece has ROLES, every role is ONE filament colour (see palettes/penguin_home.json).
 *  Local (print) frame: the piece lies flat, z = 0 is the bed.  Assembled frame: z up, front = -Y.
 *
 *  part   = piece name | "all" | "tealight" (render-only prop) | "manifest"
 *  layout = "assembled" (finished scene) | "local" (each piece flat at its own origin - for STL export)
 */

/* [Sign] */
sign_line1 = "PENGUIN";
sign_line2 = "HOME";

/* [What to build] */
part   = "all";
layout = "assembled";
// "" = every colour region of the piece, otherwise one role
role   = "";

/* [Fit and quality] */
tol = 0.2;          // clearance of every plug / socket (mm)
fn  = 36;
font_serif = "Liberation Serif:style=Bold";
font_text  = "Liberation Serif:style=Regular";
// unused by this model, accepted so that tools/export_roles.py can pass the same -D set as for the family ornament
family = "PENGUIN"; year = 2026; members = []; names = [];

/* [Hidden] */
$fn = fn;
eps = 0.01;

// ----------------------------------------------------------------------------------------
//  helpers (same idea as the family ornament)
// ----------------------------------------------------------------------------------------
module hemi() rotate_extrude() polygon(concat([[0, 0]], [for (i = [0 : 14]) [cos(90 * i / 14), sin(90 * i / 14)]]));
module D(c, r) translate([c[0], c[1], 0]) scale([r[0], r[1], r[2]]) hemi();          // flat-bottomed dome
module E(c, r) translate([c[0], c[1], c[2]]) scale([r[0], r[1], r[2]]) sphere(1);      // ellipsoid
module clipy(y0, y1 = 500) translate([-500, y0, -5]) cube([1000, y1 - y0, 100]);
function dz(c, r, x, y) = r[2] * sqrt(max(0, 1 - pow((x - c[0]) / r[0], 2) - pow((y - c[1]) / r[1], 2)));
module slab(h) linear_extrude(height = h) children();
module rrect2d(w, h, r) hull() for (sx = [-1, 1], sy = [-1, 1]) translate([sx * (w / 2 - r), sy * (h / 2 - r)]) circle(r);
module soften(r = 1) offset(r = r) offset(delta = -r) children();
module star2d(R = 5, r = 2.2, n = 5)
    polygon([for (i = [0 : 2 * n - 1]) let(a = 90 + i * 180 / n, rr = (i % 2 == 0) ? R : r) [rr * cos(a), rr * sin(a)]]);

// text (advance widths of Liberation Serif Bold per unit of font size, ASCII 32..126)
W_SERIF = [0.339, 0.452, 0.452, 0.678, 0.678, 1.356, 1.13, 0.377, 0.452, 0.452, 0.678, 0.773, 0.339, 0.452, 0.339, 0.377,
    0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.452, 0.452, 0.773, 0.773, 0.773, 0.678, 1.262,
    0.979, 0.905, 0.979, 0.979, 0.905, 0.829, 1.055, 1.055, 0.528, 0.678, 1.055, 0.905, 1.28, 0.979, 1.055, 0.829, 1.055,
    0.979, 0.754, 0.905, 0.979, 0.979, 1.356, 0.979, 0.979, 0.905, 0.452, 0.377, 0.452, 0.788, 0.678, 0.452, 0.678, 0.754,
    0.602, 0.754, 0.602, 0.452, 0.678, 0.754, 0.377, 0.452, 0.754, 0.377, 1.13, 0.754, 0.678, 0.754, 0.754, 0.602, 0.528,
    0.452, 0.754, 0.678, 0.979, 0.678, 0.678, 0.602, 0.534, 0.299, 0.534, 0.705];
function chw(c) = let(o = ord(c)) (o >= 32 && o <= 126) ? W_SERIF[o - 32] : 0.8;
function strw(s, i = 0) = i >= len(s) ? 0 : chw(s[i]) + strw(s, i + 1);
function _join(v, i = 0) = i >= len(v) ? "" : str(v[i], _join(v, i + 1));
function ucase(s) = let(v = [for (c = s) (ord(c) >= 97 && ord(c) <= 122) ? chr(ord(c) - 32) : c]) _join(v);
module fit_text(s, size, maxw, fnt = font_serif, k0 = 1) {   // never stretched, only shrunk to fit; glyph box centred on y = 0
    k = min(k0, maxw / max(strw(s) * size, 0.01));
    scale(k) text(s, size = size, font = fnt, halign = "center", valign = "center");
}

// ----------------------------------------------------------------------------------------
//  THE IGLOO  -  a sphere of radius R built from 7 courses of flat "ice blocks"
//  (a course = one ring of N flat facets, staggered half a block against the course below).
//  Boundaries between the courses are small V-grooves, so the dome reads as a stack of blocks.
// ----------------------------------------------------------------------------------------
R   = 58;
LAT = [0, 10, 20, 30, 40, 50, 60, 70];          // latitude of every course boundary (deg)
NK  = [24, 24, 22, 20, 18, 16, 14];             // blocks per course
BODY_K = 3;                                     // the body is courses 0 .. BODY_K-1 (up to latitude LAT[BODY_K]); the lid is the rest
T   = [3.2, 3.6, 4.4, 5.6, 6.0];                // horizontal wall thickness at boundaries 0..BODY_K (body only)
GD  = 0.8;                                      // groove depth
CH  = 1.0;                                      // length of the chamfer on both sides of a groove
function rc(k) = R * cos(LAT[k]);
function zc(k) = R * sin(LAT[k]);
function ri(k) = rc(k) - T[k];                  // inner radius at boundary k (k = 0..4)
SLOPE = 0.84;                                   // every overhang in the model leans at most atan(0.84) = 40 deg
function ucrs(k) = let(dx = rc(k + 1) - rc(k), dz = zc(k + 1) - zc(k), L = sqrt(dx * dx + dz * dz)) [dx / L, dz / L];
function tl(k) = -(rc(k + 1) - rc(k)) / (zc(k + 1) - zc(k));              // tan of the inward lean of course k
// Staggered courses have their corners in different places.  So that the upper course never reaches beyond the
// polygon below it (that would be a ledge hanging in the air, and the slicer would put pillars under it),
// the upper course starts DELTA smaller than the inscribed circle of the lower one.
function dlt(k) = k == 0 ? 0 : rc(k) * (1 - cos(180 / NK[k - 1])) + 0.15;
function G(k)  = [rc(k) - (k == 0 ? 0 : GD), zc(k)];                      // top of the course below boundary k (floor of the groove)
// (the first course of the lid starts at the full radius: it is printed flat on the bed, so it must not start with an overhanging flank)
function Gu(k) = [rc(k) - (k == 0 || k == BODY_K ? 0 : GD + dlt(k)), zc(k)];   // bottom of the course above boundary k
function tup(k) = (GD + dlt(k)) / (SLOPE + tl(k));                         // the flank climbs at atan(SLOPE) until it meets the course line
function Pup(k) = Gu(k) + tup(k) * [SLOPE, 1];                             // just above boundary k
function Plow(k) = [rc(k + 1), zc(k + 1)] - CH * ucrs(k);                  // just below boundary k+1
LIP = 0;                                        // (the body has no lip: its bottom is flat, the ground has a low ridge that centres it from inside)
function course_pts(k) = concat(k == 0 && LIP > 0 ? [[0, -LIP], [rc(0), -LIP]] : [[0, zc(k)]], [Gu(k)], k == 0 || k == BODY_K ? [] : [Pup(k)], [Plow(k)], [G(k + 1)], [[0, zc(k + 1)]]);
Z_CUT = zc(BODY_K);                             // the lid sits here
Z_TOP = zc(7);                                  // flat top of the lid

module course_solid(k) rotate(k % 2 * 180 / NK[k]) rotate_extrude($fn = NK[k]) polygon(course_pts(k));
module outer_solid(k0, k1) for (k = [k0 : k1]) course_solid(k);

// vertical joints: a small V-groove on every facet corner, so each facet reads as one ice block
VG = 0.9;
module vdiamond() linear_extrude(0.02) polygon([[-VG, 0], [0, VG], [VG, 0], [0, -VG]]);
module vjoint(k) {
    n = NK[k];  st = k % 2 * 180 / n;
    p0 = k == 0 ? [rc(0), zc(0) + 0.8] : [rc(k), zc(k)];
    p1 = [rc(k + 1), zc(k + 1)];
    for (j = [0 : n - 1]) rotate(st + j * 360 / n) hull() {
        translate([p0[0], 0, p0[1]]) vdiamond();
        translate([p1[0], 0, p1[1]]) vdiamond();
    }
}
module blocks(k0, k1) difference() { outer_solid(k0, k1); for (k = [k0 : k1]) vjoint(k); }

// the hollow inside (same facets, so the wall has the same thickness everywhere)
function cav_pts(k) =
    k == 0 ? [[0, -1], [ri(0), -1], [ri(0), zc(0)], [ri(1), zc(1)], [0, zc(1)]] :
    k == BODY_K - 1 ? [[0, zc(k)], [ri(k), zc(k)], [ri(k + 1), zc(k + 1)], [ri(k + 1), zc(k + 1) + 1], [0, zc(k + 1) + 1]] :
             [[0, zc(k)], [ri(k), zc(k)], [ri(k + 1), zc(k + 1)], [0, zc(k + 1)]];
// the inside is smooth (a faceted inside would have ledges over the empty cavity)
module cavity() rotate_extrude($fn = 96) polygon(concat([[0, -LIP - 1], [ri(0), -LIP - 1]], [for (k = [0 : BODY_K]) [ri(k), zc(k)]], [[ri(BODY_K), zc(BODY_K) + 1], [0, zc(BODY_K) + 1]]));

// ---- the entrance: a low-poly arch (vertical walls, then 45-degree roof, flat top) -----------
BORE_W = 11;  BORE_H1 = 6.5;  BORE_TOP = 0;              // half width, height of the walls, half width of the flat top (0 = pointed 45-degree roof)
TUN_WALL = 6;                                           // wall of the tunnel
TUN_FRONT = -(R + 20);                                  // y of the front face of the tunnel
TRIM_T = 5;  TRIM_W = 3.5;                              // the blue arch ring (separate piece)
PEG_X = BORE_W + TUN_WALL / 2;  PEG_Z = 8;              // the two pegs of the trim
module peg2d(g = 0) offset(delta = g) polygon([[-2, -2], [2, -2], [2, 0.6], [0, 0.6 + 2 / SLOPE], [-2, 0.6]]);   // house-shaped: no flat or 45-degree roof
module arch2d(w, h1, top) polygon(top <= 0 ? [[-w, 0], [w, 0], [w, h1], [0, h1 + w / SLOPE], [-w, h1]]
                                       : [[-w, 0], [w, 0], [w, h1], [top, h1 + (w - top)], [-top, h1 + (w - top)], [-w, h1]]);
module bore2d() arch2d(BORE_W, BORE_H1, BORE_TOP);
module above0() translate([-200, 0]) square([400, 400]);
// the tunnel must stay below the rim of the body (the lid sits there), so its ridge is clipped 0.5 mm under it
module tunnel_outer2d() intersection() { offset(delta = TUN_WALL) bore2d(); above0(); translate([-200, 0]) square([400, Z_CUT - 0.5]); }
module trim_ring2d() difference() { intersection() { offset(delta = TUN_WALL + TRIM_W) bore2d(); above0(); } bore2d(); }
module prism_y(y0, y1) translate([0, y0, 0]) rotate([90, 0, 0]) linear_extrude(height = y0 - y1) children();   // 2D (x, z) -> solid from y0 down to y1

// the lid is located by a continuous V-ridge on the rim of the body and a V-groove in the underside of the lid
// (both only 30 deg from vertical: nothing to bridge, nothing to support)
RIM_R = (rc(BODY_K) - GD + ri(BODY_K)) / 2 - 0.3;       // middle of the rim
module ridge2d(w, h) polygon([[RIM_R - w, 0], [RIM_R + w, 0], [RIM_R, h]]);
module rim_ridge() translate([0, 0, Z_CUT - eps]) rotate_extrude($fn = 96) ridge2d(1.7, 2.8 + eps);

module body_solid() union() {
    difference() {
        union() { blocks(0, BODY_K - 1); prism_y(-30, TUN_FRONT) tunnel_outer2d(); }
        cavity();
        prism_y(-30, TUN_FRONT - 20) bore2d();
        // two diamond sockets for the trim, in the front face of the tunnel wall
        for (sx = [-1, 1]) translate([sx * PEG_X, TUN_FRONT - eps, PEG_Z]) rotate([-90, 0, 0]) linear_extrude(4.6) scale([1, -1]) peg2d(tol + 0.2);
    }
    rim_ridge();
}
BELT_D = 0.05;                                          // the colour change sits 0.05 mm above the groove floor (a cut exactly in the groove plane would make a non-manifold edge)
module belt_zone() translate([-200, -200, zc(1) + BELT_D]) cube([400, 400, zc(2) - zc(1)]);
module body(r) {
    if (r == "belt") intersection() { body_solid(); belt_zone(); }
    if (r == "ice")  difference() { body_solid(); belt_zone(); }
}

// ---- the lid: a solid lens, flat underneath, with a raised snow crystal in a blue inlay --------
module flake2d(Rf = 15.5) union() {
    for (a = [0 : 60 : 300]) rotate(a) {
        translate([0, -1.1]) square([Rf, 2.2]);
        for (d = [6.5, 10.5], s = [-1, 1]) translate([d, 0]) rotate(s * 50) translate([0, -0.95]) square([d == 6.5 ? 5.6 : 4.2, 1.9]);
        translate([Rf, 0]) circle(r = 2.1, $fn = 6);
    }
    circle(r = 3.4, $fn = 6);
}
module flake_prism() translate([0, 0, Z_TOP - 2]) slab(3) rotate(15) flake2d();           // 2 mm inlay + 1 mm raised
module lid_solid() difference() {
    blocks(BODY_K, 6);
    translate([0, 0, Z_CUT - eps]) rotate_extrude($fn = 96) ridge2d(2.0, 3.4);
}
module lid(r) {
    if (r == "ice")   difference() { lid_solid(); flake_prism(); }
    if (r == "flake") flake_prism();
}

// ---- the arch trim: flat ring, front face on the bed, two diamond pegs on the back --------------
module trim(r) {
    if (r == "ring") union() {
        slab(TRIM_T) trim_ring2d();
        for (sx = [-1, 1]) translate([sx * PEG_X, PEG_Z, TRIM_T - eps]) linear_extrude(4.0 + eps) peg2d();
    }
}

// ----------------------------------------------------------------------------------------
//  THE GROUND: a snow plate with a seat for the igloo, a tea-light pocket and sockets
// ----------------------------------------------------------------------------------------
GH = 8;                                                 // plate thickness (top at z = 0)
SLOT_D = 6.2;  TABL = 5.6;                              // socket depth, tab length of all standees
// standing pieces: [x, y, yaw, tab width, tab thickness]
function stand(p) =
    p == "sign"  ? [-64, -58,  26, 12, 5.5] :
    p == "tree0" ? [-78,  30, -18, 10, 4.0] :
    p == "tree1" ? [ 72, -22,  14, 10, 4.0] :
    p == "peng0" ? [ 47, -72, -10, 14, 5.0] :
    p == "peng1" ? [ 70, -52, -24, 14 * PS1, 5.0 * PS1] : [0, 0, 0, 0, 0];
PS1 = 0.62;                                             // the chick is the adult at 62 %
MOUNDS = [[-39, -62, 6, 3.4], [-69, -34, 5, 3.0], [54, -36, 5.5, 3.2]];     // x, y, radius, height - all fully on the plate (checked)
module ground_outline() offset(r = -7) offset(r = 7) union() {   // closing: fills the notches between the lobes
    translate([0, 2]) circle(r = 66, $fn = 120);
    translate([-62, -56]) circle(r = 30, $fn = 80);
    translate([48, -66]) circle(r = 34, $fn = 80);
    translate([0, -66]) circle(r = 26, $fn = 80);
    translate([-76, 28]) circle(r = 24, $fn = 60);
    translate([72, -24]) circle(r = 24, $fn = 60);
}
module standee_slots() for (p = ["sign", "tree0", "tree1", "peng0", "peng1"]) let(s = stand(p))
    translate([s[0], s[1], 0]) rotate(s[2]) translate([-s[3] / 2 - tol, -s[4] - tol, -SLOT_D]) cube([s[3] + 2 * tol, s[4] + 2 * tol, SLOT_D + 1]);
module ground(r) {
    if (r == "snow") difference() {
        union() {
            translate([0, 0, -GH]) slab(GH) ground_outline();
            for (m = MOUNDS) translate([0, 0, -eps]) D([m[0], m[1]], [m[2], m[2] * 0.9, m[3] + eps]);
            // a low ring that fits inside the body and centres it (the body itself stays flat underneath: printable)
            translate([0, 0, -eps]) rotate_extrude($fn = 120) polygon([[52.0, 0], [54.2, 0], [53.6, 1.6], [52.6, 1.6]]);
        }
        translate([0, 0, -5.2]) cylinder(r = 19.4, h = 6, $fn = 72);          // tea-light pocket (38 mm LED tea-light)
        standee_slots();
    }
}

// ----------------------------------------------------------------------------------------
//  THE SIGN  (flat: post + board + tab, text raised, snow on the board)       roles: wood, text, snow
// ----------------------------------------------------------------------------------------
BOARD_W = 56;  BOARD_H = 26;  BOARD_T = 5.5;  BOARD_Y = 34;
module sign2d() union() {
    translate([0, BOARD_Y]) rrect2d(BOARD_W, BOARD_H, 2.5);
    translate([-4, 0]) square([8, BOARD_Y]);
    translate([-6, -TABL]) square([12, TABL + 1]);
}
module sign_snow() {
    D([-17, BOARD_Y + BOARD_H / 2 - 0.5], [8, 3.6, 4.6]);
    D([4, BOARD_Y + BOARD_H / 2 - 0.2], [10, 4.0, 5.0]);
    D([20, BOARD_Y + BOARD_H / 2 - 0.8], [6, 3.2, 4.2]);
}
SIGN_SIZE = 10.5;
function sign_k() = min(1, (BOARD_W - 9) / (strw(ucase(sign_line1)) * SIGN_SIZE), (BOARD_W - 9) / (strw(ucase(sign_line2)) * SIGN_SIZE));
module sign_text2d() {
    translate([0, BOARD_Y + 6.2]) fit_text(ucase(sign_line1), SIGN_SIZE, BOARD_W - 9, k0 = sign_k());
    translate([0, BOARD_Y - 6.2]) fit_text(ucase(sign_line2), SIGN_SIZE, BOARD_W - 9, k0 = sign_k());
}
module sign(r) {
    if (r == "wood") slab(BOARD_T) sign2d();
    if (r == "text") translate([0, 0, BOARD_T]) slab(0.9) sign_text2d();
    if (r == "snow") difference() { sign_snow(); translate([0, 0, -1]) slab(BOARD_T + 3) sign2d(); }   // the part that sticks out above the board
}

// ----------------------------------------------------------------------------------------
//  TREES  (flat three-tier pine with snow on every tier)                         roles: green, snow
// ----------------------------------------------------------------------------------------
function tree_w(i) = i == 0 ? 36 : 28;
function tree_h(i) = i == 0 ? 58 : 44;
module tier(w, y0, h) polygon([[-w / 2, y0], [w / 2, y0], [0, y0 + h]]);
module tier_k(k, w, h) {
    if (k == 0) tier(w, 3, 0.42 * h);
    if (k == 1) tier(0.78 * w, 0.28 * h, 0.40 * h);
    if (k == 2) tier(0.55 * w, 0.52 * h, 0.46 * h);
}
module tree_tiers2d(w, h) soften(1.0) union() { for (k = [0 : 2]) tier_k(k, w, h); translate([-2.2, -1]) square([4.4, 7]); }
module tree_snow2d(w, h) intersection() {
    tree_tiers2d(w, h);
    soften(0.9) union() for (k = [0 : 2]) difference() { tier_k(k, w, h); translate([0, -5.0]) tier_k(k, w, h); }
}
module tree_tab2d() translate([-5, -TABL]) square([10, TABL + 1.5]);
module tree(i, r) {
    w = tree_w(i);  h = tree_h(i);
    if (r == "green") union() {
        slab(1.0) union() { tree_tiers2d(w, h); tree_tab2d(); }
        slab(4.0) union() { difference() { tree_tiers2d(w, h); tree_snow2d(w, h); } tree_tab2d(); }
    }
    if (r == "snow") translate([0, 0, 1.0]) slab(4.2) tree_snow2d(w, h);
}

// ----------------------------------------------------------------------------------------
//  PENGUINS (flat-back relief of flat-bottomed domes, y = 0 at the feet)
//  roles: body, belly, patch, eyes, beak, feet, scarf
// ----------------------------------------------------------------------------------------
module P_BODY() union() {
    intersection() { D([0, 16], [11.5, 16.5, 9.5]); clipy(0.4); }
    D([0, 33.5], [8.6, 8.4, 8.2]);                                           // head
    for (s = [-1, 1]) translate([s * 11.4, 20.5]) rotate(s * 14) D([0, 0], [2.9, 8.8, 4.2]);   // flippers
    translate([-7 / 1, -TABL]) cube([14, TABL + 1.5, 5]);                    // tab
}
module P_BELLY() intersection() { D([0, 15], [7.5, 12.6, 10.6]); clipy(1.6); }
module P_PATCH() union() { for (s = [-1, 1]) D([s * 3.1, 33.6], [4.3, 4.7, 9.1]); D([0, 31.2], [3.2, 3.2, 9.0]); }
module P_EYES() for (s = [-1, 1]) E([s * 3.2, 35.2, dz([s * 3.1, 33.6], [4.3, 4.7, 9.1], s * 3.2, 35.2) - 0.3], [1.0, 1.25, 1.0]);
module P_BEAK() hull() { D([0, 33.0], [3.0, 1.9, 9.6]); D([0, 30.0], [1.5, 1.1, 9.9]); }
module P_FEET() intersection() { for (s = [-1, 1]) D([s * 4.3, 2.4], [4.0, 2.6, 6.4]); clipy(0.05); }
module P_SCARF() union() { D([0, 25.2], [9.8, 2.9, 9.2]); translate([4.8, 19.4]) rotate(-8) D([0, 0], [2.3, 5.4, 9.6]); }
module peng_role(r) {
    if (r == "body")   difference() { P_BODY(); P_BELLY(); P_PATCH(); P_EYES(); P_BEAK(); P_FEET(); P_SCARF(); }
    if (r == "belly")  difference() { P_BELLY(); P_SCARF(); P_FEET(); }
    if (r == "patch")  difference() { P_PATCH(); P_EYES(); P_BEAK(); P_SCARF(); }
    if (r == "eyes")   P_EYES();
    if (r == "beak")   difference() { P_BEAK(); P_EYES(); }
    if (r == "feet")   P_FEET();
    if (r == "scarf")  P_SCARF();
}
module peng(i, r) { s = i == 0 ? 1 : PS1; scale([s, s, s]) peng_role(r); }

// ---- a tea-light, only for the renders ----------------------------------------------------
module tealight(r) {
    if (r == "cup")   translate([0, 0, -5.2]) difference() { cylinder(r = 18.9, h = 14, $fn = 72); translate([0, 0, 11]) cylinder(r = 16.5, h = 4, $fn = 72); }
    if (r == "flame") translate([0, 0, 12.5]) scale([1, 1, 1.9]) sphere(r = 3.2, $fn = 24);
}

// ----------------------------------------------------------------------------------------
//  PIECES, ROLES, PLACEMENT
// ----------------------------------------------------------------------------------------
function piece_names() = ["ground", "body", "lid", "trim", "sign", "tree0", "tree1", "peng0", "peng1"];
function starts(p, pre) = len(p) >= len(pre) && _join([for (i = [0 : len(pre) - 1]) p[i]]) == pre;
function pidx(p, n) = ord(p[n]) - 48;
function piece_roles(p) =
    p == "ground" ? ["snow"] : p == "body" ? ["ice", "belt"] : p == "lid" ? ["ice", "flake"] : p == "trim" ? ["ring"] :
    p == "sign" ? ["wood", "text", "snow"] : starts(p, "tree") ? ["green", "snow"] :
    starts(p, "peng") ? ["body", "belly", "patch", "eyes", "beak", "feet", "scarf"] :
    p == "tealight" ? ["cup", "flame"] : [];

module emit(p, r) {
    if (p == "ground") ground(r);
    else if (p == "body") body(r);
    else if (p == "lid") lid(r);
    else if (p == "trim") trim(r);
    else if (p == "sign") sign(r);
    else if (starts(p, "tree")) tree(pidx(p, 4), r);
    else if (starts(p, "peng")) peng(pidx(p, 4), r);
    else if (p == "tealight") tealight(r);
}
module standing(p) let(s = stand(p)) translate([s[0], s[1], 0]) rotate([0, 0, s[2]]) rotate([90, 0, 0]) children();
module place(p) {
    if (p == "ground" || p == "body" || p == "lid" || p == "tealight") children();
    else if (p == "trim") translate([0, TUN_FRONT - TRIM_T, 0]) rotate([0, 0, 180]) rotate([90, 0, 0]) children();   // local (x,y,z) -> (-x, z, y)
    else standing(p) children();
}
module local(p) {
    if (p == "ground") translate([0, 0, GH]) children();
    else if (p == "lid") translate([0, 0, -Z_CUT]) children();
    else children();
}

// colours of the preview (the real filament colours are in palettes/penguin_home.json)
C_WHITE = "#F5F7FA";  C_BLUE = "#8CC4EA";  C_GREEN = "#2F7048";  C_BLACK = "#20232B";  C_ORANGE = "#E58A2E";
function filament(p, r) =
    p == "ground" ? C_WHITE : p == "body" ? (r == "belt" ? C_BLUE : C_WHITE) : p == "lid" ? (r == "flake" ? C_BLUE : C_WHITE) :
    p == "trim" ? C_BLUE : p == "sign" ? (r == "wood" ? C_ORANGE : C_WHITE) : starts(p, "tree") ? (r == "green" ? C_GREEN : C_WHITE) :
    starts(p, "peng") ? ((r == "body" || r == "eyes") ? C_BLACK : (r == "belly" || r == "patch") ? C_WHITE : C_ORANGE) :
    p == "tealight" ? (r == "flame" ? "#FFC060" : "#C9CED6") : "#999999";

module show(p) {
    for (r = piece_roles(p)) if (role == "" || role == r)
        color(filament(p, r)) { if (layout == "local") local(p) emit(p, r); else place(p) emit(p, r); }
}

if (part == "all" || part == "assembled") { for (p = piece_names()) show(p); }
else if (part == "manifest") { for (p = piece_names()) echo(PIECE = p, ROLES = piece_roles(p)); }
else if (part != "none") show(part);
