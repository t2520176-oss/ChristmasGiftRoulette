"""Application logging with secret redaction. Secrets must never reach a log file."""
from __future__ import annotations

import logging
import logging.handlers
import re
import threading

from . import paths

_secrets: set[str] = set()
_lock = threading.Lock()
_PATTERNS = [
    (re.compile(r"(?i)(bearer\s+)[A-Za-z0-9._\-~+/=]{6,}"), r"\1***"),
    (re.compile(r"(?i)((?:api[_-]?key|token|secret|password|authorization)[\"']?\s*[:=]\s*[\"']?)[^\s\"',;&]{4,}"), r"\1***"),
]


def register_secret(value: str | None) -> None:
    if value and len(value) >= 4:
        with _lock:
            _secrets.add(value)


def redact(text: str) -> str:
    with _lock:
        secrets = list(_secrets)
    for s in secrets:
        text = text.replace(s, "***")
    for pat, repl in _PATTERNS:
        text = pat.sub(repl, text)
    return text


class RedactingFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        try:
            record.msg = redact(record.getMessage())
            record.args = ()
        except Exception:  # never let logging crash the app
            pass
        return True


_configured = False


def setup_logging(level: int = logging.INFO) -> logging.Logger:
    global _configured
    root = logging.getLogger("figurecraft")
    if _configured:
        return root
    root.setLevel(level)
    fmt = logging.Formatter("%(asctime)s %(levelname)-7s %(name)s: %(message)s")
    fh = logging.handlers.RotatingFileHandler(
        paths.logs_dir() / "figurecraft.log", maxBytes=2_000_000, backupCount=5, encoding="utf-8")
    fh.setFormatter(fmt)
    fh.addFilter(RedactingFilter())
    root.addHandler(fh)
    sh = logging.StreamHandler()
    sh.setFormatter(fmt)
    sh.addFilter(RedactingFilter())
    root.addHandler(sh)
    root.propagate = False
    _configured = True
    root.info("FigureCraft logging started")
    return root


def get_logger(name: str) -> logging.Logger:
    return logging.getLogger(f"figurecraft.{name}")
