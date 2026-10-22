/*
 * SANTA SOUND AMPLIFIER  -  a passive horn for a smartphone                                  (OpenSCAD 2021.01+, millimetres)
 * ----------------------------------------------------------------------------------------------------------------
 * Same idea as the pumpkin / the user's frog horn:   phone slot -> sound chamber under the phone -> throat -> horn -> mouth.
 * Here the mouth is the big bell in Santa's fluffy white beard; the phone stands in a slot in the back of the knitted hat.
 *
 * ONE PIECE, printed the way it stands (flat bottom), designed to need no support:
 *   - the figure is two eggs cut off at z = 0 (lean 37 deg): the round front (head, body, hat dome) and the tall hat tail behind it that
 *     carries the phone slot; the beard and the boots are eggs, too
 *   - the mouth: a flared oval whose roof FALLS towards the back wall (a megaphone), so each layer grows forward out of the solid back wall
 *   - the fur brim has a 45-degree underside, the nose, the eyes, the brows and the pompom are drops (a ball with a 45-degree cone below it)
 *   - the chamber has no ceiling (it is the lower end of the phone slot), the throat is a straight A-frame (pointed 38.7-degree roof)
 *
 *  Roles (every role is ONE filament colour, see tools/family_palette.py):
 *     santa :  red (body, hat, cradle)   white (beard, fur brim, pompom, brows, buckle)   peach (face, nose)   black (eyes, boots, belt)
 *     phone :  render-only prop (not printed)
 *  part   = "santa" | "all" | "phone" | "manifest"
 */

part   = "all";
layout = "assembled";
role   = "";

/* [Phone] */
phone_w = 74;        // width of the slot   (iPhone 13: 71.5 mm - bare or in a thin case)
phone_t = 12;        // thickness of the slot (7.65 mm + case; keep it snug)
slot_d  = 40;        // how deep the phone stands in the figure (below its flat top)

/* [Front: head, body and the dome of the hat]  one egg: red body below, knitted hat above */
FR    = 47;          // radius at the widest point (x)
FRY   = 0.90;        // y radius = FRY * x radius
FZC   = 30;          // height of the widest point
FZL   = 60;          // lower semi-axis (cut off at z = 0: lean 37 deg)
FZU   = 58;          // upper semi-axis
FPU   = 2.8;         // squareness of the upper half
FZT   = 85;          // top of the dome
FY    = 4;           // centre of the front egg (y)
HAT_N = 44;  HAT_A = 0.9;  HAT_Z0 = 66;  HAT_Z1 = 72;      // knit wales: number, depth, where they fade in

/* [Back: the tail of the hat, it carries the phone slot] */
RR    = 48;          // radius at the widest point (x)
RRY   = 0.56;        // y radius = RRY * x radius
RZC   = 30;          // height of the widest point
RZL   = 60;          // lower semi-axis
RZU   = 54;          // upper semi-axis
RPU   = 7;           // squareness of the upper half (the flat top must be wider than the 74 mm slot)
RZT   = 80;          // flat top (the phone slot is cut into it)
RY    = -25;         // centre of the tail egg (y)

/* [Beard and mouth] */
BD_R = 45;  BD_RY = 0.62;  BD_ZC = 22;  BD_ZL = 46;  BD_ZU = 29;  BD_PU = 2.4;  BD_Y = 30;      // the beard: an egg in front of the body
MX  = 26;            // half width of the mouth
MZC = 24;            // centre height of the mouth
MZH = 19;            // half height
RK  = 1.2;           // the roof of the mouth FALLS RK mm per mm towards the back (40 deg from the vertical)
ROOM = 10;           // depth of the mouth (the back wall is ROOM mm behind the front)

/* [Sound path] */
Y_S     = -25;       // centre of the phone slot (y)
HZ0     = 6;         // floor of chamber, horn and mouth
HH1     = 3;         // height of the vertical walls of the A-frame throat
W_H     = 28;        // width of the horn (straight: a flat ridge needs no support)
Y_E     = 46;        // the horn ends inside the mouth
SHAFT_W = 50;        // width of the chamber under the phone
SLH     = 0.80;      // lean (tan) of the horn roof

/* [Fit and quality] */
fn = 64;
// unused here, accepted so that tools/export_roles.py can pass the same -D set as for the other models
family = "SANTA"; year = 2026; members = []; names = []; font_serif = ""; font_text = ""; tol = 0.2;

