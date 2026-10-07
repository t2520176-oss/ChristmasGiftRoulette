// =====================================================================
//  Summit Lantern  |  "Go Hiking" contest entry
//  Turns any headlamp into a hanging tent lantern that projects a
//  360-degree mountain panorama and a starry sky onto the tent walls.
//
//  - Shade: lithophane-style wall. Thin "sky" glows, thick "mountains"
//    stay dark, pinhole stars (incl. the Big Dipper) and a crescent moon
//    throw sharp points of light.
//  - Roof lid: faceted summit roof, inner cone reflects the headlamp beam
//    sideways; screws on with a round-bead thread (2.4 turns, pitch 5).
//  - Print-in-place swivel hanging loop on the summit.
//  - Drop-in headlamp cradle (strap threads through two slots).
//  - Matching S-hook for tent loops, branches and guylines.
//
//  part = "shade" | "lid" | "cradle" | "hook" | "assembly" | "exploded" | "section"
//  Hardware: none (uses your own headlamp)
// =====================================================================

/* [Part] */
part = "assembly";   // [shade, lid, cradle, hook, assembly, exploded, section]

/* [Shade] */
shade_r   = 37;      // outer radius
shade_h   = 90;      // height of the panorama wall (thread neck adds 10)
t_sky     = 1.0;     // thin wall = glowing sky (keep >= 2 perimeters)
t_far     = 1.8;     // medium wall = distant ranges (soft glow)
t_mtn     = 2.6;     // thick wall = dark foreground forest & tent
star_seed = 42;
star_count = 46;

/* [Headlamp cradle] */
lamp_w    = 46;      // headlamp body width the strap slots straddle
strap_w   = 32;      // strap width (most headlamps 20-30 mm)

/* [Hook] */
branch_d  = 22;      // biggest branch / pole the S-hook takes
hook_t    = 7;

/* [Thread] */
// Round-bead thread taken from the box file (solid_thread): a chain of
// hulled spheres swept along a helix.
pitch        = 5;     // rise per turn
thread_len   = 12;    // height of the threaded neck (2.4 turns)
thread_r     = 1.2;   // bead radius (ridge is 2*thread_r wide)
groove_extra = 0.3;   // lid groove bead is this much fatter than the ridge
clr          = 0.4;   // radial clearance neck <-> lid
neck_wall    = 1.6;   // wall of the neck tube the thread sits on

/* [Hidden] */
$fn = 120;
eps = 0.01;
r_sky_in = shade_r - t_sky;
r_mtn_in = shade_r - t_mtn;
r_far_in = shade_r - t_far;

// ---------------------------------------------------------------------
//  Panorama: ridge height (mm) as a function of angle (deg)
// ---------------------------------------------------------------------
function adiff(a, b) = abs(((a - b + 540) % 360) - 180);
function peak(a, a0, h, w) = max(0, h*(1 - adiff(a, a0)/w));
function tree(a, a0, h) = max(0, h*(1 - adiff(a, a0)/3.6));

// big ranges: rolling base + sharp named-style summits
function range_h(a) =
    34 + 7*sin(2*a + 20) + 5*sin(5*a + 60) + 2.5*sin(13*a + 10) + 1.2*sin(29*a)
    + peak(a,  40, 22, 18) + peak(a,  52, 14, 10)              // jagged massif
    + peak(a, 130, 18, 30)                                       // broad volcano
    + peak(a, 205, 16, 12) + peak(a, 215, 20, 9) + peak(a, 224, 12, 8)  // spires
    + peak(a, 300, 15, 22) + peak(a, 318, 10, 9);                // twin domes

// foreground: pine forest edge and a tent
function fore_h(a) =
    14 + 3*sin(3*a + 40) + 1.5*sin(9*a)
    + max([for (t = [0:7.5:359]) if (adiff(t, 328) > 16 && adiff(t, 40) > 25)
           tree(a, t + 2*sin(t*13), 7 + 4*abs(sin(t*7)))])
    + ((adiff(a, 328) < 9) ? 13*(1 - adiff(a, 328)/9) : 0);    // tent

function ridge_z(a) = min(shade_h - 22, max(range_h(a), fore_h(a)));
function far_z(a)   = min(shade_h - 22, range_h(a));
function near_z(a)  = min(shade_h - 22, fore_h(a));

