"""Role -> filament colour table of the Christmas family ornament (shared by the planner and Blender).

Eight filament colours are used in total (the eight colour options of the product):
BLUE, YELLOW, WHITE, BROWN, GREEN, RED, BLACK, PINK.  A bed can hold at most 4 of them (AMS / MMU),
so the planner puts the pieces into as few beds as possible and starts a new bed when a 5th colour
would be needed.  Change a colour here (or pass --palette my.json) and the beds are re-planned.
"""
import json

COLOR_HEX = {
    "BLUE": "#24407A", "YELLOW": "#F2C230", "WHITE": "#F2F2EC", "BROWN": "#7A4A2B",
    "GREEN": "#1F6B3A", "RED": "#C8202F", "BLACK": "#1E1E22", "PINK": "#F2B8A8",
}
ORDER = ["BLUE", "YELLOW", "WHITE", "BROWN", "GREEN", "RED", "BLACK", "PINK"]

# "piece-kind.role" -> colour
BASE_ROLES = {
    "sky.panel": "BLUE", "sky.stars": "YELLOW",
    "tree.green": "GREEN", "tree.snow": "WHITE", "tree.star": "RED",
    "house.wall": "BROWN", "house.snow": "WHITE", "house.window": "YELLOW",
    "base.wood": "BROWN", "base.snow": "WHITE",
    "plaque.plate": "YELLOW", "plaque.text": "BROWN",
    "holly.leaf": "GREEN", "holly.berry": "RED",
    "lantern.frame": "BLACK", "lantern.glass": "WHITE",
    "tag.plate": "YELLOW", "tag.text": "BROWN",
    # figures, by member kind
    "boy.jacket": "WHITE", "boy.scarf": "RED", "boy.hat": "RED", "boy.pom": "WHITE", "boy.face": "PINK", "boy.eyes": "BLACK",
    "girl.dress": "RED", "girl.collar": "WHITE", "girl.hair": "BLACK", "girl.bow": "RED", "girl.face": "PINK", "girl.eyes": "BLACK",
    "snowman.body": "WHITE", "snowman.hat": "BLACK", "snowman.band": "RED", "snowman.scarf": "RED", "snowman.nose": "RED",
    "snowman.eyes": "BLACK",
    "dog.fur": "YELLOW", "dog.ears": "BROWN", "dog.scarf": "WHITE", "dog.eyes": "BROWN",
    "cat.fur": "BLACK", "cat.white": "WHITE", "cat.red": "RED",
    "reindeer.fur": "BROWN", "reindeer.antlers": "YELLOW", "reindeer.scarf": "WHITE", "reindeer.nose": "RED",
    "reindeer.eyes": "BLACK",
}


def kind_of(piece, members):
    for pre in ("tree", "holly", "lantern", "tag"):
        if piece.startswith(pre):
            return pre
    if piece.startswith("fig"):
        return members[int(piece[3:])]
    return piece


def role_color(piece, role, members, overrides=None):
    key = f"{kind_of(piece, members)}.{role}"
    table = dict(BASE_ROLES)
    if overrides:
        table.update(overrides)
    return table[key]


def load_overrides(path):
    return json.load(open(path)) if path else None
