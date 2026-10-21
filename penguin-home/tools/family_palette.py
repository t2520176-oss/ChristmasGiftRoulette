"""Role -> filament colour table of PENGUIN HOME (shared by the planner, the legend and Blender).

Four filament colours are used (a bed holds at most 4: AMS / MMU); BLACK is only there for your own overrides:
   WHITE   snow, ice blocks, text                   BLUE    ice band, snow crystal, arch trim
   GREEN   trees                                    ORANGE  sign board
Change a colour here (or pass --palette my.json, optionally with "_hex") and the beds are re-planned.
"""
import json
import re

COLOR_HEX = {"WHITE": "#F5F7FA", "BLUE": "#8CC4EA", "GREEN": "#2F7048", "BLACK": "#20232B", "ORANGE": "#E58A2E"}
ORDER = ["WHITE", "BLUE", "GREEN", "BLACK", "ORANGE"]

# "piece-kind.role" -> colour
BASE_ROLES = {
    "ground.snow": "WHITE",
    "body.ice": "WHITE", "body.belt": "BLUE",
    "lid.ice": "WHITE", "lid.flake": "BLUE",
    "trim.ring": "BLUE",
    "sign.wood": "ORANGE", "sign.text": "WHITE", "sign.snow": "WHITE",
    "tree.green": "GREEN", "tree.snow": "WHITE",
    "tealight.cup": "WHITE", "tealight.flame": "ORANGE",
}


def kind_of(piece, members=None):
    return re.sub(r"\d+$", "", piece)          # tree0 -> tree, ground -> ground


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
