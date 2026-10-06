// =====================================================================
//  Samara Twister  -  print-in-place hanging wind spinner
//  Autumn Nest contest, entry #5 (Autumn in Motion)
//
//  A stylised maple key: a nut-shaped hub, a thin "spine" and cambered,
//  helically twisted wings with leaf-vein relief. The rotor spins on a
//  captive cone swivel that is printed in place (no support, no assembly).
//
//  Tested with OpenSCAD 2021.01. Open it in Customizer to tweak values.
// =====================================================================

/* [Part to render] */
// spinner = full model (slow with the old CGAL backend), hub = hub+shaft without wings, blade = ONE wing, blades = all wings, coupon = swivel clearance test
part = "spinner"; // [spinner, hub, blade, blades, coupon]

/* [Wings] */
// Number of wings (2 = classic maple key pair)
blades = 2; // [2:1:4]
// Wing length along the axis (mm)
blade_height = 60;
// Widest point of the wing, measured from the spine (mm)
blade_width = 22;
// Wall thickness of the wing (mm). 1.6 = 4 perimeters with a 0.4 nozzle
blade_thick = 1.6;
// Total twist over the wing length (degrees). Negative = opposite hand.
// Keep twist <= 180 * length / (PI * outer radius) (about 130 here) so the wing prints without support
twist = 130;
// Cup depth as a fraction of wing width (0 = flat, more = more torque and more drag)
camber = 0.16;
// Where the wing is widest, 0..1 along its length (real maple keys: 0.6-0.7)
peak_pos = 0.62; // [0.4:0.01:0.8]
// Wing tip shape: 1 = pointed, lower = rounder
tip_round = 0.6; // [0.3:0.05:1]

/* [Veins] */
// Height of the raised leaf-vein ribs (mm). 0 = smooth wings
vein_height = 0.5;
// Distance between veins along the axis (mm)
vein_pitch = 5;
// How fast veins sweep toward the tip (mm of height per mm of width)
vein_slope = 0.9;
// Higher = thinner ribs
vein_sharp = 3;

/* [Hub and nut] */
// Radius of the spine (mm)
stem_r = 5;
// Radius of the nut bulge (mm). The nut is a sphere of this radius
nut_r = 9;

/* [Swivel] */
// Radius of the shaft (mm)
shaft_r = 2;
// Radius of the cone flange and head (mm)
cone_r = 5;
// Print clearance between moving parts (mm). Tune with part = "coupon": 0.3 tight, 0.5 loose
clr = 0.4;
// Eyelet: outer radius, hole radius and thickness (mm)
eye_r = 5;
eye_hole = 1.8;
eye_t = 3;

/* [Quality] */
// Wing slice height (mm). Lower = smoother, slower to render
slice_dz = 0.5;
// Samples across the wing width
n_s = 24;
$fn = 64;

// ---------------------------------------------------------------------
//  Derived values
// ---------------------------------------------------------------------
base_t   = 0.8;                  // flat part of the cone flange (bed adhesion)
cone_h   = cone_r - shaft_r;     // 45 degree cone: no support needed
guide_len = 9;                   // length of the tight guide bore at each end
mid_bore_r = shaft_r + 1.0;      // loose bore in the middle (less stringing)
wall     = 1.8;
collar_r = cone_r + clr + wall;
collar_h = 7;
flare_h  = collar_r - stem_r;    // 45 degree flare from spine to collar
nut_h    = 2 * nut_r;
z0       = nut_h - 6;            // wing starts inside the nut
Hh       = z0 + blade_height + 8; // hub height = shaft length
r_in     = stem_r - 1.2;         // wing root is buried in the spine wall
n_z      = ceil(blade_height / slice_dz);
skew     = ln(0.5) / ln(peak_pos);
// Veins go on the wing face that looks UP while printing; on the other face the
// rib flanks would add overhang. Which face that is depends on the twist hand.
vein_side = (twist >= 0) ? -1 : 1;

// ---------------------------------------------------------------------
//  Wing geometry (a polyhedron swept along z, one cross-section per slice)
// ---------------------------------------------------------------------
function wprof(u) = max(0.9, blade_width * pow(max(0, sin(180 * pow(u, skew))), tip_round));

// returns [x, y, nx, ny, ridge] for position s (0..1) across a wing of width w
function wpt(w, s, zl) =
    let(xl = w * s,
        ty = camber * w * PI * cos(180 * s),
        tl = norm([w, ty]),
        rg = vein_height
             * pow(max(0, cos(360 * (zl - vein_slope * xl) / vein_pitch)), vein_sharp)
             * max(0, min(1, xl / 2, w * (1 - s) / 1.5)))
    [r_in + xl, camber * w * sin(180 * s), -ty / tl, w / tl, rg];

function wsect(i) =
    let(u = i / n_z, w = wprof(u), z = z0 + blade_height * u, a = twist * u,
        zl = z - z0,
        pl = [for (j = [0 : n_s]) let(q = wpt(w, j / n_s, zl), h = blade_thick / 2 + (vein_side > 0 ? q[4] : 0))
                [q[0] + q[2] * h, q[1] + q[3] * h]],
        pr = [for (j = [n_s : -1 : 0]) let(q = wpt(w, j / n_s, zl), h = blade_thick / 2 + (vein_side < 0 ? q[4] : 0))
                [q[0] - q[2] * h, q[1] - q[3] * h]])
    [for (p = concat(pl, pr))
        [p[0] * cos(a) - p[1] * sin(a), p[0] * sin(a) + p[1] * cos(a), z]];

