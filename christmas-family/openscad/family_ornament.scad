/*
 * CHRISTMAS FAMILY ORNAMENT   -   "a family of little figures in a snowy night scene"
 * ------------------------------------------------------------------------------------
 *  family name, year, up to 6 members and their names are parameters.
 *  The model is built as flat PIECES (sky panel, trees, house, base, snow, plaque, holly,
 *  lanterns, figures, name tags).  Every piece has ROLES (hat, face, scarf ...) and every role
 *  is exactly one colour.  All pieces lie flat on their back in their print orientation, so
 *  nothing needs supports.   tools/plan_beds.py gathers the roles into print beds with at most
 *  4 colours per bed (AMS / MMU) and starts a new bed when more colours are needed.
 *
 *  OpenSCAD 2021.01 or newer.  Units: mm.  Front of the finished ornament is -Y.
 *  Local (print) frame of a relief piece:  x = right, y = up, z = towards the viewer; z = 0 is the bed.
 */

/* [Family] */
family = "PIXELFORM";
year = 2026;
// boy, girl, snowman, dog, cat, reindeer (3 to 6 members)
members = ["boy", "snowman", "girl", "dog", "cat"];
names   = ["Ethan", "Lily", "Hazel", "Buddy", "Mochi"];

/* [What to build] */
// piece name (sky, tree0..3, house, base, snow, plaque, holly0/1, lantern0/1, fig0.., tag0..) or "assembled" / "manifest"
part = "assembled";
// role of the piece ("" = all roles of the piece)
role = "";
// assembled = position in the finished ornament, print = flat in its print orientation
layout = "assembled";

/* [Fit and quality] */
// clearance of slots (mm)
tol = 0.2;
// sphere resolution
fn = 36;
// serif font (Liberation fonts ship with OpenSCAD)
font_serif = "Liberation Serif:style=Bold";
font_text  = "Liberation Serif:style=Regular";

/* [Hidden] */
$fn = fn;
eps = 0.01;
N   = len(members);
W   = max(120, min(200, 40 * N));                  // length of the base / width of the arch
PITCH = N > 1 ? min(32, (W - 66) / (N - 1)) : 0;   // distance between figures
BASE_D = 46;  BASE_H = 22;  SNOW_H = 5;
TOP    = BASE_H + SNOW_H;                          // z of the top of the snow
AH     = 0.48 * W;                                 // height of the arch
TAB    = 7;                                        // depth of every tab in the base
PY     = 19;                                       // y of the back face of the arch panel
FY     = 3;                                        // y of the back face of the figures
TAGY   = -9;                                       // y of the back face of the name tags
function fx(i) = (i - (N - 1) / 2) * PITCH;        // x of figure i

// ----------------------------------------------------------------------------------------
//  helpers
// ----------------------------------------------------------------------------------------
module hemi() rotate_extrude() polygon(concat([[0, 0]], [for (i = [0 : 14]) [cos(90 * i / 14), sin(90 * i / 14)]]));
// flat-bottomed dome: centre c = [x, y], radii r = [rx, ry, height]
module D(c, r) translate([c[0], c[1], 0]) scale([r[0], r[1], r[2]]) hemi();
// full ellipsoid centred at c = [x, y, z]
module E(c, r) translate([c[0], c[1], c[2]]) scale([r[0], r[1], r[2]]) sphere(1);
module clipy(y0, y1 = 500) translate([-500, y0, -5]) cube([1000, y1 - y0, 100]);
module clipx(x0, x1) translate([x0, -500, -5]) cube([x1 - x0, 1000, 100]);
function dz(c, r, x, y) = r[2] * sqrt(max(0, 1 - pow((x - c[0]) / r[0], 2) - pow((y - c[1]) / r[1], 2)));
module chain(pts, r, z = 0) for (i = [0 : len(pts) - 2]) hull() {
    translate([pts[i][0], pts[i][1], z]) sphere(r);
    translate([pts[i + 1][0], pts[i + 1][1], z]) sphere(r);
}
module slab(h) linear_extrude(height = h) children();
module rrect2d(w, h, r) hull() for (sx = [-1, 1], sy = [-1, 1]) translate([sx * (w / 2 - r), sy * (h / 2 - r)]) circle(r);
module star2d(R = 5, r = 2.2, n = 5)
    polygon([for (i = [0 : 2 * n - 1]) let(a = 90 + i * 180 / n, rr = (i % 2 == 0) ? R : r) [rr * cos(a), rr * sin(a)]]);
