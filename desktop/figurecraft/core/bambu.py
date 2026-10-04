"""Bambu Studio detection and launching (argument arrays only - never a shell string)."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

from ..errors import FigureCraftError
from ..logging_setup import get_logger

log = get_logger("bambu")

EXE_NAMES = ("bambu-studio.exe", "BambuStudio.exe", "bambu-studio", "BambuStudio", "Bambu Studio")


def _candidates() -> list[Path]:
    out: list[Path] = []
    if sys.platform == "win32":
        roots = [os.environ.get("ProgramFiles"), os.environ.get("ProgramFiles(x86)"), os.environ.get("ProgramW6432"),
                 os.environ.get("LOCALAPPDATA") and str(Path(os.environ["LOCALAPPDATA"]) / "Programs")]
        for r in filter(None, roots):
            for d in ("Bambu Studio", "BambuStudio"):
                for exe in ("bambu-studio.exe", "BambuStudio.exe"):
                    out.append(Path(r) / d / exe)
        out += [Path(r"C:\Program Files\Bambu Studio\bambu-studio.exe")]
    elif sys.platform == "darwin":
        out.append(Path("/Applications/BambuStudio.app/Contents/MacOS/BambuStudio"))
    else:
        out += [Path("/usr/bin/bambu-studio"), Path("/usr/local/bin/bambu-studio"), Path("/opt/BambuStudio/bambu-studio")]
    return out


def _from_registry() -> Path | None:
    if sys.platform != "win32":
        return None
    try:
        import winreg
        for hive in (winreg.HKEY_LOCAL_MACHINE, winreg.HKEY_CURRENT_USER):
            for sub in (r"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall",
                        r"SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall"):
                try:
                    with winreg.OpenKey(hive, sub) as k:
                        for i in range(winreg.QueryInfoKey(k)[0]):
                            try:
                                with winreg.OpenKey(k, winreg.EnumKey(k, i)) as sk:
                                    name = winreg.QueryValueEx(sk, "DisplayName")[0]
                                    if "bambu studio" in str(name).lower():
                                        loc = winreg.QueryValueEx(sk, "InstallLocation")[0]
                                        for exe in ("bambu-studio.exe", "BambuStudio.exe"):
                                            p = Path(loc) / exe
                                            if p.is_file():
                                                return p
                            except OSError:
                                continue
                except OSError:
                    continue
    except Exception as exc:  # registry is best-effort
        log.debug("registry lookup failed: %s", exc)
    return None


def detect() -> Path | None:
    for c in _candidates():
        if c.is_file():
            return c
    return _from_registry()


def resolve(configured: str | None) -> Path | None:
    if configured:
        p = Path(configured)
        if p.is_file():
            return p
    return detect()


def open_in_bambu(exe: str | os.PathLike | None, model_path: str | os.PathLike) -> None:
    model = Path(model_path)
    if not model.is_file():
        raise FigureCraftError("file_missing", str(model))
    exe_p = resolve(str(exe) if exe else None)
    if exe_p is None:
        raise FigureCraftError("bambu_not_found")
    try:
        subprocess.Popen([str(exe_p), str(model.resolve())], shell=False, close_fds=True,
                         stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except OSError as exc:
        raise FigureCraftError("bambu_launch_failed", str(exc)) from exc
    log.info("launched Bambu Studio with %s", model.name)
