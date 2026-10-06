// =====================================================================
//  Maple Chime  -  pentatonic wind chime with a leaf sail
//  Autumn Nest contest, entry #6 (Autumn in Motion)
//
//  A maple-leaf canopy hangs five tuned aluminium tubes (one per lobe, one per
//  note). An acorn striker hangs from the middle on a cord, a leaf-shaped sail
//  hangs below it and catches the wind. The striker is a plain pendulum: it has
//  no bearing, so there is no friction to overcome.
//
//  Printed parts: canopy, striker, sail, drill jig.
//  Bought parts:  5 aluminium tubes, cord, see README.
//
//  Tested with OpenSCAD 2021.01.
// =====================================================================

/* [Part to render] */
// canopy / striker / sail / jig = printable parts, asm = assembled preview (use asm_part)
part = "canopy"; // [canopy, striker, sail, jig, asm]
// Which piece of the assembled preview to export
asm_part = "canopy"; // [canopy, tubes, striker, sail, cords]

/* [Tubes - not printed] */
// Outer diameter (mm)
tube_od = 12;
// Wall thickness (mm)
tube_wall = 1;
// Young's modulus (GPa), 6063 aluminium is about 69
tube_E_gpa = 69;
// Density (kg/m3)
tube_rho = 2700;
// Pitches in Hz, one tube each (C5 D5 E5 G5 A5 = C major pentatonic)
notes = [523.25, 587.33, 659.25, 783.99, 880.00];
// Where the striker hits, as a fraction of the tube length from its top end
strike_frac = 0.42;

/* [Layout] */
// Radius of the striker's rim (mm)
strike_r = 13;
// Rest gap between the striker rim and a tube (mm). Smaller rings in lighter wind
gap = 7;
// Canopy underside to the striker rim (mm)
pend_len = 170;
// Hole for cord, two strands plus a knot pocket above it (mm)
hole_d = 3.4;

/* [Canopy and sail] */
canopy_r = 60;
canopy_t = 4;
sail_r = 52;
sail_t = 1.2;
// Radius of the canopy's centre disc as a fraction of canopy_r (the lobes grow out of it)
valley = 0.55;
// Edge shape of a lobe: 1 = straight, lower = more rounded (convex)
lobe_p = 0.55;
// Depth of the sawtooth edge, fraction of the radius
tooth = 0.06;
// Depth of the engraved veins on the canopy (mm)
vein_cut = 0.8;
// Height of the raised veins on the sail (mm)
vein_rib = 0.4;

/* [Quality] */
$fn = 64;

// ---------------------------------------------------------------------
//  Derived values and tube tuning (Euler-Bernoulli, free-free tube)
// ---------------------------------------------------------------------
n_tubes   = len(notes);
tube_id   = tube_od - 2 * tube_wall;
tube_R    = strike_r + gap + tube_od / 2;       // tube axis radius around the centre
node_frac = 0.2241;                             // nodes of the fundamental, from each end
rg_m      = sqrt(tube_od * tube_od + tube_id * tube_id) / 4 / 1000;
tune_k    = 22.3733 / (2 * PI) * sqrt(tube_E_gpa * 1e9 / tube_rho) * rg_m;
function tlen(f) = 1000 * sqrt(tune_k / f);     // tube length for pitch f (mm)

l_max     = tlen(min(notes));
sail_hole_z = -pend_len - (1 - strike_frac) * l_max - 25;
rim_z     = 26.25;                              // striker rim height above its tip (mm)

assert(pend_len >= strike_frac * l_max + 15, "pend_len too short: the longest tube would touch the canopy");
assert(tube_R + (hole_d + 4) / 2 + 2 < canopy_r * valley, "canopy too small: the cord holes would reach the leaf edge");