module arch2d(w, h) { translate([-w / 2, 0]) square([w, h - w / 2]); translate([0, h - w / 2]) circle(d = w); }
module soften(r = 1) offset(r = r) offset(delta = -r) children();

// ----------------------------------------------------------------------------------------
//  text (advance widths of Liberation Serif Bold, per unit of font size, ASCII 32..126)
// ----------------------------------------------------------------------------------------
W_SERIF = [0.339, 0.452, 0.452, 0.678, 0.678, 1.356, 1.13, 0.377, 0.452, 0.452, 0.678, 0.773, 0.339, 0.452, 0.339, 0.377,
    0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.678, 0.452, 0.452, 0.773, 0.773, 0.773, 0.678, 1.262,
    0.979, 0.905, 0.979, 0.979, 0.905, 0.829, 1.055, 1.055, 0.528, 0.678, 1.055, 0.905, 1.28, 0.979, 1.055, 0.829, 1.055,
    0.979, 0.754, 0.905, 0.979, 0.979, 1.356, 0.979, 0.979, 0.905, 0.452, 0.377, 0.452, 0.788, 0.678, 0.452, 0.678, 0.754,
    0.602, 0.754, 0.602, 0.452, 0.678, 0.754, 0.377, 0.452, 0.754, 0.377, 1.13, 0.754, 0.678, 0.754, 0.754, 0.602, 0.528,
    0.452, 0.754, 0.678, 0.979, 0.678, 0.678, 0.602, 0.534, 0.299, 0.534, 0.705];
function chw(c) = let(o = ord(c)) (o >= 32 && o <= 126) ? W_SERIF[o - 32] : 0.8;
function strw(s, i = 0) = i >= len(s) ? 0 : chw(s[i]) + strw(s, i + 1);
function ucase(s) = let(v = [for (c = s) (ord(c) >= 97 && ord(c) <= 122) ? chr(ord(c) - 32) : c]) _join(v);
function _join(v, i = 0) = i >= len(v) ? "" : str(v[i], _join(v, i + 1));

// text of cap height `cap`, shrunk (never stretched) so that it is at most `maxw` wide
module fit_text(s, cap, maxw, fnt = font_serif) {
    k = min(1, maxw / max(strw(s) * cap * 1.0, 0.01));
    scale(k) translate([0, -cap / 2]) text(s, size = cap, font = fnt, halign = "center", valign = "baseline");
}

// ----------------------------------------------------------------------------------------
//  BASE (wood), SNOW cap, name PLAQUE            (assembled frame: z up, front = -Y)
// ----------------------------------------------------------------------------------------
module base_outline(r = 5, grow = 0) hull() for (sx = [-1, 1], sy = [-1, 1])
    translate([sx * (W / 2 - r), sy * (BASE_D / 2 - r)]) circle(r + grow);

module slots() {            // pockets for the tabs: figures, lanterns, arch panel
    for (i = [0 : N - 1]) translate([fx(i) - 8 - tol, FY - 6 - 0.2 - tol, TOP - TAB - 0.4]) cube([16 + 2 * tol, 6.4 + 2 * tol, TAB + 1]);
    for (s = [-1, 1]) translate([s * (W / 2 - 9) - 4 - tol, 6 - 6 - tol, TOP - TAB - 0.4]) cube([8 + 2 * tol, 6 + 2 * tol, TAB + 1]);
    translate([-(W - 8) / 2 - tol, PY - 3 - tol, TOP - TAB - 0.4]) cube([W - 8 + 2 * tol, 3 + 2 * tol, TAB + 1]);
}

// V-shaped wood-grain groove on the two ends of the front face (45 deg flanks, nothing to bridge)
module vgroove(z) for (sx = [-1, 1]) translate([sx > 0 ? PQ_W / 2 + 1 : -W, 0, 0]) rotate([90, 0, 90]) linear_extrude(sx > 0 ? W / 2 - PQ_W / 2 : W / 2 - PQ_W / 2 - 1)
    polygon([[-BASE_D / 2 - 0.5, z - 1.6], [-BASE_D / 2 - 0.5, z + 1.6], [-BASE_D / 2 + 1.0, z]]);
