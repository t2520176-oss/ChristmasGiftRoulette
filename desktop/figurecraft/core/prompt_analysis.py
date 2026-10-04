"""Pull printable hints (height, colour count, named colours, wall thickness) out of the free-text prompt."""
from __future__ import annotations

import re
from dataclasses import dataclass, field

from . import color

_NUM = r"(\d+(?:\.\d+)?)"


@dataclass
class PromptHints:
    height_mm: float | None = None
    max_colors: int | None = None
    colors: list[tuple[str, str]] = field(default_factory=list)   # [(NAME, '#hex')] in order of appearance
    flat_bottom: bool = False
    min_wall_mm: float | None = None
    warnings: list[str] = field(default_factory=list)


def analyze(prompt: str) -> PromptHints:
    text = prompt or ""
    low = text.lower()
    h = PromptHints()

    m = (re.search(r"(?:높이|키|height|tall)\s*[:=]?\s*" + _NUM + r"\s*(mm|cm|밀리|센티)?", low)
         or re.search(_NUM + r"\s*(mm|cm)\s*(?:높이|tall|height)?", low))
    if m:
        val = float(m.group(1))
        unit = (m.group(2) or "mm") if m.lastindex and m.lastindex >= 2 else "mm"
        if unit in ("cm", "센티"):
            val *= 10
        if 10 <= val <= 400:
            h.height_mm = val
        else:
            h.warnings.append("height_out_of_range")

    m = re.search(r"최대\s*(\d)\s*색", low) or re.search(r"(?:max(?:imum)?\.?\s*)?(\d)\s*colou?rs?", low) \
        or re.search(r"(\d)\s*색", low)
    if m and 1 <= int(m.group(1)) <= 4:
        h.max_colors = int(m.group(1))

    if "평평한 바닥" in low or "flat bottom" in low or "평평한바닥" in low:
        h.flat_bottom = True

    m = re.search(r"최소\s*" + _NUM + r"\s*mm", low) or re.search(r"(?:min(?:imum)?)\s*" + _NUM + r"\s*mm", low)
    if m:
        h.min_wall_mm = float(m.group(1))

    words = sorted(color.HINT_WORDS, key=len, reverse=True)
    found: list[tuple[int, str]] = []
    taken: list[tuple[int, int]] = []
    for w in words:
        pat = re.escape(w) if re.search(r"[가-힣]", w) else r"\b" + re.escape(w) + r"\b"
        for mm in re.finditer(pat, low):
            s, e = mm.span()
            if any(not (e <= a or s >= b) for a, b in taken):
                continue
            taken.append((s, e))
            found.append((s, color.HINT_WORDS[w]))
    seen: set[str] = set()
    for _, name in sorted(found):
        if name not in seen:
            seen.add(name)
            h.colors.append((name, color.NAMED[name]))
    return h
