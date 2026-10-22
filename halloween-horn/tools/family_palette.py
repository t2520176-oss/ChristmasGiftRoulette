"""Role -> filament colour table of the HALLOWEEN PUMPKIN SOUND AMPLIFIER (shared by the planner, the legend and Blender).

Two filament colours are used:  ORANGE (the pumpkin)  and  GREEN (stem and leaves).
Change a colour here (or pass --palette my.json, optionally with "_hex") and the bed is re-planned.
"""
import json
import re

COLOR_HEX = {"ORANGE": "#E8751A", "GREEN": "#4F8F3A", "BLACK": "#15171C", "BLUE": "#6FA8FF"}
ORDER = ["ORANGE", "GREEN", "BLACK", "BLUE"]

# "piece-kind.role" -> colour
BASE_ROLES = {
    "body.shell": "ORANGE", "face.shell": "ORANGE", "garnish.green": "GREEN",
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
