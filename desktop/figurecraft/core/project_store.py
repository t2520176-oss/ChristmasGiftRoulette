"""Local project library: one folder per project, versions preserved under versions/vN."""
from __future__ import annotations

import json
import shutil
import time
import zipfile
from dataclasses import asdict, dataclass, field
from pathlib import Path

from .. import paths
from ..errors import FigureCraftError
from ..logging_setup import get_logger
from .models import new_id

log = get_logger("project")

VERSIONED_FILES = ["original.glb", "repaired.glb", "color_preview.glb", "model.3mf", "model.stl", "preview.png",
                   "labels.npy", "prompt.txt", "state.json"]
STATUS = ("ready", "failed", "running", "empty")


def now_iso() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%S")


@dataclass
class ProjectMeta:
    id: str = field(default_factory=new_id)
    title: str = ""
    created: str = field(default_factory=now_iso)
    updated: str = field(default_factory=now_iso)
    status: str = "empty"
    is_demo: bool = False
    prompt: str = ""
    request: dict = field(default_factory=dict)
    palette: list = field(default_factory=list)        # [{hex,name,locked,visible}]
    colors: int = 0
    size_mm: list = field(default_factory=lambda: [0.0, 0.0, 0.0])
    inspection: dict = field(default_factory=dict)
    timing: dict = field(default_factory=dict)         # started/finished/gpu_seconds...
    provider: str = ""
    engine: str = ""
    active_version: int = 0
    versions: list = field(default_factory=list)       # [{n, created, note, demo}]
    last_job_id: str = ""
    notes: list = field(default_factory=list)

    def to_json(self) -> str:
        return json.dumps(asdict(self), indent=2, ensure_ascii=False)


class Project:
    def __init__(self, root: Path, meta: ProjectMeta):
        self.root = Path(root)
        self.meta = meta

    # ---- paths
    def file(self, name: str) -> Path:
        return self.root / name

    @property
    def versions_dir(self) -> Path:
        return self.root / "versions"

    def save(self) -> None:
        self.meta.updated = now_iso()
        self.root.mkdir(parents=True, exist_ok=True)
        tmp = self.file("project.json.tmp")
        tmp.write_text(self.meta.to_json(), encoding="utf-8")
        tmp.replace(self.file("project.json"))

    # ---- versions
    def snapshot_version(self, note: str = "") -> int:
        n = max([v["n"] for v in self.meta.versions], default=0) + 1
        vdir = self.versions_dir / f"v{n}"
        vdir.mkdir(parents=True, exist_ok=True)
        for name in VERSIONED_FILES:
            src = self.file(name)
            if src.exists():
                shutil.copy2(src, vdir / name)
        (vdir / "version.json").write_text(json.dumps({
            "n": n, "created": now_iso(), "note": note, "prompt": self.meta.prompt,
            "request": self.meta.request, "palette": self.meta.palette, "demo": self.meta.is_demo,
        }, indent=2, ensure_ascii=False), encoding="utf-8")
        self.meta.versions.append({"n": n, "created": now_iso(), "note": note, "demo": self.meta.is_demo})
        self.meta.active_version = n
        self.save()
        return n

    def activate_version(self, n: int) -> None:
        vdir = self.versions_dir / f"v{n}"
        if not vdir.is_dir():
            raise FigureCraftError("no_project", f"version {n} missing")
        for name in VERSIONED_FILES:
            src = vdir / name
            if src.exists():
                shutil.copy2(src, self.file(name))
            else:
                self.file(name).unlink(missing_ok=True)
        info = json.loads((vdir / "version.json").read_text(encoding="utf-8"))
        self.meta.prompt = info.get("prompt", self.meta.prompt)
        self.meta.request = info.get("request", self.meta.request)
        self.meta.palette = info.get("palette", self.meta.palette)
        self.meta.is_demo = bool(info.get("demo", False))
        self.meta.active_version = n
        self.save()

    def update_version_files(self) -> None:
        """Sync the working files back into the active version snapshot (after colour edits)."""
        n = self.meta.active_version
        if not n:
            return
        vdir = self.versions_dir / f"v{n}"
        if vdir.is_dir():
            for name in ("color_preview.glb", "model.3mf", "model.stl", "labels.npy", "state.json", "preview.png"):
                if self.file(name).exists():
                    shutil.copy2(self.file(name), vdir / name)


class ProjectStore:
    def __init__(self, root: Path):
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)

    def create(self, title: str = "", prompt: str = "", is_demo: bool = False) -> Project:
        meta = ProjectMeta(title=title or "Untitled", prompt=prompt, is_demo=is_demo, status="running")
        p = Project(self.root / meta.id, meta)
        p.root.mkdir(parents=True, exist_ok=True)
        p.save()
        log.info("project created %s", meta.id)
        return p

    def open(self, pid: str) -> Project:
        pid = paths.sanitize_filename(pid, default="x", max_len=64)
        root = self.root / pid
        try:
            meta_d = json.loads((root / "project.json").read_text(encoding="utf-8"))
        except Exception as exc:
            raise FigureCraftError("no_project", str(exc)) from exc
        names = ProjectMeta.__dataclass_fields__.keys()
        return Project(root, ProjectMeta(**{k: v for k, v in meta_d.items() if k in names}))

    def list(self) -> list[Project]:
        out = []
        for d in sorted(self.root.iterdir()) if self.root.exists() else []:
            if (d / "project.json").is_file():
                try:
                    out.append(self.open(d.name))
                except FigureCraftError:
                    continue
        out.sort(key=lambda p: p.meta.updated, reverse=True)
        return out

    def rename(self, pid: str, title: str) -> None:
        p = self.open(pid)
        p.meta.title = title.strip() or p.meta.title
        p.save()

    def duplicate(self, pid: str) -> Project:
        src = self.open(pid)
        new_meta = ProjectMeta(**{**asdict(src.meta), "id": new_id(), "title": src.meta.title + " (copy)",
                                  "created": now_iso()})
        dst_root = self.root / new_meta.id
        shutil.copytree(src.root, dst_root)
        p = Project(dst_root, new_meta)
        p.save()
        return p

    def delete(self, pid: str) -> None:
        p = self.open(pid)
        shutil.rmtree(p.root)

    def export_zip(self, pid: str, dest: str | Path) -> Path:
        p = self.open(pid)
        dest = Path(dest)
        with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as z:
            for f in p.root.rglob("*"):
                if f.is_file():
                    z.write(f, f.relative_to(p.root).as_posix())
        return dest
