"""Stage 1 of the two-stage pipeline: text -> clean reference image (diffusers). Loaded lazily, GPU only."""
from __future__ import annotations

from PIL import Image

from .base import printable_image_prompt


class TextToImage:
    def __init__(self, cfg):
        self.cfg = cfg
        self.pipe = None

    def load(self):
        if self.pipe is not None:
            return
        import torch
        from diffusers import AutoPipelineForText2Image
        kw = {"torch_dtype": torch.float16, "use_safetensors": True}
        if self.cfg.hf_token:
            kw["token"] = self.cfg.hf_token
        self.pipe = AutoPipelineForText2Image.from_pretrained(self.cfg.t2i_model, **kw).to("cuda")

    def __call__(self, params: dict, seed: int | None = None) -> Image.Image:
        import torch
        self.load()
        pos, neg = printable_image_prompt(params)
        gen = torch.Generator("cuda").manual_seed(int(seed)) if seed is not None else None
        img = self.pipe(prompt=pos, negative_prompt=neg, num_inference_steps=self.cfg.t2i_steps, generator=gen,
                        width=1024, height=1024).images[0]
        return img.convert("RGB")

    def unload(self):
        import gc
        import torch
        self.pipe = None
        gc.collect()
        torch.cuda.empty_cache()
