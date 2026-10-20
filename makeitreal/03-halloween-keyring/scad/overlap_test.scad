// Overlap test: every pair of colour parts must share (almost) zero volume.   openscad -D 'A="white"' -D 'B="green"' ...
include <keyring.scad>;
PART = "none"; A = "white"; B = "green";
module p(n) { if (n == "cream") cream(); else if (n == "black") black(); else if (n == "white") white(); else if (n == "green") green();
              else if (n == "gold") gold(); else if (n == "purple") purple(); else if (n == "pink") pink(); }
intersection() { p(A); p(B); }