/* [Hidden] */
$fn = fn;
eps = 0.01;
Z_SF = RZT - slot_d;                                         // floor of the slot (the phone's bottom edge rests here)
Y_H0 = Y_S + phone_t / 2 - 1;                                // the throat starts 1 mm inside the chamber

// ---------------------------------------------------------------------------------------------
//  eggs: a surface of revolution cut off at z = 0, built as one polyhedron
// ---------------------------------------------------------------------------------------------
function pprof(z, R, zc, rzl, rzu, pu) = z <= zc ? R * sqrt(max(0, 1 - pow((zc - z) / rzl, 2))) : R * pow(max(0, 1 - pow((z - zc) / rzu, pu)), 1 / pu);
function smooth01(t) = t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
module egg(R, ryf, zc, rzl, rzu, pu, ztop, nr = 0, rib = 0, na = 96, wales = false) {
    zs = concat([for (k = [0 : 8]) k * zc / 8], [for (k = [1 : 24]) zc + k * (ztop - zc) / 24]);
    nz = len(zs);
    polyhedron(
        points = concat(
            [for (k = [0 : nz - 1]) for (j = [0 : na - 1])
                let(a = 360 * j / na,
                    r = pprof(zs[k], R, zc, rzl, rzu, pu) * (1 + rib * cos(nr * a)) + (wales ? HAT_A * smooth01((zs[k] - HAT_Z0) / (HAT_Z1 - HAT_Z0)) * cos(HAT_N * a) : 0))
                [r * cos(a), ryf * r * sin(a), zs[k]]],
            [[0, 0, 0], [0, 0, ztop]]),
        faces = concat(
            [for (k = [0 : nz - 2]) for (j = [0 : na - 1]) each [
                [k * na + j, (k + 1) * na + j, (k + 1) * na + (j + 1) % na],
                [k * na + j, (k + 1) * na + (j + 1) % na, k * na + (j + 1) % na]]],
            [for (j = [0 : na - 1]) [j, (j + 1) % na, nz * na]],
            [for (j = [0 : na - 1]) [(nz - 1) * na + (j + 1) % na, (nz - 1) * na + j, nz * na + 1]]));
}
module ball()  translate([0, FY, 0]) egg(FR, FRY, FZC, FZL, FZU, FPU, FZT, 0, 0, 240, true);       // the front egg with the knitted dome
module tail()  translate([0, RY, 0]) egg(RR, RRY, RZC, RZL, RZU, RPU, RZT, 0, 0, 120, false);
function BD_ztop() = BD_ZC + 0.96 * BD_ZU;
module beard() translate([0, BD_Y, 0]) egg(BD_R, BD_RY, BD_ZC, BD_ZL, BD_ZU, BD_PU, BD_ztop(), 0, 0, 120);
// y of the front of the beard at (x, z)
function bprof(z) = pprof(z, BD_R, BD_ZC, BD_ZL, BD_ZU, BD_PU);
function ysb(x, z) = BD_Y + BD_RY * bprof(z) * sqrt(max(0, 1 - pow(x / bprof(z), 2)));
// the front egg: radius at height z, y of its front at (x, z)
function bl(z) = pprof(z, FR, FZC, FZL, FZU, FPU);
function ysball(x, z) = FY + FRY * bl(z) * sqrt(max(0, 1 - pow(x / bl(z), 2)));
function rl(z) = pprof(z, RR, RZC, RZL, RZU, RPU);          // the tail egg

// a "drop": a ball with a cone below it that points mostly DOWN (the front-lower side of the cone must stay >= 50 deg from the horizontal)
module drop(p, r, dir, len = 0) hull() {
    translate(p) sphere(r, $fn = 24);
    translate(p + dir * (len > 0 ? len : r * 1.5)) sphere(r * 0.25, $fn = 10);
}

