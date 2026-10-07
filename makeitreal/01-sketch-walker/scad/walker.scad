// =====================================================================================
//  SKETCH WALKER  -  a hand-drawn creature that walks off the page   (Make It Real 2026)
//  Parametric OpenSCAD model (tested with OpenSCAD 2021.01).  Printer: Bambu Lab P2S, FDM.
//
//  One printable part :  openscad -D 'PART="crank_0"' -o crank_0.stl walker.scad
//  Whole print job    :  PART = "print_links" | "print_feet" | "print_drive" | "print_plates" | "print_deck" | "print_body"
//  Preview            :  PART = "assembly";  THETA = crank angle (deg)
//
//  Axes: x = walking direction, y = left, z = up.  Crank axes are parallel to y.
//  Leg layout: 3 legs per side (front x=+S, mid x=0, rear x=-S), tripod gait,
//  every leg is a six-bar linkage synthesised in design/linkage6.py.
// =====================================================================================
include <walker_params.scad>;      // linkage dimensions from the optimiser (design/gen_params.py)
include <body_outline.scad>;       // creature silhouette from YOUR drawing (design/sketch_to_svg.py)

PART  = "assembly";
THETA = 0;
SHOW_BODY = true;
LINK_FILTER = "";                  // preview helper: draw only one link type ("crank", "coupler", ...)
$fn   = 40;

// ---------- printer-dependent clearances: calibrate with tolerance_coupon.scad ----------
pin_d     = 1.75;                  // filament offcut used as hinge pins
pin_hole  = 2.00;                  // free-running hole for hinge pins
fix_hole  = 1.80;                  // press-fit hole for fixed pins in the plates
shaft_d   = 5.0;                   // printed D-shaft
d_flat    = 0.7;                   // depth of the D flat
d_clear   = 0.15;                  // clearance of D-bores on the shaft
brg_hole  = 5.45;                  // round hole in the plates the shaft turns in
gear_bl   = 0.20;                  // tooth backlash (tangential, mm)

// ---------- links and layers ----------
tl      = 3.0;                     // thickness of every moving link
lay_gap = 1.2;                     // free space between neighbouring links (a flattened filament-pin head is <= 1 mm)
pitch   = tl + lay_gap;
rL      = 4.0;                     // link half-width (joint boss radius)
hubC    = 5.2;                     // crank hub radius around the shaft
pad_r   = 5.5;                     // foot pad radius
plate_t = 4.0;                     // chassis side plate thickness
W_in    = 36.0;                    // free width between the plates
y_in    = W_in / 2;                // inner face of a plate (|y|)
y_out   = y_in + plate_t;          // outer face of a plate (|y|)
lay0    = y_out + lay_gap;         // |y| where layer 0 starts
// layer index of every link inside a leg set (found by design/layout_check.py)
L_CRANK = 0; L_COUPLER = 1; L_ROCKER = 2; L_ELINK = 2; L_FOOT = 3;
SET_LAYERS = 4;                    // front+rear legs use layers 0-3, mid legs layers 4-7
function layer_y(j) = lay0 + j * pitch;           // |y| of the inner face of layer j

// ---------- drivetrain ----------
S       = 60;                      // axle spacing
gear_z  = 20;  gear_m = 1.5;  gear_w = 5;
gear_rp = gear_z * gear_m / 2;     // 15 mm pitch radius -> centre distance 30 = S/2
pin_z   = 10;                      // pinion teeth (on the motor)
motor_cd = (gear_z + pin_z) * gear_m / 2;      // 22.5 mm above the mid axle
motor_w = 12.2;  motor_h = 10.2;   // N20 gear-motor cross-section (12 x 10) + 0.2 clearance - MEASURE YOURS
motor_len = 25;                    // N20 body incl. gearbox - MEASURE YOURS
gear_y0 = y_in - 0.4 - gear_w;     // gears hug the +y plate, the motor lives on the other side
hub_len = gear_y0 + y_in - 0.4;    // hub of every gear reaches the -y plate

