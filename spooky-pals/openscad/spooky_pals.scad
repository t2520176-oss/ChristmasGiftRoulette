/*
 * SPOOKY PALS  -  Swap-a-Costume Halloween Pet Pedestals
 * ------------------------------------------------------------------
 * One parametric OpenSCAD file that builds 6 pets x 6 costumes x 3 bases
 * (108 mix-and-match combinations) with a personalised name ramp.
 *
 * Design rules (why it prints with NO supports):
 *   - every part is modelled so that its flat face lies on the bed
 *   - every surface is <= 45 deg from vertical, bridges are <= 8 mm
 *   - sockets have sloped roofs, pegs are tapered, ears/tails are anchored
 *     on the bed or on the body
 *   - hats / capes are separate, universal parts. They are carved
 *     automatically around the ears / tail of the chosen pet.
 *
 * OpenSCAD 2021.01 or newer.   Units: mm.
 *   Front of the model is -Y (use the "Front" view).
 */

/* [What to build] */
// Which piece to show / export
part = "assembled"; // [assembled:Assembled preview, plate:Print plate (all parts laid out), pet:Pet body, marks:Light fur markings (multicolor), face:Eyes / nose / mouth (multicolor), shine:Eye shine (multicolor), hat:Hat or headpiece, trim:Hat trim (multicolor), cape:Cape or back piece, charm:Charm (bow / clasp), base:Base with name ramp, deco:Base decor (multicolor), text:Name and paw prints (multicolor), none:Nothing]
pet = "dog"; // [dog, cat, rabbit, hamster, bird, squirrel]
costume = "witch"; // [none, vampire, witch, ghost, pumpkin, devil, wizard]
base_style = "stump"; // [stump:Pumpkin-patch stump, crypt:Haunted crypt, cauldron:Witch cauldron]
// Name on the ramp (A-Z, 0-9, up to ~9 characters)
name = "LUNA";

/* [Layout of single parts] */
// print = lay the part in its supportless print orientation, assembled = keep the position it has on the finished model
layout = "print"; // [print, assembled]

/* [Options] */
// Split into separate colour parts (eyes, markings, trim, decor, name) for multi-colour / AMS printing
multicolor = false;
// Pumpkins, leaves, tombstones ... on the base
base_decor = true;
// Raised (true) or engraved (false) name
raise_text = true;
// Font for the name. Liberation Sans ships with OpenSCAD; any installed bold font works.
font = "Liberation Sans:style=Bold";
// Overall scale (1 = about 92 mm tall with hat)
model_scale = 1.0;

/* [Magnets (optional)] */
// Pockets for disc magnets (hat <-> head, pet <-> base) instead of the cone pegs. Mind the polarity!
magnets = false;
// Magnet diameter / thickness in mm (a 5 x 2 mm neodymium disc is typical)
magnet_d = 5;
magnet_h = 2;

/* [Fit and quality] */
// Clearance between mating parts (mm). 0.15 suits a 0.4 nozzle; use 0.2 for rougher printers.
tol = 0.15;
// Sphere resolution (24 = fast preview, 40+ = final export)
fn = 32;
// Preview colours (no influence on the STL)
show_colors = true;

/* [Colours - preview only] */
fur_color    = "#D9A25B";
marks_color  = "#F6EEDD";
face_color   = "#2B1B17";
base_color   = "#6B4A33";
text_color   = "#F3E2C0";

/* [Hidden] */
$fn = fn;
fuse_face = !multicolor;                      // single colour: eyes, nose, mouth are part of the pet
MG_D = magnet_d + 0.25;                       // magnet pocket (press fit)
MG_H = magnet_h + 0.2;
eps = 0.01;

// ---------------------------------------------------------------------
//  SHARED ANATOMY  -  every pet uses the same torso and head so that
//  every cape / hat fits every pet.
// ---------------------------------------------------------------------
HC    = [0, -2.5, 34];          // head centre
HR    = [20, 18, 17];           // head radii (x, y, z)
HAT_Z = HC[2] + HR[2] - 2;      // flat crown where hats sit (z = 49)
TAX   = [0, 1.2];               // torso axis (x, y)
BH    = 14;                     // base height below the deck (z = 0)

BOSS_R1 = 5.8;  BOSS_R2 = 0.5;  BOSS_H = 4.8;   // pet-to-base peg: a cone, so the socket roof is never flat
HPEG_R1 = 4.4;  HPEG_R2 = 0.6;  HPEG_H = 4.4;   // head-to-hat peg

// ---------------------------------------------------------------------
//  SMALL HELPERS
// ---------------------------------------------------------------------
module ell(r, c = [0, 0, 0], g = 0) {
    translate(c) scale([r[0] + g, r[1] + g, r[2] + g]) sphere(1);
}

function P3(p) = [p[0], p[1], p[2]];

// chain of hulled spheres, pts = [[x,y,z,r], ...]  (g grows every radius)
module chain(pts, g = 0) {
    for (i = [0 : len(pts) - 2])
        hull() {
            translate(P3(pts[i]))     sphere(pts[i][3] + g);
            translate(P3(pts[i + 1])) sphere(pts[i + 1][3] + g);
        }
}

// point on the head ellipsoid. az = 0 is straight ahead (-Y), el = 0 is the equator
function hs(az, el, off = 0) = [
    (HR[0] + off) * sin(az) * cos(el) + HC[0],
    -(HR[1] + off) * cos(az) * cos(el) + HC[1],
    (HR[2] + off) * sin(el) + HC[2]];

// point on the front of an ellipsoid c/r at lateral x and height z
function efront(c, r, x, z) = [x,
    c[1] - r[1] * sqrt(max(0, 1 - pow((x - c[0]) / r[0], 2) - pow((z - c[2]) / r[2], 2))),
    z];

module at_head(s, az, el, out = 0, back = 0, off = 0) {
    translate(hs(s * az, el, off)) rotate([0, s * out, 0]) rotate([-back, 0, 0]) children();
}

// ---------------------------------------------------------------------
//  COMMON BODY
// ---------------------------------------------------------------------
module torso(g = 0) {
    hull() {
        ell([13.5, 12.5, 6],  [0, 1.5, 3],  g);   // hips (spans z -3 .. 9)
        ell([11.5, 10.5, 8.5], [0, 1.0, 17.5], g); // shoulders (z 9 .. 26)
    }
    for (s = [-1, 1]) ell([6.6, 10.5, 7], [s * 11.2, 3.2, 5.2], g);   // haunches
}

module legs(g = 0) {
    for (s = [-1, 1])
        hull() {                                                       // stout front legs
            ell([4.3, 4.4, 5.6], [s * 6.7, -9.3, 12.0], g);
            ell([4.9, 6.0, 3.2], [s * 6.9, -11.6, 2.4], g);             // paw
        }
}

module bib(g = 0) { ell([9.5, 7.5, 8.5], [0, -6.5, 15.5], g); }