// ---------------------------------------------------------------------
//  Maple leaf outline. A leaf is a list of lobes [direction deg, length, half-width deg];
//  the outline is star shaped about the origin: r(theta) = max of all lobes and a centre disc.
// ---------------------------------------------------------------------
function sym_lobes(n) = [for (k = [0 : n - 1]) [90 + 360 / n * k, 1, 180 / n]];
// a natural maple: long centre lobe, two upper side lobes, two small lower lobes
natural_lobes = [[90, 1, 36], [145, 0.92, 34], [35, 0.92, 34], [210, 0.62, 30], [330, 0.62, 30]];

function lobe_r(th, l) =
    let(d = abs(((th - l[0] + 540) % 360) - 180), x = max(0, 1 - d / l[2]))
    l[1] * pow(x, lobe_p) * (1 + tooth * cos(1440 * d / l[2]));

function leaf_r(th, lobes, R, base) = R * max(base, max([for (l = lobes) lobe_r(th, l)]));

module leaf2d(lobes, R, base) {
    polygon([for (a = [0 : 1 : 359]) leaf_r(a, lobes, R, base) * [cos(a), sin(a)]]);
}

module vein(p0, p1, w = 1.0) {
    hull() { translate(p0) circle(d = w, $fn = 12); translate(p1) circle(d = w, $fn = 12); }
}

// a main vein down every lobe and two pairs of side veins
module leaf_veins(lobes, R, r0) {
    for (l = lobes) rotate(l[0]) {
        vein([r0, 0], [0.9 * l[1] * R, 0]);
        for (f = [0.5, 0.68]) for (s = [-1, 1])
            vein([f * l[1] * R, 0],
                 [f * l[1] * R + 0.13 * l[1] * R * cos(40), s * 0.13 * l[1] * R * sin(40)], 0.8);
    }
}

// ---------------------------------------------------------------------
//  Canopy: a leaf plate with a knot pocket over every cord hole
// ---------------------------------------------------------------------
module cord_hole(t) {
    translate([0, 0, -1]) cylinder(d = hole_d, h = t + 2);
    translate([0, 0, t - 2]) cylinder(d1 = hole_d, d2 = hole_d + 4, h = 2.01);  // countersink for the knot
}

module canopy() {
    difference() {
        linear_extrude(canopy_t) leaf2d(sym_lobes(n_tubes), canopy_r, valley);
        for (k = [0 : n_tubes - 1])
            rotate(90 + 360 / n_tubes * k) translate([tube_R, 0, 0]) cord_hole(canopy_t);
        cord_hole(canopy_t);                                        // striker cord
        for (k = [0 : 2])                                           // hanging cords (tripod)
            rotate(90 + 120 * k + 60) translate([14, 0, 0]) cord_hole(canopy_t);
        translate([0, 0, canopy_t - vein_cut])
            linear_extrude(vein_cut + 1) leaf_veins(sym_lobes(n_tubes), canopy_r, tube_R + 6);
    }
}

// ---------------------------------------------------------------------
//  Striker: an acorn that prints tip down; every slope is <= 45 degrees
// ---------------------------------------------------------------------
module striker() {
    k = strike_r / 13;
    prof = [[0, 0], [3.5, 0], [11, 9.6], [11.9, 12.5], [12, 15], [11.5, 19], [10, 22.5],
            [13, 25.5], [13, 27], [12.7, 29.5], [11.8, 31.8], [10, 33.8], [7.5, 35.2],
            [6, 35.6], [0, 35.6]];
    difference() {
        scale(k) rotate_extrude() polygon(prof);
        translate([0, 0, -1]) cylinder(d = hole_d, h = 50 * k);
        translate([0, 0, 35.6 * k - 4]) cylinder(d = 7, h = 5);     // knot pocket
    }
}

// ---------------------------------------------------------------------
//  Sail: a natural maple leaf that hangs from its stem. Veins are raised ribs.
// ---------------------------------------------------------------------
sail_hole_y = 0.78 * sail_r;      // the cord hole, at the end of the stem (leaf centre = origin)

module sail_outline() {
    scale([1, -1]) leaf2d(natural_lobes, sail_r, 0.30);       // lobes point down
    hull() { circle(d = 4.5); translate([0, sail_hole_y]) circle(d = 9); }   // stem and hole boss
}