// ---------- chassis ----------
deck_z    = 33;  deck_t = 4;       // deck underside / thickness (pinion tip reaches z = 31.5)
rail_z0   = 22;                    // lower edge of the plate's top rail
leg_x     = [S, 0, -S];            // front, mid, rear axle x
rear_end  = -138;
front_end = S + 14;
tabs_x    = [58, 24, -36, -98, -128];  // deck tab positions (x)
tab_w     = 8;
batt_l = 58; batt_w = 31; batt_h = 15; // 2xAAA holder 58x31x15 - MEASURE YOURS
batt_x0   = rear_end + 4;           // keeps the tray clear of the rear gear tip (checked in design/mass_com.py)

// ---------- creature body ----------
body_t     = 16;                   // thickness of the creature plaque
body_x0    = -112;                 // x of the body's left edge (tune with design/stability.py)
body_cut   = 6;                    // mm cut off the bottom so the belly is flat
body_pegs  = [0.3, 0.7];           // peg positions as a fraction of body_w
// ---------------------------------------------------------------------------------------

module hole2(p = [0, 0], d = pin_hole) { translate(p) circle(d = d); }
module cap2(L, r1 = rL, r2 = rL) { hull() { circle(r = r1); translate([L, 0]) circle(r = r2); } }

// D profile of the shaft: circle with one flat (flat normal = +x)
module D2d(c = 0) {
    intersection() {
        circle(d = shaft_d + 2 * c);
        translate([-shaft_d, -shaft_d]) square([shaft_d + shaft_d / 2 - d_flat + c, 2 * shaft_d]);
    }
}

// involute spur gear, 20 deg pressure angle.  Tooth 0 is centred on +x (= the D-flat direction).
function invp(rb, r) = let(al = acos(rb / r)) (tan(al) * 180 / PI - al);   // degrees
module gear2d(z, m = gear_m, bl = gear_bl) {
    pa = 20; rp = z * m / 2; rb = rp * cos(pa); ro = rp + m; rr = rp - 1.25 * m;
    half_p = 90 / z - (bl / 2) / rp * 180 / PI;
    tb = half_p + invp(rb, rp);
    n = 7;
    rs = [for (i = [0 : n]) rb + (ro - rb) * i / n];
    right = [for (r = rs) let(an = -tb + invp(rb, r)) [r * cos(an), r * sin(an)]];
    left  = [for (i = [n : -1 : 0]) let(r = rs[i], an = tb - invp(rb, r)) [r * cos(an), r * sin(an)]];
    union() {
        circle(r = rr + 0.05, $fn = 90);
        for (k = [0 : z - 1]) rotate(k * 360 / z)
            polygon(concat([[rr * cos(-tb), rr * sin(-tb)]], right, left, [[rr * cos(tb), rr * sin(tb)]]));
    }
}

// ============================ LINKS (2-D outlines in local frames) ====================
// crank: shaft at (0,0), crank pin at (a,0) rotated by ph (0 or 180) relative to the D flat
module crank2d(ph = 0) {
    difference() {
        rotate(ph) hull() { circle(r = hubC); translate([a, 0]) circle(r = rL); }
        D2d(d_clear);
        rotate(ph) hole2([a, 0]);
    }
}
module coupler2d() {            // A (0,0), B (b,0), C (pc,qc)
    difference() {
        hull() { circle(r = rL); translate([b, 0]) circle(r = rL); translate([pc, qc]) circle(r = rL); }
        hole2([0, 0]); hole2([b, 0]); hole2([pc, qc]);
    }
}
module rocker2d() { difference() { cap2(d); hole2([0, 0]); hole2([d, 0]); } }       // O4 -> B
module foot2d() {               // C (0,0), E (e1,0), foot P (pf,qf)
    difference() {
        union() {
            cap2(e1);
            hull() { circle(r = rL); translate([pf, qf]) circle(r = rL * 0.9); }
            translate([pf, qf]) circle(r = pad_r);
        }
        hole2([0, 0]); hole2([e1, 0]);
    }
}
module elink2d() { difference() { cap2(e2); hole2([0, 0]); hole2([e2, 0]); } }       // E -> O6
module crank_part(ph = 0, lbl = "", lp = [0, 0]) {
    difference() {
        extrude_link(lbl, lp) crank2d(ph);
        // set pin: hub radius + shaft, along the arm axis, from the back side of the hub
        rotate([0, 0, ph]) translate([-hubC - 1, 0, tl / 2]) rotate([0, 90, 0]) cylinder(d = fix_hole, h = hubC + 1 + shaft_d / 2 + 0.6);
    }
}
module extrude_link(lbl = "", lp = [0, 0]) {
    difference() {
        linear_extrude(height = tl, convexity = 6) children();
        if (lbl != "") translate([lp[0], lp[1], tl - 0.4]) linear_extrude(height = 0.5)
            text(lbl, size = 3.0, halign = "center", valign = "center", font = "Liberation Sans:style=Bold");
    }
}

