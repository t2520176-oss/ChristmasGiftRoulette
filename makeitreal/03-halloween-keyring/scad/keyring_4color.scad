
/*
 Halloween Ghost Name Keyring
 Ghost in a witch hat + your name

 4 COLORS MAX
 1. White : Border, ghost and key-ring loop
 2. Black : Plate, witch hat, eyes and mouth
 3. Green : Name, star and hat buckle
 4. Pink  : Cheeks, tongue and hat band

 Units: mm
 OpenSCAD 2021.01 compatible

 Export:
 part = "all"   -> Full color preview
 part = "white" -> White STL
 part = "black" -> Black STL
 part = "green" -> Green STL
 part = "pink"  -> Pink STL
 part = "onecolor" -> One-color STL (paint it afterwards)

 Bambu Studio:
 Load the 4 STL files together, answer "load as one object with
 multiple parts", then give each part its own filament.
*/

// ================================
// CUSTOMIZER
// ================================

// Personalization
name_text = "Liam";

// English: Liberation Sans
// Korean on Windows: Malgun Gothic
font_name = "Liberation Sans:style=Bold";

// Model selection
part = "all";

// Model size in x and y (1.0 = about 57 x 60 mm, 0.9 = about 51 x 54 mm). Height is not scaled.
model_scale = 0.9;

// Layer thickness (all multiples of 0.2 mm)
base_h = 1.6;
black_h = 1.2;
detail_h = 0.8;

// Border thickness
border_width = 2.0;

// Keyring hole (before model_scale)
hole_d = 5.3;

// Display
$fn = 48;

// ================================
// 4 COLOR PALETTE
// ================================

white = [1.00, 1.00, 1.00];
black = [0.065, 0.065, 0.075];
green = [0.52, 0.78, 0.20];
pink  = [1.00, 0.60, 0.65];

// ================================
// BASIC GEOMETRY
// ================================

eyelet = [49.6, 35.4];

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

// Cap height of the name: as large as possible (max 15) but never wider than name_fit_w
function name_size() =
    min(15, name_fit_w / (max(1.5, name_em()) * 1.3 * 1.13));

module name_shape() {
    translate([25, 4.2])
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
// STAR
// ================================

module star_shape() {
    translate([44.6, 22.4])
        offset(r = 0.55)
            offset(delta = -0.55)
                polygon([
                    for (i = [0 : 9])
                        let(
                            a = 90 + i * 36,
                            r = (i % 2 == 0) ? 4.6 : 2.05
                        )
                        [r * cos(a), r * sin(a)]
                ]);
}

// ================================
// WITCH HAT
// ================================

module hat_brim() {
    ellipse2d(21.5, 33.2, 14.2, 3.1, 8);
}

// Cone that bends to the right
module hat_cone() {
    hull() {
        ellipse2d(21.8, 34.2, 9.6, 1.8, 8);
        translate([25.2, 43]) circle(r = 4.6);
    }
    hull() {
        translate([25.2, 43]) circle(r = 4.6);
        translate([29.6, 50]) circle(r = 2.7);
    }
    hull() {
        translate([29.4, 50]) circle(r = 2.6);
        translate([35, 54.4]) circle(r = 1.4);
    }
}

module hat_shape() {
    union() {
        hat_brim();
        hat_cone();
    }
}

module hat_band_shape() {
    intersection() {
        hat_cone();
        translate([22.6, 38.6])
            rotate(7)
                translate([-20, -2.1])
                    square([40, 4.2]);
    }
}

module hat_buckle_shape() {
    translate([22.6, 38.6])
        rotate(7)
            difference() {
                rrect2d(-3.5, -2.7, 3.5, 2.7, 0.9);
                rrect2d(-1.8, -1.2, 1.8, 1.2, 0.4);
            }
}

// ================================
// GHOST
// ================================

module ghost_shape() {
    union() {
        translate([22, 24]) circle(r = 12.5);
        translate([9.5, 9.6]) square([25, 14.4]);

        // Scalloped hem
        for (x = [13.7, 22, 30.3])
            translate([x, 9.8])
                circle(r = 4.15);

