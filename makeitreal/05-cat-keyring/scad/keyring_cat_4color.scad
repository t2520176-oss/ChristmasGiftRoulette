
/*
 Halloween Black Cat Name Keyring
 Black cat peeking over your name, with a paw print

 4 COLORS MAX (the 5th color, white whiskers, is optional)
 1. Lavender : Border, loop, name, paw print (and whiskers)
 2. Black    : Plate, cat head, ears, paw and pupils
 3. Yellow   : Eyes (and eye shine)
 4. Pink     : Inner ears, nose, smile and cheeks
 5. White    : Whiskers and eye shine  (only if use_white = true)

 Units: mm
 OpenSCAD 2021.01 compatible

 Export:
 part = "all"      -> Full color preview
 part = "lavender" -> Lavender STL
 part = "black"    -> Black STL
 part = "yellow"   -> Yellow STL
 part = "pink"     -> Pink STL
 part = "white"    -> White STL (needs use_white = true)
 part = "onecolor" -> One-color STL (paint it afterwards)

 Bambu Studio:
 Load the STL files together, answer "load as one object with
 multiple parts", then give each part its own filament.
*/

// ================================
// CUSTOMIZER
// ================================

// Personalization
name_text = "Oliver";

// English: Liberation Sans
// Korean on Windows: Malgun Gothic
font_name = "Liberation Sans:style=Bold";

// Model selection
part = "all";

// false = 4 colors (whiskers and eye shine use lavender / yellow)
// true  = 5 colors (white whiskers and eye shine)
use_white = false;

// Model size in x and y (1.0 = about 56 x 49 mm). Height is not scaled.
model_scale = 1.0;

// Layer thickness (all multiples of 0.2 mm)
base_h = 1.6;
black_h = 1.2;
detail_h = 0.8;

// Border thickness
border_width = 2.0;

// Keyring hole (before model_scale)
hole_d = 4.8;

// Display
$fn = 48;

// ================================
// COLOR PALETTE
// ================================

lavender = [0.70, 0.60, 0.95];
black    = [0.065, 0.065, 0.075];
yellow   = [1.00, 0.78, 0.10];
pink     = [1.00, 0.60, 0.70];
white    = [1.00, 1.00, 1.00];

// ================================
// BASIC GEOMETRY
// ================================

eyelet = [21.0, 42.6];

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

module segment2d(a, b, w) {
    hull() {
        translate(a) circle(d = w);
        translate(b) circle(d = w);
    }
}

// ================================
// NAME
// ================================

// Width available for the name inside the plate (before model_scale)
name_fit_w = 46;

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
// and never taller than 12.4 (so short names do not touch the cat)
function name_size() =
    min(12.4, name_fit_w / (max(1.5, name_em()) * 1.3 * 1.13));

