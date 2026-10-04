"""Encrypt/decrypt small secrets (API keys) for the current OS user.

Windows: DPAPI (CryptProtectData) - bound to the Windows user account.
Other OS (used for development/tests): Fernet with a key file readable only by the user.
"""
from __future__ import annotations

import base64
import os
import sys

from . import paths


def _dpapi(data: bytes, protect: bool) -> bytes:
    import ctypes
    from ctypes import wintypes

    class BLOB(ctypes.Structure):
        _fields_ = [("cbData", wintypes.DWORD), ("pbData", ctypes.POINTER(ctypes.c_char))]

    buf = ctypes.create_string_buffer(data, len(data))
    inb = BLOB(len(data), ctypes.cast(buf, ctypes.POINTER(ctypes.c_char)))
    outb = BLOB()
    crypt32, kernel32 = ctypes.windll.crypt32, ctypes.windll.kernel32
    fn = crypt32.CryptProtectData if protect else crypt32.CryptUnprotectData
    ok = fn(ctypes.byref(inb), None, None, None, None, 0, ctypes.byref(outb))
    if not ok:
        raise OSError("DPAPI call failed")
    try:
        return ctypes.string_at(outb.pbData, outb.cbData)
    finally:
        kernel32.LocalFree(outb.pbData)


def _fernet():
    from cryptography.fernet import Fernet

    keyfile = paths.app_data_dir() / ".fckey"
    if not keyfile.exists():
        keyfile.write_bytes(Fernet.generate_key())
        try:
            os.chmod(keyfile, 0o600)
        except OSError:
            pass
    return Fernet(keyfile.read_bytes())


def encrypt(plain: str) -> str:
    if not plain:
        return ""
    if sys.platform == "win32":
        return "dpapi:" + base64.b64encode(_dpapi(plain.encode("utf-8"), True)).decode("ascii")
    return "fernet:" + _fernet().encrypt(plain.encode("utf-8")).decode("ascii")


def decrypt(token: str) -> str:
    if not token:
        return ""
    try:
        kind, _, body = token.partition(":")
        if kind == "dpapi" and sys.platform == "win32":
            return _dpapi(base64.b64decode(body), False).decode("utf-8")
        if kind == "fernet":
            return _fernet().decrypt(body.encode("ascii")).decode("utf-8")
    except Exception:
        return ""
    return ""