// ============================ kinematics (same maths as design/linkage6.py) ==========
function cc_hit(P0, r0, P1, r1, br) =
    let(v = P1 - P0, dd = norm(v), l = (r0 * r0 - r1 * r1 + dd * dd) / (2 * dd),
        h = sqrt(max(r0 * r0 - l * l, 0)), e = v / dd, n = [-e[1], e[0]])
    P0 + e * l + br * n * h;
function leg_pose(th) =
    let(A = [a * cos(th), a * sin(th)], B = cc_hit(A, b, O4, d, br1),
        u = (B - A) / b, n = [-u[1], u[0]], C = A + pc * u + qc * n,
        E = cc_hit(C, e1, O6, e2, br2), u2 = (E - C) / e1, n2 = [-u2[1], u2[0]],
        P = C + pf * u2 + qf * n2)
    [A, B, C, E, P];
function ang(v) = atan2(v[1], v[0]);

// put a link (local origin p=(x,z), local +x along phi) into layer j on side s (+1 left, -1 right)
module place_link(p, phi, j, s, x0) {
    y0 = layer_y(j);
    translate([x0 + p[0], s > 0 ? y0 + tl : -y0, p[1]]) rotate([90, 0, 0]) rotate(phi) children();
}
// th = angle of the crank ARM; ph = arm offset relative to the shaft's D flat (0 for left cranks, 180 for right cranks)
// sphi = angle of the shaft (and of its D flat) -> the crank part is turned with the shaft
module leg(th, x0, base, s, ph, sphi) {
    P = leg_pose(th);  A = P[0]; B = P[1]; C = P[2]; E = P[3];
    if (LINK_FILTER == "" || LINK_FILTER == "crank")   color("tomato")      place_link([0, 0], sphi, base + L_CRANK, s, x0) crank_part(ph);
    if (LINK_FILTER == "" || LINK_FILTER == "coupler") color("gold")        place_link(A, ang(B - A), base + L_COUPLER, s, x0) extrude_link() coupler2d();
    if (LINK_FILTER == "" || LINK_FILTER == "rocker")  color("deepskyblue") place_link(O4, ang(B - O4), base + L_ROCKER, s, x0) extrude_link() rocker2d();
    if (LINK_FILTER == "" || LINK_FILTER == "foot")    color("seagreen")    place_link(C, ang(E - C), base + L_FOOT, s, x0) extrude_link() foot2d();
    if (LINK_FILTER == "" || LINK_FILTER == "elink")   color("orchid")      place_link(E, ang(O6 - E), base + L_ELINK, s, x0) extrude_link() elink2d();
}