module name_shape() {
    translate([26, 3.2])
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
// CAT HEAD AND EARS
// ================================

module head_shape() {
    ellipse2d(21, 28, 12.5, 8.8);
}

module ear2d(mx) {
    // mx = 0 for the left ear, 1 for the right ear (mirrored around x = 21)
    translate([21, 0])
        mirror([mx, 0])
            translate([-21, 0])
                offset(r = 1.0)
                    offset(delta = -1.0)
                        polygon([[9.4, 30.0], [10.0, 43.2], [19.2, 35.4]]);
}

module ears_shape() {
    ear2d(0);
    ear2d(1);
}

module inner_ears_shape() {
    offset(delta = -1.7)
        ears_shape();
}

module cat_head_shape() {
    union() {
        head_shape();
        ears_shape();
    }
}

// ================================
// FACE
// ================================

module eye_disc(x) {
    ellipse2d(x, 27.5, 2.7, 3.0);
}

module eye_pupil(x) {
    ellipse2d(x + 0.2, 27.5, 1.0, 2.0);
}

module eye_shine(x) {
    translate([x + 0.45, 28.4]) circle(r = 0.5);
}

module eyes_yellow_shape() {
    for (x = [15.4, 26.6])
        difference() {
            eye_disc(x);
            eye_pupil(x);
        }
}

module eye_shines_shape() {
    for (x = [15.4, 26.6])
        eye_shine(x);
}

module nose_shape() {
    translate([21, 24.5])
        offset(r = 0.3)
            offset(delta = -0.3)
                polygon([[-1.35, 0.95], [1.35, 0.95], [0, -0.8]]);
}

module smile_shape() {
    for (x = [20.0, 22.0])
        translate([x, 23.0])
            intersection() {
                difference() {
                    circle(r = 1.5);
                    circle(r = 0.6);
                }
                translate([-2, -2]) square([4, 2]);
            }
}

module cheeks_shape() {
    for (x = [14.0, 28.0])
        ellipse2d(x, 22.3, 1.4, 0.85);
}

module whiskers_shape() {
    // left side
    segment2d([10.8, 25.4], [5.2, 25.9], 1.0);
    segment2d([10.8, 24.0], [5.2, 22.4], 1.0);
    // right side (shorter, the paw is there)
    segment2d([31.2, 25.4], [35.8, 25.8], 1.0);
    segment2d([31.2, 24.0], [35.8, 22.6], 1.0);
}

// ================================
// PAW WITH PAW PRINT
// ================================

module paw_shape() {
    ellipse2d(43.0, 25.0, 6.6, 5.7);
}

module paw_print_shape() {
    // main pad
    ellipse2d(43.0, 22.9, 2.7, 2.1);
    // four toes
    for (p = [[39.7, 25.6], [41.5, 27.9], [44.5, 27.9], [46.3, 25.6]])
        translate(p) circle(r = 1.15);
}

// ================================
// MAIN SILHOUETTE
// ================================

module plate_shape() {
    rrect2d(0, 0, 52, 26, 8);
}

module keyhole2d() {
    translate(eyelet) circle(d = hole_d);
}

// Black plate, cat, paw, and a margin around the whiskers
module black_shape2d() {
    difference() {
        union() {
            plate_shape();
            cat_head_shape();
            paw_shape();
            offset(delta = 0.8) whiskers_shape();
        }
        keyhole2d();
    }
}

module loop_disc2d() {
    translate(eyelet) circle(r = 5.0);
}

// Lavender border around everything, plus the key-ring loop
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
// TOP LAYER REGIONS
// Each region is cut by whatever lies in front of it,
// so no two colors ever overlap.
// ================================

module lavender_top2d() {
    union() {
        name_shape();
        difference() {
            paw_print_shape();
            offset(delta = 0.2) whiskers_shape();
        }
        if (!use_white)
            whiskers_shape();
    }
}

module black_top2d() {
    difference() {
        union() {
            cat_head_shape();
            paw_shape();
        }
        eyes_yellow_shape();
        eye_shines_shape();
        inner_ears_shape();
        nose_shape();
        smile_shape();
        cheeks_shape();
        whiskers_shape();
        paw_print_shape();
        name_shape();
    }
}

module yellow_top2d() {
    union() {
        eyes_yellow_shape();
        if (!use_white)
            eye_shines_shape();
    }
}

module pink_top2d() {
    union() {
        inner_ears_shape();
        nose_shape();
        smile_shape();
        cheeks_shape();
    }
}

module white_top2d() {
    union() {
        whiskers_shape();
        eye_shines_shape();
    }
}

// ================================
// 3D COLOR PARTS
// ================================

// 1. Lavender border, loop, name and paw print
module lavender_part() {

    linear_extrude(height = base_h)
        border_shape2d();

    // Thicker loop for strength
    translate([0, 0, base_h])
        linear_extrude(height = black_h)
            loop_ring2d();

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            lavender_top2d();
}

// 2. Black plate, cat, paw and pupils
module black_part() {

    translate([0, 0, base_h])
        linear_extrude(height = black_h)
            black_shape2d();

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            black_top2d();
}

// 3. Yellow eyes
module yellow_part() {

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            yellow_top2d();
}

// 4. Pink ears, nose, smile and cheeks
module pink_part() {

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            pink_top2d();
}

// 5. White whiskers and eye shine (optional)
module white_part() {

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            white_top2d();
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
                union() {
                    lavender_top2d();
                    black_top2d();
                    yellow_top2d();
                    pink_top2d();
                    if (use_white) white_top2d();
                }
    }
}

// ================================
// FINAL ASSEMBLY
// ================================

module full_model() {

    if (part == "all" || part == "lavender")
        color(lavender)
            lavender_part();

    if (part == "all" || part == "black")
        color(black)
            black_part();

    if (part == "all" || part == "yellow")
        color(yellow)
            yellow_part();

    if (part == "all" || part == "pink")
        color(pink)
            pink_part();

    if (use_white && (part == "all" || part == "white"))
        color(white)
            white_part();

    if (part == "onecolor")
        onecolor_part();
}

// Only x and y are scaled, so the layer heights stay multiples of 0.2 mm
scale([model_scale, model_scale, 1])
    full_model();
