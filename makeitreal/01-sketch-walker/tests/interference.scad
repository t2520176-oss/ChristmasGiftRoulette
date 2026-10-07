// Boolean interference test: every pair listed must have (almost) zero overlap volume.
// Run:  openscad -D 'PAIR=1' -D 'THETA=0' -o /dev/null interference.scad   (PAIR 1..N), see ../tests/run_interference.sh
include <../scad/walker.scad>;
PART = "none";
PAIR = 1;
module A() { children(PAIR); }
if (PAIR == 1)  intersection() { pinion_asm(); deck_asm(); }                       // pinion tip vs deck
if (PAIR == 2)  intersection() { pinion_asm(); plates_asm(); }
if (PAIR == 3)  intersection() { gears_asm(); deck_asm(); }
if (PAIR == 4)  intersection() { gears_asm(); plates_asm(); }
if (PAIR == 5)  intersection() { motor_asm(); deck_asm(); }                        // cradle pockets must clear the motor
if (PAIR == 6)  intersection() { motor_asm(); gears_asm(); }
if (PAIR == 7)  intersection() { motor_asm(); plates_asm(); }
if (PAIR == 8)  intersection() { motor_asm(); shafts_asm(); }
if (PAIR == 9)  intersection() { battery_asm(); gears_asm(); }
if (PAIR == 10) intersection() { battery_asm(); plates_asm(); }
if (PAIR == 11) intersection() { battery_asm(); deck_asm(); }                      // battery pocket must clear the holder
if (PAIR == 12) intersection() { legs_asm(); plates_asm(); }
if (PAIR == 13) intersection() { legs_asm(); deck_asm(); }
if (PAIR == 14) intersection() { legs_asm(); gears_asm(); }
if (PAIR == 15) intersection() { legs_asm(); shafts_asm(); }                       // must only touch at the crank bores (cranks have D-holes)
if (PAIR == 16) intersection() { legs_asm(); motor_asm(); }
if (PAIR == 17) intersection() { legs_asm(); battery_asm(); }
if (PAIR == 18) intersection() { gears_asm(); shafts_asm(); }                      // D-bores: tiny clearance only
if (PAIR == 19) intersection() { body_placed() body_part(); legs_asm(); }
if (PAIR == 20) intersection() { body_placed() body_part(); plates_asm(); }