// ============================ drivetrain parts ======================================
// shaft: axis along y, D flat DOWN (z = 0) so it prints without supports; vertical cross holes take 1.75 mm cotter pins
shaft_end_ext = 0.8;               // shaft sticks out this far beyond the crank's outer face (gap to next layer is lay_gap)
function shaft_len(base) = 2 * (layer_y(base + L_CRANK) + tl + shaft_end_ext);
function shaft_yc(base)  = layer_y(base + L_CRANK) + tl / 2;      // |y| of the crank mid-plane = set-pin position
module shaft_part(len, yc) {
    // axis along y, flat on z=0; the set-pin holes run along the flat's normal (vertical here), through crank hub + shaft
    difference() {
        translate([0, 0, shaft_d / 2 - d_flat]) mirror([0, 0, 1])
            rotate([90, 0, 0]) rotate([0, 0, 90]) translate([0, 0, -len / 2]) linear_extrude(height = len, convexity = 4) D2d(0);
        for (sy = [-1, 1]) translate([0, sy * yc, 0]) cylinder(d = fix_hole, h = 20, center = true);
    }
}
// gear with hub on one side, printed gear-down (hub up).  D bore for shaft gears, round bore for idlers
module gear_part(z, dbore = true, hub_r = 4.8, hub = hub_len) {
    difference() {
        union() { linear_extrude(height = gear_w) gear2d(z); cylinder(r = hub_r, h = gear_w + hub); }
        translate([0, 0, -1]) linear_extrude(height = gear_w + hub + 2) { if (dbore) D2d(d_clear); else circle(d = pin_hole + 0.1); }
    }
}
// pinion for a N20 gear motor (3 mm D shaft, flat to 2.5 mm)
module pinion_part() {
    difference() {
        union() { linear_extrude(height = gear_w) gear2d(pin_z); cylinder(r = 4.6, h = gear_w + 4); }
        translate([0, 0, -1]) linear_extrude(height = gear_w + 6)
            intersection() { circle(d = 3.05); translate([-3, -3]) square([3 + 1.25 + 0.1, 6]); }
    }
}

// ============================ chassis ===============================================
module plate2d() {
    difference() {
        union() {
            for (i = [0 : 2]) hull() {                       // leg blocks: axle + rocker pivot + e-link pivot
                translate([leg_x[i], 0]) circle(r = (i == 1) ? 9 : 11);
                translate([leg_x[i] + O4[0], O4[1]]) circle(r = 5.5);
                translate([leg_x[i] + O6[0], O6[1]]) circle(r = 5.5);
            }
            hull() { translate([leg_x[2], 0]) circle(r = 8); translate([leg_x[0], 0]) circle(r = 8); }   // spine
            translate([rear_end - 6, rail_z0]) square([front_end - rear_end + 6, deck_z + deck_t - rail_z0]);  // top rail
            hull() { translate([rear_end + 6, rail_z0 + 4]) circle(r = 5); translate([leg_x[2] - 6, 0]) circle(r = 9); }
        }
        for (x = leg_x) translate([x, 0]) circle(d = brg_hole);                        // shaft bearings
        for (x = [S / 2, -S / 2]) translate([x, 0]) circle(d = fix_hole);              // idler pins
        for (x = leg_x) {                                                              // fixed pins
            translate([x + O4[0], O4[1]]) circle(d = fix_hole);
            translate([x + O6[0], O6[1]]) circle(d = fix_hole);
        }
        for (x = tabs_x) translate([x - tab_w / 2 - 0.1, deck_z - 0.1]) square([tab_w + 0.2, deck_t + 5]);   // deck notches
        for (w = [[S / 2, 22, 12], [-S / 2, 22, 12], [S / 2, -21, 8], [-S / 2, -21, 8], [-S - 38, 4, 12], [-S - 62, 6, 10]])
            translate([w[0], w[1]]) circle(r = w[2] / 2 + 1);                         // lightening windows
    }
}
module plate_part() { linear_extrude(height = plate_t, convexity = 8) plate2d(); }