// head with a rounded "mochi" jaw and the flat crown that carries the hats
module head(g = 0) {
    intersection() {
        hull() {
            ell(HR, HC, g);
            ell([17.2, 15.6, 9], [0, -3, 25.5], g);
        }
        translate([-60, -60, -10]) cube([120, 120, HAT_Z + 10 + (g > 0 ? g : 0)]);
    }
}

module head_peg() {
    if (!magnets) translate([HC[0], HC[1], HAT_Z - eps]) cylinder(r1 = HPEG_R1, r2 = HPEG_R2, h = HPEG_H + eps);
}
module head_magnet() { translate([HC[0], HC[1], HAT_Z - MG_H]) cylinder(d = MG_D, h = MG_H + eps); }
module foot_magnets(up = 0) {               // two pockets at the centres of the stadium shaped boss
    for (sx = [-5, 5]) translate([TAX[0] + sx, TAX[1], -eps - up]) cylinder(d = MG_D, h = MG_H + eps);
}

// ---------------------------------------------------------------------
//  EARS / TAILS / FACE PARTS PER SPECIES
// ---------------------------------------------------------------------
module ear_leaf(len = 12, w = 10, th = 4.5, g = 0) {
    hull() {
        scale([w / 2 + g, th / 2 + g, w * 0.45 + g]) sphere(1);
        translate([0, 0, len]) scale([w * 0.2 + g, th * 0.32 + g, w * 0.2 + g]) sphere(1);
    }
}

module cat_ear(len = 12, w = 11, th = 4, g = 0) {
    hull() {
        scale([w / 2 + g, th / 2 + g, 2 + g]) sphere(1);
        translate([0, 0, len]) sphere(0.9 + g);
    }
}

module dog_ears(g = 0) {                       // perky ears with a folded-over tip
    for (s = [-1, 1])
        at_head(s, 58, 40, out = 24, back = 0) {
            ear_leaf(len = 8.5, w = 12, th = 4.6, g = g);
            translate([0, 0, 7.4]) rotate([42, 0, 0]) rotate([0, s * 8, 0])
                ear_leaf(len = 6.5, w = 9.5, th = 4.2, g = g);
        }
}
module cat_ears(g = 0) {
    for (s = [-1, 1]) at_head(s, 57, 44, out = 18, back = 4) cat_ear(len = 14, w = 13, th = 4.4, g = g);
}
module rabbit_ears(g = 0) {
    for (s = [-1, 1])
        at_head(s, 50, 50, out = 8, back = 6)
            hull() {
                ell([5.0, 2.7, 4.6], [0, 0, 0], g);
                ell([4.3, 2.4, 4.2], [0, 0, 24], g);
            }
}
module hamster_ears(g = 0) {
    for (s = [-1, 1]) at_head(s, 60, 42, out = 22, back = 0) ell([5.0, 2.8, 5.0], [0, 0, 1.5], g);
}
module squirrel_ears(g = 0) {
    for (s = [-1, 1]) at_head(s, 56, 46, out = 15, back = 2) {
        cat_ear(len = 10.5, w = 9.4, th = 4.0, g = g);
        translate([0, 0, 10.6]) sphere(1.35 + g);                           // tuft
    }
}

module bird_forelock(g = 0) {
    for (i = [-1, 0, 1])
        translate(hs(i * 16, 50, 0)) rotate([0, i * 20, 0]) rotate([-30, 0, 0])
            hull() {
                scale([1.9 + g, 1.9 + g, 1.5 + g]) sphere(1);
                translate([0, 0, 6.2 - abs(i) * 1.2]) sphere(0.65 + g);
            }
}

module ears_of(p, g = 0) {
    if (p == "dog") dog_ears(g);
    else if (p == "cat") cat_ears(g);
    else if (p == "rabbit") rabbit_ears(g);
    else if (p == "hamster") hamster_ears(g);
    else if (p == "squirrel") squirrel_ears(g);
    else if (p == "bird") bird_forelock(g);
}

// muzzle ellipsoid [centre, radii]: nose and mouth are placed on it
function muzzle(p) =
    p == "dog"      ? [[0, -18.3, 29.4], [8.0, 6.5, 5.8]] :
    p == "cat"      ? [[0, -18.0, 29.3], [6.6, 4.8, 4.3]] :
    p == "rabbit"   ? [[0, -18.0, 29.3], [6.6, 5.2, 4.6]] :
    p == "hamster"  ? [[0, -18.0, 29.2], [6.0, 4.8, 4.2]] :
    p == "squirrel" ? [[0, -18.0, 29.3], [6.4, 5.2, 4.6]] :
                      [[0, -18.2, 30.4], [4.0, 3.2, 3.0]];      // bird

function nose_r(p) =
    p == "dog" ? [3.4, 2.4, 2.5] : p == "cat" ? [2.3, 1.6, 1.7] : p == "rabbit" ? [2.4, 1.7, 1.8] :
    p == "hamster" ? [1.9, 1.4, 1.5] : p == "squirrel" ? [2.4, 1.8, 1.9] : [0, 0, 0];

module tail_of(p, g = 0) {
    if (p == "dog")
        chain([[0, 12, 3, 3.5], [0, 17, 5.5, 3.3], [0, 19.5, 10.5, 3.0], [0, 18.5, 15.5, 2.5]], g);
    else if (p == "cat")
        chain([[5, 14, 3.2, 3.2], [12, 16.5, 3.0, 3.1], [18.5, 12, 3.0, 3.0],
               [21.5, 3, 3.0, 2.9], [21, -6, 3.4, 2.8], [19, -12.5, 6.5, 2.6]], g);
    else if (p == "rabbit")
        ell([5.6, 5.6, 5.6], [0, 14, 5.4], g);
    else if (p == "bird")
        hull() { ell([5.2, 3, 4.2], [0, 13.5, 7], g); ell([4.2, 2.2, 2.7], [0, 20.5, 3.5], g); }
    else if (p == "squirrel")
        chain([[0, 14, 5.5, 5.6], [0, 20.5, 12, 8.0], [0, 23.0, 23, 9.6], [0, 20.5, 32, 8.8], [0, 16.0, 38, 6.4]], g);
}

module extras_of(p, g = 0) {
    m = muzzle(p);
    if (p == "dog") {
        ell(m[1], m[0], g);
    } else if (p == "cat") {
        for (s = [-1, 1]) ell([4.9, 4.4, 4.0], [s * 3.4, m[0][1] + 0.4, m[0][2]], g);
    } else if (p == "rabbit") {
        ell(m[1], m[0], g);
        for (s = [-1, 1]) ell([5.2, 9, 3.2], [s * 7, -14, 2.4], g);        // big hind feet
    } else if (p == "hamster") {
        for (s = [-1, 1]) ell([8.4, 7, 7.2], [s * 12, -11.5, 28], g);      // puffy cheeks
        ell(m[1], m[0], g);
        ell([10, 7.5, 10], [0, -6.5, 12.5], g);                              // round belly
    } else if (p == "squirrel") {
        for (s = [-1, 1]) ell([4.7, 4.5, 4.1], [s * 3.9, m[0][1] + 0.3, m[0][2]], g);
    } else if (p == "bird") {
        for (s = [-1, 1]) ell([3.2, 9, 11], [s * 13.3, 2, 16.5], g);       // wings
        hull() {                                                            // beak
            ell([3.9, 2.8, 2.3], [0, -19.0, 31.2], g);
            translate([0, -24.2, 29.0]) sphere(0.8 + g);
        }
    }
}

