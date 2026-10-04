"""Typed application errors carrying an i18n key so the UI can show Korean or English text."""
from __future__ import annotations

from . import i18n


class FigureCraftError(Exception):
    """Base error. `code` maps to i18n key `err.<code>`; `detail` is technical (logged, shown small)."""

    code = "unknown"

    def __init__(self, code: str | None = None, detail: str = "", **fmt):
        self.code = code or self.code
        self.detail = detail
        self.fmt = fmt
        super().__init__(f"{self.code}: {detail}" if detail else self.code)

    def user_message(self, lang: str | None = None) -> str:
        fmt = {"detail": self.detail, **self.fmt}
        return i18n.tr(f"err.{self.code}", lang=lang, **fmt)


class ProviderError(FigureCraftError):
    pass


class MeshError(FigureCraftError):
    pass


class ExportError(FigureCraftError):
    pass


class Cancelled(FigureCraftError):
    code = "cancelled"


def describe(exc: BaseException, lang: str | None = None) -> str:
    if isinstance(exc, FigureCraftError):
        return exc.user_message(lang)
    return i18n.tr("err.unknown", lang=lang, detail=str(exc))