module wood_role() difference() {
    slab(BASE_H) base_outline();
    slots();
    for (z = [5, 10, 15, 19.5]) vgroove(z);
}
// snow cap: 0.2 mm stairs that widen by 0.18 mm per step (42 deg), so every layer overhangs less than a slicer's
// 45 deg limit and the lumps need no support; 14 steps cover the widest lump (2.5 mm)
module snow_role() difference() {
    union() {
        for (k = [0 : 13]) translate([0, 0, BASE_H + 0.2 * k]) slab(0.2 + eps) intersection() { snow_outline(); offset(delta = 0.18 * (k + 1)) base_outline(); }
        translate([0, 0, BASE_H + 2.8]) slab(1.7 + eps) snow_outline();
        translate([0, 0, BASE_H + 4.5]) slab(0.5) offset(delta = -0.5) snow_outline();
    }
    slots();
}
module base_piece(r) { if (r == "wood") wood_role(); if (r == "snow") snow_role(); }

module scallops() {        // lumpy snow edge along the front
    for (i = [0 : 17]) translate([-W / 2 + 5 + i * (W - 10) / 17, -BASE_D / 2 + 1.0 + ((i * 7) % 3) * 0.5]) circle(r = 3.4 + (i % 3) * 0.3);
}
module snow_outline() union() {
    offset(delta = 1.2) base_outline(r = 5);
    scallops();
    for (s = [-1, 1]) translate([s * (W / 2 - 3.5), -BASE_D / 2 + 6]) circle(r = 5);
}

// plaque: flat plate with family name + year   (local frame; the plate is 152 x 18)
PQ_W = 150; PQ_H = 20;
module plaque_plate() slab(2.4) rrect2d(PQ_W, PQ_H, 4);
module plaque_text() {
    fam = str("THE ", ucase(family), " FAMILY");
    yr  = str("Christmas ", year);
    tw  = PQ_W - 34;
    translate([0, 0, 2.4]) slab(0.9) {
        translate([0, 3.7]) fit_text(fam, 6.4, tw);
        translate([0, -4.9]) fit_text(yr, 4.2, tw * 0.7, font_text);
        // frame
        difference() { rrect2d(PQ_W - 3, PQ_H - 3, 3); rrect2d(PQ_W - 4.4, PQ_H - 4.4, 2.4); }
        // snow flakes at both ends
        for (s = [-1, 1]) translate([s * (PQ_W / 2 - 9), 0]) for (a = [0, 60, 120]) rotate(a) square([9, 1.1], center = true);
    }
}
module plaque(r) {
    if (r == "plate") plaque_plate();
    if (r == "text") plaque_text();
}

// name tag (local frame): small block with the name raised on its face
TAG_H = 11;  TAG_T = 5.2;
function tag_w() = max(20, PITCH - 4);
module tag(i, r) {
    nm = i < len(names) ? names[i] : "";
    if (r == "plate") slab(TAG_T) translate([0, TAG_H / 2]) rrect2d(tag_w(), TAG_H, 2.2);
    if (r == "text") translate([0, TAG_H / 2 - 0.2, TAG_T]) slab(0.9) fit_text(nm, 4.6, tag_w() - 4);
}

// ----------------------------------------------------------------------------------------
//  HOLLY (flat, lies on the snow)        roles: leaf, berry
// ----------------------------------------------------------------------------------------
module holly_leaf2d(len = 12, wid = 5.4) {        // pointed leaf with wavy edge
    hull() { circle(0.01); translate([len, 0]) circle(0.01); translate([len * 0.5, wid / 2]) circle(0.01); translate([len * 0.5, -wid / 2]) circle(0.01); }
}
module holly_leaf(len = 12, wid = 5.6) {
    pts = concat([[0, 0]], [for (i = [1 : 9]) let(t = i / 10) [len * t, (wid / 2) * (sin(180 * t) + (i % 2 == 0 ? 0.28 : -0.12))]],
                 [[len, 0]], [for (i = [9 : -1 : 1]) let(t = i / 10) [len * t, -(wid / 2) * (sin(180 * t) + (i % 2 == 0 ? 0.28 : -0.12))]]);
    polygon(pts);
}
HOLLY_BERRIES = [[0, 0.8, 2.5], [2.9, -1.5, 2.3], [-2.8, -1.9, 2.2]];
module berries() intersection() {
    union() for (b = HOLLY_BERRIES) translate([b[0], b[1], 0.6]) scale([1, 1, 1.15]) sphere(b[2]);
    translate([-50, -50, 0]) cube([100, 100, 20]);
}
module holly(r) {
    if (r == "leaf") difference() {
        slab(2.4) union() {
            for (a = [-20, 38, 110, 180, 245]) rotate(a) translate([1.2, 0]) holly_leaf(10.5, 5);
        }
        berries();
    }
    if (r == "berry") berries();
}

