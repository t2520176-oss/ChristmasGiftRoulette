// =====================================================================
//  Tree-Ring Rotary Organizer
//  Autumn Nest contest, entry #1 (Autumn Storage)
//
//  A round organizer built like a tree trunk: three NESTED RINGS (a core and two growth rings) stand in a
//  base. Each ring is its own print, so you can lift one out and carry it to the workbench. A lid turns on
//  the outer ring and has ONE window; it opens one sector of each ring at a time, and clicks into six
//  positions. The top of the lid shows growth rings, drawn from a random series of wide "good years" and
//  narrow "dry years".
//
//    core   : round cup, always open through the hole in the middle of the lid
//    ring A : six sectors, each holds AAA batteries standing
//    ring B : six sectors, each holds AA batteries standing
//
//  Parts: base, ring_b, ring_a, core, lid (+ lid_asm for checks). Tested with OpenSCAD 2021.01.
//  Use asm_parts=true to export every part in its assembled position (for checks and renders).
// =====================================================================

/* [Part to render] */
part = "lid"; // [base, ring_b, ring_a, core, lid, asm]
// export parts in the assembled position instead of the print position
asm_parts = false;
// asm / lid: which of the 6 positions the lid window shows (0..5)
k_window = 0;

/* [Rings] */
ring_h = 54;          // height of the rings; AA batteries (50.5 mm) stand in them
floor_t = 1.6;        // floor of every ring
wall = 1.2;           // thin walls and dividers
wall_bo = 2.0;        // outer wall of ring B: thicker, it carries the detent grooves
sectors = 6;
gap = 0.4;            // clearance between nested rings
core_ro = 17.6;
ra_ri = core_ro + gap;
ra_ro = 36.4;
rb_ri = ra_ro + gap;
rb_ro = 59.2;

/* [Base] */
base_t = 3;
lip_h = 6;            // the lip around ring B
lip_t = 2.4;

/* [Lid] */
lid_t = 2.4;
skirt_h = 8;          // the skirt runs around the top of ring B
skirt_t = 1.6;
brim = 2.2;           // the plate is wider than the skirt: finger grip
hole_r = 16.2;        // opening in the middle: the core is always open
win_angle = 53;       // angular width of the window (a sector is 56 to 59 degrees wide between its dividers)
win_r0 = 18.8;
win_r1 = 58.4;
detent_ribs = 3;      // vertical ribs inside the skirt; they click into the 6 grooves of ring B
groove_depth = 0.7;
groove_r = 1.5;
rib_r = 1.0;
rib_out = 0.35;       // how far a rib presses on ring B between the grooves

/* [Tree rings on the lid] */
ring_seed = 11;
ring_depth = 0.5;
ring_w = 0.8;

/* [Quality] */
$fn = 144;

top_z = base_t + ring_h;                         // top of the rings = underside of the lid plate
skirt_ri = rb_ro + gap;
skirt_ro = skirt_ri + skirt_t;
lip_ri = rb_ro + gap;
lid_r = skirt_ro + brim;
win_c = 360 / sectors / 2;                       // the window is in the middle of a sector

assert(wall_bo + 0.5 < rb_ro - rb_ri, "ring B too narrow");
assert(skirt_h < ring_h - 10, "skirt too long");

// ---- a ring with radial dividers: floor, outer wall, inner wall, dividers --------------------------
module ring_ring(ri, ro, w_in, w_out) {
    union() {
        difference() {
            cylinder(r = ro, h = floor_t);
            translate([0, 0, -1]) cylinder(r = ri, h = floor_t + 2);
        }
        difference() {
            cylinder(r = ro, h = ring_h);
            translate([0, 0, -1]) cylinder(r = ro - w_out, h = ring_h + 2);
        }
        difference() {
            cylinder(r = ri + w_in, h = ring_h);
            translate([0, 0, -1]) cylinder(r = ri, h = ring_h + 2);
        }
        for (k = [0 : sectors - 1])
            rotate(360 / sectors * k)
                translate([ri + w_in - 0.01, -wall / 2, 0]) cube([ro - w_out - ri - w_in + 0.02, wall, ring_h]);
    }
}

module core() {
    difference() {
        cylinder(r = core_ro, h = ring_h);
        translate([0, 0, floor_t]) cylinder(r = core_ro - wall, h = ring_h);
    }
}

module ring_a() { ring_ring(ra_ri, ra_ro, wall, wall); }