// ---------------------------------------------------------------------
//  FACE: eyes (+ shine), nose, mouth
// ---------------------------------------------------------------------
function eye_spec(p) =       // [azimuth, elevation, rx, ry, rz]
    p == "dog"      ? [30, -8, 3.0, 1.9, 3.8] :
    p == "cat"      ? [32, -8, 3.2, 1.9, 4.0] :
    p == "rabbit"   ? [32, -9, 2.9, 1.8, 3.6] :
    p == "hamster"  ? [36, -9, 2.7, 1.8, 3.3] :
    p == "squirrel" ? [30, -8, 3.0, 1.9, 3.7] :
                      [38, -2, 2.5, 1.8, 2.9];

module eye_at(s, g = 0) {
    e = eye_spec(pet);
    translate(hs(s * e[0], e[1])) rotate([0, 0, s * e[0]]) children();
}

module face_dark(g = 0) {
    e = eye_spec(pet);
    for (s = [-1, 1]) eye_at(s) ell([e[2], e[3], e[4]], [0, 0, 0], g);
    m = muzzle(pet);
    nr = nose_r(pet);
    if (pet != "bird") {
        zn = m[0][2] + 0.45 * m[1][2];
        np = efront(m[0], m[1], 0, zn);
        translate([np[0], np[1] + 0.5, np[2]]) scale([nr[0] + g, nr[1] + g, nr[2] + g]) sphere(1);
        // little "omega" smile
        for (s = [-1, 1])
            chain([for (a = [200 : 20 : 340]) let(
                    x = s * 2.3 + 2.3 * cos(a), z = zn - nr[2] - 0.6 + 1.9 * sin(a) + 1.5,
                    q = efront(m[0], m[1], x, z)) [q[0], q[1] + 0.15, q[2], 0.5]], g);
    }
}

module face_white(g = 0) {                    // eye shine
    e = eye_spec(pet);
    for (s = [-1, 1]) eye_at(s) translate([0.9, -1.45, 1.55]) scale([1 + g, 0.8 + g, 1.15 + g]) sphere(1.0);
}

// everything a pet is made of (g > 0 gives a grown copy for clearance cuts)
module pet_solid(g = 0) {
    union() {
        torso(g);
        legs(g);
        bib(g);
        head(g);
        ears_of(pet, g);
        tail_of(pet, g);
        extras_of(pet, g);
    }
}

// ---------------------------------------------------------------------
//  GENERIC SHAPES
// ---------------------------------------------------------------------
module star2d(R = 5, r = 2.2, n = 5)
    polygon([for (i = [0 : 2 * n - 1]) let(a = 90 + i * 180 / n, rr = (i % 2 == 0) ? R : r)
        [rr * cos(a), rr * sin(a)]]);

module rrect2d(w, h, r)
    hull() for (sx = [-1, 1], sy = [-1, 1]) translate([sx * (w / 2 - r), sy * (h / 2 - r)]) circle(r);

module paw2d(s = 1) scale(s) {
    translate([0, -1.3]) scale([1.15, 0.95]) circle(2.5);
    for (p = [[-3.5, 2.2], [-1.25, 4.0], [1.25, 4.0], [3.5, 2.2]]) translate(p) scale([0.82, 1.05]) circle(1.3);
}

module leaf2d(s = 1) scale(s)
    polygon([[0, -5], [0.7, -2.6], [2.8, -3.3], [2.2, -0.8], [5.2, 0.3], [3.4, 1.7], [4.0, 4.0], [1.7, 3.3],
             [0, 6.2], [-1.7, 3.3], [-4.0, 4.0], [-3.4, 1.7], [-5.2, 0.3], [-2.2, -0.8], [-2.8, -3.3], [-0.7, -2.6]]);

// round arch whose top is a 45 deg point, so it never needs a bridge
module teardrop2d(r) hull() { circle(r); translate([0, r * 1.414]) circle(0.01); }

module arch2d(w, h) { translate([-w / 2, 0]) square([w, h - w / 2]); translate([0, h - w / 2]) circle(d = w); }

// closed, ribbed, squashed ellipsoid (pumpkin). R = half width, H = half height
module rib_lathe(R, H, ribs = 8, depth = 0.08, nu = 64, nv = 14, p = 0.85, zc = 0) {
    N = 1 + (nv - 1) * nu;
    function idx(i, j) = 1 + (i - 1) * nu + (j % nu);
    pts = concat(
        [[0, 0, zc - H]],
        [for (i = [1 : nv - 1]) let(t = -90 + 180 * i / nv, rr = R * pow(cos(t), p), zz = zc + H * sin(t))
            for (j = [0 : nu - 1]) let(a = 360 * j / nu, k = 1 + depth * cos(t) * cos(ribs * a))
                [rr * k * cos(a), rr * k * sin(a), zz]],
        [[0, 0, zc + H]]);
    faces = concat(
        [for (j = [0 : nu - 1]) [0, idx(1, j), idx(1, j + 1)]],
        [for (i = [1 : nv - 2]) for (j = [0 : nu - 1])
            [idx(i, j), idx(i + 1, j), idx(i + 1, j + 1), idx(i, j + 1)]],
        [for (j = [0 : nu - 1]) [N, idx(nv - 1, j + 1), idx(nv - 1, j)]]);
    polyhedron(pts, faces, convexity = 6);
}

// pumpkin sitting on z = 0 (lower part is cut flat). r = radius, h = height
module pumpkin(r = 5, h = 8, ribs = 8, stem = true, depth = 0.085) {
    hh = h / 1.5;  zc = hh * 0.5;                       // flat cut sits 30 deg below the equator: flank <= 45 deg
    intersection() {
        rib_lathe(r, hh, ribs = ribs, depth = depth, nu = 48, nv = 10, zc = zc);
        translate([-r * 2, -r * 2, 0]) cube([r * 4, r * 4, h * 3]);
    }
    if (stem) chain([[0, 0, h - 0.5, r * 0.2], [r * 0.05, 0, h + r * 0.3, r * 0.17], [r * 0.2, 0, h + r * 0.53, r * 0.14]]);
}

