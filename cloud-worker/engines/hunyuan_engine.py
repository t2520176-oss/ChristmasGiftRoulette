"""Hunyuan3D-2 adapter (shape + optional texture).

LICENSE WARNING: the Tencent Hunyuan 3D community license has territorial restrictions (it excludes e.g. the EU,
UK and South Korea at the time of writing). Read the current license text before using it - TRELLIS (MIT) is the
safer default. Not exercised on a real GPU during development of this repo.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

from .base import ModelEngine, Progress
from .text2image import TextToImage


class HunyuanEngine(ModelEngine):
    name = "hunyuan3d"
    supports_text = True
    supports_image = True
    supports_text_image = True

    def __init__(self, cfg):
        super().__init__(cfg)
        self.shape = None
        self.tex = None
        self.bg = None
        self.t2i = TextToImage(cfg)

    def load(self):
        if self.shape is not None:
            return
        from hy3dgen.shapegen import Hunyuan3DDiTFlowMatchingPipeline  # type: ignore
        self.shape = Hunyuan3DDiTFlowMatchingPipeline.from_pretrained(self.cfg.hunyuan_model)
        if self.cfg.hunyuan_texture:
            from hy3dgen.texgen import Hunyuan3DPaintPipeline  # type: ignore
            self.tex = Hunyuan3DPaintPipeline.from_pretrained(self.cfg.hunyuan_model)
        try:
            from hy3dgen.rembg import BackgroundRemover  # type: ignore
            self.bg = BackgroundRemover()
        except Exception:
            self.bg = None

    def _image_to_glb(self, image: Image.Image, params: dict, out_dir: Path, progress: Progress) -> Path:
        self.load()
        if self.bg is not None and image.mode == "RGB":
            image = self.bg(image)
        progress("Hunyuan3D: shape", None)
        mesh = self.shape(image=image)[0]
        if self.tex is not None:
            progress("Hunyuan3D: texture", None)
            mesh = self.tex(mesh, image=image)
        out = out_dir / "result.glb"
        mesh.export(str(out))
        return out

    def generate_from_text(self, prompt, params, out_dir, progress: Progress):
        progress("text -> reference image", None)
        img = self.t2i(params, params.get("seed"))
        img.save(out_dir / "reference.png")
        self.t2i.unload()
        return self._image_to_glb(img, params, out_dir, progress)

    def generate_from_image(self, image, params, out_dir, progress: Progress):
        return self._image_to_glb(image.convert("RGB"), params, out_dir, progress)

    def generate_from_text_and_image(self, prompt, image, params, out_dir, progress: Progress):
        return self._image_to_glb(image.convert("RGB"), params, out_dir, progress)
