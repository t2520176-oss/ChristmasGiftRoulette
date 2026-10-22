"""Role -> filament colour table of the SANTA SOUND AMPLIFIER (shared by the planner, the legend and Blender).

Four filament colours are used:  RED (body, hat),  WHITE (beard, brim, pompom),  PEACH (face),  BLACK (eyes, boots, belt).
Change a colour here (or pass --palette my.json, optionally with "_hex") and the bed is re-planned.
"""
import json
import re

COLOR_HEX = {"RED": "#C8102E", "WHITE": "#F3EFE6", "PEACH": "#F2B79A", "BLACK": "#1A1A1E", "BLUE": "#6FA8FF"}
ORDER = ["RED", "WHITE", "PEACH", "BLACK", "BLUE"]

# "piece-kind.role" -> colour
BASE_ROLES = {
    "santa.red": "RED", "santa.white": "WHITE", "santa.peach": "PEACH", "santa.black": "BLACK",
    "phone.body": "BLACK", "phone.screen": "BLUE",
}


def kind_of(piece, members=None):
    return re.sub(r"\d+$", "", piece)          # trailing digits are dropped


def role_color(piece, role, members=None, overrides=None):
    table = dict(BASE_ROLES)
    if overrides:
        table.update(overrides)
    return table[f"{kind_of(piece)}.{role}"]


def load_overrides(path):
    """Palette json: {"kind.role": "COLOUR", ...}; the optional key "_hex" re-defines HEX values of colour names."""
    if not path:
        return None
    table = json.load(open(path))
    COLOR_HEX.update(table.pop("_hex", {}))
    return table