// ----------------------------------------------------------------------------------------
//  LANTERN (relief plate that stands on a tab)         roles: frame, glass
// ----------------------------------------------------------------------------------------
module lantern_outline2d() union() {
    translate([0, 33]) difference() { circle(3.6); circle(2.1); }           // handle
    translate([0, 29.6]) polygon([[-5.4, 0], [5.4, 0], [3.2, 4], [-3.2, 4]]);       // cap
    translate([0, 25]) polygon([[-8, 0], [8, 0], [6.6, 4.6], [-6.6, 4.6]]);        // roof
    translate([-6.8, 4]) square([13.6, 21.2]);                                     // body
    translate([0, 0]) polygon([[-8, 0], [8, 0], [6.8, 4.2], [-6.8, 4.2]]);          // foot
}
module lantern_window2d() translate([0, 14.8]) rrect2d(9.6, 16.4, 1.6);
module lantern(r) {
    if (r == "frame") union() {
        slab(6) difference() { lantern_outline2d(); lantern_window2d(); }
        translate([-4, -TAB, 0]) cube([8, TAB + 0.3, 6]);                               // tab
    }
    if (r == "glass") slab(3.4) lantern_window2d();
}

// ----------------------------------------------------------------------------------------
//  SKY panel (local frame, y = 0 is the top of the snow)     roles: panel, stars
// ----------------------------------------------------------------------------------------
SX = W / 200;
module panel2d() union() {
    intersection() { scale([W / 2, AH]) circle(1, $fn = 160); translate([-W / 2, 0]) square([W, AH + 1]); }
    translate([-(W - 8) / 2, -TAB]) square([W - 8, TAB + 0.5]);
}
module moon2d() difference() { translate([-60 * SX, 64]) circle(16.5); translate([-53 * SX, 68]) circle(14); }
module sparkle2d(R) { polygon([for (i = [0 : 7]) let(a = 90 + i * 45, rr = (i % 2 == 0) ? R : R * 0.26) [rr * cos(a), rr * sin(a)]]); }
STARS = [[-26, 86, 5.2, 5], [22, 84, 5.6, 5], [62, 71, 4.6, 5], [84, 56, 3.4, 5], [-86, 50, 3.4, 5], [-8, 62, 2.8, 4], [42, 90, 3.0, 4],
         [-33, 77, 2.6, 4], [76, 80, 2.6, 4], [-70, 86, 2.2, 4]];
module stars2d() {
    moon2d();
    for (s = STARS) translate([s[0] * SX, s[1]]) if (s[3] == 5) star2d(s[2], s[2] * 0.45); else sparkle2d(s[2]);
}
// silhouettes of the four trees (in the sky frame): the moon and the stars stay out of them, so a tree never has to
// share its space with a star (the stars behind a tree are simply hidden by it)
module tree_blockers() for (i = [0 : 3]) translate([TREES[i][0] * SX, 0.5]) offset(delta = 0.4) union() {
    tree_tiers2d(tree_w(i), tree_h(i));
    translate([0, tree_h(i) + 1.2]) star2d(5.2, 2.3);
}
module sky(r) {
    if (r == "panel") slab(3) panel2d();
    if (r == "stars") translate([0, 0, 3]) slab(1.7) difference() { intersection() { stars2d(); panel2d(); } tree_blockers(); }
}

// ----------------------------------------------------------------------------------------
//  TREES (local frame: x centred, y = 0 on the snow)     roles: green, snow, star
// ----------------------------------------------------------------------------------------
TREES = [[-80, 38, 50], [-47, 30, 68], [47, 30, 64], [80, 38, 50]];       // x, width, height (for W = 200)
function tree_w(i) = TREES[i][1] * min(1, SX * 1.02);
function tree_h(i) = TREES[i][2];
module tier(w, y0, h) polygon([[-w / 2, y0], [w / 2, y0], [0, y0 + h]]);
module tier_k(k, w, h) {
    if (k == 0) tier(w, 0, 0.44 * h);
    if (k == 1) tier(0.8 * w, 0.26 * h, 0.42 * h);
    if (k == 2) tier(0.58 * w, 0.52 * h, 0.48 * h);
}
module tree_tiers2d(w, h) soften(1.0) union() for (k = [0 : 2]) tier_k(k, w, h);
module tree_snow2d(w, h) intersection() {
    tree_tiers2d(w, h);
    soften(0.9) union() for (k = [0 : 2]) difference() { tier_k(k, w, h); translate([0, -5.2]) tier_k(k, w, h); }
}
module tree(i, r) {
    w = tree_w(i);  h = tree_h(i);
    if (r == "green") slab(4) difference() { tree_tiers2d(w, h); tree_snow2d(w, h); }
    if (r == "snow") slab(5.2) tree_snow2d(w, h);
    if (r == "star") slab(4.6) difference() { translate([0, h + 1.2]) star2d(5.2, 2.3); tree_tiers2d(w, h); }
}

