// =====================================================================
//  Forest Balance Mobile  -  a Calder-style mobile whose balance points are COMPUTED
//  Autumn Nest contest, entry #7 (Autumn in Motion)
//
//  Four arms and five woodland ornaments (owl, maple leaf, acorn, mushroom, pinecone).
//  Every arm hangs from a ring above its bar and carries two J-hooks. A ring on top of
//  each ornament (or sub-mobile) simply clips over a hook: no tools, glue or cord.
//  Each pivot ring is placed at the centre of mass of everything the arm carries,
//  so every arm hangs level. Pieces are flat 2D profiles extruded 2.4-3 mm: they print
//  lying on the bed without supports, all on one bed, and their mass follows their area,
//  which is what makes the balance predictable.
//
//  Tested with OpenSCAD 2021.01.
// =====================================================================

/* [Part to render] */
// One printable piece, or asm = the assembled mobile (use asm_part)
part = "owl"; // [owl, leaf, acorn, mushroom, pinecone, arm1, arm2, arm3, arm4, asm]
// Piece of the assembled mobile to export (world coordinates)
asm_part = "all"; // [all, arm1, arm2, arm3, arm4, owl, leaf, acorn, mushroom, pinecone]

/* [Thickness] */
// Ornament plate thickness (mm). 2.4 = six 0.4 mm perimeters, so the piece prints solid
t_orn = 2.4;
// Arm thickness (mm)
t_arm = 3.0;
// Engraving depth (mm)
engrave = 0.8;

/* [Hook joint] */
// Width of the hook wire in its plane (mm)
w_h = 3.0;
// Bend radius of the hook (mm, centre line)
r_h = 5.0;
// Straight drop from the bar to the bend (mm)
d_h = 12;
// Height of the hook tip above the bend centre (mm)
tip_h = 7;
// Outer and hole radius of every hanging ring (mm)
ring_ro = 7.5;
ring_ri = 4.5;
// Free length of the post between the bar and the pivot ring (mm)
post_h = 9;
// Bar height (mm)
bar_b = 4;

/* [Masses of the ornaments, in grams - measured by tools/mobile_check.py] */
m_owl = 3.57;
m_leaf = 2.08;
m_acorn = 2.50;
m_mushroom = 3.46;
m_pinecone = 2.39;

/* [Arm spans: distance between the two hang points (mm)] */
D1 = 120;
D2 = 66;
D3 = 80;
D4 = 62;

/* [Quality] */
$fn = 64;

// ---------------------------------------------------------------------
//  Balance: the pivot sits at the centre of mass of what the arm carries.
//  Hang points are at x = 0 and x = D along the arm. Bar and both hooks are symmetric
//  about D/2; the post and pivot ring are centred on the pivot, so they add no torque.
// ---------------------------------------------------------------------
rho = 0.00124;                                   // g/mm3, PLA. Only ratios matter
hook_area = w_h * (d_h + PI * r_h + tip_h) + PI * w_h * w_h / 4;
piv_area  = PI * (ring_ro * ring_ro - ring_ri * ring_ri) + 4 * (post_h + 1);
piv_m     = piv_area * t_arm * rho;
function arm_m(D) = ((D + 2 * r_h + w_h) * bar_b + 2 * hook_area) * t_arm * rho;   // bar + hooks
function arm_xp(D, mL, mR) = (mR * D + arm_m(D) * D / 2) / (mL + mR + arm_m(D));
function sub_m(D, mL, mR) = mL + mR + arm_m(D) + piv_m;                           // seen by the parent

// the tree: A1(A2(leaf, acorn), A3(owl, A4(mushroom, pinecone)))
M4  = sub_m(D4, m_mushroom, m_pinecone);
M2  = sub_m(D2, m_leaf, m_acorn);
M3  = sub_m(D3, m_owl, M4);
xp1 = arm_xp(D1, M2, M3);
xp2 = arm_xp(D2, m_leaf, m_acorn);
xp3 = arm_xp(D3, m_owl, M4);
xp4 = arm_xp(D4, m_mushroom, m_pinecone);

y_c = bar_b + post_h + ring_ro;                   // pivot ring centre above the bar bottom
hy  = -(d_h + r_h) - y_c;                         // hang point below the pivot ring centre

