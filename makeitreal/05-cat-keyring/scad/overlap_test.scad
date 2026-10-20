// Overlap test for the cat keyring: each pair of colour parts must share (almost) zero volume.
include <keyring_cat_4color.scad>;
part = "none"; A = "lavender"; B = "black";
module p(n) { if (n == "lavender") lavender_part(); else if (n == "black") black_part(); else if (n == "yellow") yellow_part(); else if (n == "pink") pink_part(); else if (n == "white") white_part(); }
intersection() { p(A); p(B); }
