#!/usr/bin/env python3
"""Build one double-clickable Windows installer with an embedded, hashed source ZIP."""
import argparse
import base64
import hashlib
from pathlib import Path
import subprocess
import textwrap
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def build(output: Path, archive: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    names = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=ROOT
    ).decode().split("\0")
    names = sorted(name for name in names if name and (ROOT / name).is_file())
    for name in names:
        path = Path(name)
        if path.is_absolute() or ".." in path.parts or ".env" in path.name and path.name != ".env.example":
            raise ValueError(f"Unsafe or private file in source archive: {name}")
        if any(part in {"node_modules", ".next", ".git", "test-results", "playwright-report", "__pycache__"} for part in path.parts):
            raise ValueError(f"Generated file in source archive: {name}")
    archive.parent.mkdir(parents=True, exist_ok=True)
    manifest = []
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as bundle:
        for name in names:
            data = (ROOT / name).read_bytes()
            bundle.writestr("ourchat-private/" + name, data)
            manifest.append(hashlib.sha256(data).hexdigest() + "  " + name)
        bundle.writestr("ourchat-private/SHA256SUMS.txt", "\n".join(manifest) + "\n")
    payload = archive.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    script = (ROOT / "scripts/windows/Install-OurChat.ps1").read_text().replace("__ARCHIVE_SHA256__", digest)
    # File paths are passed in an environment variable, never interpolated into PowerShell code.
    bootstrap = """@echo off
setlocal DisableDelayedExpansion
title OurChat Setup
set "OURCHAT_SETUP_FILE=%~f0"
"%SystemRoot%\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$t=[IO.File]::ReadAllText($env:OURCHAT_SETUP_FILE,[Text.Encoding]::UTF8);$m=[string][char]13+[char]10+':OURCHAT_POWERSHELL'+[char]13+[char]10;$p=$t.IndexOf($m,[StringComparison]::Ordinal);if($p -lt 0){throw 'Installer script is missing'};$s=$t.Substring($p+$m.Length);$e=$s.IndexOf([string][char]13+[char]10+':OURCHAT_ARCHIVE'+[char]13+[char]10,[StringComparison]::Ordinal);if($e -lt 0){throw 'Installer archive is missing'};& ([scriptblock]::Create($s.Substring(0,$e)))"
if errorlevel 1 (
  echo OurChat setup stopped. If Windows policy blocks PowerShell, contact your administrator.
  pause
  exit /b 1
)
exit /b 0
:OURCHAT_POWERSHELL
"""
    content = bootstrap + script + "\n:OURCHAT_ARCHIVE\n" + "\n".join(textwrap.wrap(base64.b64encode(payload).decode("ascii"), 120)) + "\n"
    target = output / "OurChat-Setup.cmd"
    target.write_bytes(content.replace("\r\n", "\n").replace("\n", "\r\n").encode("ascii"))
    checksums = [(archive.name, digest), (target.name, hashlib.sha256(target.read_bytes()).hexdigest())]
    (output / "SHA256SUMS.txt").write_text("".join(f"{checksum}  {name}\n" for name, checksum in checksums))
    print(f"Created {target} ({target.stat().st_size:,} bytes), {len(names)} source files")
    print(f"Archive SHA256: {digest}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT.parent / "downloads")
    parser.add_argument("--archive", type=Path)
    arguments = parser.parse_args()
    build(arguments.output.resolve(), (arguments.archive or arguments.output / "OurChat.zip").resolve())
