// =====================================================================
//  Summit Stamp Kit  |  "Go Hiking" contest entry
//  Trail checkpoint stamp with swappable peak heads + collectible badges
//  One handle, many mountains. Heads lock on a hex key and are held by
//  a pair of magnets. Everything prints flat, support-free.
//
//  part = "handle" | "head" | "badge" | "assembly" | "set" | "plate"
//  Hardware: 6 x 3 mm neodymium disc magnets, 1 in the handle + 1 per head
//  Recommended material: PETG (handle), TPU 95A or PETG (heads)
// =====================================================================

/* [Part] */
part = "set";               // [handle, head, badge, assembly, set, plate]
peak = "hallasan";          // [hallasan, jirisan, seoraksan, bukhansan, custom]

/* [Custom peak] */
custom_name = "MY PEAK";
custom_elev = "1234 m";
custom_shape = "alpine";    // [alpine, volcano, ridge, domes]

/* [Stamp] */
show_name = false;          // mountain name on the lower arc
show_elev = false;          // elevation figure ("1947 m") under the picture
head_d    = 37;             // stamp face diameter
head_t    = 4;              // head base thickness
relief_h  = 1.2;            // height of the raised (embossed) stamp artwork (양각 높이)
inlay_depth = 0.6;          // flush inlay depth for the handle logo and badges (표면과 평평하게 채워지는 깊이)
badge_t   = 3;

/* [Text legibility] */
// A 0.4 mm nozzle lays ~0.45 mm lines, so any stroke thinner than ~2 lines
// is dropped by the slicer and the lettering turns into broken threads
// (the 2.6 mm "SUMMIT" on the handle did exactly that). The logo text is
// therefore set larger and thickened by text_grow per side.
text_grow      = 0.2;       // extra stroke width per side (0 = font as designed)
logo_text_size = 3.8;       // "SUMMIT" on the handle's bed face

/* [Fit & Hardware] */
hex_af    = 14;             // hex key across flats
hex_h     = 2.3;
fit_clr   = 0.25;           // hex clearance
mag_d     = 6;              // magnet diameter
mag_h     = 3;              // magnet height
mag_clr   = 0.25;           // press-fit allowance

/* [Handle] */
handle_h  = 46;
knob_r    = 16.5;
waist_r   = 10.5;
lanyard_d = 3.5;

/* [Hidden] */
$fn = 96;
eps = 0.01;
font = "Liberation Sans:style=Bold";
R  = head_d/2;
art_col   = "#000000";      // 디자인 색상 검은색으로 변경
head_col  = "#2f6f4f";
badge_col = "#c9b27c";
handle_col = "#e07a2f";

// ------------------------------------------------------------------
//  Peak library: name, elevation, silhouette style
// ------------------------------------------------------------------
function peak_name(p) = p == "hallasan"  ? "HALLASAN"  :
                        p == "jirisan"   ? "JIRISAN"   :
                        p == "seoraksan" ? "SEORAKSAN" :
                        p == "bukhansan" ? "BUKHANSAN" : custom_name;
function peak_elev(p) = p == "hallasan"  ? "1947 m" :
                        p == "jirisan"   ? "1915 m" :
                        p == "seoraksan" ? "1708 m" :
                        p == "bukhansan" ? "836 m"  : custom_elev;
function peak_shape(p) = p == "hallasan"  ? "volcano" :
                         p == "jirisan"   ? "ridge"   :
                         p == "seoraksan" ? "alpine"  :
                         p == "bukhansan" ? "domes"   : custom_shape;

base_y = -3.6;

module silhouette(shape) {
    if (shape == "alpine") {
        polygon([[-14,base_y],[-14,-1],[-10,2],[-8,1],[-5,6.5],[-3,4.5],
                 [-1,10],[1.2,6.8],[3,8.5],[6,3.5],[8,5],[11,0.5],[14,-1],[14,base_y]]);
        // summit flag
        translate([-1.3,9.5]) square([0.7,3.2]);
        translate([-0.6,10.6]) polygon([[0,0],[2.6,1],[0,2.1]]);
        translate([-9,7]) circle(r = 2.2);                   // sun
    }
    if (shape == "volcano") {
        polygon([[-14,base_y],[-14,-2],[-7,3.5],[-3.5,7.5],[-1.6,6.6],
                 [1.6,6.6],[3.5,7.5],[7,3.5],[14,-2],[14,base_y]]);
        // drifting cloud band
        for (dx = [-6, 5]) translate([dx, 10.2]) hull() {
            circle(r = 1.1); translate([3.5,0]) circle(r = 1.1);
        }
        // parasitic cone
        polygon([[8,0.3],[10,1.6],[12,0],[12,-0.4]]);
    }
    if (shape == "ridge") {
        // three layered ridgelines, front one filled
        intersection() {
            union() for (k = [0:1]) {
                off = 5.2 - 2.9*k;
                difference() {
                    ridge_poly(off, 1.8, 40*k + 10);
                    translate([0,-0.9]) ridge_poly(off, 1.8, 40*k + 10);
                }
            }
            translate([-12.5,base_y]) square([25,20]);
        }
        ridge_poly(-0.8, 1.8, 95);
        translate([-8.5,10.2]) circle(r = 1.9);              // rising sun
    }
    if (shape == "domes") {
        intersection() {
            union() {
                translate([-3.5,base_y]) scale([5.2,12.5]) circle(r = 1);
                translate([4.8,base_y]) scale([4.6,9.5]) circle(r = 1);
                translate([-11,base_y]) scale([4,4.5]) circle(r = 1);
                translate([11.5,base_y]) scale([3.5,3.5]) circle(r = 1);
            }
            translate([-20,base_y]) square([40,20]);
        }
        // granite cracks are negative lines on the big dome
    }
    // ground line + pine trees
    translate([-15, base_y - 0.9]) square([30, 0.9]);
    for (t = [[-11.5,base_y],[-9.8,base_y],[10.6,base_y]])
        translate(t) polygon([[-1.1,0],[0,2.8],[1.1,0]]);
}