// deck: z=0 is its underside (world z = deck_z).  Motor cradle + battery pocket hang below.  Printed upside down.
mot_z0 = motor_cd - motor_h / 2 - deck_z;           // motor bottom, relative to the deck underside (negative)
mot_z1 = motor_cd + motor_h / 2 - deck_z;
module deck_part() {
    W = W_in + 2 * plate_t;
    x0 = rear_end; x1 = front_end - 8;
    my0 = -y_in + 0.5;                                // motor body spans y = my0 .. my0+motor_len
    difference() {
        union() {
            translate([x0, -y_in, 0]) cube([x1 - x0, W_in, deck_t]);
            for (x = tabs_x) translate([x - tab_w / 2, -W / 2, 0]) cube([tab_w, W, deck_t]);
            for (yy = [my0 + 3, my0 + 15])             // motor cradle: two U blocks
                translate([-motor_w / 2 - 2.2, yy, mot_z0 + 1.6]) cube([motor_w + 4.4, 6, -mot_z0 - 1.6]);
            translate([batt_x0 - 2, -batt_w / 2 - 2, -batt_h - 2]) cube([batt_l + 4, batt_w + 4, batt_h + 2]);   // battery tray
        }
        for (yy = [my0 + 2, my0 + 14])                 // motor pocket (open at the bottom)
            translate([-motor_w / 2, yy, mot_z0 - 1]) cube([motor_w, 8, mot_z1 - mot_z0 + 1 + 0.01 - 0]);
        translate([batt_x0, -batt_w / 2, -batt_h - 3]) cube([batt_l, batt_w, batt_h + 3 + 0.01]);               // battery pocket
        translate([batt_x0 + batt_l - 4, -4, -batt_h]) cube([10, 8, batt_h + deck_t + 1]);                      // wire passage
        for (px = body_pegs) for (py = [-5, 5]) translate([body_x0 + px * body_w, py, -1]) cylinder(d = 3.3, h = deck_t + 2);
    }
}
module deck_print() { translate([0, 0, deck_t]) mirror([0, 0, 1]) deck_part(); }   // top face on the bed

// ============================ creature body (from the user's drawing) ===============
module body_clipped() { intersection() { body_2d(); translate([-1, body_cut]) square([body_w + 2, body_h]); } }
engr_d = 0.8;                      // depth of the engraved drawing lines
module body_solid() {
    // plaque with 1.2 mm stepped chamfers + two pegs that lock into the deck
    c = 1.2; st = 3;
    translate([0, -body_cut, c]) linear_extrude(height = body_t - 2 * c, convexity = 8) body_clipped();
    for (i = [0 : st - 1]) {
        o = c * (st - i) / (st + 1);
        translate([0, -body_cut, i * c / st]) linear_extrude(height = c / st + 0.001, convexity = 8) offset(delta = -o) body_clipped();
        translate([0, -body_cut, body_t - (i + 1) * c / st]) linear_extrude(height = c / st + 0.001, convexity = 8) offset(delta = -o) body_clipped();
    }
    for (px = body_pegs) for (pz = [body_t / 2 - 5, body_t / 2 + 5]) translate([px * body_w, -5.5, pz]) rotate([-90, 0, 0]) cylinder(d = 3.0, h = 6.5);
}
module body_part() {
    // the pen strokes of YOUR drawing are engraved into both faces (the back face is its mirror image)
    difference() {
        body_solid();
        if (has_lines) {
            translate([0, -body_cut, -0.01]) linear_extrude(height = engr_d + 0.01, convexity = 6) lines_2d();
            translate([0, -body_cut, body_t - engr_d]) linear_extrude(height = engr_d + 0.01, convexity = 6)
                translate([body_w, 0]) mirror([1, 0]) lines_2d();
        }
    }
}
module body_placed() {      // body stands on the deck, thickness centred on y
    translate([body_x0, body_t / 2, deck_z + deck_t + body_cut * 0]) rotate([90, 0, 0]) translate([0, 0, 0]) children();
}

