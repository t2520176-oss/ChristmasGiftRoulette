"""Filesystem locations (settings, logs, projects) and bundled-resource lookup."""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path


def app_data_dir() -> Path:
    override = os.environ.get("FIGURECRAFT_HOME")
    if override:
        base = Path(override)
    elif sys.platform == "win32":
        base = Path(os.environ.get("APPDATA") or Path.home() / "AppData" / "Roaming") / "FigureCraft"
    else:
        base = Path(os.environ.get("XDG_CONFIG_HOME") or Path.home() / ".config") / "FigureCraft"
    base.mkdir(parents=True, exist_ok=True)
    return base


def logs_dir() -> Path:
    p = app_data_dir() / "logs"
    p.mkdir(parents=True, exist_ok=True)
    return p


def default_projects_dir() -> Path:
    p = app_data_dir() / "projects"
    p.mkdir(parents=True, exist_ok=True)
    return p


def temp_dir() -> Path:
    p = app_data_dir() / "tmp"
    p.mkdir(parents=True, exist_ok=True)
    return p


def resource_path(rel: str) -> Path:
    """Locate a bundled resource both in source checkouts and PyInstaller bundles."""
    if getattr(sys, "frozen", False):
        base = Path(getattr(sys, "_MEIPASS", Path(sys.executable).parent))
        cand = base / "figurecraft" / rel
        if cand.exists():
            return cand
        return base / rel
    return Path(__file__).resolve().parent / rel


_BAD = re.compile(r"[^0-9A-Za-z가-힣._\- ]+")


def sanitize_filename(name: str, default: str = "figure", max_len: int = 80) -> str:
    """Make a string safe as a single path component on Windows and POSIX."""
    name = os.path.basename(str(name).replace("\\", "/"))
    name = _BAD.sub("_", name).strip(" ._")
    if not name:
        name = default
    if name.split(".")[0].upper() in {"CON", "PRN", "AUX", "NUL", *(f"COM{i}" for i in range(1, 10)),
                                      *(f"LPT{i}" for i in range(1, 10))}:
        name = "_" + name
    return name[:max_len]
