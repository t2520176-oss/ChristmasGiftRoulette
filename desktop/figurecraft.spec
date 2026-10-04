# -*- mode: python ; coding: utf-8 -*-
# PyInstaller spec: portable *folder* build (more reliable than one-file with QtWebEngine).
#   pyinstaller desktop/figurecraft.spec --noconfirm --distpath output --workpath build/pyinstaller
import sys
from pathlib import Path

from PyInstaller.utils.hooks import collect_all, collect_data_files, collect_submodules

here = Path(SPECPATH)
datas, binaries, hiddenimports = [], [], []

datas += [(str(here / "figurecraft" / "viewer"), "figurecraft/viewer"),
          (str(here / "figurecraft" / "assets"), "figurecraft/assets")]

for pkg in ("lib3mf", "manifold3d", "fast_simplification", "mapbox_earcut", "skimage", "trimesh"):
    try:
        d, b, h = collect_all(pkg)
        datas += d; binaries += b; hiddenimports += h
    except Exception as exc:  # optional packages must not break the build
        print(f"[spec] warning: could not collect {pkg}: {exc}")
hiddenimports += collect_submodules("figurecraft")
hiddenimports += ["scipy.spatial.transform._rotation_groups", "scipy.special._cdflib"]

excludes = ["tkinter", "matplotlib", "IPython", "pytest", "cv2", "torch", "tensorflow", "sklearn", "PyQt5", "PyQt6",
            "PySide6.Qt3DCore", "PySide6.Qt3DRender", "PySide6.Qt3DInput", "PySide6.Qt3DLogic", "PySide6.Qt3DAnimation",
            "PySide6.Qt3DExtras", "PySide6.QtCharts", "PySide6.QtDataVisualization", "PySide6.QtMultimedia",
            "PySide6.QtMultimediaWidgets", "PySide6.QtQuick3D", "PySide6.QtSensors", "PySide6.QtSerialPort",
            "PySide6.QtBluetooth", "PySide6.QtNfc", "PySide6.QtLocation", "PySide6.QtTest", "PySide6.QtSql",
            "PySide6.QtRemoteObjects", "PySide6.QtScxml", "PySide6.QtSpatialAudio", "PySide6.QtGraphs"]

a = Analysis([str(here / "run_figurecraft.py")], pathex=[str(here)], binaries=binaries, datas=datas,
             hiddenimports=hiddenimports, hookspath=[], runtime_hooks=[], excludes=excludes, noarchive=False)
pyz = PYZ(a.pure)
icon = str(here / "figurecraft" / "assets" / "icon.ico")
exe = EXE(pyz, a.scripts, [], exclude_binaries=True, name="FigureCraft", debug=False, bootloader_ignore_signals=False,
          strip=False, upx=False, console=False, icon=icon if sys.platform == "win32" else None)
coll = COLLECT(exe, a.binaries, a.datas, strip=False, upx=False, name="FigureCraft")