module ridge_poly(y0, a, ph) {
    polygon(concat(
        [[-15, base_y]],
        [for (x = [-15:0.5:15]) [x, y0 + a*sin(18*x + ph) + 0.5*a*sin(47*x + 2*ph)]],
        [[15, base_y]]));
}

module crack_lines(shape) {   // negative details, subtracted from art
    if (shape == "domes") {
        for (c = [[-5.5,0,62],[-2.2,2.5,78],[3.6,-0.8,70]])
            translate([c[0],c[1]]) rotate(c[2]) square([6,0.7], center = true);
    }
}

// Centred text with strokes thickened to a printable width (see text_grow).
// spacing opens the letter gaps back up by the amount the strokes grew.
module bold_text(t, size) {
    offset(r = text_grow, $fn = 24)
        text(t, size = size, halign = "center", valign = "center",
             spacing = 1 + 2*text_grow/size, font = font);
}

// Name set on the lower arc, letters upright toward the centre.
// Rough per-letter advance so narrow letters (I, J) don't leave gaps.
function cw(c) = c == "I" ? 0.45 : c == "J" ? 0.7 : (c == "M" || c == "W") ? 1.15 : 0.95;
function csum(t, i) = i <= 0 ? 0 : csum(t, i-1) + cw(t[i-1]);

module arc_text(t, rb, sz) {
    n = len(t);
    total = csum(t, n);
    for (i = [0:n-1]) {
        mid = (csum(t, i) + cw(t[i])/2 - total/2)*sz;    // arc length from centre
        a = 270 + mid/rb*180/PI;
        translate(rb*[cos(a), sin(a)]) rotate(a + 90)
            text(t[i], size = sz, halign = "center", valign = "baseline", font = font);
    }
}

// With no lettering the picture is moved down so it sits in the middle of
// the face instead of leaving the lower third empty.
function art_dy(shape) = shape == "domes" ? -2.4 : -3.8;

module stamp_art(p) {
    has_text = show_name || show_elev;
    dy = has_text ? 0 : art_dy(peak_shape(p));
    intersection() {
        circle(r = R - 2.4);
        union() {
            translate([0,dy]) difference() {
                silhouette(peak_shape(p));
                crack_lines(peak_shape(p));
            }
            if (show_elev)
                translate([0, show_name ? -7.4 : -9.6])
                    text(peak_elev(p), size = show_name ? 2.6 : 3.6, halign = "center",
                         valign = "center", font = font);
            if (show_name) arc_text(peak_name(p), 14.6, 3.0);
        }
    }
    // border ring with a "N" tick at the top
    difference() { circle(r = R - 0.6); circle(r = R - 1.6); }
    translate([0, R - 1.6]) polygon([[-1.2,0],[0,-1.8],[1.2,0]]);
}

// ------------------------------------------------------------------
//  Parts (modelled in print orientation)
// ------------------------------------------------------------------
module hex(af, h) { cylinder(h = h, r = af/2/cos(30), $fn = 6); }

// Head: back on the bed, relief facing up. The artwork stands proud of the
// face by relief_h (embossed, so it picks up ink and prints an impression)
// and is mirrored so the stamp prints the right way round.
module head(p = peak) {
    // Plain base disc: the artwork is NOT cut into it
    color(head_col) difference() {
        hull() {
            translate([0,0,0.5]) cylinder(h = head_t - 0.5, r = R);
            cylinder(h = eps, r = R - 0.5);
        }
        // hex socket + magnet pocket (open on the bed side)
        translate([0,0,-eps]) rotate(30) hex(hex_af + 2*fit_clr, hex_h + 0.2);
        translate([0,0,-eps]) cylinder(h = mag_h + 0.3, d = mag_d + mag_clr);
        // orientation notch at the top of the artwork
        translate([0, R, -1]) rotate(45) cube([1.6,1.6,head_t + 2], center = true);
    }

