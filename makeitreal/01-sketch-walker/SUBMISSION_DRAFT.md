# Submission draft — Sketch Walker

> Fill the `[...]` placeholders after you have printed and photographed the real model.
> Tags: **Make It Real Contest** (required). Do NOT add the ELEGOO Generator tag (no generator was used).
> Files to upload: `print_*.stl` (+ 3MF with your own print profile), photos, GIF/video.
> Entry type: **From 2D Images to 3D Reality** *and* **From Ideas to Reality** → include the original drawing as the FIRST photo.

## Title
Sketch Walker — a doodle that walks off the page (2D → 3D)

## Photo order (first image decides the click)
1. **Original pen drawing on paper next to the finished print** (same pose, same face).
2. Walking GIF/video (side view, slow, 5–8 s).
3. Close-up of the engraved lines (the drawing's strokes become grooves).
4. Exploded view / assembly render (Blender or `out/preview_iso.png`).
5. Foot-path diagram (`out/foot_path_final.png`) — the "D" path, flat stance on the ground.
6. Print plates + BOM photo (everything laid out).

## Description (paste)
**The idea.** I drew a little creature on paper — and wanted it to actually walk. The drawing is traced from a photo of my own pen sketch, extruded into the body, and its lines are engraved into both faces, so the 3D body is literally my 2D drawing. Everything under it is a walking mechanism.

**What makes it different.**
- **Designed, not downloaded.** The leg is a six-bar linkage whose dimensions I found with my own optimizer (differential evolution). A plain four-bar can't have the foot as the lowest point *and* a long flat stance (best duty factor 0.34 in my search); adding a two-link dyad gives duty 0.62, a 14 mm foot lift and a 36° minimum transmission angle.
- **Six legs, tripod gait, one motor.** Three shafts linked by involute gears (20 teeth, module 1.5). Turn the middle shaft 180° and you get the tripod phase — no electronics.
- **Verified before printing.** Kinematic walking simulation (≥3 feet on the ground at all times, slip < 0.03 mm per 0.5°), 3D interference tests of every part pair at 5 crank angles (0 mm³ overlap), centre-of-mass analysis against the support polygon.
- **Print-friendly.** Every part prints flat without supports on a Bambu Lab P2S (256³). Hinges are plain 1.75 mm filament offcuts.

**How the 2D drawing became 3D.** `sketch_to_svg.py` thresholds the photo, extracts the closed outline, simplifies it to a polygon (mm), fills it and carves the inner pen strokes as 0.8 mm engravings. OpenSCAD extrudes it with stepped chamfers and two locating pegs.

**Print info.** [PLA / PETG], 0.4 mm nozzle, 0.16 mm links / 0.20 mm frame, 3 walls. Clearances calibrated with the included coupon: pin hole [x.xx] mm, D-bore clearance [x.xx] mm, gear backlash [x.xx] mm.
**BOM.** N20 gear motor (3 mm D-shaft, [xx] rpm), 2×AAA holder with switch, ~0.7 m of 1.75 mm filament for pins, CA glue.
**Assembly.** [~2 h] — see README (phase rule: mid shaft turned 180°).
**Post-processing.** Ream pin holes with a 2.0 mm drill bit, melt pin heads flat (≤ 1 mm), add rubber dots under the feet if the table is slippery.

**Credits / originality.** All CAD (OpenSCAD) and code (Python) are my own; no existing models were used or remixed. Gear generator, silhouette tracer, linkage optimizer and verification scripts are included in the project files.

## Short description (≤ 140 chars)
My pen doodle, traced into a 3D body and given six optimized-linkage legs. It walks. One motor, no supports, plain filament hinges.
