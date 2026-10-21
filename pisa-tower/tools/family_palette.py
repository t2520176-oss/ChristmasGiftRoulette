"""Role -> filament colour table of the PISA TOWER lamp (shared by the planner, the legend and Blender).

Four filament colours are used (a bed holds at most 4: AMS / MMU):
   IVORY   plinth, tower, belfry, flag (white stripe)     GRAY    the cornices between the floors
   GREEN   lawn, trees, bushes, flag (green stripe)       RED     flag (red stripe)
Change a colour here (or pass --palette my.json, optionally with "_hex") and the beds are re-planned.
"""
import json
import re

COLOR_HEX = {"IVORY": "#EFE6D2", "GRAY": "#8E9098", "GREEN": "#2F7048", "RED": "#C8383E"}
ORDER = ["IVORY", "GRAY", "GREEN", "RED"]

# "piece-kind.role" -> colour
BASE_ROLES = {
    "base.stone": "IVORY", "base.green": "GREEN",
    "tower.stone": "IVORY", "tower.band": "GRAY",
    "top.stone": "IVORY", "top.band": "GRAY",
    "flag.green": "GREEN", "flag.white": "IVORY", "flag.red": "RED",
    "tealight.cup": "IVORY", "tealight.flame": "RED",
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
