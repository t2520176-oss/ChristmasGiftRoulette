// =====================================================================================
//  HALLOWEEN GHOST KEYRING  -  ghost in a witch hat + your name       (OpenSCAD >= 2021.01)
//  Flat multi-colour keyring, stacked layers (all heights are multiples of 0.2 mm):
//     z 0.0-1.6  cream   : base + border + key-ring loop
//     z 1.6-2.8  black   : plate (under everything)     [loop: cream continues up to 2.8]
//     z 2.8-3.6  colours : ghost white, green name, gold star/buckle, purple band,
//                          black hat + eyes + mouth, pink cheeks + tongue
//  Every colour is its own part (own STL), all in the same coordinates:
//     openscad -D 'PART="green"' -o green.stl keyring.scad     PART = all | cream | black | white | green | gold | purple | pink
//  Change the name:  NAME = "Liam";   (long names: lower TEXT_SIZE or raise KEY_SCALE)
// =====================================================================================
NAME      = "Liam";
TEXT_SIZE = 14.4;           // cap height of the name in mm (design units, before KEY_SCALE)
TEXT_X    = 2.6;            // left edge of the name
KEY_SCALE = 0.9;            // overall size: 0.9 -> about 51 x 54 mm
HOLE_D    = 4.8;            // key-ring hole
PART      = "all";
$fn       = 48;

T_CREAM = 1.6;  T_BASE = 2.8;  T_TOP = 3.6;      // layer tops
EYELET  = [49.6, 35.4];                          // key-ring loop centre

// ------------------------------- 2-D building blocks (design units = mm / KEY_SCALE) ---------------
module ell(c, r, a = 0) { translate(c) rotate(a) scale([r[0], r[1]]) circle(1); }
module rrect(x0, y0, x1, y1, r) { hull() for (x = [x0 + r, x1 - r], y = [y0 + r, y1 - r]) translate([x, y]) circle(r); }

module plate2d() { rrect(0, 0, 50, 32, 7); }

// ---- witch hat: tilted brim + a cone that bends to the right
module brim2d() { ell([21.5, 33.2], [14.2, 3.1], 8); }
module cone2d() {
    hull() { ell([21.8, 34.2], [9.6, 1.8], 8); translate([25.2, 43]) circle(r = 4.6); }
    hull() { translate([25.2, 43]) circle(r = 4.6); translate([29.6, 50]) circle(r = 2.7); }
    hull() { translate([29.4, 50]) circle(r = 2.6); translate([35, 54.4]) circle(r = 1.4); }
}
module hat2d() { union() { brim2d(); cone2d(); } }
module band2d() { intersection() { cone2d(); translate([22.6, 38.6]) rotate(7) translate([-20, -2.1]) square([40, 4.2]); } }
module buckle2d() { translate([22.6, 38.6]) rotate(7) difference() { rrect(-3.5, -2.7, 3.5, 2.7, 0.9); rrect(-1.8, -1.2, 1.8, 1.2, 0.4); } }

// ---- ghost: round head, scalloped hem, a low left arm and a raised right arm
module ghost2d() {
    union() {
        translate([22, 24]) circle(r = 12.5);
        translate([9.5, 9.6]) square([25, 14.4]);
        for (x = [13.7, 22, 30.3]) translate([x, 9.8]) circle(r = 4.15);
        translate([8.8, 21.0]) circle(r = 3.9);
        ell([36.0, 22.4], [4.8, 3.1], 38);
    }
}
module eyes2d()   { for (x = [18.4, 25.6]) ell([x, 25.8], [1.25, 1.9]); }
module cheeks2d() { for (x = [15.0, 29.0]) ell([x, 21.5], [2.2, 1.35]); }
module mouth2d()  { translate([22, 22.7]) intersection() { circle(r = 2.7); translate([-3, -3]) square([6, 3]); } }
module tongue2d() { intersection() { mouth2d(); ell([22, 20.5], [1.7, 1.2]); } }

// ---- name (rounded, chunky) and star
module name2d() {
    translate([TEXT_X, 4.2]) offset(r = 0.55) offset(delta = -0.55)
        text(NAME, size = TEXT_SIZE, font = "Liberation Sans:style=Bold", halign = "left", spacing = 1.13);
}
module star2d() {
    translate([44.6, 22.4]) offset(r = 0.55) offset(delta = -0.55)
        polygon([for (i = [0 : 9]) let(a = 90 + i * 36, r = (i % 2 == 0) ? 4.6 : 2.05) [r * cos(a), r * sin(a)]]);
}

// ------------------------------- colour regions (each already clipped by what lies in front of it) ---------------
module black_base2d() { union() { plate2d(); hat2d(); offset(delta = 1.0) ghost2d(); offset(delta = 0.8) star2d(); } }
module cream2d()      { union() { offset(r = 2.0) black_base2d(); translate(EYELET) circle(r = 5.3); } }

module hat_black2d()  { difference() { hat2d(); band2d(); buckle2d(); } }
module band_purple2d(){ difference() { band2d(); buckle2d(); } }
module white2d()      { difference() { ghost2d(); hat2d(); name2d(); star2d(); eyes2d(); cheeks2d(); mouth2d(); } }
module gold2d()       { union() { star2d(); buckle2d(); } }
module black_top2d()  { union() { hat_black2d(); eyes2d(); difference() { mouth2d(); tongue2d(); } } }
module pink2d()       { union() { cheeks2d(); tongue2d(); } }

module slab(z0, z1) { translate([0, 0, z0]) linear_extrude(height = z1 - z0, convexity = 8) children(); }

module cream() {
    difference() {
        union() { slab(0, T_CREAM) cream2d(); slab(T_CREAM, T_BASE) difference() { translate(EYELET) circle(r = 5.3); } }
        translate(EYELET) translate([0, 0, -1]) cylinder(d = HOLE_D, h = 10);
    }
}
module black() { union() { slab(T_CREAM, T_BASE) black_base2d(); slab(T_BASE, T_TOP) black_top2d(); } }
module white()  { slab(T_BASE, T_TOP) white2d(); }
module green()  { slab(T_BASE, T_TOP) name2d(); }
module gold()   { slab(T_BASE, T_TOP) gold2d(); }
module purple() { slab(T_BASE, T_TOP) band_purple2d(); }
module pink()   { slab(T_BASE, T_TOP) pink2d(); }

// single-colour version: the top-layer regions are merged in 2-D first, so touching colours become ONE clean solid
module top_union2d() { union() { black_top2d(); white2d(); name2d(); gold2d(); band_purple2d(); pink2d(); } }
module onecolor()    { union() { cream(); slab(T_CREAM, T_BASE) black_base2d(); slab(T_BASE, T_TOP) top_union2d(); } }

module part() {
    if (PART == "all") { cream(); black(); white(); green(); gold(); purple(); pink(); }
    else if (PART == "onecolor") onecolor();
    else if (PART == "cream") cream(); else if (PART == "black") black(); else if (PART == "white") white();
    else if (PART == "green") green(); else if (PART == "gold") gold(); else if (PART == "purple") purple(); else if (PART == "pink") pink();
}
scale([KEY_SCALE, KEY_SCALE, 1]) part();