module sail() {
    difference() {
        union() {
            linear_extrude(sail_t) sail_outline();
            translate([0, 0, sail_t]) linear_extrude(vein_rib) scale([1, -1]) leaf_veins(natural_lobes, sail_r, 0.1 * sail_r);
        }
        translate([0, sail_hole_y, -1]) cylinder(d = hole_d, h = sail_t + vein_rib + 2);
    }
}

// ---------------------------------------------------------------------
//  Drill jig: holds a tube and guides a 3 mm drill through the node
// ---------------------------------------------------------------------
module jig() {
    w = 36; d = 28; h = 22; bore = tube_od + 0.4;
    difference() {
        translate([-w / 2, -d / 2, 0]) cube([w, d, h]);
        // tube bore along x. Profile is drawn in (y, z) with a 45 degree teardrop top so it prints
        // without support; rotate([90,0,90]) maps 2D x -> world y, 2D y -> world z, extrusion -> world x
        translate([0, 0, h / 2]) rotate([90, 0, 90]) linear_extrude(2 * w, center = true) {
            circle(d = bore);
            polygon([[bore / 2 * cos(45), bore / 2 * sin(45)], [0, bore / 2 * sqrt(2)],
                     [-bore / 2 * cos(45), bore / 2 * sin(45)]]);
        }
        // drill guide through the bore axis, perpendicular to the tube
        translate([0, 0, h / 2]) rotate([90, 0, 0]) cylinder(d = 3.2, h = d + 2, center = true);
        // window to see the pencil mark of the node
        translate([-1, -d / 2 - 1, h / 2]) cube([2, d + 2, h]);
    }
}

// ---------------------------------------------------------------------
//  Assembled preview (z = 0 is the canopy underside)
// ---------------------------------------------------------------------
module tubes_asm() {
    for (k = [0 : n_tubes - 1]) {
        l = tlen(notes[k]);
        rotate(90 + 360 / n_tubes * k) translate([tube_R, 0, -pend_len - (1 - strike_frac) * l])
            difference() {
                cylinder(d = tube_od, h = l);
                translate([0, 0, -1]) cylinder(d = tube_id, h = l + 2);
            }
    }
}

module cord(p0, p1) {
    hull() { translate(p0) sphere(d = 1.4, $fn = 8); translate(p1) sphere(d = 1.4, $fn = 8); }
}

module cords_asm() {
    for (k = [0 : n_tubes - 1]) {
        l = tlen(notes[k]);
        a = 90 + 360 / n_tubes * k;
        node_z = -pend_len + (strike_frac - node_frac) * l;
        for (s = [-1, 1])   // both strands run beside the tube, tangentially offset
            rotate(a) cord([tube_R, s * (tube_od / 2 + 0.8), node_z], [tube_R, 0, canopy_t + 4]);
    }
    cord([0, 0, canopy_t + 4], [0, 0, sail_hole_z]);                // striker + sail cord
    for (k = [0 : 2])                                               // hanging tripod
        rotate(90 + 120 * k + 60) cord([14, 0, canopy_t], [0, 0, canopy_t + 70]);
}

module asm() {
    if (asm_part == "canopy") canopy();
    else if (asm_part == "tubes") tubes_asm();
    else if (asm_part == "striker") translate([0, 0, -pend_len - rim_z]) striker();
    else if (asm_part == "sail")
        translate([0, 0, sail_hole_z - sail_hole_y]) rotate([90, 0, 0]) sail();
    else if (asm_part == "cords") cords_asm();
}

if (part == "canopy") canopy();
else if (part == "striker") striker();
else if (part == "sail") sail();
else if (part == "jig") jig();
else asm();

echo(str("tube lengths (mm, Euler-Bernoulli): ", [for (f = notes) round(tlen(f) * 10) / 10]));
echo(str("tube axis radius ", tube_R, " mm, sail hole at z=", round(sail_hole_z), " mm"));
