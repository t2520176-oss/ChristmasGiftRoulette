
/*
 Halloween Skull Name Keyring
 Skull with two bones + your name

 4 COLORS MAX
 1. Gray  : Border and key-ring loop
 2. Black : Plate and the outline around skull and bones
 3. White : Skull and bones
 4. Blue  : Name

 Units: mm
 OpenSCAD 2021.01 compatible

 Export:
 part = "all"      -> Full color preview
 part = "gray"     -> Gray STL
 part = "black"    -> Black STL
 part = "white"    -> White STL
 part = "blue"     -> Blue STL
 part = "onecolor" -> One-color STL (paint it afterwards)

 Bambu Studio:
 Load the 4 STL files together, answer "load as one object with
 multiple parts", then give each part its own filament.
*/

// ================================
// CUSTOMIZER
// ================================

// Personalization
name_text = "Noah";

// English: Liberation Sans
// Korean on Windows: Malgun Gothic
font_name = "Liberation Sans:style=Bold";

// Model selection
part = "all";

// Model size in x and y (1.0 = about 54 x 43 mm). Height is not scaled.
model_scale = 1.0;

// Layer thickness (all multiples of 0.2 mm)
base_h = 1.6;
black_h = 1.2;
detail_h = 0.8;

// Border thickness
border_width = 2.0;

// Black outline around skull and bones
halo_width = 1.5;

// Keyring hole (before model_scale)
hole_d = 4.8;

// Display
$fn = 48;

// ================================
// 4 COLOR PALETTE
// ================================

gray  = [0.80, 0.82, 0.85];
black = [0.065, 0.065, 0.075];
white = [0.98, 0.98, 0.97];
blue  = [0.10, 0.60, 0.92];

// ================================
// BASIC GEOMETRY
// ================================

eyelet = [48.6, 33.4];

module ellipse2d(x, y, rx, ry, a = 0) {
    translate([x, y])
        rotate(a)
            scale([rx, ry])
                circle(r = 1);
}

module rrect2d(x0, y0, x1, y1, r) {
    hull()
        for (x = [x0 + r, x1 - r], y = [y0 + r, y1 - r])
            translate([x, y])
                circle(r = r);
}

// ================================
// NAME
// ================================

// Width available for the name inside the plate (before model_scale)
name_fit_w = 44;

// Approximate letter widths (em) of Liberation Sans Bold. They are only used to
// shrink long names so they fit the plate. Characters that are not listed
// (for example Korean) count as 1.0 em.
adv_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";
adv_w = [
    .722,.722,.722,.722,.667,.611,.778,.722,.278,.556,.722,.611,.833,
    .722,.778,.667,.778,.722,.667,.611,.722,.667,.944,.667,.667,.611,
    .556,.611,.556,.611,.556,.333,.611,.611,.278,.278,.556,.278,.889,
    .611,.611,.611,.611,.389,.556,.333,.611,.556,.778,.556,.556,.500,
    .556,.556,.556,.556,.556,.556,.556,.556,.556,.556,.278
];

function char_adv(c) =
    len(search(c, adv_chars)) > 0 ? adv_w[search(c, adv_chars)[0]] : 1.0;

function name_em() =
    [for (i = [0 : len(name_text) - 1]) char_adv(name_text[i])]
    * [for (i = [0 : len(name_text) - 1]) 1];

// Cap height of the name: as large as possible but never wider than name_fit_w,
// and never taller than 12.4 (so short names do not touch the skull's jaw)
function name_size() =
    min(12.4, name_fit_w / (max(1.5, name_em()) * 1.3 * 1.13));

module name_shape() {
    translate([25, 3.4])
        offset(r = 0.5)
            offset(r = min(0.5, name_size() * 0.035))
                offset(delta = -min(0.5, name_size() * 0.035))
                    text(
                        name_text,
                        size = name_size(),
                        font = font_name,
                        halign = "center",
                        valign = "baseline",
                        spacing = 1.13
                    );
}

// ================================
// SKULL
// ================================

// Outline of the skull: wide round cranium tapering into a narrower jaw
module skull_shape() {
    union() {
        hull() {
            ellipse2d(25, 29.6, 11.6, 7.6);
            rrect2d(18.4, 21.8, 31.6, 24.6, 1.2);
        }
        rrect2d(18.2, 17.4, 31.8, 25.0, 2.4);
    }
}