// ============================ assembly preview ======================================
module plates_asm()  { for (s = [-1, 1]) translate([0, s > 0 ? y_out : -y_in, 0]) rotate([90, 0, 0]) plate_part(); }
module deck_asm()    { translate([0, 0, deck_z]) deck_part(); }
// TRIPOD PHASE RULE: the mid shaft (with its gear and both cranks) is mounted turned by 180 deg relative to the
// front/rear shafts.  All three shaft gears are identical, so the D-flats stay aligned with the gear teeth.
function phi(i) = THETA + (i == 1 ? 180 : 0);      // angle of shaft i (0 front, 1 mid, 2 rear)
module shafts_asm() {
    for (i = [0 : 2]) {
        base = (i == 1) ? SET_LAYERS : 0;
        len = shaft_len(base);
        translate([leg_x[i], 0, 0]) rotate([90, 0, 0]) rotate(phi(i)) translate([0, 0, -len / 2]) linear_extrude(height = len) D2d(0);
    }
}
module gears_asm() {            // 3 shaft gears + 2 idlers (idlers run backwards, half a tooth out of phase)
    for (i = [0 : 4]) {
        gx = [S, S / 2, 0, -S / 2, -S][i];
        ang_i = (i % 2 == 0) ? phi(i / 2) : 9 - THETA;
        translate([gx, gear_y0 + gear_w, 0]) rotate([90, 0, 0]) rotate(ang_i) gear_part(gear_z, i % 2 == 0);
    }
}
module pinion_asm()  { translate([0, gear_y0 + gear_w, motor_cd]) rotate([90, 0, 0]) pinion_part(); }
module motor_asm()   { translate([-motor_w / 2 + 0.1, -y_in + 0.5, motor_cd - motor_h / 2 + 0.1]) cube([motor_w - 0.2, motor_len, motor_h - 0.2]); }
module battery_asm() { translate([batt_x0, -batt_w / 2 + 0.2, deck_z - batt_h + 0.2]) cube([batt_l - 0.4, batt_w - 0.4, batt_h - 0.4]); }
module legs_asm() {
    for (i = [0 : 2]) for (s = [-1, 1]) {      // six legs: left cranks = crank_0, right cranks = crank_180
        ph = s > 0 ? 0 : 180;
        leg(phi(i) + ph, leg_x[i], (i == 1) ? SET_LAYERS : 0, s, ph, phi(i));
    }
}
module assembly() {
    color("lightgray") plates_asm();
    color("silver") deck_asm();
    color("dimgray") shafts_asm();
    color("peru") gears_asm();
    color("slategray") pinion_asm();
    color("black") motor_asm();
    color("darkgreen") battery_asm();
    legs_asm();
    if (SHOW_BODY) color("khaki") body_placed() body_part();
}

// ============================ print jobs (flat, support-free, P2S bed 256 x 256) ====
module nest(items, cols, dx, dy) { for (i = [0 : len(items) - 1]) translate([(i % cols) * dx, floor(i / cols) * dy, 0]) children(items[i]); }

