"""TRELLIS (Microsoft, MIT license) image-to-3D adapter.

Text -> reference image (diffusers) -> TRELLIS -> textured GLB.
Requires the TRELLIS repository + its CUDA extensions installed in the image (see Dockerfile / README_KR.md).
Not exercised on a real GPU during development of this repo: verify on your GPU before relying on it.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

from .base import ModelEngine, Progress
from .text2image import TextToImage


class TrellisEngine(ModelEngine):
    name = "trellis"
    supports_text = True            # via the text -> image stage
    supports_image = True
    supports_text_image = True      # the image drives the 3D shape; the text only influences nothing but logging

    def __init__(self, cfg):
        super().__init__(cfg)
        self.pipeline = None
        self.t2i = TextToImage(cfg)

    def load(self):
        if self.pipeline is not None:
            return
        from trellis.pipelines import TrellisImageTo3DPipeline  # type: ignore
        self.pipeline = TrellisImageTo3DPipeline.from_pretrained(self.cfg.trellis_model)
        self.pipeline.cuda()

    def _image_to_glb(self, image: Image.Image, params: dict, out_dir: Path, progress: Progress) -> Path:
        from trellis.utils import postprocessing_utils  # type: ignore
        self.load()
        progress("TRELLIS: generating 3D", None)
        seed = params.get("seed")
        outputs = self.pipeline.run(image, seed=int(seed) if seed is not None else 1)
        progress("TRELLIS: baking GLB", None)
        glb = postprocessing_utils.to_glb(outputs["gaussian"][0], outputs["mesh"][0],
                                          simplify=self.cfg.mesh_simplify, texture_size=self.cfg.texture_size)
        out = out_dir / "result.glb"
        glb.export(str(out))
        return out

    def generate_from_text(self, prompt, params, out_dir, progress: Progress):
        progress("text -> reference image", None)
        img = self.t2i(params, params.get("seed"))
        img.save(out_dir / "reference.png")
        self.t2i.unload()                      # free VRAM for the 3D model
        return self._image_to_glb(img, params, out_dir, progress)

    def generate_from_image(self, image, params, out_dir, progress: Progress):
        return self._image_to_glb(image.convert("RGB"), params, out_dir, progress)

    def generate_from_text_and_image(self, prompt, image, params, out_dir, progress: Progress):
        return self._image_to_glb(image.convert("RGB"), params, out_dir, progress)