// ----------------------------------------------------------------------------------------
//  HOUSE (local frame, y = 0 on the snow)        roles: wall, snow, window
// ----------------------------------------------------------------------------------------
module roof2d() soften(1.2) polygon([[-35, 25], [0, 63], [35, 25], [30.5, 23.2], [0, 54.5], [-30.5, 23.2]]);
module walls2d() polygon([[-27, 0], [27, 0], [27, 30], [0, 58], [-27, 30]]);
module chimney2d() difference() { translate([13, 38]) square([11, 27]); chimney_cap2d(); }
module chimney_cap2d() soften(0.8) translate([11.4, 64]) square([14.2, 4.6]);
module win2d(x) translate([x, 8]) arch2d(11, 16);
module win_bars2d(x) translate([x, 8]) { translate([-0.6, 0]) square([1.2, 16]); translate([-6, 7]) square([12, 1.2]); }
module door2d() translate([0, 0]) arch2d(13, 23);
module wreath2d() translate([0, 15.6]) difference() { circle(4.7); circle(2.9); }
module house(r) {
    if (r == "wall") union() {
        difference() {
            slab(4) difference() {
                union() { walls2d(); chimney2d(); }
                roof2d();
                for (x = [-14.5, 14.5]) difference() { win2d(x); win_bars2d(x); }
                door2d();
            }
            for (y = [5 : 4.6 : 30]) translate([-30, y, 3.2]) cube([60, 0.9, 2]);          // planks
        }
        slab(5) difference() { door2d(); wreath2d(); }
        translate([4.6, 10.4, 0]) slab(5.6) circle(0.9);                                      // knob (part of the door)
    }
    if (r == "snow") union() {
        slab(5.4) roof2d();
        slab(5.0) chimney_cap2d();
        slab(5.9) wreath2d();
    }
    if (r == "window") slab(3.6) union() { for (x = [-14.5, 14.5]) difference() { win2d(x); win_bars2d(x); } }
}

// ----------------------------------------------------------------------------------------
//  FIGURES (local frame: x centred, y = 0 where the bust meets the snow, tab below y = 0)
//  every figure is built from flat-bottomed domes, so it prints lying on its back with no overhang
// ----------------------------------------------------------------------------------------
HC2 = [0, 30];  HR2 = [12.5, 12, 11];
module HEAD() D(HC2, HR2);
function hz(x, y) = dz(HC2, HR2, x, y);
module TAB() translate([-8, -TAB - 0.2, 0]) cube([16, TAB + 0.4, 6]);
module BODY(rx = 14, ry = 17, h = 7, cy = 3) intersection() { D([0, cy], [rx, ry, h]); clipy(0); }
module SCARF(rx = 12.8, h = 9.0) { D([0, 19.2], [rx, 4.8, h]); D([5.6, 12.6], [3.2, 7.4, h + 0.4]); }
module eyeball(x, y, rx = 1.3, ry = 1.8, hh = 1.4, lift = -0.5) E([x, y, hz(x, y) + lift], [rx, ry, hh]);
module chain3(pts, r) for (i = [0 : len(pts) - 2]) hull() { translate(pts[i]) sphere(r); translate(pts[i + 1]) sphere(r); }
module smile(y0 = 25.6, w = 3.4, d = 1.0, r = 0.55) chain3([for (t = [-1 : 0.25 : 1]) let(x = w * t, y = y0 - d * (1 - t * t)) [x, y, hz(x, y) - 0.05]], r);
module EYES2(x = 4.7, y = 30, rx = 1.3, ry = 1.8) for (s = [-1, 1]) eyeball(s * x, y, rx, ry);