// ---------------------------------------------------------------------
//  NAME TEXT
// ---------------------------------------------------------------------
function ucase_c(c) = (ord(c) >= 97 && ord(c) <= 122) ? chr(ord(c) - 32) : c;
function join(v, i = 0) = i >= len(v) ? "" : str(v[i], join(v, i + 1));
NAME = join([for (c = name) ucase_c(c)]);
// conservative glyph width table (in units of the cap height)
function cw(c) = (c == "I" || c == "J" || c == "1" || c == " " || c == "." || c == "!") ? 0.5 :
                 (c == "W") ? 1.5 : (c == "M") ? 1.2 :
                 (c == "L" || c == "E" || c == "F" || c == "T" || c == "Z") ? 0.9 : 1.0;
function est_w(s, i = 0) = i >= len(s) ? 0 : cw(s[i]) + est_w(s, i + 1);

TXT_H   = 1.0;      // relief height of the name
TXT_CAP = 7.0;      // nominal cap height of the name
TXT_MAXW = 30;      // widest the name may become
PLQ_W = 52;  PLQ_L = 15;   // plaque border size (across x / along the slope)
PLQ_V = -10.5;      // plaque centre along the slope (from the top edge of the ramp)

module name2d() {
    ew = est_w(NAME) * TXT_CAP;
    k = min(1, TXT_MAXW / max(ew, 1));
    tw = ew * k;
    // frame
    difference() { rrect2d(PLQ_W, PLQ_L, 3.2); rrect2d(PLQ_W - 2.6, PLQ_L - 2.6, 2.2); }
    // name
    scale(k) text(NAME, size = TXT_CAP, font = font, halign = "center", valign = "center");
    // paws
    for (s = [-1, 1]) translate([s * (tw / 2 + 5.4), 0]) rotate(s * 12) paw2d(0.68);
}

// ---------------------------------------------------------------------
//  BASE  (deck at z = 0, body below it, name ramp in front)
// ---------------------------------------------------------------------
RAMP_A = 30;                 // slope of the name ramp (deg)
RAMP_Y = -19;                // where the ramp meets the deck (y)
BASE_R = 37;
TAB_W  = 56;                 // width of the name board that sticks out of the front
TAB_Y  = -37.5;              // front edge of the name board

module ramp_frame() { translate([0, RAMP_Y, 0]) rotate([RAMP_A, 0, 0]) children(); }

// removes everything above the ramp plane, but only across the width of the name board
module ramp_wedge() {
    translate([0, RAMP_Y, 0]) rotate([RAMP_A, 0, 0]) translate([-TAB_W / 2, -90, 0]) cube([TAB_W, 90, 80]);
}

// wedge-shaped name board that grows out of the front of every base
module sign_tab() {
    translate([0, 0, -BH]) linear_extrude(height = BH) hull() {
        for (sx = [-1, 1]) translate([sx * (TAB_W / 2 - 7), TAB_Y + 7]) circle(7);
        translate([-TAB_W / 2, -16]) square([TAB_W, 2]);
    }
}

module name_relief(h = TXT_H, sink = 0.3) {          // raised name sitting on the ramp
    ramp_frame() translate([0, PLQ_V, -sink]) linear_extrude(h + sink) name2d();
}
module name_cut(depth = 1.0) {                        // engraved name
    ramp_frame() translate([0, PLQ_V, -depth]) linear_extrude(depth + 1) name2d();
}

// peg that holds the pet (stadium shaped so it can only go on two ways)
module pet_boss(ex = 0, hh = BOSS_H) {
    translate([TAX[0], TAX[1], -eps])
        hull() for (sx = [-5, 5]) translate([sx, 0, 0]) cylinder(r1 = BOSS_R1 + ex, r2 = BOSS_R2 + ex, h = hh + eps);
}

// ---- stump -----------------------------------------------------------
function bark_r(a) = BASE_R - 1.0 + 1.2 * sin(5 * a + 40) + 0.8 * sin(11 * a + 110)
                     + 0.55 * sin(27 * a) + 0.35 * sin(41 * a + 5);

module stump_body() {
    translate([0, 0, -BH]) linear_extrude(height = BH, scale = 0.93)
        polygon([for (a = [0 : 2 : 358]) let(r = bark_r(a)) [r * cos(a), r * sin(a)]]);
    // flat top edge ring a bit lighter than bark: growth rings are engraved later
}

module stump_rings() {                       // shallow growth rings on the deck
    for (rr = [21.5, 26.0, 30.0]) difference() {
        translate([0, 0, -0.45]) linear_extrude(height = 0.6) polygon(
            [for (a = [0 : 4 : 356]) let(r = rr + 0.5 * sin(3 * a + rr * 9)) [r * cos(a), r * sin(a)]]);
        translate([0, 0, -1]) linear_extrude(height = 2) polygon(
            [for (a = [0 : 4 : 356]) let(r = rr - 0.85 + 0.5 * sin(3 * a + rr * 9)) [r * cos(a), r * sin(a)]]);
    }
}

module stump_deco() {
    translate([-26, -12.5, -1.0]) pumpkin(5.3, 7.9, ribs = 7);
    translate([28.5, -9.5, -1.0]) rotate(-30) pumpkin(4.5, 6.7, ribs = 7);
    for (l = [[-18, -16, 30], [21, -16.5, -50], [-25.5, -3.5, 200]])
        translate([l[0], l[1], -0.2]) rotate(l[2]) linear_extrude(height = 1.0) leaf2d(0.8);
}

// ---- crypt (octagonal stone plinth) --------------------------------------
CR_R = BASE_R + 2.5;                         // circumradius of the octagon
function crypt_rw(z) = CR_R * (1 - 0.06 * (z + BH) / BH);

module crypt_body() {
    difference() {
        translate([0, 0, -BH]) rotate(22.5) linear_extrude(height = BH, scale = 0.94) circle(r = CR_R, $fn = 8);
        crypt_courses();
    }
}
module crypt_courses() {                      // V-shaped stone courses cut into the wall (45 deg flanks)
    for (z = [-4.6, -9.4]) difference() {
        translate([0, 0, z - 0.9]) rotate(22.5) cylinder(r = CR_R + 3, h = 1.8, $fn = 8);
        rotate(22.5) hull() {
            translate([0, 0, z]) cylinder(r = crypt_rw(z) - 0.9, h = 0.01, $fn = 8);
            translate([0, 0, z - 0.9]) cylinder(r = crypt_rw(z) + 0.1, h = 0.01, $fn = 8);
            translate([0, 0, z + 0.9]) cylinder(r = crypt_rw(z) + 0.1, h = 0.01, $fn = 8);
        }
    }
}
module tombstone(w = 11.5, h = 20, t = 4.2) {   // stands on its own origin, engraved face towards +z
    difference() {
        linear_extrude(height = t, center = true) arch2d(w, h);
        translate([0, 0, t / 2 - 0.8]) linear_extrude(height = 1.0) {
            translate([-0.8, h * 0.40]) square([1.6, h * 0.34]);
            translate([-h * 0.12, h * 0.62]) square([h * 0.24, 1.6]);
        }
    }
}
module crypt_deco() {
    // grave stones stand on the front corners (the rear half belongs to the cape), lean a little, face the front
    translate([-27.5, -9.5, -3.2]) rotate([0, -8, 0]) rotate([90, 0, 0]) tombstone(9.5, 17, 3.8);
    translate([26.5, -14.5, -3.4]) rotate([0, 10, 0]) rotate([90, 0, 0]) tombstone(8, 13, 3.4);
    // crossed bones next to the name board
    for (a = [35, -35]) translate([-21.5, -15.5, 0.2]) rotate(a) hull() {
        translate([-4.2, 0, 0]) sphere(0.95);  translate([4.2, 0, 0]) sphere(0.95);
    }
}

