# Submission draft — Hidden Doodle Lantern
Tags: **Make It Real Contest** (required). No Generator tag (none used). Upload: `lantern.3mf` (or STL), `lid.stl`, `riser.stl`.
Entry type: **From 2D Images to 3D Reality** → the FIRST photo must show the original paper drawing.

## Title
Hidden Doodle Lantern — a drawing hidden inside the wall thickness (2D → 3D)

## Photo order
1. Paper drawing strip next to the lantern, lights ON (same scene, glowing).
2. The same lantern in daylight: a plain white cylinder — "where did the drawing go?"
3. Close-up of a lit window/star holes and the soft glow areas.
4. The flat paper strip wrapped around the lantern (before/after).
5. Wall cross-section/inside view showing the stepped thickness (45° steps).
6. Everything laid out: lantern, lid, riser, LED tealight.

## Description (paste)
**Idea.** I wanted my pen drawing to live *inside* a 3D object, not on its surface. So the picture is stored as wall thickness: ink = 3.2 mm (blocks light), paper = 0.8 mm (glows), tiny enclosed paper dots = real holes (stars, windows). The outside is a perfectly smooth white cylinder — by day it shows nothing; switch on an LED tealight and the drawing appears.

**Why it prints easily.** One part (plus lid and a riser). No supports: a script converts the drawing and *enforces* a 45° overhang limit — wall thickness may only grow by one 0.8 mm step per 0.8 mm of height, and through-holes are limited to bridgeable sizes. The mesh is generated directly on a lattice, so it is watertight by construction (verified).

**How the 2D drawing became 3D.** `lantern.py` thresholds the photo of the pen strip, wraps it around the cylinder, finds enclosed paper blobs for holes, applies the 45° rule and writes STL/3MF. Included in the files.

**Print info.** White PLA, 0.4 mm nozzle, 0.2 mm layers, 8 wall loops, no supports, 5 mm brim. [print time], [weight].
**BOM.** LED (flameless) tealight. Nothing else.
**Post-processing.** None. (Never use a real candle.)
**Credits.** All code and CAD are my own; no existing models or remixes.