function fig_roles(kind) =
    kind == "boy" ? ["jacket", "scarf", "hat", "pom", "face", "eyes"] :
    kind == "girl" ? ["dress", "collar", "hair", "bow", "face", "eyes"] :
    kind == "snowman" ? ["body", "hat", "band", "scarf", "nose", "eyes"] :
    kind == "dog" ? ["fur", "ears", "scarf", "eyes"] :
    kind == "cat" ? ["fur", "white", "red"] :
    kind == "reindeer" ? ["fur", "antlers", "scarf", "nose", "eyes"] : [];

// ---- boy ---------------------------------------------------------------------------------
module boy_cap() intersection() { D([0, 34.8], [13.7, 11.6, 12.4]); clipy(32.6); }
module boy_brim() intersection() { D([0, 33], [14.1, 12.6, 12.7]); clipy(31.2, 34.2); }
module boy_pom() D([0, 47.2], [3.8, 3.8, 6.2]);
module boy_features() { EYES2(); smile(); }
module boy(r) {
    if (r == "jacket") difference() { union() { BODY(); TAB(); } HEAD(); SCARF(); }
    if (r == "scarf") SCARF();
    if (r == "hat") difference() { union() { boy_cap(); boy_brim(); } boy_pom(); }
    if (r == "pom") boy_pom();
    if (r == "face") difference() { HEAD(); boy_cap(); boy_brim(); SCARF(); boy_features(); }
    if (r == "eyes") difference() { boy_features(); boy_cap(); boy_brim(); }
}

// ---- girl --------------------------------------------------------------------------------
module girl_hairback() union() { D([0, 31], [14.9, 14.6, 8.0]); for (s = [-1, 1]) translate([s * 12.4, 22]) rotate(-s * 10) D([0, 0], [2.7, 5.6, 6.2]); }
module girl_bangs() intersection() { D([0, 37.5], [13.4, 8.4, 12.7]); clipy(34.0); }
module girl_bow() translate([-8.4, 44.6]) { rotate(28) D([-4.3, 0], [4.8, 3.0, 5.0]); rotate(-28) D([4.3, 0], [4.8, 3.0, 5.0]); D([0, 0], [2.2, 2.2, 5.6]); }
module girl_collar() D([0, 19.3], [9.6, 3.8, 8.8]);
module girl_features() { EYES2(4.6, 29.6); smile(25.4); }
module girl(r) {
    if (r == "dress") difference() { union() { BODY(); TAB(); } HEAD(); girl_collar(); girl_hairback(); }
    if (r == "collar") girl_collar();            // wins over the hair where both meet (hair, face and dress cut the collar out)
    if (r == "hair") difference() { union() { difference() { girl_hairback(); HEAD(); } girl_bangs(); } girl_bow(); girl_collar(); }
    if (r == "bow") girl_bow();
    if (r == "face") difference() { HEAD(); girl_bangs(); girl_collar(); girl_features(); }
    if (r == "eyes") difference() { girl_features(); girl_bangs(); girl_collar(); }
}

// ---- snowman -----------------------------------------------------------------------------
module sm_crown() intersection() { D([0, 48.6], [7.3, 7.6, 9.6]); clipy(42.8); }
module sm_brim() intersection() { D([0, 42.8], [11.4, 3.2, 7.8]); clipy(40.9, 43.9); }
module sm_band() intersection() { D([0, 45.0], [7.7, 7.9, 10.0]); clipy(43.4, 46.4); }
module sm_nose() translate([0, 29.0, hz(0, 29.0) - 1.0]) cylinder(r1 = 2.0, r2 = 0.35, h = 5.6);
function sbz(y) = 8.2 * sqrt(max(0, 1 - pow((y - 2) / 17.6, 2)));
module sm_features() {
    EYES2(4.6, 30.6, 1.2, 1.5);
    for (a = [-2 : 2]) { x = 3.0 * a; y = 25.8 - 0.55 * (4 - a * a) * 0.5; translate([x, y, hz(x, y) - 0.2]) sphere(0.75); }
    for (y = [14, 8.6, 3.2]) E([0, y, sbz(y) - 0.4], [1.6, 1.6, 1.3]);
}
module snowman(r) {
    if (r == "body") difference() {
        union() { BODY(15.8, 17.6, 8.2, 2); TAB(); HEAD(); }
        sm_crown(); sm_brim(); sm_band(); SCARF(13.4, 9.2); sm_nose(); sm_features();
    }
    if (r == "hat") difference() { union() { sm_crown(); sm_brim(); } sm_band(); }
    if (r == "band") sm_band();
    if (r == "scarf") difference() { SCARF(13.4, 9.2); sm_features(); }
    if (r == "nose") sm_nose();
    if (r == "eyes") sm_features();
}