// thick-wall band whose top edge follows the ridge line
module band(rin, near, n = 720) {
    pts = [for (i = [0:n-1]) let(a = i*360/n, z = near ? near_z(a) : far_z(a))
              each [[rin*cos(a), rin*sin(a), 0],
                    [(r_sky_in+0.2)*cos(a), (r_sky_in+0.2)*sin(a), 0],
                    [(r_sky_in+0.2)*cos(a), (r_sky_in+0.2)*sin(a), z],
                    [rin*cos(a), rin*sin(a), z]]];
    faces = concat(
        [for (i = [0:n-1]) let(j = (i+1)%n) each [
            [4*i+0, 4*i+1, 4*j+1, 4*j+0],     // bottom
            [4*i+3, 4*j+3, 4*j+2, 4*i+2],     // top
            [4*i+1, 4*i+2, 4*j+2, 4*j+1],     // outer
            [4*i+0, 4*j+0, 4*j+3, 4*i+3]]]);  // inner
    polyhedron(pts, faces, convexity = 6);
}

// ---------------------------------------------------------------------
//  Stars: Big Dipper + random field, all in the sky region
// ---------------------------------------------------------------------
dipper = [[0,0],[5.6,2.6],[10.4,3.4],[15.2,2.6],[16.2,-3.6],[23.4,-4.6],[24.6,2.2]];
dipper_a = 300;  dipper_z = 72;   // where it sits (deg, mm)
mm2deg = 180/(PI*shade_r);

rnd = rands(0, 1, star_count*3, star_seed);
stars = [for (k = [0:star_count-1])
            let(a = rnd[3*k]*360,
                zlo = ridge_z(a) + 5, zhi = shade_h - 6,
                z = zlo + rnd[3*k+1]*(zhi - zlo),
                d = 0.7 + 0.6*rnd[3*k+2])
            if (zhi - zlo > 4 && !(adiff(a, dipper_a + 12*mm2deg) < 22 && abs(z - dipper_z) < 9)
                              && !(adiff(a, 20) < 10 && abs(z - 74) < 9))
            [a, z, d]];

module pinhole(a, z, d) {
    rotate(a) translate([shade_r - t_mtn - 1, 0, z]) rotate([0,90,0])
        cylinder(h = t_mtn + 2, d = d, $fn = 12);
}

module sky_cutouts() {
    for (s = stars) pinhole(s[0], s[1], s[2]);
    for (p = dipper) pinhole(dipper_a + p[0]*mm2deg, dipper_z + p[1], 1.5);
    // crescent moon
    rotate(20) translate([shade_r - 3, 0, 74]) rotate([0,90,0])
        linear_extrude(6) difference() {
            circle(d = 9, $fn = 48);
            translate([1.8, 1.4]) circle(d = 8.4, $fn = 48);
        }
}

// ---------------------------------------------------------------------
//  Thread (round bead, single start, right-handed)
// ---------------------------------------------------------------------
r_neck = r_mtn_in + neck_wall;               // neck tube; bead centres ride on it
r_cav  = r_neck + clr;                       // lid socket; groove bead centres ride on it
r_ff   = r_cav + thread_r + groove_extra;    // outer radius of the lid groove
lid_r  = r_ff + 2.2;

// Chain of hulled spheres along a helix (same idea as the box file).
// The helix angle is tied to z (angle = 360*z/p), so neck and lid grooves
// built from the same pitch are phase-locked and mate with the lid at
// rotation 0; z0 only says where the sweep starts.
module solid_thread(r, h, p, thickness, z0 = 0, step_deg = 4) {
    turns   = h / p;
    steps   = ceil(turns * 360 / step_deg);
    dz      = h / steps;
    d_angle = (turns * 360) / steps;
    a0      = 360 * z0 / p;
    for (i = [0 : steps - 1]) {
        hull() {
            translate([0, 0, z0 + i * dz])
                rotate([0, 0, a0 + i * d_angle])
                    translate([r, 0, 0]) sphere(r = thickness, $fn = 16);
            translate([0, 0, z0 + (i + 1) * dz])
                rotate([0, 0, a0 + (i + 1) * d_angle])
                    translate([r, 0, 0]) sphere(r = thickness, $fn = 16);
        }
    }
}