assert(d_h - tip_h >= t_orn + 2, "hook tip too close to the bar: a ring could not be put on");
assert(2 * ring_ri > sqrt(w_h * w_h + t_arm * t_arm) + 1, "ring hole too small for the hook wire");

// ---------------------------------------------------------------------
//  Maple leaf outline (same construction as the wind chime sail)
// ---------------------------------------------------------------------
lobe_p = 0.55;
tooth = 0.06;
natural_lobes = [[90, 1, 36], [145, 0.92, 34], [35, 0.92, 34], [210, 0.62, 30], [330, 0.62, 30]];

function lobe_r(th, l) =
    let(d = abs(((th - l[0] + 540) % 360) - 180), x = max(0, 1 - d / l[2]))
    l[1] * pow(x, lobe_p) * (1 + tooth * cos(1440 * d / l[2]));
function leaf_r(th, lobes, R, base) = R * max(base, max([for (l = lobes) lobe_r(th, l)]));

module leaf2d(lobes, R, base) {
    polygon([for (a = [0 : 1 : 359]) leaf_r(a, lobes, R, base) * [cos(a), sin(a)]]);
}

module line2d(p0, p1, w = 0.9) {
    hull() { translate(p0) circle(d = w, $fn = 10); translate(p1) circle(d = w, $fn = 10); }
}

module leaf_veins(lobes, R, r0) {
    for (l = lobes) rotate(l[0]) {
        line2d([r0, 0], [0.9 * l[1] * R, 0]);
        for (f = [0.5, 0.68]) for (s = [-1, 1])
            line2d([f * l[1] * R, 0],
                   [f * l[1] * R + 0.13 * l[1] * R * cos(40), s * 0.13 * l[1] * R * sin(40)], 0.8);
    }
}

// ---------------------------------------------------------------------
//  Ornaments. Origin = centre of the ring hole. Each has:
//  shape_X (outline of the body, top at y = 0), holes_X (cut through), engr_X (engraved)
// ---------------------------------------------------------------------
body_top = -(ring_ro + 3);

module ring2d() { difference() { circle(r = ring_ro); circle(r = ring_ri); } }
// short neck joining the ring to the body
module neck2d() { translate([-2, body_top - 0.5]) square([4, (1 - ring_ro) - (body_top - 0.5)]); }

// owl
module shape_owl() {
    translate([0, -21]) scale([1, 1.25]) circle(r = 17);
    for (s = [-1, 1]) polygon([[s * 14, -12], [s * 16, 4], [s * 5, -2]]);
}
module holes_owl() { for (s = [-1, 1]) translate([s * 7, -16]) circle(d = 9); }
module engr_owl() {
    polygon([[-3, -21], [3, -21], [0, -28]]);                                    // beak
    for (s = [-1, 1]) translate([s * 7, -16]) difference() { circle(d = 13); circle(d = 11.8); }   // brows
    for (k = [0 : 3]) {                                                          // belly feathers
        line2d([-8, -31 - 3.2 * k], [0, -35 - 3.2 * k]);
        line2d([8, -31 - 3.2 * k], [0, -35 - 3.2 * k]);
    }
}

// mushroom
module shape_mushroom() {
    intersection() { translate([0, -22]) circle(r = 23, $fn = 96); translate([-30, -22]) square([60, 30]); }
    translate([0, -32]) offset(r = 2) offset(delta = -2) square([14, 24], center = true);
}
module holes_mushroom() {
    translate([-11, -12]) circle(d = 6);
    translate([5, -8]) circle(d = 5);
    translate([13, -15]) circle(d = 4);
    translate([-3, -17]) circle(d = 3.5);
}
module engr_mushroom() {
    intersection() {
        translate([0, -22]) difference() { circle(r = 21, $fn = 96); circle(r = 20.1, $fn = 96); }
        translate([-30, -22]) square([60, 30]);
    }
    line2d([-7, -33], [7, -33]);
    line2d([-6, -39], [6, -39]);
}