// ---- dog ---------------------------------------------------------------------------------
module dog_muzzle() D([0, 26.4], [6.6, 5.0, 12.7]);
module dog_ears() for (s = [-1, 1]) translate([s * 10.8, 34]) rotate(s * 8) D([0, -2], [4.6, 9.8, 8.8]);
module dog_nose() E([0, 29.4, dz([0, 26.4], [6.6, 5.0, 12.7], 0, 29.4) - 0.5], [2.9, 2.3, 2.1]);
module dog_features() { EYES2(5.3, 33.0, 1.3, 1.7); dog_nose(); smile(25.2, 3.0, 0.9); }
module dog(r) {
    if (r == "fur") difference() { union() { HEAD(); dog_muzzle(); BODY(); TAB(); } dog_ears(); SCARF(); dog_features(); }
    if (r == "ears") dog_ears();
    if (r == "scarf") difference() { SCARF(); }
    if (r == "eyes") dog_features();
}

// ---- cat ---------------------------------------------------------------------------------
module cat_ears() for (s = [-1, 1]) hull() { D([s * 8.6, 38.2], [4.9, 3.6, 5.8]); D([s * 12.0, 46.6], [1.0, 1.0, 2.4]); }
module cat_inner() for (s = [-1, 1]) hull() { D([s * 8.9, 39.4], [2.5, 2.0, 6.9]); D([s * 11.2, 44.4], [0.6, 0.6, 3.6]); }
module cat_muzzle() { for (s = [-1, 1]) D([s * 3.6, 26.8], [4.7, 4.5, 11.8]); D([0, 25.2], [3.5, 3.2, 11.8]); }
module cat_bib() D([0, 8.5], [6.6, 8.8, 7.9]);
module cat_eye(s) E([s * 5.4, 32.4, hz(s * 5.4, 32.4) - 0.5], [2.6, 3.1, 1.6]);
module cat_pupil(s) E([s * 5.4, 32.4, hz(s * 5.4, 32.4) + 0.1], [1.3, 2.3, 1.5]);
function muz_z(x, y) = max(dz([-3.6, 26.8], [4.7, 4.5, 11.8], x, y), dz([3.6, 26.8], [4.7, 4.5, 11.8], x, y), dz([0, 25.2], [3.5, 3.2, 11.8], x, y));
module cat_nose() E([0, 27.8, muz_z(0, 27.8) - 0.2], [1.6, 1.2, 1.1]);
module cat_mouth() chain3([for (p = [[-3, 24.6], [-1.5, 23.4], [0, 24.7], [1.5, 23.4], [3, 24.6]]) [p[0], p[1], muz_z(p[0], p[1]) - 0.1]], 0.45);
module cat(r) {
    if (r == "fur") union() {
        difference() {
            union() { HEAD(); cat_ears(); BODY(); TAB(); }
            cat_muzzle(); cat_bib(); cat_inner(); for (s = [-1, 1]) { cat_eye(s); cat_pupil(s); } SCARF(); cat_nose();
        }
        for (s = [-1, 1]) difference() { cat_pupil(s); }
        cat_mouth();
    }
    if (r == "white") difference() {
        union() { cat_muzzle(); cat_bib(); cat_inner(); for (s = [-1, 1]) difference() { cat_eye(s); cat_pupil(s); } }
        SCARF(); cat_nose(); cat_mouth();
    }
    if (r == "red") union() { difference() { SCARF(); } cat_nose(); }
}

// ---- reindeer ----------------------------------------------------------------------------
module rd_ears() for (s = [-1, 1]) translate([s * 13.2, 36]) rotate(s * 38) D([0, 0], [3.4, 6.0, 7.4]);
module rd_antlers() for (s = [-1, 1]) {
    chain([[s * 6, 40], [s * 9, 47], [s * 11, 54]], 1.5, 1.5);
    chain([[s * 9, 47], [s * 15, 49.5]], 1.4, 1.4);
    chain([[s * 10.4, 52], [s * 15.5, 55.5]], 1.3, 1.3);
}
module rd_muzzle() D([0, 26.2], [6.4, 5.0, 12.4]);
module rd_nose() E([0, 27.6, dz([0, 26.2], [6.4, 5.0, 12.4], 0, 27.6) - 0.3], [3.0, 2.7, 2.5]);
module rd_features() { EYES2(5.2, 33, 1.3, 1.7); smile(23.6, 3.0, 0.9); }
module reindeer(r) {
    if (r == "fur") difference() { union() { HEAD(); rd_muzzle(); rd_ears(); BODY(); TAB(); } rd_antlers(); SCARF(); rd_nose(); rd_features(); }
    if (r == "antlers") rd_antlers();
    if (r == "scarf") SCARF();
    if (r == "nose") rd_nose();
    if (r == "eyes") rd_features();
}

