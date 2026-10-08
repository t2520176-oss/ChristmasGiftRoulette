/*
 * Scene props for the Blender showcase renders (NOT part of the printable set).
 *   openscad -D 'prop="lantern"' -o lantern.stl scene_props.scad
 */
include <../spooky_pals.scad>
part = "none";
prop = "lantern";   // [lantern, leaf, candy]

module lantern() {
    R = 30; H = 46;
    difference() {
        pumpkin(R, H, ribs = 9, stem = false, depth = 0.075);
        // hollow inside
        translate([0, 0, 4]) scale([0.86, 0.86, 0.84]) pumpkin(R, H, ribs = 9, stem = false, depth = 0.075);
        // carved face, cut right through the wall
        translate([0, 0, H * 0.42]) rotate([90, 0, 0]) linear_extrude(height = 80)
            union() {
                for (s = [-1, 1]) translate([s * 11.5, 7]) polygon([[-6, 0], [6, 0], [0, 10]]);
                translate([0, 0.5]) polygon([[-2.6, 0], [2.6, 0], [0, 4.6]]);
                translate([0, -5]) polygon([[-15, 4.5], [-9, -2], [-5, 2.2], [0, -3], [5, 2.2], [9, -2], [15, 4.5],
                                            [11, -8], [5, -11], [0, -12], [-5, -11], [-11, -8]]);
            }
    }
    // stem
    chain([[0, 0, H * 0.99 - 3, 5], [1, 0, H * 0.99 + 4, 4.2], [4, 0, H * 0.99 + 10, 3.4], [8, 0, H * 0.99 + 13.5, 2.8]]);
}
module candy() {                                  // candy corn
    hull() { cylinder(r = 5.5, h = 0.5); translate([0, 0, 9]) cylinder(r = 2.4, h = 0.5); }
}

if (prop == "lantern") lantern();
else if (prop == "leaf") translate([0, 0, 0]) linear_extrude(height = 0.8) leaf2d(2.2);
else if (prop == "candy") candy();