// acorn
module shape_acorn() {
    translate([-1.5, -5]) square([3, 8]);                                        // stem reaches into the cap
    intersection() { translate([0, -12]) scale([1, 0.6]) circle(r = 15); translate([-20, -12]) square([40, 20]); }
    hull() { translate([0, -23]) circle(r = 12); translate([0, -43]) circle(r = 1.2); }   // nut overlaps the cap
}
module holes_acorn() { }
module engr_acorn() {
    intersection() {                                                             // cap cross-hatch
        translate([-15, -12]) square([30, 9]);
        union() {
            for (k = [-6 : 6]) line2d([k * 4 - 6, -12], [k * 4 + 6, -2], 0.8);
            for (k = [-6 : 6]) line2d([k * 4 + 6, -12], [k * 4 - 6, -2], 0.8);
        }
    }
    line2d([0, -17], [0, -37]);
    translate([-12, -12.6]) square([24, 0.8]);                                   // cap rim
}

// leaf: hangs by its stem, lobes pointing down
module shape_leaf() {
    translate([0, -12]) scale([1, -1]) leaf2d(natural_lobes, 24, 0.30);
    line2d([0, 2], [0, -14], 3.5);
}
module holes_leaf() { }
module engr_leaf() { translate([0, -12]) scale([1, -1]) leaf_veins(natural_lobes, 24, 3); }

// pinecone
module shape_pinecone() {
    translate([-1.5, -6]) square([3, 9]);                                        // stem reaches into the body
    hull() { translate([0, -17]) circle(r = 13); translate([0, -42]) circle(r = 2.5); }
}
module holes_pinecone() { }
module engr_pinecone() {
    for (r = [0 : 7]) for (c = [-2 : 2])
        translate([c * 6.2 + (r % 2) * 3.1, -9 - r * 4.4])
            difference() {
                circle(d = 7.4);
                circle(d = 6.5);
                translate([-5, 0]) square([10, 5]);
            }
}

module orn2d(id) {
    union() {
        ring2d();
        neck2d();
        translate([0, body_top]) difference() {
            if (id == "owl") shape_owl();
            else if (id == "mushroom") shape_mushroom();
            else if (id == "acorn") shape_acorn();
            else if (id == "leaf") shape_leaf();
            else shape_pinecone();
            if (id == "owl") holes_owl();
            else if (id == "mushroom") holes_mushroom();
        }
    }
}

module engr2d(id) {
    translate([0, body_top]) intersection() {
        offset(delta = -1.2) {
            if (id == "owl") shape_owl();
            else if (id == "mushroom") shape_mushroom();
            else if (id == "acorn") shape_acorn();
            else if (id == "leaf") shape_leaf();
            else shape_pinecone();
        }
        if (id == "owl") engr_owl();
        else if (id == "mushroom") engr_mushroom();
        else if (id == "acorn") engr_acorn();
        else if (id == "leaf") engr_leaf();
        else engr_pinecone();
    }
}

module ornament(id) {
    difference() {
        linear_extrude(t_orn) orn2d(id);
        translate([0, 0, t_orn - engrave]) linear_extrude(engrave + 1) engr2d(id);
    }
}

// ---------------------------------------------------------------------
//  Arm: bar + two J-hooks + post + pivot ring. Hang points at x = 0 and x = D.
// ---------------------------------------------------------------------
module wire(pts) {
    for (i = [0 : len(pts) - 2])
        hull() { translate(pts[i]) circle(d = w_h, $fn = 24); translate(pts[i + 1]) circle(d = w_h, $fn = 24); }
}

// hook hanging at x = xh; s = +1 for the right end (bend opens toward -x), -1 for the left end
module hook2d(xh, s) {
    arc = [for (a = [0 : 10 : 180]) [xh + s * r_h * cos(a), -d_h - r_h * sin(a)]];
    wire(concat([[xh + s * r_h, 0]], arc, [[xh - s * r_h, -d_h + tip_h]]));
}

module arm2d(D, xp) {
  difference() {
    union() { arm_body2d(D, xp); }
    translate([xp, y_c]) circle(r = ring_ri);       // the ring hole stays clear
  }
}

module arm_body2d(D, xp) {
    translate([-(r_h + w_h / 2), 0]) square([D + 2 * r_h + w_h, bar_b]);
    hook2d(0, -1);
    hook2d(D, 1);
    // post up to the bottom of the ring (1 mm overlap); it must NOT reach the ring hole
    translate([xp - 2, bar_b - 0.01]) square([4, post_h + 1]);
    translate([xp, y_c]) ring2d();
}

