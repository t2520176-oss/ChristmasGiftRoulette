/*
 * LEANING TOWER OF PISA  -  a desk lamp for an LED tea-light            (OpenSCAD 2021.01+, millimetres)
 * ----------------------------------------------------------------------------------------------
 *   base    snow-white plinth with a lawn, a stone plaza, steps, a fence, cypress trees and the tea-light pocket
 *   tower   ground floor + 6 arcaded floors (tapering a little) with a stone cornice between the floors
 *   top     the belfry with its roof, a parapet and a flag pole
 *   flag    the Italian tricolour (printed flat, 3 colours in 5 layers), slides into the slot of the pole
 *
 * Built so that (almost) nothing needs support:
 *   - the lean is a SHEAR (every layer is a plain circle, shifted by 0.1 mm per mm of height) - no layer hangs over the one below
 *   - all arch heads, cornice undersides and roofs lean <= 36 deg (a slicer's limit is 45 deg)
 *   - the cavity under the belfry roof is a cone, the funnel on top of the tower is a cone: nothing flat to bridge
 *   - the pieces are located by low vertical collars (no thin grooves)
 *
 *  Every piece has ROLES, every role is ONE filament colour (see tools/family_palette.py).
 *  part   = piece name | "all" | "tealight" (render-only prop) | "manifest"
 *  layout = "assembled" (finished lamp) | "local" (each piece at its own origin, as it is printed - for STL export)
 */

part   = "all";
layout = "assembled";
role   = "";                    // "" = every colour region of the piece, otherwise one role

/* [Shape] */
LEAN = 5.5;                     // lean of the tower (deg)

/* [Fit and quality] */
tol = 0.2;                      // clearance of every plug (mm)
fn  = 96;
// unused here, accepted so that tools/export_roles.py can pass the same -D set as for the other models
family = "PISA"; year = 2026; members = []; names = []; font_serif = ""; font_text = "";

/* [Hidden] */
$fn = fn;
eps = 0.01;
LT  = tan(LEAN);
SL  = 0.72;                     // lean (tan) of the cornice undersides
SLF = 0.78;                     // lean (tan) of the cones (funnel, roof cavity)

