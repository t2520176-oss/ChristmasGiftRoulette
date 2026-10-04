"""Write output/BUILD_INFO.txt (honest build report). Usage:
   python tools/make_build_info.py --out ../output --exe ../output/FigureCraft/FigureCraft.exe [--selftest selftest.log]
"""
from __future__ import annotations

import argparse
import datetime as dt
import importlib.metadata as md
import platform
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from figurecraft import __version__  # noqa: E402

DEPS = ["PySide6", "trimesh", "numpy", "scipy", "Pillow", "lib3mf", "manifold3d",
        "scikit-image", "lxml", "fast-simplification", "mapbox-earcut", "networkx", "requests", "cryptography", "pyinstaller"]


def dep_versions() -> list[str]:
    out = []
    for d in DEPS:
        try:
            out.append(f"  {d} {md.version(d)}")
        except md.PackageNotFoundError:
            out.append(f"  {d} (not installed)")
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--exe", required=True)
    ap.add_argument("--selftest", default="")
    ap.add_argument("--smoke", default="not run")
    ap.add_argument("--target", default=platform.system())
    a = ap.parse_args()
    out = Path(a.out).resolve()
    exe = Path(a.exe).resolve()
    st = Path(a.selftest).read_text(encoding="utf-8", errors="replace") if a.selftest and Path(a.selftest).exists() else ""
    passed = "SELF-TEST PASSED" in st
    lines = [
        "FigureCraft - BUILD INFO",
        "=" * 60,
        f"Build date        : {dt.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        f"Version           : {__version__}",
        f"Target platform   : {a.target} ({platform.platform()})",
        f"Build path        : {out}",
        f"Executable path   : {exe}   (exists: {exe.exists()})",
        f"Python version    : {sys.version.split()[0]}",
        "Major dependencies:",
        *dep_versions(),
        "",
        "Status (verified by running the packaged build, see self-test below)",
        "-" * 60,
        f"Packaged self-test       : {'PASSED' if passed else ('FAILED / not run' if st else 'not run')}",
        f"GUI smoke test           : {a.smoke}",
        f"Demo mode                : {'WORKS (demo figures, pipeline, exports verified)' if passed else 'not verified'}",
        f"3MF export               : {'VERIFIED - multipart + colour 3MF written and validated (lib3mf read-back)' if passed else 'not verified'}",
        "Cloud provider           : Generic HTTP + RunPod providers implemented. The HTTP protocol is verified against",
        "                           the bundled cloud-worker (mock engine). NOT verified against a real RunPod account or",
        "                           a real GPU model (TRELLIS / Hunyuan3D adapters are untested on GPU).",
        "Bambu Studio             : detection + launch implemented (argument list, no shell). Opening in a real Bambu",
        "                           Studio install was NOT verified in the build environment.",
        "",
        "Known limitations",
        "-" * 60,
        "- Real AI generation needs a cloud GPU worker that you deploy yourself (see cloud-worker/README_KR.md).",
        "- Demo figures are hand-built shapes, NOT AI output (they are labelled DEMO everywhere).",
        "- Bambu Studio may ignore per-triangle colours of the 'Color 3MF'; use the Multipart 3MF and assign AMS",
        "  filaments per part. Compatibility with every Bambu Studio version could not be tested here.",
        "- Thin-feature / overhang / watertight checks are estimates; the final check belongs to the slicer.",
        "- Colour parts are closed solids built from the colour borders at ~0.5-1 mm resolution.",
        "- Windows packaging is a portable FOLDER (QtWebEngine makes one-file EXEs unreliable).",
        "",
    ]
    if st:
        lines += ["Self-test output", "-" * 60, st.strip(), ""]
    out.mkdir(parents=True, exist_ok=True)
    (out / "BUILD_INFO.txt").write_text("\n".join(lines), encoding="utf-8")
    print("wrote", out / "BUILD_INFO.txt")


if __name__ == "__main__":
    main()
