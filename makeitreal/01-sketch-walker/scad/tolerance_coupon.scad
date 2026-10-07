// Sketch Walker - printer calibration coupon (Bambu Lab P2S, 0.4 mm nozzle)
// Print this FIRST (~12 min). It decides the clearances used by walker.scad.
//   Row 1: pin holes  - push a 1.75 mm filament offcut through each; pick the hole that
//          lets it spin freely with no visible wobble.  -> pin_hole_d in walker_params.scad
//   Row 2: link-in-link slot (side gap) for plates stacked next to each other
//   Row 3: gear backlash test (two gears, three centre distances)
//   Row 4: D-shaft sockets (3 mm D shaft / printed shaft)
// Print flat, 0.2 mm layers, 3 walls, no supports. Turn OFF "ironing" and "fuzzy skin".

$fn = 48;
t = 3;                       // plate thickness (same as link thickness)
holes = [1.80, 1.90, 2.00, 2.10, 2.20, 2.30];
pitch = 11;

module label(s, size = 3) {
    linear_extrude(0.6) text(s, size = size, halign = "center", valign = "center");
}

// ---- base plate -------------------------------------------------------------------
difference() {
    union() {
        translate([-6, -6, 0]) cube([pitch * len(holes) + 4, 90, t]);
    }
    // row 1: pin holes
    for (i = [0 : len(holes) - 1])
        translate([i * pitch + 2, 66, -1]) cylinder(d = holes[i], h = t + 2);
    // row 4: shaft sockets: round + D-flat variants
    for (i = [0 : 5])
        translate([i * pitch + 2, 8, -1])
            intersection() {
                cylinder(d = 3.0 + i * 0.1, h = t + 2);
                translate([-5, -5 + (i % 2 == 0 ? 0 : 0.0), 0]) cube([10, 10, t + 2]);
            }
}
// engraved-ish labels (raised 0.6 mm) so you can tell the holes apart after printing
for (i = [0 : len(holes) - 1])
    translate([i * pitch + 2, 74, t]) label(str(holes[i]), 2.4);
for (i = [0 : 5])
    translate([i * pitch + 2, 15, t]) label(str(3.0 + i * 0.1), 2.4);

// ---- row 2: stacked-plate side gap test (two plates with a 0.2/0.3/0.4 mm gap) -----
for (k = [0 : 2]) {
    gap = 0.2 + 0.1 * k;
    translate([k * 22, 40, 0]) {
        cube([8, 14, t]);
        translate([8 + gap, 0, 0]) cube([8, 14, t]);
        translate([4, 17, t]) label(str(gap), 2.2);
    }
}

// ---- row 3: gear backlash test ----------------------------------------------------
// simple involute-ish spur gears (trapezoid teeth are plenty for a toy train)
module gear2d(teeth, mod = 1.5, play = 0) {
    r = teeth * mod / 2;
    union() {
        circle(r = r - mod * 1.1, $fn = 64);
        for (i = [0 : teeth - 1])
            rotate(i * 360 / teeth)
                polygon([[r - mod * 1.25, -mod * 0.62 + play], [r - mod * 1.25, mod * 0.62 - play],
                         [r + mod * 0.0 - play, mod * 0.34 - play], [r + mod * 0.0 - play, -mod * 0.34 + play]]);
    }
}
module gear(teeth, h = t, play = 0) {
    difference() {
        linear_extrude(h) gear2d(teeth, 1.5, play);
        translate([0, 0, -1]) cylinder(d = 3.2, h = h + 2);
    }
}
translate([pitch * len(holes) + 8, 0, 0]) {
    // three centre distances: nominal, +0.2, +0.4 (nominal = 20T@1.5 => pitch dia 30)
    for (k = [0 : 2]) {
        translate([0, k * 34, 0]) {
            gear(20);
            translate([30 + 0.2 * k + 0.1, 0, 0]) gear(20);
            translate([15, -15, 0]) label(str("+", 0.1 + 0.2 * k), 2.4);
        }
    }
}