// ---------------------------------------------------------------------------------------------
//  the sound path
// ---------------------------------------------------------------------------------------------
module slot() translate([-phone_w / 2, Y_S - phone_t / 2, Z_SF]) cube([phone_w, phone_t, 200]);
module shaft() translate([-SHAFT_W / 2, Y_S - phone_t / 2, HZ0]) cube([SHAFT_W, phone_t, Z_SF - HZ0 + 1]);
function sec_a(w) = [[-w / 2, HZ0], [w / 2, HZ0], [w / 2, HZ0 + HH1], [0, HZ0 + HH1 + w / 2 / SLH], [-w / 2, HZ0 + HH1]];
module slab(y, w) translate([0, y, 0]) rotate([90, 0, 0]) linear_extrude(0.02, center = true) polygon(sec_a(w));
module horn_body() hull() { slab(Y_H0, W_H); slab(Y_E, W_H); }
module prism_front(y0, y1) translate([0, y0, 0]) mirror([0, 1, 0]) rotate([90, 0, 0]) linear_extrude(y1 - y0) children();

// the mouth: a flared oval.  Its lower half is a "bowl" (up-facing); the upper half is a roof sheet that passes through the oval's
// upper edge on the front of the beard and falls RK per mm towards the back (the back wall follows the front of the beard).
function ze(x) = MZC + MZH * sqrt(max(0, 1 - pow(x / MX, 2)));
function bowl() = concat(
    [for (i = [0 : 24]) let(t = 180 + 180 * i / 24) [MX * cos(t), MZC + MZH * sin(t)]],
    [[MX, 120], [-MX, 120]]);
RX_N = 64;
module roof_solid() {
    n = RX_N;
    P = [for (i = [0 : n]) let(x = -MX - 2 + (2 * MX + 4) * i / n, xc = max(-MX, min(MX, x)), yf0 = ysb(xc, ze(xc)), yb = yf0 - ROOM, yf = yf0 + 16)
        each [[x, yb, -1], [x, yf, -1], [x, yf, ze(xc) + RK * 16], [x, yb, ze(xc) - RK * ROOM]]];
    polyhedron(points = P, faces = concat(
        [for (i = [0 : n - 1]) for (k = [0 : 3]) each [
            [i * 4 + k, (i + 1) * 4 + k, (i + 1) * 4 + (k + 1) % 4],
            [i * 4 + k, (i + 1) * 4 + (k + 1) % 4, i * 4 + (k + 1) % 4]]],
        [[0, 1, 2], [0, 2, 3]],
        [[n * 4, n * 4 + 3, n * 4 + 2], [n * 4, n * 4 + 2, n * 4 + 1]]));
}
module mouth() intersection() { roof_solid(); prism_front(0, 120) polygon(bowl()); }

module cavities() { slot(); shaft(); horn_body(); mouth(); }

// ---------------------------------------------------------------------------------------------
//  the colour regions (each one is a solid; the roles are made from them with a priority, see role_solid)
// ---------------------------------------------------------------------------------------------
// white
BRIM_Z = 60;
module brim() translate([0, FY, 0]) scale([1, FRY, 1]) rotate_extrude($fn = 160) {
    r0 = bl(BRIM_Z);  r1 = bl(BRIM_Z + 14);
    polygon([[r0 - 4, BRIM_Z - 1], [r0 - 0.4, BRIM_Z - 1], [r0, BRIM_Z], [r0 + 6.0, BRIM_Z + 8.5], [r0 + 6.8, BRIM_Z + 10.5], [r0 + 6.0, BRIM_Z + 12.5], [r1 + 4.2, BRIM_Z + 14], [r1 - 4, BRIM_Z + 14]]);
}
module pompom() translate([-16, FY + 10, FZT - 12]) scale([1, 1, 0.9]) sphere(10.5, $fn = 40);      // sits on the dome (its lower half is inside it)
module brows() for (s = [-1, 1]) intersection() {                       // coloured patches (1.5 mm) flush with the face - a raised brow would hang in the air
    difference() { ball(); scale([0.965, 0.965, 1]) ball(); }
    translate([s * 14, FY + 40, 59]) rotate([0, 0, 0]) scale([8, 30, 1.7]) sphere(1, $fn = 32);
}
module buckle() translate([0, RY - RRY * rl(15), 0]) rotate([90, 0, 90]) translate([0, 0, -8.5]) linear_extrude(17)      // a plate (17 wide) that sticks out 3 mm at the back; its underside leans 50 deg
    polygon([[6, 8], [2.4, 8], [-3.2, 14.6], [-3.2, 21.5], [6, 21.5]]);
