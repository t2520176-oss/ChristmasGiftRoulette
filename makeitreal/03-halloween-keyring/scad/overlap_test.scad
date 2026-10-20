// Overlap test for the 4-colour keyring: each pair of colour parts must share (almost) zero volume.
include <keyring_4color.scad>;
part = "none"; A = "white"; B = "black";
module p(n) { if (n == "white") white_part(); else if (n == "black") black_part(); else if (n == "green") green_part(); else if (n == "pink") pink_part(); }
intersection() { p(A); p(B); }
