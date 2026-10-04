"""Replaceable 3D engine interface. Add a new open-source model by subclassing ModelEngine."""
from __future__ import annotations

import abc
from pathlib import Path
from typing import Callable

from PIL import Image

Progress = Callable[[str, float | None], None]     # (stage text, fraction or None)


class ModelEngine(abc.ABC):
    name = "base"
    supports_text = False
    supports_image = False
    supports_text_image = False

    def __init__(self, cfg):
        self.cfg = cfg

    def load(self) -> None:
        """Load weights lazily (first job), not at import time."""

    @abc.abstractmethod
    def generate_from_text(self, prompt: str, params: dict, out_dir: Path, progress: Progress) -> Path: ...

    def generate_from_image(self, image: Image.Image, params: dict, out_dir: Path, progress: Progress) -> Path:
        raise NotImplementedError(f"{self.name} does not support image input")

    def generate_from_text_and_image(self, prompt: str, image: Image.Image, params: dict, out_dir: Path,
                                     progress: Progress) -> Path:
        raise NotImplementedError(f"{self.name} does not support text+image input")

    def capabilities(self) -> dict:
        return {"text": self.supports_text, "image": self.supports_image, "text_image": self.supports_text_image,
                "shutdown": bool(getattr(self.cfg, "allow_shutdown", False)), "progress": False}


def printable_image_prompt(params: dict) -> tuple[str, str]:
    """Prompt/negative prompt for the text -> reference-image stage (image-to-3D models want a clean single object)."""
    base = (params.get("prompt") or "").strip()
    style = params.get("style_prompt") or ""
    hints = params.get("printability_hints") or ""
    positive = (f"{base}. {style}. single figurine, full body, front view, centered, plain white background, "
                f"soft studio lighting, clean simple shapes, thick sturdy limbs, matte plastic toy, {hints}")
    negative = ("text, watermark, multiple objects, cropped, cut off, thin spindly parts, floating pieces, "
                "background scenery, blurry, low quality")
    return positive, negative