module fig(i, r) {
    k = members[i];
    if (k == "boy") boy(r); else if (k == "girl") girl(r); else if (k == "snowman") snowman(r);
    else if (k == "dog") dog(r); else if (k == "cat") cat(r); else if (k == "reindeer") reindeer(r);
}

// ----------------------------------------------------------------------------------------
//  PIECES, ROLES, PLACEMENT
// ----------------------------------------------------------------------------------------
function piece_names() = concat(["sky"], [for (i = [0 : 3]) str("tree", i)], ["house", "base", "plaque", "holly0", "holly1", "lantern0", "lantern1"],
                                [for (i = [0 : N - 1]) str("fig", i)], [for (i = [0 : N - 1]) str("tag", i)]);
function idx(p, n) = parse_int(substr(p, n));
function parse_int(s, i = 0, v = 0) = i >= len(s) ? v : parse_int(s, i + 1, v * 10 + (ord(s[i]) - 48));
function substr(s, n) = _join([for (i = [n : len(s) - 1]) s[i]]);
function starts(p, pre) = len(p) >= len(pre) && _join([for (i = [0 : len(pre) - 1]) p[i]]) == pre;

function piece_roles(p) =
    p == "sky" ? ["panel", "stars"] :
    starts(p, "tree") ? ["green", "snow", "star"] :
    p == "house" ? ["wall", "snow", "window"] :
    p == "base" ? ["wood", "snow"] :
    p == "plaque" ? ["plate", "text"] :
    starts(p, "holly") ? ["leaf", "berry"] :
    starts(p, "lantern") ? ["frame", "glass"] :
    starts(p, "fig") ? fig_roles(members[idx(p, 3)]) :
    starts(p, "tag") ? ["plate", "text"] : [];

module emit(p, r) {
    if (p == "sky") sky(r);
    else if (starts(p, "tree")) tree(idx(p, 4), r);
    else if (p == "house") house(r);
    else if (p == "base") base_piece(r);
    else if (p == "plaque") plaque(r);
    else if (starts(p, "holly")) holly(r);
    else if (starts(p, "lantern")) lantern(r);
    else if (starts(p, "fig")) fig(idx(p, 3), r);
    else if (starts(p, "tag")) tag(idx(p, 3), r);
}

module place(p) {
    if (p == "sky") translate([0, PY, TOP]) rotate([90, 0, 0]) children();
    else if (starts(p, "tree")) translate([TREES[idx(p, 4)][0] * SX, PY - 3, TOP + 0.5]) rotate([90, 0, 0]) children();
    else if (p == "house") translate([0, PY - 3, TOP + 0.5]) rotate([90, 0, 0]) children();
    else if (p == "base") children();
    else if (p == "plaque") translate([0, -BASE_D / 2, BASE_H / 2]) rotate([90, 0, 0]) children();
    else if (starts(p, "holly")) let(s = idx(p, 5) == 0 ? -1 : 1) translate([s * (W / 2 - 10), -BASE_D / 2 + 11, TOP]) rotate(s * -20) children();
    else if (starts(p, "lantern")) let(s = idx(p, 7) == 0 ? -1 : 1) translate([s * (W / 2 - 9), 6, TOP]) rotate([90, 0, 0]) children();
    else if (starts(p, "fig")) translate([fx(idx(p, 3)), FY, TOP]) rotate([90, 0, 0]) children();
    else if (starts(p, "tag")) translate([fx(idx(p, 3)), TAGY, TOP]) rotate([90, 0, 0]) children();
}

module show(p) for (r = piece_roles(p)) if (role == "" || role == r) {
    if (layout == "assembled") place(p) emit(p, r); else emit(p, r);
}

if (part == "assembled") for (p = piece_names()) show(p);
else if (part == "manifest") for (p = piece_names()) echo(PIECE = p, ROLES = piece_roles(p));
else if (part != "none") show(part);