module arm_part(k) {
    D = [D1, D2, D3, D4][k - 1];
    xp = [xp1, xp2, xp3, xp4][k - 1];
    difference() {
        linear_extrude(t_arm) arm2d(D, xp);
        // tally marks: k slits tell the arms apart
        translate([0, 0, t_arm - engrave])
            linear_extrude(engrave + 1)
                for (i = [0 : k - 1]) translate([D / 2 + (i - (k - 1) / 2) * 3 - 0.4, 0.8]) square([0.8, bar_b - 1.6]);
    }
}

// ---------------------------------------------------------------------
//  Assembly (world coordinates, z up, first arm's pivot ring at the origin)
// ---------------------------------------------------------------------
function hang(P, yaw, lx, ly) = P + [lx * cos(yaw), lx * sin(yaw), ly];

P1 = [0, 0, 0];
P2 = hang(P1, 0, -xp1, hy);       P3 = hang(P1, 0, D1 - xp1, hy);
Pleaf = hang(P2, 90, -xp2, hy);   Pacorn = hang(P2, 90, D2 - xp2, hy);
Powl = hang(P3, 90, -xp3, hy);    P4 = hang(P3, 90, D3 - xp3, hy);
Pmush = hang(P4, 0, -xp4, hy);    Pcone = hang(P4, 0, D4 - xp4, hy);

module stand(P, yaw, t) {
    translate(P) rotate([0, 0, yaw]) rotate([90, 0, 0]) linear_extrude(t, center = true) children();
}

module stand_orn(id, P, yaw) {
    translate(P) rotate([0, 0, yaw]) rotate([90, 0, 0]) translate([0, 0, -t_orn / 2]) ornament(id);
}

module stand_arm(k, P, yaw) {
    D = [D1, D2, D3, D4][k - 1];
    xp = [xp1, xp2, xp3, xp4][k - 1];
    translate(P) rotate([0, 0, yaw]) rotate([90, 0, 0]) translate([-xp, -y_c, -t_arm / 2]) arm_part(k);
}

module asm() {
    if (asm_part == "all" || asm_part == "arm1") stand_arm(1, P1, 0);
    if (asm_part == "all" || asm_part == "arm2") stand_arm(2, P2, 90);
    if (asm_part == "all" || asm_part == "arm3") stand_arm(3, P3, 90);
    if (asm_part == "all" || asm_part == "arm4") stand_arm(4, P4, 0);
    if (asm_part == "all" || asm_part == "leaf") stand_orn("leaf", Pleaf, 0);
    if (asm_part == "all" || asm_part == "acorn") stand_orn("acorn", Pacorn, 0);
    if (asm_part == "all" || asm_part == "owl") stand_orn("owl", Powl, 0);
    if (asm_part == "all" || asm_part == "mushroom") stand_orn("mushroom", Pmush, 90);
    if (asm_part == "all" || asm_part == "pinecone") stand_orn("pinecone", Pcone, 90);
}

if (part == "asm") asm();
else if (part == "arm1") arm_part(1);
else if (part == "arm2") arm_part(2);
else if (part == "arm3") arm_part(3);
else if (part == "arm4") arm_part(4);
else ornament(part);

echo(str("PIVOTS xp = ", [xp1, xp2, xp3, xp4], "  arm masses (bar+hooks) = ", [arm_m(D1), arm_m(D2), arm_m(D3), arm_m(D4)]));
echo(str("SUBTREE masses A2,A3,A4 = ", [M2, M3, M4]));
echo(str("PIV|A1|", P1[0], "|", P1[1], "|", P1[2], "|0"));
echo(str("PIV|A2|", P2[0], "|", P2[1], "|", P2[2], "|90"));
echo(str("PIV|A3|", P3[0], "|", P3[1], "|", P3[2], "|90"));
echo(str("PIV|A4|", P4[0], "|", P4[1], "|", P4[2], "|0"));
echo(str("PIV|leaf|", Pleaf[0], "|", Pleaf[1], "|", Pleaf[2], "|0"));
echo(str("PIV|acorn|", Pacorn[0], "|", Pacorn[1], "|", Pacorn[2], "|0"));
echo(str("PIV|owl|", Powl[0], "|", Powl[1], "|", Powl[2], "|0"));
echo(str("PIV|mushroom|", Pmush[0], "|", Pmush[1], "|", Pmush[2], "|90"));
echo(str("PIV|pinecone|", Pcone[0], "|", Pcone[1], "|", Pcone[2], "|90"));
