// Overlap test for the 4-colour skull keyring: each pair of colour parts must share (almost) zero volume.
include <keyring_skull_4color.scad>;
part = "none"; A = "gray"; B = "black";
module p(n) { if (n == "gray") gray_part(); else if (n == "black") black_part(); else if (n == "white") white_part(); else if (n == "blue") blue_part(); }
intersection() { p(A); p(B); }