    // Raised artwork = the stamping surface (eps overlap fuses it to the base)
    color(art_col) translate([0,0,head_t - eps])
        linear_extrude(relief_h + eps) mirror([1,0]) stamp_art(p);
}

// Handle: printed upside down (flat top on the bed). Profile keeps every
// outward slope at 45 deg or less.
module handle() {
    flange_r = R - 0.5;
    z_waist0 = 26;  z_waist1 = 31;
    z_flare  = z_waist1 + (flange_r - waist_r);
    prof = [[0,0],[knob_r-1.5,0],[knob_r,1.5],[knob_r,11],
            [waist_r,z_waist0],[waist_r,z_waist1],[flange_r,z_flare],
            [flange_r,handle_h],[0,handle_h]];
            
    // Base handle with cutout for flush inlay
    color(handle_col) difference() {
        union() {
            rotate_extrude() polygon(prof);
            translate([0,0,handle_h - eps]) rotate(30) hex(hex_af, hex_h);
        }
        // magnet pocket in the hex key
        translate([0,0,handle_h + hex_h - mag_h - 0.3])
            cylinder(h = mag_h + 1, d = mag_d + mag_clr);
        // lanyard hole, teardrop so it bridges cleanly
        translate([0,0,6.5]) rotate([90,0,0])
            linear_extrude(2*knob_r + 2, center = true)
                union() { circle(d = lanyard_d, $fn = 32);
                          rotate(45) square(lanyard_d/2); }
        // orientation notch on the flange, matches the head notch
        translate([0, flange_r, handle_h - 4]) rotate([0,0,45]) cube([1.6,1.6,10], center = true);
        // finger grip flutes on the knob
        for (i = [0:11]) rotate(i*30)
            translate([knob_r + 0.9, 0, 3]) cylinder(h = 7, r = 1.5, $fn = 24);
            
        // Cut out the logo for AMS flush inlay
        translate([0,0,-eps]) linear_extrude(inlay_depth + eps) mirror([1,0]) top_logo();
    }
    
    // Black logo flush with the bed surface
    color(art_col) translate([0,0,0]) linear_extrude(inlay_depth) mirror([1,0]) top_logo();
}

// Logo on the knob's flat bed face (flat area is ~30 mm across).
// Mountain and lettering are sized to fill it so every stroke stays well
// above the nozzle width.
module top_logo() {
    translate([0,1.6]) scale(1.45) polygon([[-7,-2],[-3,3],[-1.5,1.5],[1.5,5],[7,-2]]);
    translate([0,-6.2]) bold_text("SUMMIT", logo_text_size);
}

// Collectible badge: the same artwork, readable, with a keyring loop.
module badge(p = peak) {
    // Base badge with cutout for flush inlay
    color(badge_col) difference() {
        union() {
            hull() {
                translate([0,0,0.4]) cylinder(h = badge_t - 0.4, r = R);
                cylinder(h = eps, r = R - 0.4);
            }
            hull() {
                translate([0, R + 1.5, 0]) cylinder(h = badge_t, r = 4.5);
                translate([0, R - 4, 0]) cylinder(h = badge_t, r = 4.5);
            }
        }
        translate([0, R + 1.8, -1]) cylinder(h = badge_t + 2, d = 4.2, $fn = 32);
        
        // Subtract art for flush inlay
        translate([0,0,badge_t - inlay_depth])
            linear_extrude(inlay_depth + eps) stamp_art(p);
    }
    
    // Black artwork flush with the top surface
    color(art_col) translate([0,0,badge_t - inlay_depth])
        linear_extrude(inlay_depth) stamp_art(p);
}

// ------------------------------------------------------------------
//  Layouts
// ------------------------------------------------------------------
peaks = ["hallasan", "jirisan", "seoraksan", "bukhansan"];
tot_handle = handle_h + hex_h;

if (part == "handle") handle();
if (part == "head")   head();
if (part == "badge")  badge();
if (part == "art")    stamp_art(peak);
if (part == "plate") {                // one handle + four heads
    handle();
    for (i = [0:3]) translate([(i%2 ? 1 : -1)*(R + 24), (i < 2 ? 1 : -1)*(R + 3), 0])
        head(peaks[i]);
}
if (part == "assembly") {             // handle on head, as used
    translate([0,0,head_t + tot_handle]) rotate([180,0,0]) handle();
    translate([0,0,head_t]) rotate([180,0,0]) head();
}
if (part == "set") {                  // handle + 4 heads + 4 badges
    translate([0,0,tot_handle + head_t]) rotate([180,0,0]) handle();
    translate([0,0,head_t]) rotate([180,0,0]) head(peaks[0]);
    for (i = [1:3]) translate([(i - 2)*(head_d + 6) + 6, -head_d - 12, 0])
        head(peaks[i]);
    for (i = [0:3]) translate([(i - 1.5)*(head_d + 6), head_d + 14, 0])
        badge(peaks[i]);
}