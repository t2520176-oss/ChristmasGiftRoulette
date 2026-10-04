"""Tiny dictionary-based i18n (Korean primary, English secondary)."""
from __future__ import annotations

from .strings import STRINGS

_lang = "ko"
SUPPORTED = ("ko", "en")


def set_language(lang: str) -> None:
    global _lang
    _lang = lang if lang in SUPPORTED else "ko"


def get_language() -> str:
    return _lang


def tr(key: str, lang: str | None = None, **fmt) -> str:
    entry = STRINGS.get(key)
    if entry is None:
        return key
    text = entry[0] if (lang or _lang) == "ko" else entry[1]
    if fmt:
        try:
            return text.format(**fmt)
        except (KeyError, IndexError, ValueError):
            return text
    return text