// ---- cauldron ------------------------------------------------------------
function pot_r(z) = z > -2.2 ? 35.2 + (z + 2.2) * (1.6 / 2.4) :
                    z > -5.4 ? 35.4 + (z + 5.4) * (-0.2 / 3.2) :
                               30.4 + (z + 11.5) * (5.0 / 6.1);
module cauldron_body() {
    rotate_extrude() polygon([[0, -BH], [28, -BH], [30.4, -11.5], [35.4, -5.4], [35.2, -2.2], [36.8, 0.2],
                              [36.8, 2.0], [32.6, 2.0], [32.6, 0], [0, 0]]);
}
module cauldron_deco() {                      // green slime: bubbles on the deck + drips over the rim
    for (b = [[-27.5, -6, 3.4], [27.8, -5, 3.0], [-24.5, -13.5, 2.0], [24.5, -14.5, 1.8]])
        translate([b[0], b[1], 0.2]) scale([1, 1, 0.85]) sphere(b[2]);
}
module cauldron_drips() {
    for (d = [[-14, 8.5], [28, 6.5], [72, 9.5], [118, 7.0], [160, 9.0], [205, 7.5]])
        rotate(d[0]) hull() {
            translate([pot_r(-0.5) + 0.7, 0, -0.3]) sphere(2.3);
            translate([pot_r(-d[1]) + 0.5, 0, -d[1]]) sphere(1.8);
        }
}

// ---- assembly of the base ---------------------------------------------------
module base_body() {
    difference() {
        union() {
            if (base_style == "stump") stump_body();
            else if (base_style == "crypt") crypt_body();
            else cauldron_body();
            sign_tab();
        }
        ramp_wedge();
        if (base_style == "stump") stump_rings();
        if (!raise_text) name_cut(1.0);
    }
}
// decor that sits on the deck. Everything is sunk into the deck so it is one piece with the base
module base_deco() {
    if (base_decor) {
        intersection() {
            union() {
                if (base_style == "stump") stump_deco();
                else if (base_style == "crypt") crypt_deco();
                else cauldron_deco();
            }
            translate([-100, -100, -1.0]) cube([200, 200, 60]);
        }
        if (base_style == "cauldron") cauldron_drips();
    }
}
// name relief that is ADDED to the base (raised name); nothing when the name is engraved
module text_part() { if (raise_text) name_relief(); }

// ---------------------------------------------------------------------
//  PET PART  (flat bottom, socket for the base boss, nubs for the cape)
// ---------------------------------------------------------------------
module ground() { translate([-80, -80, 0]) cube([160, 160, 160]); }

module shoulder_nubs(g = 0) {                 // two small snap bumps on the back of the shoulders
    for (s = [-1, 1]) translate([s * 8.9, 7.4, 19.0]) sphere(1.35 + g);
}

module pet_pocket() { if (magnets) foot_magnets(); else pet_boss(ex = tol, hh = BOSS_H + 0.5); }

module pet_core() {
    difference() {
        union() {
            intersection() { pet_solid(); ground(); }
            head_peg();
            shoulder_nubs();
            if (fuse_face) { face_dark(); face_white(); }
        }
        pet_pocket();
        if (magnets) head_magnet();
        if (!fuse_face) { face_dark(0.1); face_white(0.1); }
    }
}

// light fur patches: muzzle, chest, socks and a few species extras (own colour part when multicolor)
module marks_volume() {
    m = muzzle(pet);
    if (pet != "bird") ell(m[1], m[0], 0.9);
    ell([9.8, 7.8, 8.8], [0, -6.5, 15.5], 0.9);                                   // chest
    for (s = [-1, 1]) ell([5.8, 7.4, 4.6], [s * 6.9, -11.8, 2.2]);                // socks
    if (pet == "dog") ell([2.6, 3.0, 9.5], [0, -19.0, 37.0]);                     // blaze
    if (pet == "cat") ell([3.4, 3.4, 3.4], [19, -12.5, 6.5]);                     // tail tip
    if (pet == "hamster") ell([11, 8, 10.5], [0, -6.5, 12.5]);                    // belly
    if (pet == "squirrel") ell([8, 8, 8], [0, 16, 39]);                           // tail tip
    if (pet == "rabbit") ell([6.4, 6.4, 6.4], [0, 14, 5.4]);                      // cotton tail
    if (pet == "bird") {                                                          // orange robin belly + beak
        ell([9.5, 6.5, 9], [0, -8, 14]);
        hull() { ell([3.9, 2.8, 2.3], [0, -19.0, 31.2], 0.5); translate([0, -24.2, 29.0]) sphere(1.3); }
    }
}
module pet_part()   { if (multicolor) difference() { pet_core(); marks_volume(); } else pet_core(); }
module marks_part() { if (multicolor) intersection() { pet_core(); marks_volume(); } }

// ---------------------------------------------------------------------
//  COSTUMES
//  hat   : flat-bottomed solid that plugs onto the peg on the crown
//  trim  : band / buckle / star / stem (own colour when multicolour)
//  cape  : shell that wraps the back and clicks onto the shoulder nubs
//  charm : flat-backed ornament that is glued on the chest
// ---------------------------------------------------------------------
function hat_col(c) = c == "vampire" ? "#2B2230" : c == "witch" ? "#35283F" : c == "ghost" ? "#F4F1EA" :
                      c == "pumpkin" ? "#E8731A" : c == "devil" ? "#B3202A" : "#2E2A7A";
function trim_col(c) = c == "vampire" ? "#B3202A" : c == "witch" ? "#7B3FA0" : c == "ghost" ? "#2B1B17" :
                       c == "pumpkin" ? "#4F8A3B" : c == "devil" ? "#B3202A" : "#F2C230";
function cape_col(c) = c == "vampire" ? "#7E1022" : c == "witch" ? "#7B3FA0" : c == "ghost" ? "#F4F1EA" :
                       c == "pumpkin" ? "#E8731A" : c == "devil" ? "#8C1B24" : "#3B3AA0";