// Male thread: z = 0 is the rim of the shade. Neck tube + bead, with the
// last 1.2 mm of the bead tapered away so the lid starts easily.
module neck() {
    translate([0, 0, -eps]) cylinder(h = thread_len + eps, r = r_neck);
    intersection() {
        solid_thread(r_neck, thread_len + 2*thread_r, pitch, thread_r, z0 = -thread_r);
        translate([0, 0, -eps]) union() {
            cylinder(h = thread_len - 1.2 + eps, r = shade_r + 2);
            translate([0, 0, thread_len - 1.2 + eps])
                cylinder(h = 1.2, r1 = r_neck + thread_r + eps, r2 = r_neck);
        }
    }
}

// Female thread: z = 0 is the lid's bottom face. Plain socket plus a groove
// along the same helix, fatter than the bead by groove_extra.
module lid_socket() {
    translate([0, 0, -eps]) cylinder(h = sock_h + eps, r = r_cav);
    g_r = thread_r + groove_extra;
    solid_thread(r_cav, thread_len + thread_r + g_r, pitch, g_r, z0 = -g_r);
}

// ---------------------------------------------------------------------
//  Shade
// ---------------------------------------------------------------------
module shade() {
    difference() {
        union() {
            difference() {
                cylinder(h = shade_h, r = shade_r);
                translate([0,0,-1]) cylinder(h = shade_h + 2, r = r_sky_in);
            }
            band(r_far_in, false);
            band(r_mtn_in, true);
            // solid collar under the neck
            translate([0,0,shade_h - 6]) difference() {
                cylinder(h = 6, r = shade_r);
                translate([0,0,-1]) cylinder(h = 8, r1 = r_sky_in + 1, r2 = r_mtn_in);
            }
            // cradle ledge (45 deg underside, no support)
            rotate_extrude() polygon([[r_mtn_in + 0.1, 3], [r_mtn_in - 3.4, 6.4],
                                      [r_mtn_in - 3.4, 8], [r_mtn_in + 0.1, 8]]);
            // threaded neck
            translate([0,0,shade_h]) neck();
        }
        translate([0,0,shade_h - 10]) cylinder(h = 30, r = r_mtn_in);
        sky_cutouts();
        // anti elephant-foot chamfer
        translate([0,0,-eps]) difference() {
            cylinder(h = 0.8, r = shade_r + 1);
            cylinder(h = 0.8, r1 = shade_r - 0.6, r2 = shade_r);
        }
    }
}

// ---------------------------------------------------------------------
//  Lid: summit roof + print-in-place swivel loop
// ---------------------------------------------------------------------
sock_h  = thread_len + 0.6;
z_eave  = sock_h + 3;
roof_fn = 9;
Zt      = z_eave + lid_r;                    // apex (ridges at 45 deg, facets steeper)
g       = 0.45;                              // print-in-place clearance
gv      = g*sqrt(2);
ri      = 3 + g;     ro = 8.5;
z_ct    = Zt + 8 + (3 + g + 2.5 - 3) - gv;   // collar top

module lid_body() {
    difference() {
        union() {
            cylinder(h = z_eave, r = lid_r);
            // faceted roof: flats rise at 45 deg
            translate([0,0,z_eave - eps])
                cylinder(h = lid_r, r1 = lid_r, r2 = 0, $fn = roof_fn);
        }
        // female thread
        lid_socket();
        translate([0,0,-eps]) cylinder(h = 1.0, r1 = r_ff + 0.6, r2 = r_ff - 0.4);
        // reflector cone (smooth, 45 deg)
        translate([0,0,sock_h - eps]) cylinder(h = r_ff, r1 = r_ff, r2 = 0);
        // snow line groove, nice spot for a filament colour change
        translate([0,0,Zt - 15]) difference() {
            cylinder(h = 0.8, r = 40);
            translate([0,0,-1]) cylinder(h = 3, r = 15*cos(180/roof_fn) - 0.6, $fn = roof_fn);
            // (groove depth follows the facets)
        }
    }
    // swivel post with 45-degree retaining cap
    rotate_extrude() polygon([[0, Zt - 6], [3, Zt - 6], [3, Zt + 8],
                              [5.5, Zt + 10.5], [5.5, Zt + 12], [0, Zt + 12]]);
}