module blade() {
    m = 2 * (n_s + 1);
    ring = [for (i = [0 : n_z]) each wsect(i)];
    // centre points close the two ends as fans (a single n-gon cap makes sliver triangles)
    cb = [for (k = [0 : 2]) (ring[0][k] + ring[m / 2][k]) / 2];
    ct = [for (k = [0 : 2]) (ring[n_z * m][k] + ring[n_z * m + m / 2][k]) / 2];
    pts = concat(ring, [cb, ct]);
    side = [for (i = [0 : n_z - 1], j = [0 : m - 1])
        let(a = i * m + j, b = i * m + (j + 1) % m,
            c = (i + 1) * m + (j + 1) % m, d = (i + 1) * m + j)
        each [[a, b, c], [a, c, d]]];
    bottom = [for (j = [0 : m - 1]) [(n_z + 1) * m, (j + 1) % m, j]];
    top    = [for (j = [0 : m - 1]) [(n_z + 1) * m + 1, n_z * m + j, n_z * m + (j + 1) % m]];
    polyhedron(points = pts, faces = concat(side, bottom, top), convexity = 10);
}

module blades() {
    for (k = [0 : blades - 1]) rotate([0, 0, 360 / blades * k]) blade();
}

// ---------------------------------------------------------------------
//  Swivel: cone flange at the bottom, cone head + eyelet at the top.
//  Profiles are in (r, z). The hub gets the same profile offset by clr.
// ---------------------------------------------------------------------
module flange2d() {
    polygon([[0, 0], [cone_r, 0], [cone_r, base_t],
             [shaft_r, base_t + cone_h], [0, base_t + cone_h]]);
}

module cavity2d() {
    intersection() {
        offset(delta = clr) flange2d();
        translate([0, -10]) square([50, 100]);
    }
}

module eyelet() {
    zc = eye_r - 0.2;   // centre height of the ring above the head
    // half-width of the flared base so its corners stay inside the head disc
    eye_base = 2 * sqrt(cone_r * cone_r - eye_t * eye_t / 4) - 0.2;
    translate([0, 0, Hh - 0.6]) rotate([90, 0, 0])
        linear_extrude(height = eye_t, center = true)
            difference() {
                // outline: round top, flared base as wide as the head (no overhang)
                hull() {
                    translate([0, zc + 0.6]) circle(r = eye_r - 0.6);
                    translate([-eye_base / 2, 0]) square([eye_base, 0.01]);
                }
                // teardrop hole: its top is a 45 degree point, so it prints without support
                translate([0, zc + 0.6]) hull() {
                    circle(r = eye_hole);
                    translate([0, eye_hole * sqrt(2)]) square(0.01, center = true);
                }
            }
}

module shaft() {
    cylinder(r = shaft_r, h = Hh);
    rotate_extrude() flange2d();
    translate([0, 0, Hh]) mirror([0, 0, 1]) rotate_extrude() flange2d();
    eyelet();
}

// ---------------------------------------------------------------------
//  Hub: acorn-like nut + spine + collar, with the swivel cavities cut out
// ---------------------------------------------------------------------
module hub_outer() {
    hull() {
        cylinder(r = collar_r, h = 0.01);
        translate([0, 0, nut_r]) sphere(r = nut_r);
    }
    rotate_extrude()
        polygon([[0, nut_r], [stem_r, nut_r],
                 [stem_r, Hh - collar_h - flare_h],
                 [collar_r, Hh - collar_h], [collar_r, Hh], [0, Hh]]);
}

module hub() {
    difference() {
        hub_outer();
        // guide bores (tight) at both ends, loose bore in between
        translate([0, 0, -1]) cylinder(r = shaft_r + clr, h = Hh + 2);
        // loose bore with a 45 degree chamfer at the top so the ceiling needs no support
        rotate_extrude()
            polygon([[0, guide_len], [mid_bore_r, guide_len],
                     [mid_bore_r, Hh - guide_len - (mid_bore_r - shaft_r - clr)],
                     [shaft_r + clr, Hh - guide_len], [0, Hh - guide_len]]);
        rotate_extrude() cavity2d();
        translate([0, 0, Hh]) mirror([0, 0, 1]) rotate_extrude() cavity2d();
    }
}

module rotor() {
    union() { hub(); blades(); }
}

// ---------------------------------------------------------------------
//  Parts
// ---------------------------------------------------------------------
module coupon() {
    // Short swivel only: print this first to dial in `clr` for your printer.
    ch = 18;
    difference() {
        union() {
            cylinder(r = collar_r, h = ch, $fn = 72);
        }
        translate([0, 0, -1]) cylinder(r = shaft_r + clr, h = ch + 2);
        rotate_extrude() cavity2d();
        translate([0, 0, ch]) mirror([0, 0, 1]) rotate_extrude() cavity2d();
    }
    cylinder(r = shaft_r, h = ch);
    rotate_extrude() flange2d();
    translate([0, 0, ch]) mirror([0, 0, 1]) rotate_extrude() flange2d();
}

if (part == "coupon") coupon();
else if (part == "blades") blades();
else if (part == "blade") blade();
else if (part == "hub") { shaft(); hub(); }
else {
    shaft();
    rotor();
}