module skull_eyes() {
    for (x = [20.3, 29.7])
        translate([x, 29.4])
            circle(r = 3.15);
}

module skull_nose() {
    translate([25, 24.9])
        offset(r = 0.35)
            offset(delta = -0.35)
                polygon([[-1.9, 1.5], [1.9, 1.5], [0, -1.8]]);
}

// Gaps between the teeth (cut from the bottom edge of the jaw)
module skull_teeth_gaps() {
    for (x = [21.6, 25, 28.4])
        hull() {
            translate([x, 21.4]) circle(d = 1.0);
            translate([x - 0.5, 16.5]) square([1.0, 1.0]);
        }
}

module skull_white() {
    difference() {
        skull_shape();
        skull_eyes();
        skull_nose();
        skull_teeth_gaps();
    }
}

// ================================
// BONES
// ================================

module bone2d(x, y, rot, len = 11.6, w = 2.7, kr = 2.1, ko = 1.45) {
    translate([x, y])
        rotate(rot)
            union() {
                rrect2d(-len / 2 + 1.6, -w / 2, len / 2 - 1.6, w / 2, 0.6);
                for (sx = [-1, 1], sy = [-1, 1])
                    translate([sx * (len / 2 - kr), sy * ko])
                        circle(r = kr);
            }
}

module bones_shape() {
    bone2d(6.4, 21.8, -20);
    bone2d(41.6, 22.2, 28);
}

// ================================
// MAIN SILHOUETTE
// ================================

module plate_shape() {
    rrect2d(0, 0, 50, 27, 8);
}

module keyhole2d() {
    translate(eyelet) circle(d = hole_d);
}

// Black plate and the outline around skull and bones
module black_shape2d() {
    difference() {
        union() {
            plate_shape();
            offset(delta = halo_width) skull_shape();
            offset(delta = halo_width) bones_shape();
        }
        keyhole2d();
    }
}

module loop_disc2d() {
    translate(eyelet) circle(r = 5.0);
}

// Gray border around everything, plus the key-ring loop
module border_shape2d() {
    difference() {
        union() {
            offset(r = border_width) black_shape2d();
            loop_disc2d();
        }
        keyhole2d();
    }
}

// The loop is thicker than the border: only the part that is not black
module loop_ring2d() {
    difference() {
        loop_disc2d();
        keyhole2d();
        offset(delta = 0.01) black_shape2d();
    }
}

// ================================
// 3D COLOR PARTS
// ================================

// 1. Gray border and loop
module gray_part() {

    linear_extrude(height = base_h)
        border_shape2d();

    // Thicker loop for strength
    translate([0, 0, base_h])
        linear_extrude(height = black_h)
            loop_ring2d();
}

// 2. Black plate and outline
module black_part() {

    translate([0, 0, base_h])
        linear_extrude(height = black_h)
            black_shape2d();
}

// 3. White skull and bones
module white_part() {

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            union() {
                skull_white();
                bones_shape();
            }
}

// 4. Blue name
module blue_part() {

    // Safety: whatever the white skull and bones cover is never blue
    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            difference() {
                name_shape();
                offset(delta = 0.3) { skull_shape(); bones_shape(); }
            }
}

// Everything merged into one clean solid (touching regions are merged in 2D first)
module onecolor_part() {

    union() {
        linear_extrude(height = base_h)
            border_shape2d();
        translate([0, 0, base_h])
            linear_extrude(height = black_h)
                union() { black_shape2d(); loop_ring2d(); }
        translate([0, 0, base_h + black_h])
            linear_extrude(height = detail_h)
                union() { skull_white(); bones_shape(); difference() { name_shape(); offset(delta = 0.3) { skull_shape(); bones_shape(); } } }
    }
}

// ================================
// FINAL ASSEMBLY
// ================================

module full_model() {

    if (part == "all" || part == "gray")
        color(gray)
            gray_part();

    if (part == "all" || part == "black")
        color(black)
            black_part();

    if (part == "all" || part == "white")
        color(white)
            white_part();

    if (part == "all" || part == "blue")
        color(blue)
            blue_part();

    if (part == "onecolor")
        onecolor_part();
}

// Only x and y are scaled, so the layer heights stay multiples of 0.2 mm
scale([model_scale, model_scale, 1])
    full_model();