module print_feet() {            // 6 foot links, 3 x 2
    for (i = [0 : 5]) translate([(i % 3) * 68 + (pad_r - pf + 4), floor(i / 3) * 64 + (pad_r - qf + 4), 0]) extrude_link("FT", [e1 / 2, 0]) foot2d();
}
module print_links() {           // 6 cranks (3 per phase) + 6 couplers + 6 rockers + 6 e-links
    for (i = [0 : 5]) {
        if (i < 3) translate([12, 10 + i * 26, 0]) crank_part(0, "S0", [a * 0.55, 0]);
        else       translate([12, 10 + i * 26, 0]) crank_part(180, "S1", [-a * 0.55, 0]);
        translate([48, 10 + i * 26, 0]) extrude_link("CP", [b * 0.5 + 1.5, -1.0]) coupler2d();
        translate([88, 10 + i * 26, 0]) extrude_link("RK", [d / 2, 0]) rocker2d();
        translate([130, 10 + i * 26, 0]) extrude_link("EL", [e2 / 2, 0]) elink2d();
    }
}
module print_drive() {           // 3 shaft gears + 2 idlers (gear-down), pinion, 2 shafts end + 1 mid
    for (i = [0 : 2]) translate([20 + i * 40, 20, 0]) gear_part(gear_z, true);
    for (i = [0 : 1]) translate([20 + i * 40, 62, 0]) gear_part(gear_z, false);
    translate([140, 20, 0]) pinion_part();
    for (i = [0 : 1]) translate([40, 100 + i * 9, 0]) rotate(90) shaft_part(shaft_len(0), shaft_yc(0));
    translate([70, 120, 0]) rotate(90) shaft_part(shaft_len(SET_LAYERS), shaft_yc(SET_LAYERS));
}
// ---- single-leg test rig: mini plate with a hand grip.  Print this + 1 of every link + 1 end shaft BEFORE the full walker.
module leg_rig_plate2d() {
    difference() {
        union() {
            hull() { circle(r = 9); translate(O4) circle(r = 5.5); translate(O6) circle(r = 5.5); }
            hull() { translate(O6) circle(r = 5.5); translate([O6[0] + 2, O6[1] + 44]) circle(r = 7); }     // grip
        }
        circle(d = brg_hole);
        translate(O4) circle(d = fix_hole);
        translate(O6) circle(d = fix_hole);
        translate([O6[0] + 2, O6[1] + 44]) circle(d = 6);                                               // finger hole
    }
}
module print_leg_rig() {         // plate + one leg (crank_0, coupler, rocker, foot, elink) + one end shaft
    translate([60, 70, 0]) linear_extrude(height = plate_t) leg_rig_plate2d();
    translate([140, 14, 0]) crank_part(0, "S0", [a * 0.55, 0]);
    translate([140, 36, 0]) extrude_link("CP", [b * 0.5 + 1.5, -1.0]) coupler2d();
    translate([140, 56, 0]) extrude_link("RK", [d / 2, 0]) rocker2d();
    translate([140, 76, 0]) extrude_link("EL", [e2 / 2, 0]) elink2d();
    translate([(pad_r - pf + 4) + 110, 100 + (pad_r - qf + 4), 0]) extrude_link("FT", [e1 / 2, 0]) foot2d();
    translate([40, 8, 0]) rotate(90) shaft_part(shaft_len(0), shaft_yc(0));
}
module print_plates() { translate([150, 30, 0]) { plate_part(); translate([0, 74, 0]) plate_part(); } }
module print_deck() { translate([140, 24, 0]) deck_print(); }
module print_body() { body_part(); }

// ============================ part selector =========================================
if      (PART == "assembly")     assembly();
else if (PART == "crank_0")      crank_part(0, "S0", [a * 0.55, 0]);
else if (PART == "crank_180")    crank_part(180, "S1", [-a * 0.55, 0]);
else if (PART == "coupler")      extrude_link("CP", [b * 0.5 + 1.5, -1.0]) coupler2d();
else if (PART == "rocker")       extrude_link("RK", [d / 2, 0]) rocker2d();
else if (PART == "foot")         extrude_link("FT", [e1 / 2, 0]) foot2d();
else if (PART == "elink")        extrude_link("EL", [e2 / 2, 0]) elink2d();
else if (PART == "plate")        plate_part();
else if (PART == "deck")         deck_print();
else if (PART == "shaft_end")    shaft_part(shaft_len(0), shaft_yc(0));
else if (PART == "shaft_mid")    shaft_part(shaft_len(SET_LAYERS), shaft_yc(SET_LAYERS));
else if (PART == "gear")         gear_part(gear_z, true);
else if (PART == "idler")        gear_part(gear_z, false);
else if (PART == "pinion")       pinion_part();
else if (PART == "body")         body_part();
else if (PART == "print_links")  print_links();
else if (PART == "print_feet")   print_feet();
else if (PART == "print_drive")  print_drive();
else if (PART == "print_plates") print_plates();
else if (PART == "print_leg_rig") print_leg_rig();
else if (PART == "print_deck")   print_deck();
else if (PART == "print_body")   print_body();