function charm_col(c) = c == "vampire" ? "#D4263B" : c == "witch" ? "#F08A2C" : c == "ghost" ? "#E8731A" :
                        c == "pumpkin" ? "#4F8A3B" : c == "devil" ? "#1E1A22" : "#F2C230";
function charm_kind(c) = c == "ghost" ? "pump" : c == "pumpkin" ? "leaf" : c == "wizard" ? "star" : "bow";

module hat_place() { translate([HC[0], HC[1], HAT_Z]) children(); }
module hat_socket() {
    if (magnets) translate([0, 0, -eps]) cylinder(d = MG_D, h = MG_H + eps);
    else translate([0, 0, -eps]) cylinder(r1 = HPEG_R1 + tol, r2 = HPEG_R2 + tol, h = HPEG_H + 0.6 + eps);
}
module hat_carve() { translate([-HC[0], -HC[1], -HAT_Z]) ears_of(pet, 0.55); }

// cone whose axis bends (parabola) towards (bx, by)
module bend_cone(h, r0, r1, bx, by, z0 = 0, n = 10, pw = 1.3) {
    function cen(t) = [bx * t * t, by * t * t, z0 + h * t];
    function rad(t) = r1 + (r0 - r1) * pow(1 - t, pw);
    for (i = [0 : n - 1]) hull() {
        translate(cen(i / n)) cylinder(r = rad(i / n), h = 0.02);
        translate(cen((i + 1) / n)) cylinder(r = rad((i + 1) / n), h = 0.02);
    }
}
// carve a face (2D polygon list) into the front of an upright body: prism minus the shrunk body
module face_carve(zc, k = 0.9) {
    difference() {
        rotate([90, 0, 0]) linear_extrude(height = 40) children(0);
        translate([0, 0, zc]) scale(k) translate([0, 0, -zc]) children(1);
    }
}

// ---- witch -------------------------------------------------------------
WH_H = 25; WH_R0 = 9.6; WH_Z0 = 2.0;
function wh_rad(z) = 0.9 + (WH_R0 - 0.9) * pow(max(0, 1 - (z - WH_Z0) / WH_H), 1.3);
module witch_body() {
    rotate_extrude() polygon([[0, 0], [16.6, 0], [17.0, 0.7], [16.4, 1.6], [11, 2.5], [0, 2.5]]);
    bend_cone(WH_H, WH_R0, 0.9, -6, 5.5, z0 = WH_Z0, n = 12);
}
module witch_trim() {
    rotate_extrude() polygon([[wh_rad(2.4) - 0.8, 2.4], [wh_rad(2.4) + 0.1, 2.4], [wh_rad(3.2) + 0.9, 3.2],
                              [wh_rad(5.6) + 0.9, 5.6], [wh_rad(6.4) + 0.1, 6.4], [wh_rad(6.4) - 0.8, 6.4]]);
    // buckle on the front
    translate([0, -(wh_rad(4.2) + 0.55), 4.2]) rotate([90 - 9, 0, 0]) difference() {
        linear_extrude(height = 1.6, center = true) rrect2d(6.6, 5.6, 1.2);
        linear_extrude(height = 4, center = true) rrect2d(3.4, 2.4, 0.6);
    }
}

// ---- wizard ------------------------------------------------------------
WZ_H = 36; WZ_R0 = 8.8; WZ_Z0 = 2.0;
function wz_rad(z) = 1.0 + (WZ_R0 - 1.0) * pow(max(0, 1 - (z - WZ_Z0) / WZ_H), 1.15);
module wizard_body() {
    rotate_extrude() polygon([[0, 0], [16.2, 0], [16.6, 0.7], [16.0, 1.6], [10, 2.4], [0, 2.4]]);
    bend_cone(WZ_H, WZ_R0, 1.0, -9, 6, z0 = WZ_Z0, n = 14, pw = 1.15);
}
module wizard_trim() {
    rotate_extrude() polygon([[wz_rad(2.4) - 0.8, 2.4], [wz_rad(2.4) + 0.1, 2.4], [wz_rad(3.2) + 0.9, 3.2],
                              [wz_rad(5.6) + 0.9, 5.6], [wz_rad(6.4) + 0.1, 6.4], [wz_rad(6.4) - 0.8, 6.4]]);
    translate([-0.45, 0.4 - (wz_rad(13) + 0.1), 13]) rotate([72, 0, 0]) translate([0, 0, -0.9])
        linear_extrude(height = 2.4) star2d(4.2, 1.8);
}

// ---- pumpkin -----------------------------------------------------------
PK_R = 14.4; PK_H = 14.2;
module pumpkin_shape() { pumpkin(PK_R, PK_H, ribs = 8, stem = false, depth = 0.07); }
module pumpkin_body() {
    difference() {
        pumpkin_shape();
        face_carve(0, 0.9) {
            union() {                                           // jack-o'-lantern face
                for (s = [-1, 1]) translate([s * 5.2, 7.6]) polygon([[-2.5, 0], [2.5, 0], [0, 4.4]]);
                translate([0, 5.4]) polygon([[-1.2, 0], [1.2, 0], [0, 2.0]]);
                translate([0, 3.2]) scale(0.78) polygon([[-7.0, 1.6], [-4.6, -0.6], [-2.4, 1.0], [0, -0.8], [2.4, 1.0],
                                             [4.6, -0.6], [7.0, 1.6], [5.6, -2.6], [2.8, -3.8], [0, -4.2],
                                             [-2.8, -3.8], [-5.6, -2.6]]);
            }
            pumpkin_shape();
        }
    }
}
module pumpkin_trim() {                                       // stem
    chain([[0, 0, PK_H - 1.5, 2.3], [0.3, 0, PK_H + 1.4, 2.0], [1.6, 0, PK_H + 3.6, 1.7],
           [3.4, 0, PK_H + 4.8, 1.4]]);
}

// ---- devil -------------------------------------------------------------
module devil_body() {
    rotate_extrude() polygon([[0, 0], [11.0, 0], [11.4, 1.0], [10.8, 2.2], [0, 2.6]]);
    cylinder(r1 = 6.8, r2 = 5.4, h = 6);
    for (s = [-1, 1]) chain([[s * 6.2, -1.0, 1.8, 3.6], [s * 7.2, -1.3, 6.6, 3.0], [s * 8.8, -1.8, 11.4, 2.3],
                             [s * 10.8, -2.6, 16.0, 1.5], [s * 13.0, -3.4, 19.6, 0.55]]);
}
module devil_trim() {}

// ---- vampire: a little bat that perches on the head ------------------------
BAT_WING = [[2.0, 0], [2.0, 9.0], [7.0, 13.6], [12.0, 17.8], [16.2, 20.2], [15.8, 0], [13.9, 0], [12.9, 5.4],
            [11.0, 0], [9.2, 0], [8.0, 5.0], [6.3, 0], [4.5, 0]];       // fingers touch the bed, V notches between