        // Low left arm and raised right arm
        translate([8.8, 21.0]) circle(r = 3.9);
        ellipse2d(36.0, 22.4, 4.8, 3.1, 38);
    }
}

module ghost_eyes() {
    for (x = [18.4, 25.6])
        ellipse2d(x, 25.8, 1.25, 1.9);
}

module ghost_cheeks() {
    for (x = [15.0, 29.0])
        ellipse2d(x, 21.5, 2.2, 1.35);
}

module ghost_mouth() {
    translate([22, 22.7])
        intersection() {
            circle(r = 2.7);
            translate([-3, -3]) square([6, 3]);
        }
}

module ghost_tongue() {
    intersection() {
        ghost_mouth();
        ellipse2d(22, 20.5, 1.7, 1.2);
    }
}

// ================================
// MAIN SILHOUETTE
// ================================

module plate_shape() {
    rrect2d(0, 0, 50, 32, 7);
}

// Black plate, hat, and a margin around ghost and star
module black_shape2d() {
    union() {
        plate_shape();
        hat_shape();
        offset(delta = 1.0) ghost_shape();
        offset(delta = 0.8) star_shape();
    }
}

// White border and key-ring loop
module border_shape2d() {
    union() {
        offset(r = border_width) black_shape2d();
        translate(eyelet) circle(r = 5.3);
    }
}

module loop_ring2d() {
    translate(eyelet) circle(r = 5.3);
}

// ================================
// TOP LAYER REGIONS
// Each region is already cut by whatever lies in front of it,
// so no two colors ever overlap.
// ================================

module white_top2d() {
    difference() {
        ghost_shape();
        hat_shape();
        name_shape();
        star_shape();
        ghost_eyes();
        ghost_cheeks();
        ghost_mouth();
    }
}

module black_top2d() {
    union() {
        difference() {
            hat_shape();
            hat_band_shape();
            hat_buckle_shape();
        }
        ghost_eyes();
        difference() {
            ghost_mouth();
            ghost_tongue();
        }
    }
}

module green_top2d() {
    union() {
        name_shape();
        star_shape();
        hat_buckle_shape();
    }
}

module pink_top2d() {
    union() {
        ghost_cheeks();
        ghost_tongue();
        difference() {
            hat_band_shape();
            hat_buckle_shape();
        }
    }
}

// ================================
// 3D COLOR PARTS
// ================================

// 1. White border, loop and ghost
module white_part() {

    // Border with key-ring hole
    difference() {
        union() {
            linear_extrude(height = base_h)
                border_shape2d();

            // Thicker loop for strength
            translate([0, 0, base_h])
                linear_extrude(height = black_h)
                    loop_ring2d();
        }

        translate([eyelet[0], eyelet[1], -1])
            cylinder(d = hole_d, h = 10);
    }

    // Ghost
    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            white_top2d();
}

// 2. Black plate, hat, eyes and mouth
module black_part() {

    translate([0, 0, base_h])
        linear_extrude(height = black_h)
            black_shape2d();

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            black_top2d();
}

// 3. Green name, star and buckle
module green_part() {

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            green_top2d();
}

// 4. Pink cheeks, tongue and hat band
module pink_part() {

    translate([0, 0, base_h + black_h])
        linear_extrude(height = detail_h)
            pink_top2d();
}

// Everything merged into one clean solid (touching regions are merged in 2D first)
module onecolor_part() {

    difference() {
        union() {
            linear_extrude(height = base_h)
                border_shape2d();
            translate([0, 0, base_h])
                linear_extrude(height = black_h)
                    union() { black_shape2d(); loop_ring2d(); }
            translate([0, 0, base_h + black_h])
                linear_extrude(height = detail_h)
                    union() {
                        white_top2d();
                        black_top2d();
                        green_top2d();
                        pink_top2d();
                    }
        }

        translate([eyelet[0], eyelet[1], -1])
            cylinder(d = hole_d, h = 10);
    }
}

// ================================
// FINAL ASSEMBLY
// ================================

module full_model() {

    if (part == "all" || part == "white")
        color(white)
            white_part();

    if (part == "all" || part == "black")
        color(black)
            black_part();

    if (part == "all" || part == "green")
        color(green)
            green_part();

    if (part == "all" || part == "pink")
        color(pink)
            pink_part();

    if (part == "onecolor")
        onecolor_part();
}

// Only x and y are scaled, so the layer heights stay multiples of 0.2 mm
scale([model_scale, model_scale, 1])
    full_model();