module lean() multmatrix([[1, 0, LT, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]) children();
module hemi() rotate_extrude($fn = 24) polygon(concat([[0, 0]], [for (i = [0 : 12]) [cos(90 * i / 12), sin(90 * i / 12)]]));
module tube(ro, ri, h) rotate_extrude() polygon([[ri, 0], [ro, 0], [ro, h], [ri, h]]);
// An arch with round shoulders: a circle of radius w/2 up to ARCH_PHI deg, then straight on along the tangent to the peak.
// (a true semicircle would hang over by up to 90 deg at the crown; here nothing leans more than ARCH_PHI deg)
ARCH_PHI = 38;
function arch_pts(w, zs, h1) = let(a = w / 2, zc = zs + h1, rise = a * sin(ARCH_PHI) + a * cos(ARCH_PHI) * cos(ARCH_PHI) / sin(ARCH_PHI)) concat(
    [[-a, zs], [a, zs]],
    [for (k = [0 : 6]) [a * cos(ARCH_PHI * k / 6), zc + a * sin(ARCH_PHI * k / 6)]],
    [[0, zc + rise]],
    [for (k = [6 : -1 : 0]) [-a * cos(ARCH_PHI * k / 6), zc + a * sin(ARCH_PHI * k / 6)]]);
// the arch cut radially (at the angle 0) from radius r0 to r1
module arch_cut(w, zs, h1, r0, r1) translate([r0, 0, 0]) rotate([90, 0, 90]) linear_extrude(r1 - r0) polygon(arch_pts(w, zs, h1));

// ----------------------------------------------------------------------------------------
//  THE TOWER
// ----------------------------------------------------------------------------------------
T_R = 24;  T_RI = 19.5;         // outer / inner radius of the ground floor
N_ARC = 14;                     // arches per floor
G0 = 11;                        // height of the ground floor
FH = 9.5;                       // height of an arcaded floor
BH = 2.2;  LIP = 1.2;           // cornice: height and projection
NF = 6;                         // arcaded floors above the ground floor
TAPER = 0.023;                  // every floor is 2.3 % narrower than the one below
BEL_R = 17;  BEL_RI = 11;       // belfry: outer / inner radius
COLLAR_I = BEL_R + 0.3;  COLLAR_W = 1.6;  COLLAR_H = 2.5;     // the belfry stands inside this ring

function sc(i)   = 1 - TAPER * i;
function z_fl(i) = i == 0 ? 0 : G0 + BH + (i - 1) * (FH + BH);      // bottom of floor i (0 = ground floor, 1..NF arcaded)
function h_fl(i) = i == 0 ? G0 : FH;
function z_bd(i) = z_fl(i) + h_fl(i);                                // the cornice above floor i
FUN  = (T_RI * sc(NF) - BEL_RI) / SLF;                               // height of the funnel in the top cornice
BHT  = FUN + 0.8;                                                    // height of the top cornice
ZT   = z_bd(NF) + BHT + COLLAR_H;                                    // height of the tower

module arcade_unit() difference() {                                  // one arcaded floor, full size (the floors are scaled copies of it)
    union() {
        tube(T_R, T_RI, FH);
        for (k = [0 : N_ARC - 1]) rotate(270 + (k + 0.5) * 360 / N_ARC) translate([T_R - 0.5, 0, 0]) cylinder(r = 1.5, h = FH, $fn = 16);
    }
    for (k = [0 : N_ARC - 1]) rotate(270 + k * 360 / N_ARC) arch_cut(6.0, 1.3, 2.0, T_RI - 2, T_R + 4);
}
module ground_unit() difference() {                                  // blind arcade (niches) and the door
    tube(T_R, T_RI, G0);
    for (k = [1 : N_ARC - 1]) rotate(270 + k * 360 / N_ARC) arch_cut(6.0, 1.3, 2.0, T_R - 1.6, T_R + 4);
    rotate(270) arch_cut(7.6, 0, 3.4, T_R - 2.4, T_R + 4);
}
module band(i) {                                                     // cornice above floor i (i < NF)
    rol = T_R * sc(i);  ril = T_RI * sc(i);  riu = T_RI * sc(i + 1);  z0 = z_bd(i);
    rotate_extrude() polygon([[ril, z0], [rol, z0], [rol + LIP, z0 + LIP / SL], [rol + LIP, z0 + BH], [riu, z0 + BH]]);
}
module band_top() {                                                  // thick top cornice: funnel inside, collar on top
    rol = T_R * sc(NF);  ril = T_RI * sc(NF);  z0 = z_bd(NF);
    rotate_extrude() polygon([[ril, z0], [rol, z0], [rol + LIP, z0 + LIP / SL], [rol + LIP, z0 + BHT],
                              [COLLAR_I + COLLAR_W, z0 + BHT], [COLLAR_I + COLLAR_W, z0 + BHT + COLLAR_H], [COLLAR_I, z0 + BHT + COLLAR_H],
                              [COLLAR_I, z0 + BHT], [BEL_RI, z0 + BHT], [BEL_RI, z0 + FUN]]);
}
module tower(r) {
    if (r == "stone") for (i = [0 : NF]) translate([0, 0, z_fl(i)]) scale([sc(i), sc(i), 1]) { if (i == 0) ground_unit(); else arcade_unit(); }
    if (r == "band")  { for (i = [0 : NF - 1]) band(i); band_top(); }
}

// ----------------------------------------------------------------------------------------
//  THE BELFRY  (local frame: z = 0 is the bottom, the flag pole is at the top)
// ----------------------------------------------------------------------------------------
N_BEL = 10;  FB = 9.5;  BB = 2.2;  BLIP = 1.0;
CONE_H = BEL_RI / SLF;  ROOF_T = 1.6;
Z_ROOF = FB + BB + CONE_H + ROOF_T;                                  // flat top of the roof
PARAPET_H = 2.0;
POLE_R = 1.5;  POLE_H = 14;  Z_POLE = Z_ROOF + POLE_H;
SLIT_W = 1.3;  SLIT_Z0 = Z_POLE - 11;  SLIT_ZS = Z_POLE - 1.9;       // slit for the flag: straight up to SLIT_ZS, then a pointed head (no flat roof)
module belfry_wall() difference() {
    union() {
        tube(BEL_R, BEL_RI, FB);
        for (k = [0 : N_BEL - 1]) rotate(270 + (k + 0.5) * 360 / N_BEL) translate([BEL_R - 0.4, 0, 0]) cylinder(r = 1.2, h = FB, $fn = 16);
    }
    for (k = [0 : N_BEL - 1]) rotate(270 + k * 360 / N_BEL) arch_cut(4.8, 1.4, 1.6, BEL_RI - 2, BEL_R + 4);
}
module roof() translate([0, 0, FB + BB]) difference() {
    union() {
        cylinder(r = BEL_R, h = CONE_H + ROOF_T);
        translate([0, 0, CONE_H + ROOF_T]) tube(BEL_R - 0.4, BEL_R - 1.6, PARAPET_H);           // parapet
        translate([0, 0, CONE_H + ROOF_T - eps]) difference() {                                   // flag pole with a slit for the flag
            cylinder(r = POLE_R, h = POLE_H + eps, $fn = 32);
            translate([-5, 0, SLIT_Z0 - Z_ROOF]) rotate([90, 0, 90]) linear_extrude(10)
                polygon([[-SLIT_W / 2, 0], [SLIT_W / 2, 0], [SLIT_W / 2, SLIT_ZS - SLIT_Z0], [0, SLIT_ZS - SLIT_Z0 + SLIT_W / 2 / SL], [-SLIT_W / 2, SLIT_ZS - SLIT_Z0]]);
        }
    }
        cylinder(r1 = BEL_RI, r2 = 0.3, h = CONE_H);                                                    // cone under the roof
    for (k = [0 : N_BEL - 1]) rotate(270 + k * 360 / N_BEL) arch_cut(4.8, 1.8, 6.4, BEL_R - 1.2, BEL_R + 4);   // blind arcade on the upper stage
}
module top(r) {
    if (r == "stone") union() { belfry_wall(); roof(); }
    if (r == "band")  rotate_extrude() polygon([[BEL_RI, FB], [BEL_R, FB], [BEL_R + BLIP, FB + BLIP / SL], [BEL_R + BLIP, FB + BB], [BEL_RI, FB + BB]]);
}

// ----------------------------------------------------------------------------------------
//  THE FLAG   (flat: the tab on the left slides into the slit of the pole)    roles: green, white, red
// ----------------------------------------------------------------------------------------
FLAG_TAB = 3.0;  FLAG_L = 15.2;  FLAG_H = 8.0;  FLAG_T = SLIT_W - 2 * 0.15;
STRIPE = (FLAG_L - FLAG_TAB) / 3;
module flag(r) {
    x0 = r == "green" ? 0 : r == "white" ? FLAG_TAB + STRIPE : FLAG_TAB + 2 * STRIPE;
    x1 = r == "green" ? FLAG_TAB + STRIPE : r == "white" ? FLAG_TAB + 2 * STRIPE : FLAG_L;
    translate([x0, 0, 0]) cube([x1 - x0, FLAG_H, FLAG_T]);
}

// ----------------------------------------------------------------------------------------
//  THE BASE
// ----------------------------------------------------------------------------------------
BASE_R = 44;  BASE_H = 11;      // plinth (its top is the lawn)
PLAZA_R = 28.5;  PLAZA_H = 2.5; // raised stone plaza the tower stands on
SEAT = BASE_H + PLAZA_H;        // z of the bottom of the tower
POCKET_R = 19.4;  POCKET_D = 9;   // pocket of the 38 mm LED tea-light
COLLAR_R = T_R + 0.4;           // the tower stands inside a low ring
FENCE_R = 42.2;
TREES = [[150, 34.2, 30, 4.0], [118, 36, 22, 3.4], [62, 35.5, 27, 3.8], [27, 34.5, 20, 3.3], [195, 35.5, 25, 3.7], [335, 34.5, 23, 3.5], [92, 38, 16, 2.8]];   // angle, radius, height, foot radius
BUSHES = [[135, 36.5, 3.0], [172, 32, 2.6], [215, 36.5, 2.8], [8, 32, 2.6], [315, 37, 3.0], [45, 38.5, 2.6], [75, 32.5, 2.4]];                                        // angle, radius, radius of the dome

module plinth() rotate_extrude() polygon(concat([[0, 0], [BASE_R, 0]],
    [for (g = [3.7, 7.4]) each [[BASE_R, g - 0.8], [BASE_R - 0.55, g], [BASE_R, g + 0.8]]], [[BASE_R, BASE_H], [0, BASE_H]]));
module steps_cut() translate([-7, -33, BASE_H - 2]) cube([14, 6, 4]);
module lawn_zone() translate([0, 0, BASE_H - 1]) difference() {        // the top millimetre of the plinth around the plaza is lawn
    cylinder(r = BASE_R - 1.6, h = 2);
    translate([0, 0, -1]) cylinder(r = PLAZA_R, h = 4);
    translate([-4.5, -60, -2]) cube([9, 34, 5]);                          // the path
    translate([0, 0, 1 - BASE_H]) steps_cut();
}
module fence() {                                                         // a low wall with a pointed top and a post every 9 degrees (nothing hangs in the air)
    for (a = [0 : 9 : 359]) if (a < 254 || a > 286) rotate(a) translate([FENCE_R, 0, BASE_H]) {
        translate([-1, -1, 0]) cube([2, 2, 4.8]);
        translate([0, 0, 4.8 - eps]) rotate(45) cylinder(r1 = 1.414, r2 = 0.05, h = 1.2, $fn = 4);   // pyramid cap: 40-degree faces
    }
    translate([0, 0, BASE_H]) rotate(286) rotate_extrude(angle = 328, $fn = 80) translate([FENCE_R, 0])
        polygon([[-0.7, 0], [0.7, 0], [0.7, 3.0], [0, 3.0 + 0.7 / 0.84], [-0.7, 3.0]]);
}
module base(r) {
    if (r == "green") union() {
        intersection() { plinth(); lawn_zone(); }
        for (t = TREES) rotate(t[0]) translate([t[1], 0, BASE_H - eps]) cylinder(r1 = t[3], r2 = 0.35, h = t[2], $fn = 28);
        for (b = BUSHES) rotate(b[0]) translate([b[1], 0, BASE_H - eps]) scale([b[2], b[2], b[2] * 0.85]) hemi();
    }
    if (r == "stone") difference() {
        union() {
            difference() { plinth(); lawn_zone(); }
            translate([0, 0, BASE_H - eps]) cylinder(r = PLAZA_R, h = PLAZA_H + eps);
            translate([0, 0, SEAT - eps]) rotate(270 + 20) rotate_extrude(angle = 320) translate([COLLAR_R, 0]) square([2, 3]);
            translate([-7, -33, BASE_H - eps]) cube([14, 6, PLAZA_H / 2 + eps]);                              // the lower step
            fence();
        }
        translate([0, 0, SEAT - POCKET_D]) cylinder(r = POCKET_R, h = POCKET_D + 1);
    }
}

// ---- a tea-light, only for the renders ------------------------------------------------------
module tealight(r) {
    if (r == "cup")   translate([0, 0, SEAT - POCKET_D]) difference() { cylinder(r = 18.8, h = 12.5); translate([0, 0, 9.5]) cylinder(r = 16.4, h = 4); }
    if (r == "flame") translate([0, 0, SEAT - POCKET_D + 12]) scale([1, 1, 1.9]) sphere(r = 3.2, $fn = 24);
}

// ----------------------------------------------------------------------------------------
//  PIECES, ROLES, PLACEMENT
// ----------------------------------------------------------------------------------------
function piece_names() = ["base", "tower", "top", "flag"];
function piece_roles(p) =
    p == "base" ? ["stone", "green"] : p == "tower" ? ["stone", "band"] : p == "top" ? ["stone", "band"] :
    p == "flag" ? ["green", "white", "red"] : p == "tealight" ? ["cup", "flame"] : [];

module emit(p, r) {
    if (p == "base") base(r);
    else if (p == "tower") tower(r);
    else if (p == "top") top(r);
    else if (p == "flag") flag(r);
    else if (p == "tealight") tealight(r);
}
// the flag in the slit of the pole (world frame): the pole is sheared with the tower, the flag is not
ZP_MID  = ZT + (SLIT_Z0 + SLIT_ZS) / 2;                              // height of the middle of the slit above SEAT
module flag_assembled() translate([ZP_MID * LT - FLAG_TAB / 2, 0, SEAT + ZT + SLIT_Z0 + 0.4]) rotate([90, 0, 0]) translate([0, 0, -FLAG_T / 2]) children();
module place(p) {
    if (p == "base" || p == "tealight") children();
    else if (p == "tower") translate([0, 0, SEAT]) lean() children();
    else if (p == "top") translate([0, 0, SEAT]) lean() translate([0, 0, ZT]) children();
    else if (p == "flag") flag_assembled() children();
}
module local(p) {
    if (p == "tower" || p == "top") lean() children();
    else children();
}

// colours of the preview (the real filament colours are in tools/family_palette.py)
C_IVORY = "#EFE6D2";  C_GRAY = "#8E9098";  C_GREEN = "#2F7048";  C_RED = "#C8383E";
function filament(p, r) =
    p == "base" ? (r == "green" ? C_GREEN : C_IVORY) : (p == "tower" || p == "top") ? (r == "band" ? C_GRAY : C_IVORY) :
    p == "flag" ? (r == "green" ? C_GREEN : r == "red" ? C_RED : C_IVORY) :
    p == "tealight" ? (r == "flame" ? "#FFC060" : "#C9CED6") : "#999999";

module show(p) {
    for (r = piece_roles(p)) if (role == "" || role == r)
        color(filament(p, r)) { if (layout == "local") local(p) emit(p, r); else place(p) emit(p, r); }
}

if (part == "all" || part == "assembled") { for (p = piece_names()) show(p); }
else if (part == "manifest") { for (p = piece_names()) echo(PIECE = p, ROLES = piece_roles(p)); }
else if (part != "none") show(part);