module bat_body() {
    scale(0.86) intersection() {
        union() {
            ell([4.0, 3.4, 4.6], [0, 0, 5.2]);                                          // body
            for (s = [-1, 1]) hull() { translate([s * 2.2, 0, 9.0]) sphere(1.0); translate([s * 3.0, 0, 11.8]) sphere(0.5); }
            for (s = [-1, 1]) scale([s, 1, 1]) rotate([90, 0, 0]) linear_extrude(height = 2.2, center = true)
                polygon(BAT_WING);
            ell([2.6, 2.0, 2.0], [0, -1.4, 2.4]);                                        // feet / belly
        }
        translate([-40, -40, 0]) cube([80, 80, 60]);
    }
}
module bat_trim() {}

// ---- ghost: a mini ghost that sits on the head ------------------------------
function gh_r(z) = z <= 4 ? 8.0 : 8.0 * sqrt(max(0, 1 - pow((z - 4) / 12.4, 2)));
module ghost_shape() {
    rotate_extrude() polygon(concat([[0, 0]], [for (z = [0 : 0.8 : 16.0]) [max(gh_r(z), 0.01), z]], [[0, 16.4]]));
}
module ghost_body() {
    difference() {
        ghost_shape();
        // scalloped hem (round arches)
        for (a = [0 : 45 : 315]) rotate(a + 22.5) translate([7.6, 0, 0]) rotate([90, 0, 0]) linear_extrude(height = 6, center = true) teardrop2d(2.2);
        // engraved face
        face_carve(0, 0.9) {
            union() {
                for (s = [-1, 1]) translate([s * 2.9, 10.2]) scale([1.4, 2.0]) circle(1);
                translate([0, 5.8]) scale([1.7, 2.3]) circle(1);
            }
            ghost_shape();
        }
    }
}
module ghost_trim() {}

module hat_body() {
    if (costume == "witch") witch_body();
    else if (costume == "wizard") wizard_body();
    else if (costume == "pumpkin") pumpkin_body();
    else if (costume == "devil") devil_body();
    else if (costume == "vampire") bat_body();
    else if (costume == "ghost") ghost_body();
}
module hat_trim() {
    if (costume == "witch") witch_trim();
    else if (costume == "wizard") wizard_trim();
    else if (costume == "pumpkin") pumpkin_trim();
}
module hat_part() {                           // hat, local frame (bottom at z = 0), fused trim unless multicolour
    intersection() {
        difference() {
            union() { hat_body(); if (!multicolor) hat_trim(); }
            hat_socket();
            hat_carve();
            if (multicolor) hat_trim();
        }
        translate([-60, -60, 0]) cube([120, 120, 90]);
    }
}
module hat_trim_part() {
    if (multicolor) intersection() {
        difference() { hat_trim(); hat_carve(); }
        translate([-60, -60, 0]) cube([120, 120, 90]);
    }
}

// ---- capes -------------------------------------------------------------------
CAPE_T = 2.2;
TIN = [[-3, 10.5], [0, 12.2], [3, 13.8], [17.5, 10.8], [22, 9.2], [26, 6.4], [32, 3]];   // torso silhouette + clearance
function interp(tab, z, i = 0) = z <= tab[0][0] ? tab[0][1] :
    i >= len(tab) - 1 ? tab[len(tab) - 1][1] :
    z <= tab[i + 1][0] ? tab[i][1] + (tab[i + 1][1] - tab[i][1]) * (z - tab[i][0]) / (tab[i + 1][0] - tab[i][0]) :
    interp(tab, z, i + 1);

module cape_axis() { translate([TAX[0], TAX[1], 0]) children(); }

module cape_shell(tab, ang = 200, t = CAPE_T) {
    outer = [for (p = tab) [p[1], p[0]]];
    inner = [for (i = [len(tab) - 1 : -1 : 0]) let(p = tab[i]) [max(interp(TIN, p[0]), p[1] - t), p[0]]];
    cape_axis() rotate([0, 0, 90 - ang / 2]) rotate_extrude(angle = ang) polygon(concat(outer, inner));
}

CAPE_SHORT = [[0, 24.5], [4, 23.0], [10, 20.0], [16, 17.2], [22, 15.4], [28, 15.2], [33, 18.2]];
CAPE_LONG  = [[0, 26.0], [5, 25.2], [11, 21.8], [17, 18.2], [23, 16.0], [28, 15.6], [33, 18.4]];
CAPE_SHEET = [[0, 26.5], [4, 25.8], [10, 22.4], [16, 18.8], [22, 16.2], [28, 15.6], [33, 18.4]];
CAPE_COLLAR = [[0, 25.0], [4, 23.4], [10, 20.0], [16, 17.4], [22, 16.2], [28, 17.4], [34, 20.8], [40, 24.2], [45, 26.8]];

// notches along the hem: n notches over the arc, each "kind" = tri | round
module hem_notches(kind, n, w, hh, ang = 200, rmin = 8, rmax = 40) {
    cape_axis() for (i = [0 : n - 1]) {
        a = 90 - ang / 2 + (i + 0.5) * ang / n;
        rotate([0, 0, a]) rotate([90, 0, 90]) translate([0, 0, rmin]) linear_extrude(height = rmax - rmin) {
            if (kind == "tri") polygon([[-w / 2, -1], [w / 2, -1], [0, hh]]);
            else teardrop2d(w / 2);
        }
    }
}

// V-notches cut down from the top edge leave a crown of collar spikes (open at the top: nothing to bridge)
module top_spikes(n, w, hh, z_top, ang = 200) {
    cape_axis() for (i = [0 : n - 2]) {
        rotate([0, 0, 90 - ang / 2 + (i + 1) * ang / n]) rotate([90, 0, 90]) translate([0, 0, 10]) linear_extrude(height = 40)
            translate([0, z_top - hh]) polygon([[0, 0], [w / 2, hh + 1], [-w / 2, hh + 1]]);
    }
}

module cape_ribs(n = 9, ang = 200) {          // pumpkin-style vertical grooves
    cape_axis() for (i = [0 : n - 1]) {
        a = 90 - ang / 2 + (i + 0.5) * ang / n;
        rotate([0, 0, a]) hull() {
            translate([interp([for (p = CAPE_SHORT) [p[0], p[1]]], 3) + 0.2, 0, 3]) sphere(1.1);
            translate([interp(CAPE_SHORT, 26) + 0.2, 0, 26]) sphere(1.1);
        }
    }
}

// devil yoke + wings
DV_YOKE = [[9, 15.8], [14, 13.8], [20, 12.8], [26, 13.4], [31, 16.0]];
DV_WING = [[0, 9], [21, 31], [18.8, 27.2], [19.6, 36.5], [14.6, 29.5], [13.6, 39.5], [9.2, 30.2], [6.6, 40],
           [3.4, 31.5], [0, 34]];
