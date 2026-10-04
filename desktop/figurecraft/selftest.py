"""Headless self-test of the packaged application (no window needed).

`FigureCraft.exe --self-test` builds a demo figure, reduces colours, exports STL/GLB/OBJ/3MF (both modes),
validates the 3MF files and prints one line per check. Exit code 0 = everything worked.
Used by build_windows.bat to prove the PyInstaller bundle contains all native libraries.
"""
from __future__ import annotations

import sys
import tempfile
import traceback
from pathlib import Path


def run() -> int:
    from . import __version__
    results: list[tuple[str, bool, str]] = []

    def check(name, fn):
        try:
            detail = fn() or ""
            results.append((name, True, str(detail)))
        except Exception as exc:  # noqa: BLE001
            results.append((name, False, f"{type(exc).__name__}: {exc}"))
            traceback.print_exc()

    tmp = Path(tempfile.mkdtemp(prefix="figurecraft-selftest-"))
    state: dict = {}

    def t_demo():
        from .core import demo
        state["mesh"] = demo.build_demo("cat_wizard")
        return f"{len(state['mesh'].faces)} faces, watertight={state['mesh'].is_watertight}"

    def t_inspect():
        from .core import mesh_inspect
        r = mesh_inspect.inspect_mesh(state["mesh"])
        return f"status={r.status} thin={r.thin_ratio}"

    def t_color():
        from .core import color
        r = color.reduce_colors(state["mesh"], 4)
        state["red"] = r
        names = [p.name for p in r.palette]
        assert len(names) == 4
        return ",".join(names)

    def t_glb():
        from .core import mesh_io
        p = mesh_io.save_glb(state["mesh"], tmp / "a.glb")
        return f"reload faces={len(mesh_io.load_mesh(p).faces)}"

    def t_3mf(mode):
        def f():
            from .core import export
            r = state["red"]
            rep = export.export_3mf(tmp / f"{mode}.3mf", state["mesh"], r.labels, r.palette, mode=mode, demo=True)
            return f"backend={rep.backend} method={rep.method} objects={rep.validation.objects} lib3mf_read={rep.validation.lib3mf_read}"
        return f

    def t_stl():
        from .core import export
        r = state["red"]
        files = export.export_stl_by_color(tmp / "stl", "selftest", state["mesh"], r.labels, r.palette)
        export.export_stl(tmp / "all.stl", state["mesh"])
        export.export_obj(tmp / "a.obj", state["mesh"], r.labels, r.palette)
        return f"{len(files)} colour STL files"

    def t_lib3mf():
        from .core import threemf
        return f"lib3mf available={threemf.HAVE_LIB3MF}"

    def t_manifold():
        from .core import mesh_ops
        assert mesh_ops.HAVE_MANIFOLD, "manifold3d missing"
        return "manifold3d ok"

    def t_viewer_files():
        from . import paths
        for f in ("viewer/index.html", "viewer/viewer.js", "viewer/three-bundle.js", "assets/check.svg"):
            assert paths.resource_path(f).is_file(), f"missing resource {f}"
        return "viewer resources present"

    def t_secret():
        from . import secret_store
        assert secret_store.decrypt(secret_store.encrypt("abc123")) == "abc123"
        return "settings encryption ok"

    def t_pipeline():
        import os
        os.environ["FIGURECRAFT_HOME"] = str(tmp / "home")
        from .core import pipeline
        from .core.models import GenerationRequest
        from .core.project_store import ProjectStore
        from .providers.mock import MockProvider
        from .settings import Settings
        s = Settings()
        res = pipeline.run_generation(GenerationRequest(prompt="귀여운 고양이 마법사, 높이 100mm"), MockProvider(s), s,
                                      ProjectStore(s.projects_path()), pipeline.Hooks())
        assert res.project.meta.status == "ready"
        return f"project ready, palette={[p['name'] for p in res.project.meta.palette]}"

    for name, fn in (("demo figure", t_demo), ("mesh inspection", t_inspect), ("colour reduction", t_color),
                     ("GLB save/load", t_glb), ("lib3mf import", t_lib3mf), ("manifold3d import", t_manifold),
                     ("3MF multipart", t_3mf("multipart")), ("3MF colour", t_3mf("color")),
                     ("STL/OBJ exports", t_stl), ("viewer resources", t_viewer_files), ("encryption", t_secret),
                     ("demo pipeline", t_pipeline)):
        check(name, fn)
    print(f"FigureCraft {__version__} self-test")
    ok = True
    for name, good, detail in results:
        print(f"  [{'OK' if good else 'FAIL'}] {name}: {detail}")
        ok &= good
    print("SELF-TEST " + ("PASSED" if ok else "FAILED"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(run())
