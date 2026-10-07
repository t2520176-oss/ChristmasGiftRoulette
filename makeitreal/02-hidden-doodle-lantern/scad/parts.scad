// Hidden Doodle Lantern - lid, light riser and the 10-minute test kit.   OpenSCAD >= 2021.01
// Numbers must match design/lantern.py (checked by design/check_consistency.py).
//   openscad -D 'PART="lid"' -o lid.stl parts.scad     PART = lid | riser | testkit
R_OUT = 42.5;        // lantern outer radius
T_INK = 3.2;         // thick wall
LID_CLEAR = 0.35;    // radial clearance of the plug  <- calibrate with the test kit ring
PART = "lid";
$fn = 120;

plug_r  = R_OUT - T_INK - LID_CLEAR;
module lid() {
    // PRINT ORIENTATION = INSTALLED ORIENTATION UPSIDE DOWN: plug on the bed, a 45-degree cone widens to the flange, knob on top.
    // No overhangs, no supports.  Installed: plug goes into the tube, the cone seats on the rim, the knob is outside.
    flange_r = R_OUT + 1.0;
    cone_h   = flange_r - plug_r;                                   // 45 deg
    difference() {
        union() {
            cylinder(r = plug_r, h = 5);                             // plug (goes inside the tube)
            translate([0, 0, 5]) cylinder(r1 = plug_r, r2 = flange_r, h = cone_h);   // self-centring seat
            translate([0, 0, 5 + cone_h]) cylinder(r = flange_r, h = 2);             // flange
            translate([0, 0, 7 + cone_h]) cylinder(r1 = 7, r2 = 6, h = 8);           // knob
        }
        // 8 small vertical holes: dots of light on the ceiling
        for (a = [0 : 45 : 359]) rotate(a) translate([24, 0, -1]) cylinder(d = 3, h = 30, $fn = 24);
    }
}
module riser() {
    // 45 mm tall tube: the tealight sits on top, so the light is at mid-height of the lantern (prints upright)
    difference() { cylinder(r = 19, h = 45); translate([0, 0, -1]) cylinder(r = 15.5, h = 47); }
}
module testkit() {
    // 1) glow swatch: wall thickness steps 0.8 / 1.6 / 2.4 / 3.2 mm -> hold it in front of your LED
    for (i = [0 : 3]) translate([i * 17, 0, 0]) {
        cube([15, 15, 0.8 * (i + 1)]);
    }
    // 2) lid-fit ring: 6 mm slice of the lantern's top frame (try the lid plug on it)
    translate([40, 60, 0]) difference() { cylinder(r = R_OUT, h = 6, $fn = 180); translate([0, 0, -1]) cylinder(r = R_OUT - T_INK, h = 8, $fn = 180); }
    // 3) hole gauge: 2 / 3 / 4 / 6 mm through-holes in a 3.2 mm plate
    translate([0, 22, 0]) difference() {
        cube([68, 15, T_INK]);
        for (i = [0 : 3]) translate([8 + i * 17, 7.5, -1]) cylinder(d = [2, 3, 4, 6][i], h = T_INK + 2, $fn = 32);
    }
}
if (PART == "lid") lid(); else if (PART == "riser") riser(); else if (PART == "testkit") testkit();
