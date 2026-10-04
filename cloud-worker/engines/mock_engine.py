"""Mock engine: procedural placeholder figure. Lets you test the whole protocol (and the desktop app)
without a GPU. It is NOT an AI model and labels its output as such."""
from __future__ import annotations

import time
from pathlib import Path

import numpy as np
import trimesh
from PIL import Image

from .base import ModelEngine, Progress


def _ell(c, r, color):
    m = trimesh.creation.icosphere(subdivisions=3, radius=1.0)
    m.vertices = m.vertices * np.array(r) + np.array(c)
    m.visual.vertex_colors = np.tile(np.array(list(color) + [255], np.uint8), (len(m.vertices), 1))
    return m


class MockEngine(ModelEngine):
    name = "mock"
    supports_text = True
    supports_image = True
    supports_text_image = True

    def _build(self, out_dir: Path, label: str) -> Path:
        parts = [_ell((0, 0, 0.35), (0.35, 0.3, 0.38), (240, 235, 220)),
                 _ell((0, 0, 0.95), (0.5, 0.45, 0.42), (240, 235, 220)),
                 _ell((-0.16, -0.4, 1.0), (0.07, 0.05, 0.1), (20, 20, 20)),
                 _ell((0.16, -0.4, 1.0), (0.07, 0.05, 0.1), (20, 20, 20))]
        hat = trimesh.creation.cone(radius=0.38, height=0.7, sections=48)
        hat.apply_translation([0, 0, 1.28])
        hat.visual.vertex_colors = np.tile(np.array([122, 63, 196, 255], np.uint8), (len(hat.vertices), 1))
        parts.append(hat)
        mesh = trimesh.util.concatenate(parts)
        scene = trimesh.Scene()
        scene.add_geometry(mesh, node_name=f"MOCK_{label}")
        scene.metadata["figurecraft"] = {"engine": "mock", "note": "placeholder, not AI generated"}
        # glTF is Y-up
        mesh.apply_transform(trimesh.transformations.rotation_matrix(-np.pi / 2, [1, 0, 0]))
        out = out_dir / "result.glb"
        out.write_bytes(scene.export(file_type="glb"))
        return out

    def generate_from_text(self, prompt, params, out_dir, progress: Progress):
        for i, st in enumerate(("mock: prepare", "mock: build")):
            progress(st, None)
            time.sleep(0.4)
        return self._build(out_dir, "text")

    def generate_from_image(self, image: Image.Image, params, out_dir, progress: Progress):
        progress("mock: image", None)
        time.sleep(0.4)
        return self._build(out_dir, "image")

    def generate_from_text_and_image(self, prompt, image, params, out_dir, progress: Progress):
        return self.generate_from_image(image, params, out_dir, progress)
