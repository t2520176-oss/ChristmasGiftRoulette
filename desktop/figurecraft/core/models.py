"""Plain data classes shared between the UI, the pipeline and the providers (no Qt here)."""
from __future__ import annotations

import enum
import uuid
from dataclasses import asdict, dataclass, field, fields
from typing import Any


def new_id() -> str:
    return str(uuid.uuid4())


class Stage(enum.IntEnum):
    PROMPT = 1
    CONNECT = 2
    GENERATE = 3
    DOWNLOAD = 4
    VALIDATE = 5
    REPAIR = 6
    OPTIMIZE = 7
    COLOR_ANALYSIS = 8
    COLOR_REDUCE = 9
    EXPORT = 10
    READY = 11

    @property
    def i18n_key(self) -> str:
        return f"stage.{self.name.lower()}"


class StageState(str, enum.Enum):
    PENDING = "pending"
    ACTIVE = "active"
    DONE = "done"
    SKIPPED = "skipped"
    FAILED = "failed"


class CloudStatus(str, enum.Enum):
    DISCONNECTED = "disconnected"
    CONNECTING = "connecting"
    READY = "ready"
    GENERATING = "generating"
    STOPPING = "stopping"


STYLES = ["chibi", "cartoon_animal", "fantasy", "anime", "mini_figure", "stylized_statue", "toy", "realistic_stylized"]
STYLE_PROMPT = {
    "chibi": "chibi cute style, big head small body",
    "cartoon_animal": "cartoon animal style",
    "fantasy": "fantasy creature style",
    "anime": "anime-inspired style",
    "mini_figure": "miniature tabletop figure",
    "stylized_statue": "stylized statue",
    "toy": "vinyl toy style",
    "realistic_stylized": "realistic but stylized",
}


@dataclass
class PrintOptions:
    """Printability preferences. Real-world dimensions are always millimetres."""

    height_mm: float = 100.0
    flat_bottom: bool = True
    add_base: bool = False
    base_shape: str = "round"        # none | round | oval | square | custom
    base_thickness_mm: float = 3.0
    base_margin_mm: float = 3.0
    base_custom_w_mm: float = 40.0
    base_custom_d_mm: float = 40.0
    min_thickness_protection: bool = True
    support_friendly: bool = True
    strengthen_thin_parts: bool = True
    remove_floating: bool = True
    close_holes: bool = True
    make_watertight: bool = True
    simplify_tiny_details: bool = False
    nozzle_mm: float = 0.4
    min_feature_mm: float = 1.2
    wall_mm: float = 1.5

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, d: dict[str, Any]) -> "PrintOptions":
        names = {f.name for f in fields(cls)}
        return cls(**{k: v for k, v in d.items() if k in names})


@dataclass
class PaletteEntry:
    hex: str = "#FFFFFF"
    name: str = "WHITE"
    locked: bool = False
    visible: bool = True

    @property
    def rgb(self) -> tuple[int, int, int]:
        h = self.hex.lstrip("#")
        return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


@dataclass
class GenerationRequest:
    prompt: str = ""
    style: str = "chibi"
    mode: str = "text"                # text | image_text
    reference_image: str | None = None
    engine: str = "trellis"
    print: PrintOptions = field(default_factory=PrintOptions)
    max_colors: int = 4
    palette_mode: str = "auto"        # auto | custom
    custom_palette: list[str] = field(default_factory=list)  # hex list
    seed: int | None = None
    # Revision metadata (set when the user asks for a model change)
    revision_of: str | None = None
    revision_instruction: str | None = None
    previous_job_id: str | None = None

    def full_prompt(self) -> str:
        parts = [self.prompt.strip()]
        if self.revision_instruction:
            parts.append("Revision request: " + self.revision_instruction.strip())
        return "\n".join(p for p in parts if p)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, d: dict[str, Any]) -> "GenerationRequest":
        d = dict(d)
        d["print"] = PrintOptions.from_dict(d.get("print", {}))
        names = {f.name for f in fields(cls)}
        return cls(**{k: v for k, v in d.items() if k in names})


@dataclass
class JobStatus:
    state: str = "queued"             # queued | running | succeeded | failed | cancelled
    stage: str = ""
    progress: float | None = None     # only when the worker really reports it
    message: str = ""
    gpu_seconds: float | None = None
    error: str | None = None

    @property
    def done(self) -> bool:
        return self.state in ("succeeded", "failed", "cancelled")


@dataclass
class ProviderCapabilities:
    text: bool = True
    image: bool = False
    text_image: bool = False
    lifecycle: bool = False           # can start/stop the GPU automatically
    reports_progress: bool = False
    is_demo: bool = False
    engines: list[str] = field(default_factory=list)


@dataclass
class ConnectionInfo:
    ok: bool
    message: str = ""
    engine: str = ""
    capabilities: ProviderCapabilities = field(default_factory=ProviderCapabilities)
    latency_ms: float | None = None
