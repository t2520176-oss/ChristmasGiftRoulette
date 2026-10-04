from __future__ import annotations

ENGINES = {
    "mock": ("engines.mock_engine", "MockEngine"),
    "trellis": ("engines.trellis_engine", "TrellisEngine"),
    "hunyuan3d": ("engines.hunyuan_engine", "HunyuanEngine"),
}


def create_engine(name: str, cfg):
    import importlib
    if name not in ENGINES:
        raise SystemExit(f"Unknown ENGINE '{name}'. Available: {', '.join(ENGINES)}")
    mod, cls = ENGINES[name]
    return getattr(importlib.import_module(mod), cls)(cfg)