module devil_cape_raw() {
    cape_shell(DV_YOKE, ang = 150);
    cape_axis() for (s = [-1, 1]) rotate([0, 0, 90 + s * 32]) translate([13.2, 0, 0])
        rotate([90, 0, 0]) linear_extrude(height = 2.6, center = true) polygon(DV_WING);
}

module cape_raw() {
    if (costume == "vampire") {
        difference() {
            cape_shell(CAPE_COLLAR);
            hem_notches("tri", 9, 8.4, 6.2, rmin = 10);
            top_spikes(8, 9.0, 11.0, z_top = 45.5);
        }
    }
    else if (costume == "witch") difference() { cape_shell(CAPE_SHORT); hem_notches("tri", 7, 9.0, 5.4, rmin = 12); }
    else if (costume == "ghost") difference() { cape_shell(CAPE_SHEET); hem_notches("round", 13, 6.4, 0, rmin = 12); }
    else if (costume == "pumpkin") difference() { cape_shell(CAPE_SHORT); cape_ribs(); }
    else if (costume == "wizard") cape_shell(CAPE_LONG);
    else if (costume == "devil") devil_cape_raw();
}

module cape_part() {
    difference() {
        cape_raw();
        pet_solid(0.35);
        shoulder_nubs(0.2);
    }
}

// ---- charm -------------------------------------------------------------------
CH_POS = [0, -14.0, 14.6];                    // where the charm touches the bib
module bow_charm() {
    for (s = [-1, 1]) scale([s, 1, 1]) rotate([90, 0, 0]) linear_extrude(height = 2.6)
        hull() { translate([1.6, 0]) circle(1.3); translate([7.6, 3.4]) circle(1.7); translate([7.6, -3.4]) circle(1.7); }
    ell([2.4, 1.9, 2.5], [0, -1.9, 0]);
}
module star_charm() { rotate([90, 0, 0]) linear_extrude(height = 2.4) star2d(5.4, 2.4); ell([1.6, 1.2, 1.6], [0, -2.5, 0]); }
module leaf_charm() { rotate([90, 0, 0]) linear_extrude(height = 2.0) rotate(90) leaf2d(1.1); }
module pump_charm() { rotate([90, 0, 0]) translate([0, 0, 0]) pumpkin(4.6, 6.2, ribs = 6, stem = true); }
module charm_part() {
    k = charm_kind(costume);
    if (costume != "none") {
        if (k == "bow") bow_charm();
        else if (k == "star") star_charm();
        else if (k == "leaf") leaf_charm();
        else pump_charm();
    }
}

// ---------------------------------------------------------------------
//  BASE PART  (fused = body + decor + boss + raised name)
// ---------------------------------------------------------------------
module base_part() {
    difference() {
        union() {
            base_body();
            if (!magnets) pet_boss();
            if (!multicolor) { base_deco(); text_part(); }
        }
        if (magnets) foot_magnets(up = MG_H);                 // open-top pockets in the deck
        if (multicolor && base_decor) base_deco();           // decor gets its own colour part
    }
}
module deco_part() { if (multicolor) base_deco(); }
module text_only_part() { if (multicolor) text_part(); }

// ---------------------------------------------------------------------
//  PRINT ORIENTATIONS  (all parts lie flat, no supports)
// ---------------------------------------------------------------------
module L_pet()   { pet_part(); }
module L_marks() { marks_part(); }
module L_face()  { if (!fuse_face) { face_dark(); } }
module L_shine() { if (!fuse_face) { face_white(); } }
module L_hat()   {
    if (costume != "none") {
        if (layout == "print") translate([-HC[0], -HC[1], 0]) hat_part();
        else hat_place() hat_part();
    }
}
module L_trim()  {
    if (costume != "none") {
        if (layout == "print") translate([-HC[0], -HC[1], 0]) hat_trim_part();
        else hat_place() hat_trim_part();
    }
}
function cape_z0() = costume == "devil" ? DV_YOKE[0][0] : 0;     // lowest point of the cape
module L_cape()  {
    if (costume != "none") {
        if (layout == "print") translate([-TAX[0], -TAX[1], -cape_z0()]) cape_part();
        else cape_part();
    }
}
module L_charm() {
    if (costume != "none") {
        if (layout == "print") rotate([-90, 0, 0]) charm_part();
        else translate(CH_POS) charm_part();
    }
}
module L_base()  { if (layout == "print") translate([0, 0, BH]) base_part(); else base_part(); }
module L_deco()  { if (layout == "print") translate([0, 0, BH]) deco_part(); else deco_part(); }
module L_text()  { if (layout == "print") translate([0, 0, BH]) text_only_part(); else text_only_part(); }

// ---------------------------------------------------------------------
//  ASSEMBLED PREVIEW (with colours)
// ---------------------------------------------------------------------
module col(c) { if (show_colors) color(c) children(); else children(); }

module assembled() {
    col(fur_color) pet_part();
    if (multicolor) col(marks_color) marks_part();
    if (!fuse_face) { col(face_color) face_dark(); col("#FFFFFF") face_white(); }
    col(base_color) base_part();
    if (multicolor) { col("#E8791C") deco_part(); col(text_color) text_only_part(); }
    if (costume != "none") {
        hat_place() { col(hat_col(costume)) hat_part(); col(trim_col(costume)) hat_trim_part(); }
        col(cape_col(costume)) cape_part();
        translate(CH_POS) col(charm_col(costume)) charm_part();
    }
}

// print plate: every part of the chosen set, flat, ready for a 220 x 220 bed
module plate() {
    translate([-52, 36, 0]) L_base();
    translate([20, 40, 0]) { L_pet(); L_marks(); }
    if (costume != "none") {
        translate([78, 38, 0]) { layout_cape(); }
        translate([-70, -48, 0]) { layout_hat(); }
        translate([-28, -48, 0]) layout_charm();
        if (multicolor) translate([-28, -70, 0]) { layout_trim(); }
    }
    if (!fuse_face) translate([20, -20, 0]) { L_face(); }
}
// plate helpers always use the print layout
module layout_cape()  { translate([-TAX[0], -TAX[1], -cape_z0()]) cape_part(); }
module layout_hat()   { translate([-HC[0], -HC[1], 0]) hat_part(); }
module layout_trim()  { translate([-HC[0], -HC[1], 0]) hat_trim_part(); }
module layout_charm() { rotate([-90, 0, 0]) charm_part(); }

// ---------------------------------------------------------------------
//  WHAT TO SHOW / EXPORT
// ---------------------------------------------------------------------
scale(model_scale) {
    if (part == "assembled") assembled();
    else if (part == "plate") plate();
    else if (part == "pet") L_pet();
    else if (part == "marks") L_marks();
    else if (part == "face") L_face();
    else if (part == "shine") L_shine();
    else if (part == "hat") L_hat();
    else if (part == "trim") L_trim();
    else if (part == "cape") L_cape();
    else if (part == "charm") L_charm();
    else if (part == "base") L_base();
    else if (part == "deco") L_deco();
    else if (part == "text") L_text();
}