module swivel_loop() {
    // collar: rides on the roof cone and under the post cap
    rotate_extrude() polygon([[ri, Zt - ri + gv], [ro, Zt - ro + gv],
                              [ro, z_ct], [5.5 + g, z_ct], [ri, Zt + 8 + g - gv]]);
    // peak-shaped hanging loop (45 deg inner roof, no support)
    rotate([90,0,0]) linear_extrude(5, center = true) difference() {
        polygon([[-10, z_ct - 0.5], [10, z_ct - 0.5], [10, z_ct + 9],
                 [0, z_ct + 19], [-10, z_ct + 9]]);
        polygon([[-6, z_ct - 1], [6, z_ct - 1], [6, z_ct + 7],
                 [0, z_ct + 13], [-6, z_ct + 7]]);
    }
}

module lid() { lid_body(); swivel_loop(); }

// ---------------------------------------------------------------------
//  Headlamp cradle (drops in, rests on the ledge)
// ---------------------------------------------------------------------
module cradle() {
    cr = r_mtn_in - 1.2;
    difference() {
        cylinder(h = 2.4, r = cr);
        for (s = [-1, 1]) translate([s*(lamp_w/2 + 2.5), 0, -1])
            linear_extrude(5) offset(r = 1) square([2.5, strap_w], center = true);
        // light ports: a soft glow pool below the lantern
        for (i = [0:7]) rotate(i*45 + 22.5) translate([cr - 6, 0, -1])
            cylinder(h = 5, d = 6, $fn = 32);
        // finger notches for lifting it out
        for (s = [-1, 1]) translate([0, s*cr, -1]) cylinder(h = 5, d = 12, $fn = 32);
    }
}

// ---------------------------------------------------------------------
//  S-hook (flat print)
// ---------------------------------------------------------------------
module arc_band(r_in, w, a0, a1) {
    intersection() {
        difference() { circle(r = r_in + w); circle(r = r_in); }
        polygon(concat([[0,0]], [for (a = [a0:5:a1]) 3*(r_in + w)*[cos(a), sin(a)]],
                       [3*(r_in + w)*[cos(a1), sin(a1)]]));
    }
    for (a = [a0, a1]) translate((r_in + w/2)*[cos(a), sin(a)]) circle(d = w);
}

module hook() {
    w = 5;
    r1 = branch_d/2;  r2 = 7;
    U = r1 + r2 + 2*w + 8;
    linear_extrude(hook_t) {
        translate([0, U]) arc_band(r1, w, -60, 180);        // top: branch / tent loop
        arc_band(r2, w, 170, 360);                            // bottom: lantern loop
        hull() {                                              // spine
            translate([r2 + w/2, 0]) circle(d = w);
            translate([-(r1 + w/2), U]) circle(d = w);
        }
    }
}

// ---------------------------------------------------------------------
//  Layouts
// ---------------------------------------------------------------------
shade_col = "#f3efe4"; lid_col = "#e07a2f"; loop_col = "#2f6f4f";

if (part == "shade")  shade();
if (part == "lid")    lid();
if (part == "cradle") cradle();
if (part == "hook")   hook();
if (part == "assembly") {
    color(shade_col) shade();
    color("#666") translate([0,0,8.1]) cradle();
    translate([0,0,shade_h]) { color(lid_col) lid_body(); color(loop_col) swivel_loop(); }
}
if (part == "exploded") {
    color(shade_col) shade();
    color("#666") translate([0,0,50]) cradle();
    translate([0,0,shade_h + 40]) { color(lid_col) lid_body(); color(loop_col) swivel_loop(); }
    color(loop_col) translate([70, 0, 0]) rotate([90,0,0]) hook();
}
if (part == "section") {
    difference() {
        union() {
            color(shade_col) shade();
            color("#666") translate([0,0,8.1]) cradle();
            translate([0,0,shade_h]) { color(lid_col) lid_body(); color(loop_col) swivel_loop(); }
        }
        translate([-100,-200,-1]) cube([200,200,400]);
    }
}
