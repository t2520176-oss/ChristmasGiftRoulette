import re
from pathlib import Path

from figurecraft import i18n
from figurecraft.strings import STRINGS

SRC = Path(__file__).resolve().parents[1] / "figurecraft"


def _keys():
    keys = set()
    for f in SRC.rglob("*.py"):
        if f.name == "strings.py":
            continue
        keys |= set(re.findall(r'\btr\(\s*"([A-Za-z0-9_.\-]+)"', f.read_text(encoding="utf-8")))
    return {k for k in keys if not k.endswith(".")}


def test_every_used_key_is_translated():
    assert [k for k in _keys() if k not in STRINGS] == []


def test_error_codes_have_messages():
    codes = set()
    for f in SRC.rglob("*.py"):
        codes |= set(re.findall(r'(?:FigureCraftError|ProviderError|MeshError|ExportError)\(\s*"([a-z_0-9]+)"', f.read_text(encoding="utf-8")))
    assert [c for c in codes if f"err.{c}" not in STRINGS] == []


def test_both_languages_filled_and_placeholders_match():
    for key, (ko, en) in STRINGS.items():
        assert ko.strip() and en.strip(), key
        assert sorted(re.findall(r"\{(\w+)\}", ko)) == sorted(re.findall(r"\{(\w+)\}", en)), key


def test_language_switch():
    i18n.set_language("en")
    assert i18n.tr("nav.generate") == "Generate"
    i18n.set_language("ko")
    assert i18n.tr("nav.generate") == "생성"


def test_dynamic_keys_exist():
    for k in ("style.chibi", "base.round", "prov.runpod", "engine.trellis", "cloud.generating", "status.GOOD",
              "export.method.solid", "lib.status.ready"):
        assert k in STRINGS
    from figurecraft.core.mesh_inspect import Issue
    for code in ("empty", "degenerate_model", "degenerate_faces", "duplicate_faces", "non_manifold", "open_edges",
                 "not_watertight", "tiny_components", "floating", "multi_component", "flat_bottom_missing", "overhangs",
                 "thin_features", "thin_skipped"):
        assert f"issue.{code}" in STRINGS
    for code in ("nonfinite", "degenerate", "duplicates", "weld", "winding", "inversion", "fragments", "holes_filled",
                 "holes_left", "smoothed"):
        assert f"repair.{code}" in STRINGS