module white_parts() { beard(); brim(); pompom(); brows(); buckle(); }
// peach: the face is a coloured skin (3.5 mm, flush with the ball: a raised patch would have an overhanging lower rim) between the brim and the beard; the nose is a drop
module face() intersection() {
    difference() { ball(); scale([0.93, 0.93, 1]) ball(); }
    translate([0, FY + 40, 55]) scale([36, 28, 11.5]) sphere(1, $fn = 48);
}
module nose() { yn = ysb(0, 50) + 0.5;  drop([0, yn, 54.5], 6.4, [0, -0.3, -0.954], 15); }
module peach_parts() { face(); nose(); }
// black
module belt_f() rotate_extrude($fn = 160)
    polygon(concat([[bl(9) - 2, 9], [bl(11), 11]], [for (z = [13.5 : 1 : 17.5]) [bl(z) + 1.3, z]], [[bl(19), 19], [bl(20.5) - 2, 20.5]]));
module belt_r() rotate_extrude($fn = 160)
    polygon(concat([[rl(9) - 2, 9], [rl(11), 11]], [for (z = [13.5 : 1 : 17.5]) [rl(z) + 1.3, z]], [[rl(19), 19], [rl(20.5) - 2, 20.5]]));
module belt() { translate([0, FY, 0]) scale([1, FRY, 1]) belt_f(); translate([0, RY, 0]) scale([1, RRY, 1]) belt_r(); }
module boots() for (s = [-1, 1]) translate([s * 34, FY + 36, 0]) egg(12.5, 1, 6, 12, 10, 2.2, 6 + 0.95 * 10, 6, 0.06, 60);
module eyes() for (s = [-1, 1]) {
    ye = ysball(13, 55.5) + 0.6;
    drop([s * 13, ye, 55.5], 3.4, [0, -0.35, -0.937], 9);
}
module black_parts() { boots(); eyes(); belt(); }

// ---------------------------------------------------------------------------------------------
//  the roles.  Priority (a part is cut by everything above it):  eyes/boots > nose > white > face > belt-under-beard > red
// ---------------------------------------------------------------------------------------------
module role_solid(r) {
    if (r == "black") difference() { union() { boots(); eyes(); difference() { belt(); beard(); buckle(); } } cavities(); }
    if (r == "white") difference() { white_parts(); nose(); boots(); eyes(); cavities(); }
    if (r == "peach") difference() { union() { nose(); difference() { face(); white_parts(); } } boots(); eyes(); cavities(); }
    if (r == "red")   difference() { union() { ball(); tail(); } white_parts(); peach_parts(); boots(); eyes(); belt_cut(); cavities(); }
}
// where the belt is (it is black) - but the beard and the buckle hide it
module belt_cut() difference() { belt(); beard(); buckle(); }

// ---- the phone, only for the renders ----------------------------------------------------------
module phone(r) {
    if (r == "body")   translate([0, Y_S, Z_SF + 0.5]) hull() for (x = [-1, 1], z = [0, 1]) translate([x * (71.5 / 2 - 8), 0, 8 + z * (146.7 - 16)]) rotate([90, 0, 0]) cylinder(r = 8, h = 7.65, center = true, $fn = 32);
    if (r == "screen") translate([0, Y_S + 3.9, Z_SF + 0.5]) hull() for (x = [-1, 1], z = [0, 1]) translate([x * (71.5 / 2 - 8.5), 0, 8 + z * (146.7 - 16)]) rotate([90, 0, 0]) cylinder(r = 7.5, h = 0.1, center = true, $fn = 32);
}

// ---------------------------------------------------------------------------------------------
function piece_names() = ["santa"];
function piece_roles(p) = p == "santa" ? ["red", "white", "peach", "black"] : p == "phone" ? ["body", "screen"] : [];
module emit(p, r) {
    if (p == "santa") role_solid(r);
    else if (p == "phone") phone(r);
}
C_RED = "#C8102E";  C_WHITE = "#F3EFE6";  C_PEACH = "#F2B79A";  C_BLACK = "#1A1A1E";
function filament(p, r) = p == "phone" ? (r == "screen" ? "#6FA8FF" : "#15171C") : r == "red" ? C_RED : r == "white" ? C_WHITE : r == "peach" ? C_PEACH : C_BLACK;
module show(p) { for (r = piece_roles(p)) if (role == "" || role == r) color(filament(p, r)) emit(p, r); }

if (part == "all" || part == "assembled") { show("santa"); }
else if (part == "manifest") { for (p = piece_names()) echo(PIECE = p, ROLES = piece_roles(p)); }
else if (part != "none") show(part);