module ring_b() {
    difference() {
        ring_ring(rb_ri, rb_ro, wall, wall_bo);
        // detent grooves: one in the middle of each sector, vertical, in the top 12 mm
        for (k = [0 : sectors - 1])
            rotate(win_c + 360 / sectors * k)
                translate([rb_ro - groove_depth + groove_r, 0, ring_h - 12]) cylinder(r = groove_r, h = 13, $fn = 36);
    }
}

// ---- base -------------------------------------------------------------------------------------
module base() {
    union() {
        cylinder(r1 = lip_ri + lip_t - 1, r2 = lip_ri + lip_t, h = 1);
        translate([0, 0, 1 - 0.01]) cylinder(r = lip_ri + lip_t, h = base_t - 1 + 0.01);
        difference() {
            translate([0, 0, base_t - 0.01]) cylinder(r = lip_ri + lip_t, h = lip_h + 0.01);
            translate([0, 0, base_t - 1]) cylinder(r = lip_ri, h = lip_h + 2);
        }
    }
}

// ---- lid --------------------------------------------------------------------------------------
function cum(v, i = 0, s = 0) = i >= len(v) ? [] : concat([s + v[i]], cum(v, i + 1, s + v[i]));
ring_widths = rands(1.4, 4.6, 24, ring_seed);
ring_radii = [for (r = cum(ring_widths)) 17 + r];

module window2d() {
    h = win_angle / 2;
    offset(r = 1.5) offset(delta = -1.5)
        intersection() {
            difference() { circle(r = win_r1); circle(r = win_r0); }
            polygon([[0, 0], [100 * cos(-h), 100 * sin(-h)], [100 * cos(h), 100 * sin(h)]]);
        }
}

// the lid in its assembled orientation: plate from z = top_z up, the skirt hangs down around ring B
module lid_in_place() {
    difference() {
        union() {
            translate([0, 0, top_z]) cylinder(r = lid_r, h = lid_t);
            translate([0, 0, top_z - skirt_h])
                difference() {
                    cylinder(r = skirt_ro, h = skirt_h + 0.01);
                    translate([0, 0, -1]) cylinder(r = skirt_ri, h = skirt_h + 2);
                }
            // detent ribs: pressed against ring B between its grooves
            for (j = [0 : detent_ribs - 1])
                rotate(win_c + 360 / detent_ribs * j)
                    translate([rb_ro - rib_out + rib_r, 0, top_z - skirt_h + 1])
                        cylinder(r = rib_r, h = skirt_h - 1, $fn = 24);
        }
        // opening in the middle
        translate([0, 0, top_z - 1]) cylinder(r = hole_r, h = lid_t + 2);
        // window
        rotate(win_c) translate([0, 0, top_z - 1]) linear_extrude(lid_t + 2) window2d();
        // tree rings engraved in the top face
        for (r = ring_radii)
            if (r > win_r0 - 1 && r < lid_r - 3)
                translate([0, 0, top_z + lid_t - ring_depth])
                    difference() {
                        cylinder(r = r + ring_w / 2, h = ring_depth + 1);
                        translate([0, 0, -1]) cylinder(r = r - ring_w / 2, h = ring_depth + 3);
                    }
        // finger scallops in the brim
        for (i = [0 : 23])
            rotate(i * 15) translate([lid_r + 0.9, 0, top_z - 1]) cylinder(r = 2.3, h = lid_t + 2, $fn = 32);
    }
}

module lid_print() {
    // plate face down on the bed, skirt up
    translate([0, 0, top_z + lid_t]) rotate([180, 0, 0]) lid_in_place();
}

// ---- output -----------------------------------------------------------------------------------
module ring_pos(z) { translate([0, 0, z]) children(); }

if (part == "base") base();
else if (part == "ring_b") ring_pos(asm_parts ? base_t : 0) ring_b();
else if (part == "ring_a") ring_pos(asm_parts ? base_t : 0) ring_a();
else if (part == "core") ring_pos(asm_parts ? base_t : 0) core();
else if (part == "lid") {
    if (asm_parts) rotate(60 * k_window) lid_in_place();
    else lid_print();
} else {            // asm: everything in place, for a quick look
    base();
    ring_pos(base_t) { core(); ring_a(); ring_b(); }
    rotate(60 * k_window) lid_in_place();
}

echo(str("outer diameter ", 2 * (lip_ri + lip_t), " mm (lid ", 2 * lid_r, "), height ", top_z + lid_t, " mm"));
echo(str("ring cavities: core r ", core_ro - wall, "; ring A ", ra_ri + wall, " to ", ra_ro - wall, "; ring B ", rb_ri + wall, " to ", rb_ro - wall_bo));
echo(str("tree rings on the lid at r = ", ring_radii));
