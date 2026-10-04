"""Remove files the app never uses from the PyInstaller output (Qt translations for other languages, etc.)."""
from __future__ import annotations

import sys
from pathlib import Path

KEEP_LANGS = ("ko", "en")


def slim(dist: Path) -> int:
    saved = 0
    for tdir in dist.rglob("translations"):
        if not tdir.is_dir():
            continue
        for f in list(tdir.glob("*.qm")):
            stem = f.stem.split("_", 1)[-1] if "_" in f.stem else ""
            if not any(stem == k or stem.startswith(k + "_") for k in KEEP_LANGS):
                saved += f.stat().st_size
                f.unlink()
        pak = tdir / "qtwebengine_locales"
        if pak.is_dir():
            for f in list(pak.glob("*.pak")):
                if f.stem not in ("en-US", "ko"):
                    saved += f.stat().st_size
                    f.unlink()
    return saved


if __name__ == "__main__":
    d = Path(sys.argv[1])
    print(f"slim_dist: removed {slim(d) / 1e6:.1f} MB of unused translations from {d}")
