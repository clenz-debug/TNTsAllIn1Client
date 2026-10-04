"""Builds the TNT 3D Blocks packs for the 1.8.9 mod: the same 3D models as build.py makes for the
Fabric versions, for the blocks that version has, in the form that version reads.

Usage: python legacy.py [--legacy-jar <1.8.9 client jar>] [--new-jar <newest version's client jar>]
       (defaults: the jar mod/1.8.9's build keeps, and Loom's cache for the newest supported version)

Two packs, because a 3D model follows the pixels of the picture it wears and 1.8.9 can show two
sets of pictures: its own, and the newer versions' through the launcher's "New Textures" pack.
"classic" is shaped after the first, "new" after the second - the mod switches on the one that fits
(Blocks3d.java in mod/1.8.9). Both name the same textures; only the shapes differ.

The packs hold nothing but our own model files and go into the mod's jar
(mod/1.8.9/src/main/resources/assets/tntsallin1client/packs/): the launcher's pack bundle does not
reach versions without a mod loader. Rerun after changing models3d.py or this file, then rebuild the
mod.

How it works: models3d.py's generators run again with this version's pictures (their masks, and for
the classic set its own door tables), and what they make is renamed to the model files 1.8.9's
blockstates ask for. Where 1.8.9 puts a block together differently - iron bars and vines are single
models per combination of sides there, not parts a blockstate assembles - the model is put together
here from the same pieces. Not covered: redstone dust (other pictures and models before 1.16).

The items of these blocks are 3D models too (items() below). 1.8.9 has one model per item, for every
place it is shown in, so the "3D items in inventory & hand" switch is the mod's (Items3d.java): each
pack lists the items it changes in assets/tntsallin1client/flat_items.json, and for those the mod
also keeps the model the game would show without this pack.
"""
import copy
import io
import json
import math
import pathlib
import sys
import zipfile

from PIL import Image

import entity_items
import models3d
from build import TIMESTAMP, overlapping_faces, validate

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "mod" / "1.8.9" / "src" / "main" / "resources" / "assets" / "tntsallin1client" / "packs"
LEGACY_JAR = ROOT / "mod" / "1.8.9" / "build" / "minecraft" / "minecraft-1.8.9-named.jar"
NEW_VERSION = "26.3"
NEW_JAR = pathlib.Path.home() / ".gradle" / "caches" / "fabric-loom" / NEW_VERSION / "minecraft-client.jar"

# Resource pack format of 1.6.1 to 1.8.9
PACK_FORMAT = 1
# The items whose look a pack changes, for the mod's "3D items in inventory & hand" switch (Items3d.java)
FLAT_ITEMS_LIST = "assets/tntsallin1client/flat_items.json"
DESCRIPTIONS = {
    "classic": "§6TNT 3D-Blöcke\n§7Eigene 3D-Modelle für TNT's All-In-1 Client",
    "new": "§6TNT 3D-Blöcke (Neue Texturen)\n§7Eigene 3D-Modelle für TNT's All-In-1 Client",
}

# A picture's name today -> its name in 1.8.9 (below textures/blocks/). The same table, read the other
# way, as the launcher's "New Textures" rules (newTexturesRules.ts) - only the pictures our models wear.
TEXTURES = {
    "ladder": "ladder",
    "rail": "rail_normal",
    "rail_corner": "rail_normal_turned",
    "detector_rail": "rail_detector",
    "detector_rail_on": "rail_detector_powered",
    "powered_rail": "rail_golden",
    "powered_rail_on": "rail_golden_powered",
    "stone": "stone",
    "gold_block": "gold_block",
    "iron_block": "iron_block",
    "activator_rail": "rail_activator",
    "iron_bars": "iron_bars",
    "vine": "vine",
    "lily_pad": "waterlily",
    "sugar_cane": "reeds",
    "brown_mushroom": "mushroom_brown",
    "red_mushroom": "mushroom_red",
    "brown_mushroom_block": "mushroom_block_skin_brown",
    "red_mushroom_block": "mushroom_block_skin_red",
    "mushroom_stem": "mushroom_block_skin_stem",
    "mushroom_block_inside": "mushroom_block_inside",
    "bookshelf": "bookshelf",
    "oak_planks": "planks_oak",
    "flower_pot": "flower_pot",
    "dirt": "dirt",
    "oak_trapdoor": "trapdoor",
    "iron_trapdoor": "iron_trapdoor",
}
# wood today -> (its doors' picture names, its doors' model names) in 1.8.9
DOORS = {
    "oak": ("wood", "wooden"),
    "spruce": ("spruce", "spruce"),
    "birch": ("birch", "birch"),
    "jungle": ("jungle", "jungle"),
    "acacia": ("acacia", "acacia"),
    "dark_oak": ("dark_oak", "dark_oak"),
    "iron": ("iron", "iron"),
}
for _wood, (_picture, _) in DOORS.items():
    TEXTURES[f"{_wood}_door_top"] = f"door_{_picture}_upper"
    TEXTURES[f"{_wood}_door_bottom"] = f"door_{_picture}_lower"
TRAPDOORS = {"oak": "wooden", "iron": "iron"}

# The classic door pictures, read by hand like models3d's tables for today's (pixel grids of all 14):
# hinges, latches and handles sit on the same pixels as in today's pictures, and so do the spruce
# door's iron bands, ring and plank gaps - so those entries are taken over as they are. What differs:
# the jungle door's lower hinge sits a row lower, and the iron door has no latch painted. Of the
# sunken panels, the classic dark oak door paints the same two per half as today's (a light line along
# their top and left, a dark one along the right and bottom - own user report: without them the door
# came out flat); the classic oak door paints none and gets the plain shape (a frame, a set-back
# filling, real windows).
CLASSIC_PANELS = ("dark_oak_door_top", "dark_oak_door_bottom")
CLASSIC_FITTINGS_CHANGED = {
    "jungle_door_bottom": models3d.HINGES_BOTTOM,
    "iron_door_top": models3d.HINGES_TOP,
}

VINE_DEPTH = 0.75
VINE_LEAF_DEPTH = 1.5  # the lighter leaf pixels stand out further than the stems, as in models3d.vine
VINE_LIGHT_NEW = 131   # extract_masks.LIGHT["vine"]


class TextureSet:
    """One set of pictures the 1.8.9 game can be showing: "classic" (its own) or "new" (the newest version's)."""

    def __init__(self, name: str, jar: pathlib.Path):
        if not jar.is_file():
            sys.exit(f"No client jar at {jar} - build that mod once, or pass --legacy-jar / --new-jar <path>")
        self.name = name
        self.jar = zipfile.ZipFile(jar)
        self.cache: dict = {}

    def picture(self, texture: str) -> Image.Image:
        """The picture by today's name, 16x16 (first frame)."""
        if texture not in self.cache:
            path = (f"assets/minecraft/textures/blocks/{TEXTURES[texture]}.png" if self.name == "classic"
                    else f"assets/minecraft/textures/block/{texture}.png")
            image = Image.open(io.BytesIO(self.jar.read(path))).convert("RGBA")
            self.cache[texture] = image.crop((0, 0, image.width, image.width)).resize((16, 16), Image.NEAREST)
        return self.cache[texture]

    def mask(self, texture: str, counts=lambda pixel: True) -> list:
        image = self.picture(texture)
        return ["".join("#" if image.getpixel((x, y))[3] > 0 and counts(image.getpixel((x, y))) else "." for x in range(16))
                for y in range(16)]


def brightness(pixel) -> int:
    return max(pixel[:3])


def masks_for(textures: TextureSet) -> dict:
    """What models3d reads through mask_of, for this set of pictures - made the way extract_masks.py does it."""
    masks = {}
    for wood in DOORS:
        for half in ("top", "bottom"):
            masks[f"{wood}_door_{half}"] = " ".join(textures.mask(f"{wood}_door_{half}"))
    for wood in TRAPDOORS:
        masks[f"{wood}_trapdoor"] = " ".join(textures.mask(f"{wood}_trapdoor"))
    masks["lily_pad"] = " ".join(textures.mask("lily_pad"))
    grey = lambda pixel: max(pixel[:3]) - min(pixel[:3]) < 12  # extract_masks.GREY_SPREAD
    masks["rail_corner_metal"] = " ".join(textures.mask("rail_corner", grey))
    masks["rail_corner_wood"] = " ".join(textures.mask("rail_corner", lambda pixel: not grey(pixel)))
    return masks


def runs(flags: list) -> list:
    """(start, end) of every stretch of True in a list."""
    found, start = [], None
    for index, flag in enumerate(list(flags) + [False]):
        if flag and start is None:
            start = index
        elif not flag and start is not None:
            found.append((start, index))
            start = None
    return found


# --- Ladder -----------------------------------------------------------------------------------------
# models3d.ladder's shape - two side rails standing off the wall, the rungs running through them -
# with rails and rungs found in the picture instead of fixed: the classic picture has them a pixel
# off from today's.

def ladder(textures: TextureSet) -> None:
    mask = textures.mask("ladder")
    t = "#texture"
    # A rail: a column solid nearly all the way down. A rung: a row solid across the middle.
    rails = runs([sum(mask[v][u] == "#" for v in range(16)) >= 13 for u in range(16)])
    rungs = runs([mask[v][7] == "#" and mask[v][8] == "#" for v in range(16)])
    elements = []
    for c1, c2 in rails:
        # The picture is seen from the north, so its column c sits at x = 16 - c
        side = models3d.face([c1, 0, c2, 16], t)
        elements.append(models3d.box([16 - c2, 0, 13], [16 - c1, 16, 16], {
            "north": side,
            "south": models3d.face([c1, 0, c2, 16], t, cullface="south"),
            "east": side,
            "west": side,
            "up": models3d.face([c1, 0, c2, 3], t),
            "down": models3d.face([c1, 13, c2, 16], t),
        }))
    for r1, r2 in rungs:
        solid = [mask[v][u] == "#" for v in (r1,) for u in range(16)]
        c1, c2 = next((a, b) for a, b in runs(solid) if a <= 7 < b)
        front = models3d.face([c1, r1, c2, r2], t)
        elements.append(models3d.box([16 - c2, 16 - r2, 14], [16 - c1, 16 - r1, 15], {
            "north": front,
            "south": front,
            "up": models3d.face([c1, r1, c2, r1 + 1], t),
            "down": models3d.face([c1, r2 - 1, c2, r2], t),
            "east": models3d.face([c1, r1, c1 + 1, r2], t),
            "west": models3d.face([c2 - 1, r1, c2, r2], t),
        }))
    models3d.model("block/ladder", {"particle": "block/ladder", "texture": "block/ladder"}, elements)


# --- Iron bars --------------------------------------------------------------------------------------
# models3d.bars' pieces - a pole in the middle, one more rod in each connected half, two thin cross
# bars holding them together - put together per combination of sides, the way 1.8.9 has its bar
# models: one model each for north, north+east, north+south, north+south+east and all four (the
# blockstate turns them; bars connected to nothing show all four, as in that version).

BAR_ROD = [2, 0, 4, 16]
BAR_SIDES = {"bars_n": "n", "bars_ne": "ne", "bars_ns": "ns", "bars_nse": "nse", "bars_nsew": "nsew"}


def bar_arm(side: str) -> list:
    t = "#bars"
    along_z = side in "ns"
    rod_at = {"n": 2, "s": 12, "w": 2, "e": 12}[side]
    low, high = {"n": (0, 7), "s": (9, 16), "w": (0, 7), "e": (9, 16)}[side]

    def placed(a1, a2, y1, y2, across1, across2):
        """A box running a1..a2 along the arm, across1..across2 across it."""
        return ([across1, y1, a1], [across2, y2, a2]) if along_z else ([a1, y1, across1], [a2, y2, across2])

    rod_faces = {name: models3d.face(BAR_ROD, t) for name in ("north", "south", "east", "west")}
    rod_faces["up"] = models3d.face([2, 0, 4, 2], t)
    rod_faces["down"] = models3d.face([2, 14, 4, 16], t)
    parts = [models3d.box(*placed(rod_at, rod_at + 2, 0, 16, 7, 9), rod_faces)]
    for y in (2, 13):
        cross = models3d.face([2, 0, 3, high - low], t, rotation=90)
        end = models3d.face([2, 0, 3, 1], t)
        long_sides = ("east", "west") if along_z else ("north", "south")
        ends = ("north", "south") if along_z else ("east", "west")
        faces = {name: cross for name in long_sides + ("up", "down")}
        faces.update({name: end for name in ends})
        parts.append(models3d.box(*placed(low, high, y, y + 1, 7.5, 8.5), faces))
    return parts


def bars() -> None:
    t = "#bars"
    pole = models3d.box([7, 0, 7], [9, 16, 9], {name: models3d.face([7, 0, 9, 16], t) for name in ("north", "south", "east", "west")}
                        | {"up": models3d.face([7, 7, 9, 9], t), "down": models3d.face([7, 7, 9, 9], t)})
    for name, sides in BAR_SIDES.items():
        elements = [copy.deepcopy(pole)] + [part for side in sides for part in bar_arm(side)]
        models3d.model(f"block/{name}", {"particle": "block/iron_bars", "bars": "block/iron_bars"}, elements)


# --- Vines ------------------------------------------------------------------------------------------
# models3d.vine's shape - the picture a little thick, its lighter leaf pixels standing out further than
# the stems - on every side a vine can cover. 1.8.9 has one model per combination of sides (vine_1 for
# one wall, vine_2 for two next to each other, ...), each naming the sides it covers.

# model -> the sides it covers (as vanilla's models have them; the blockstate turns them)
VINE_MODELS = {
    "vine_1": "s", "vine_2": "ne", "vine_2_opposite": "ew", "vine_3": "esn", "vine_4": "wesn",
    "vine_u": "u", "vine_1u": "us", "vine_2u": "une", "vine_2u_opposite": "uew", "vine_3u": "uesn", "vine_4u": "uwesn",
}
# Where a picture pixel (u across, v down) lands at depth t off the block it grows on - so that the
# picture reads the right way round for someone looking at that side from inside the block's space
VINE_PLACES = {
    "s": lambda u, v, t: (16 - u, 16 - v, 16 - t),
    "n": lambda u, v, t: (u, 16 - v, t),
    "e": lambda u, v, t: (16 - t, 16 - v, u),
    "w": lambda u, v, t: (t, 16 - v, 16 - u),
    "u": lambda u, v, t: (u, 16 - t, v),
}


def vines(textures: TextureSet) -> None:
    image = textures.picture("vine")
    solid = [image.getpixel((u, v)) for v in range(16) for u in range(16) if image.getpixel((u, v))[3] > 0]
    # The classic picture is grey (the game tints it) and darker overall: its lighter half counts as leaves
    light = VINE_LIGHT_NEW if textures.name == "new" else sorted(brightness(pixel) for pixel in solid)[len(solid) // 2]
    mask = textures.mask("vine")

    def thickness_at(u, v):
        return (0.0, VINE_LEAF_DEPTH if brightness(image.getpixel((u, v))) >= light else VINE_DEPTH)

    quads = models3d.slab_quads(models3d.thickness_ranges(mask, thickness_at), lambda u, v: (None, True))
    # The item's vine (items() below) hangs on the north side: its picture faces the inventory slot
    for name, sides in {**VINE_MODELS, VINE_ITEM: "n"}.items():
        elements = []
        for side in sides:
            # The side lying on the block the vine grows on is the slab's t = 0
            elements += models3d.place_slab(quads, VINE_PLACES[side], "#vine", (True, False))
        for element in elements:
            for spec in element["faces"].values():
                spec["tintindex"] = 0
        models3d.model(f"block/{name}", {"particle": "block/vine", "vine": "block/vine"}, elements)


# --- Items ------------------------------------------------------------------------------------------
# models3d.items_3d's item models - the block's 3D model moved into the middle of the item's space -
# with the display settings as 1.8.9 reads them. That version shows an item differently from today's:
#  - In the inventory, a model made of boxes is already turned like a block (and lit); the model's
#    own "gui" entry comes on top of that. So today's entry is taken back by that turn here.
#  - In the hand, on the ground and in an item frame the game treats a model of boxes like a block
#    and a flat picture like an item. The numbers below are the ones 1.8.9's own block and item
#    models have, changed only where a model of boxes is handled differently from a flat picture
#    (dropped: half the size and lifted by its scale; in a frame: half the size, not turned round).

VINE_ITEM = "vine_item"
# The turn and size 1.8.9 gives a model of boxes in the inventory before its "gui" entry
GUI_BLOCK_TURN = (30, 225)
GUI_BLOCK_SCALE = 0.625
HELD_AS_BLOCK = {"rotation": [10, -45, 170], "translation": [0, 1.5, -2.75], "scale": [0.375] * 3}
LEGACY_DISPLAY = {
    "block": {"thirdperson": HELD_AS_BLOCK},
    "flat": {
        "thirdperson": {"rotation": [-90, 0, 0], "translation": [0, 1, -3], "scale": [0.55] * 3},
        "firstperson": {"rotation": [0, -135, 25], "translation": [0, 4, 2], "scale": [1.7] * 3},
        "ground": {"translation": [0, -8, 0], "scale": [2] * 3},
        "fixed": {"rotation": [0, 180, 0], "scale": [2] * 3},
    },
    # Two blocks tall: smaller everywhere, by as much as in today's versions (models3d.DOOR_DISPLAY)
    "door": {
        "thirdperson": {**HELD_AS_BLOCK, "scale": [0.25] * 3},
        "firstperson": {"scale": [0.75] * 3},
        "ground": {"scale": [0.8] * 3},
        "fixed": {"rotation": [0, 90, 0], "scale": [0.8] * 3},
    },
}
# 1.8.9's item -> (our block model, textures a child model would fill in, today's display, how it is
# held[, how much of the slot it fills - the ladder and the vine as in models3d])
ITEM_MODELS = {
    "ladder": ("block/ladder", {}, models3d.FLAT_DISPLAY, "flat", models3d.WALL_ITEM_GUI_FILL),
    "vine": (f"block/{VINE_ITEM}", {}, models3d.FLAT_DISPLAY, "flat", models3d.WALL_ITEM_GUI_FILL),
    "rail": ("block/rail_flat", {"rail": "block/rail"}, models3d.RAIL_DISPLAY, "block"),
    "golden_rail": ("block/powered_rail", {}, models3d.RAIL_DISPLAY, "block"),
    "activator_rail": ("block/rail_flat", {"rail": "block/activator_rail"}, models3d.RAIL_DISPLAY, "block"),
    "detector_rail": ("block/detector_rail", {}, models3d.RAIL_DISPLAY, "block"),
    "iron_bars": ("block/bars_ns", {}, models3d.BLOCK_DISPLAY, "block"),
    "waterlily": ("block/lily_pad", {}, models3d.FLOOR_DISPLAY, "block"),
    "reeds": ("block/sugar_cane", {}, models3d.BLOCK_DISPLAY, "block"),
    "brown_mushroom": ("block/brown_mushroom", {}, models3d.BLOCK_DISPLAY, "block"),
    "red_mushroom": ("block/red_mushroom", {}, models3d.BLOCK_DISPLAY, "block"),
}
# Items that are their block's model in 1.8.9 already, and so are 3D with this pack without an item
# model of ours - listed for the mod's switch like the others
BLOCK_MODEL_ITEMS = ["trapdoor", "iron_trapdoor", "bookshelf"]
# Pressure plates: 1.8.9's item is a plate four pixels thick, its own inventory model. Here it is as
# thin as the plate lying in the world - and as today's item (own user feedback: too thick).
# item -> the picture it wears
PRESSURE_PLATES = {
    "stone_pressure_plate": "stone",
    "wooden_pressure_plate": "oak_planks",
    "light_weighted_pressure_plate": "gold_block",
    "heavy_weighted_pressure_plate": "iron_block",
}
PRESSURE_PLATE_GUI_FILL = 14  # the plate's own width: as big in the slot as the game's

# Items of blocks that are 3D in 1.8.9 already, but a flat picture as an item (as in models3d): the
# item points at the game's block model, only moved into the middle of the slot and scaled like a
# block - no geometry of the game's copied. The block models' bounds (from the 1.8.9 jar; a torch's
# are its stick's - its model is three crossed pictures a block wide): item -> (model, low, high).
VANILLA_3D_ITEMS = {
    "lever": ("lever_off", (5, 0, 4), (11, 11, 12)),
    "tripwire_hook": ("tripwire_hook", (6, 1, 7.9), (10, 9, 16)),
    "repeater": ("repeater_1tick", (0, 0, 0), (16, 7, 16)),
    "comparator": ("comparator_unlit", (0, 0, 0), (16, 7, 16)),
    "torch": ("normal_torch", (7, 0, 7), (9, 10, 9)),
    "redstone_torch": ("lit_redstone_torch", (7, 0, 7), (9, 10, 9)),
    "cauldron": ("cauldron_empty", (0, 0, 0), (16, 16, 16)),
    "hopper": ("hopper_down", (0, 0, 0), (16, 16, 16)),
    "brewing_stand": ("brewing_stand_empty", (1, 0, 1), (15, 14, 15)),
    "flower_pot": ("flower_pot", (5, 0, 5), (11, 6, 11)),
    "cake": ("cake_uneaten", (1, 0, 1), (15, 8, 15)),
    # Drawn through a block model in this version already, although it is an entity
    "item_frame": ("item_frame", (2, 2, 15), (14, 14, 16)),
}
# 1.8.9's names for the sixteen colors of stained glass
GLASS_COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple",
                "blue", "brown", "green", "red", "black"]

# The entity models of 1.8.9 that differ from today's (entity_items.py has the others; numbers from
# that version's own model classes). The boat was five plain boards then; the chest's picture was
# laid out for a model built upside down.
HALF_PI, PI = entity_items.HALF_PI, entity_items.PI
BOAT = [
    {"offset": (0, 4, 0), "rotation": (HALF_PI, 0, 0), "cubes": [((0, 8), (-12, -8, -3), (24, 16, 4), False)]},      # bottom
    {"offset": (-11, 4, 0), "rotation": (0, 3 * HALF_PI, 0), "cubes": [((0, 0), (-10, -7, -1), (20, 6, 2), False)]},
    {"offset": (11, 4, 0), "rotation": (0, HALF_PI, 0), "cubes": [((0, 0), (-10, -7, -1), (20, 6, 2), False)]},
    {"offset": (0, 4, -9), "rotation": (0, PI, 0), "cubes": [((0, 0), (-10, -7, -1), (20, 6, 2), False)]},
    {"offset": (0, 4, 9), "cubes": [((0, 0), (-10, -7, -1), (20, 6, 2), False)]},
]
CHEST = [
    {"offset": (1, 6, 1), "cubes": [((0, 19), (0, 0, 0), (14, 10, 14), False)]},                    # bottom
    {"offset": (1, 7, 15), "inset": 0.01, "cubes": [((0, 0), (0, -5, -14), (14, 5, 14), False)]},  # lid
    {"offset": (8, 7, 15), "cubes": [((0, 0), (-1, -2, -15), (2, 4, 1), False)]},                  # lock
]
# The boat's, the minecart's and the sign's pictures are 64 x 32, and 1.8.9 takes only square pictures
# for models. The mod makes them square as the game reads them, with nothing in the lower half
# (SpriteMixin.java) - so a model counts their pixels on 64 x 64.
ENTITY_PICTURE = (64, 64)
# A minecart's load: item -> (pictures, shape), the shapes as in models3d (None: the chest)
CART_LOADS = {
    "chest_minecart": None,
    "furnace_minecart": {"furnace_front": "blocks/furnace_front_off", "furnace_side": "blocks/furnace_side", "furnace_top": "blocks/furnace_top"},
    "tnt_minecart": {"tnt_side": "blocks/tnt_side", "tnt_top": "blocks/tnt_top", "tnt_bottom": "blocks/tnt_bottom"},
    "command_block_minecart": {"command_front": "blocks/command_block", "command_back": "blocks/command_block",
                               "command_side": "blocks/command_block"},
    "hopper_minecart": {"hopper_outside": "blocks/hopper_outside", "hopper_inside": "blocks/hopper_inside", "hopper_top": "blocks/hopper_top"},
}


def turn(axis: str, degrees: float) -> list:
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    return {"x": [[1, 0, 0], [0, c, -s], [0, s, c]],
            "y": [[c, 0, s], [0, 1, 0], [-s, 0, c]],
            "z": [[c, -s, 0], [s, c, 0], [0, 0, 1]]}[axis]


def combined(*matrices) -> list:
    result = matrices[0]
    for matrix in matrices[1:]:
        result = [[sum(result[i][k] * matrix[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return result


def legacy_gui(gui: dict) -> dict:
    """Today's "gui" entry as 1.8.9 needs it for a model of boxes. Today's game turns the model around
    x, then y, then z, seen face-on; 1.8.9 first gives it a block's turn, then turns it by the entry
    around y, then x, then z."""
    x, y, z = gui["rotation"]
    wanted = combined(turn("x", x), turn("y", y), turn("z", z))
    rest = combined(turn("y", -GUI_BLOCK_TURN[1]), turn("x", -GUI_BLOCK_TURN[0]), wanted)
    rotation = [math.degrees(math.asin(max(-1.0, min(1.0, -rest[1][2])))),
                math.degrees(math.atan2(rest[0][2], rest[2][2])),
                math.degrees(math.atan2(rest[1][0], rest[1][1]))]
    again = combined(turn("y", rotation[1]), turn("x", rotation[0]), turn("z", rotation[2]))
    if any(abs(again[i][j] - rest[i][j]) > 1e-6 for i in range(3) for j in range(3)):
        raise SystemExit(f"No 1.8.9 turn for the gui rotation {gui['rotation']}")
    return {"rotation": [round(angle, 2) + 0.0 for angle in rotation], "translation": [0, 0, 0],
            "scale": [round(gui["scale"][0] / GUI_BLOCK_SCALE, 3)] * 3}


# The item frame hangs flat on its block's back wall, half a block from the middle a held item turns
# around. Held like any block it ended up out of sight (own user report: nothing in the hand) - so in
# the hand it is moved to that middle first. And it is turned in first person: the game holds a block
# a quarter turn to the side of where this flat thing's front would face the player (own user report,
# with a screenshot: seen almost edge-on). item -> its turn in first person.
HELD_BY_ITS_MIDDLE = {"item_frame": [0, 90, 0]}


def way_to_middle(low, high, rotation: list, scale: float) -> list:
    """How far a display entry with that turn and size has to move a model for the middle of its shape
    to land where the middle of its block would - turned, scaled and halved like in vanilla_3d_items."""
    offset = [(l + h) / 2 - 8 for l, h in zip(low, high)]
    turned = combined(turn("y", rotation[1]), turn("x", rotation[0]), turn("z", rotation[2]))
    return [-sum(turned[i][k] * scale / 2 * offset[k] for k in range(3)) for i in range(3)]


def held_by_middle(low, high, first_person_turn: list) -> dict:
    """"firstperson" and "thirdperson" for a block model whose shape is not in the middle of its block:
    the way to the middle, added to the place a block is held at. A block has no "firstperson" entry
    of its own - there it is the way and the turn alone."""
    first = way_to_middle(low, high, first_person_turn, 1)
    third = way_to_middle(low, high, HELD_AS_BLOCK["rotation"], HELD_AS_BLOCK["scale"][0])
    return {
        "firstperson": {"rotation": first_person_turn, "translation": [round(value, 3) + 0.0 for value in first]},
        "thirdperson": {**HELD_AS_BLOCK, "translation": [round(base + value, 3) + 0.0 for base, value in zip(HELD_AS_BLOCK["translation"], third)]},
    }


def vanilla_3d_items() -> dict:
    """Item models on the game's own block models (VANILLA_3D_ITEMS), their middle moved to the slot's
    middle. 1.8.9 moves a model before it turns and scales it, and halves every item model after
    that - so the way to the middle is turned, scaled and halved here."""
    found = {}
    for item, (model, low, high) in VANILLA_3D_ITEMS.items():
        today = models3d.VANILLA_3D_GUI_TURN.get(item, models3d.BLOCK_DISPLAY["gui"]["rotation"])
        longest = max(h - l for l, h in zip(low, high))
        scale = min(models3d.GUI_MAX_SCALE, models3d.BLOCK_DISPLAY["gui"]["scale"][0] * models3d.GUI_FILL / longest)
        gui = legacy_gui({"rotation": today, "scale": [scale] * 3})
        turned = combined(turn("y", gui["rotation"][1]), turn("x", gui["rotation"][0]), turn("z", gui["rotation"][2]))
        offset = [gui["scale"][0] / 2 * ((l + h) / 2 - 8) for l, h in zip(low, high)]
        gui["translation"] = [round(-sum(turned[i][k] * offset[k] for k in range(3)), 3) + 0.0 for i in range(3)]
        display = {**LEGACY_DISPLAY["block"], "gui": gui}
        if item in HELD_BY_ITS_MIDDLE:
            display.update(held_by_middle(low, high, HELD_BY_ITS_MIDDLE[item]))
        found[item] = {"parent": f"block/{model}", "display": display}
    return found


def items(made: dict) -> dict:
    """The item models, by 1.8.9's item names - still with today's picture names (see legacy_model)."""
    found = {}

    def add(name, elements, textures, display, held, fill=models3d.GUI_FILL):
        data = models3d.item_model(elements, textures, display, fill)
        data["display"] = {**LEGACY_DISPLAY[held], "gui": legacy_gui(data["display"]["gui"])}
        found[name] = data

    for name, (block_model, textures, display, held, *fill) in ITEM_MODELS.items():
        block = made[block_model]
        add(name, block["elements"], {**block["textures"], **textures}, display, held, *fill)
    for name, picture in PRESSURE_PLATES.items():
        t = "#texture"
        edge = models3d.face([1, 15, 15, 16], t)
        plate = models3d.box([1, 0, 1], [15, 1, 15], {"up": models3d.face([1, 1, 15, 15], t), "down": models3d.face([1, 1, 15, 15], t),
                                                       "north": edge, "south": edge, "east": edge, "west": edge})
        add(name, [plate], {"particle": f"block/{picture}", "texture": f"block/{picture}"}, models3d.BLOCK_DISPLAY, "block", PRESSURE_PLATE_GUI_FILL)
    # Glass panes: a straight piece of pane, glass on its faces, the pane's edge picture around it
    for color in [None] + GLASS_COLORS:
        name = f"{color}_stained_glass_pane" if color else "glass_pane"
        glass, top = (f"blocks/glass_{color}", f"blocks/glass_pane_top_{color}") if color else ("blocks/glass", "blocks/glass_pane_top")
        edge = models3d.face([7, 0, 9, 16], "#edge")
        pane = models3d.box([7, 0, 0], [9, 16, 16], {"east": models3d.face([0, 0, 16, 16], "#pane"), "west": models3d.face([0, 0, 16, 16], "#pane"),
                                                      "north": edge, "south": edge, "up": edge, "down": edge})
        add(name, [pane], {"particle": glass, "pane": glass, "edge": top}, models3d.BLOCK_DISPLAY, "block")
    # Shaped like their entities (see entity_items.py): boat, sign, armor stand, and the minecarts
    e = entity_items
    planks = "block/oak_planks"
    add("boat", e.model_elements(BOAT, "#texture", ENTITY_PICTURE), {"texture": "entity/boat", "particle": planks}, models3d.BLOCK_DISPLAY, "block")
    add("sign", e.model_elements(e.SIGN, "#texture", ENTITY_PICTURE), {"texture": "entity/sign", "particle": planks},
        models3d.FLAT_DISPLAY, "flat", models3d.SIGN_GUI_FILL)
    add("armor_stand", e.model_elements(e.ARMOR_STAND, "#texture", (64, 64)), {"texture": "entity/armorstand/wood", "particle": planks},
        models3d.FLAT_DISPLAY, "flat", models3d.ARMOR_STAND_GUI_FILL)
    # Minecarts: the cart, and on its floor the load - a block made small, as in models3d
    cart = e.model_elements(e.MINECART, "#cart", ENTITY_PICTURE)
    floor = max(el["to"][1] for el in cart if el["to"][1] - el["from"][1] <= 2.01)  # top of the bottom plate
    middle = [(min(el["from"][i] for el in cart) + max(el["to"][i] for el in cart)) / 2 for i in range(3)]
    cart_textures = {"cart": "entity/minecart", "particle": "items/minecart_normal"}
    add("minecart", cart, cart_textures, models3d.BLOCK_DISPLAY, "block")
    size = 16 * models3d.CART_LOAD_SCALE
    for name, textures in CART_LOADS.items():
        if textures is None:
            textures = {"chest": "entity/chest/normal"}
            elements = e.model_elements(CHEST, "#chest", (64, 64))
        else:
            elements = models3d.CART_LOADS[name][1]()
        low = [min(el["from"][i] for el in elements) for i in range(3)]
        high = [max(el["to"][i] for el in elements) for i in range(3)]
        models3d.scaled(elements, size / max(h - l for l, h in zip(low, high)), low)
        models3d.moved(elements, middle[0] - low[0] - size / 2, floor - low[1], middle[2] - low[2] - size / 2)
        add(name, copy.deepcopy(cart) + elements, {**cart_textures, **textures}, models3d.BLOCK_DISPLAY, "block")
    # Doors: the whole door, both halves on top of each other
    for wood in DOORS:
        bottom, top = made[f"block/{wood}_door_bottom_left"], made[f"block/{wood}_door_top_left"]
        elements = bottom["elements"] + models3d.moved(copy.deepcopy(top["elements"]), dy=16)
        add(f"{wood}_door", elements, {**top["textures"], **bottom["textures"]}, models3d.DOOR_DISPLAY, "door", models3d.DOOR_GUI_FILL)
    return found


# --- Putting a pack together --------------------------------------------------------------------------

def generate(textures: TextureSet) -> tuple:
    """(block models, blockstates, item models) for 1.8.9, shaped after this set of pictures - by the
    names that version uses."""
    saved = {name: getattr(models3d, name) for name in ("MASKS", "MODELS", "BLOCKSTATES", "DOORS", "FITTINGS", "PANELS", "PLANK_GAPS")}
    try:
        models3d.MASKS = masks_for(textures)
        models3d.MODELS = {}
        models3d.BLOCKSTATES = {}
        if textures.name == "classic":
            models3d.PANELS = {picture: models3d.PANELS[picture] for picture in CLASSIC_PANELS}
            models3d.FITTINGS = {**models3d.FITTINGS, **CLASSIC_FITTINGS_CHANGED}

        ladder(textures)
        models3d.rail_flat()
        models3d.rail_raised("block/template_rail_raised_ne", 45)
        models3d.rail_raised("block/template_rail_raised_sw", -45)
        models3d.rail_curved()
        models3d.detector_rails()
        models3d.powered_rails()
        bars()
        vines(textures)
        models3d.lily_pad()
        models3d.sugar_cane()
        models3d.mushrooms()
        models3d.bookshelf()
        models3d.DOORS = list(DOORS)
        models3d.doors()
        models3d.DOORS = list(TRAPDOORS)
        models3d.trapdoors()
        made = models3d.MODELS
        item_models = {name: legacy_model(data) for name, data in items(made).items()}
        item_models.update(vanilla_3d_items())
    finally:
        for name, value in saved.items():
            setattr(models3d, name, value)

    # What models3d calls a model -> what 1.8.9's blockstates and child models call it
    names = {
        "block/ladder": "ladder",
        "block/rail_flat": "rail_flat",
        "block/template_rail_raised_ne": "rail_raised_ne",
        "block/template_rail_raised_sw": "rail_raised_sw",
        "block/rail_curved": "rail_curved",
        "block/detector_rail": "detector_rail_flat",
        "block/detector_rail_raised_ne": "detector_rail_raised_ne",
        "block/detector_rail_raised_sw": "detector_rail_raised_sw",
        "block/detector_rail_on": "detector_rail_powered_flat",
        "block/detector_rail_on_raised_ne": "detector_rail_powered_raised_ne",
        "block/detector_rail_on_raised_sw": "detector_rail_powered_raised_sw",
        "block/powered_rail": "golden_rail_flat",
        "block/powered_rail_raised_ne": "golden_rail_raised_ne",
        "block/powered_rail_raised_sw": "golden_rail_raised_sw",
        "block/powered_rail_on": "golden_rail_active_flat",
        "block/powered_rail_on_raised_ne": "golden_rail_active_raised_ne",
        "block/powered_rail_on_raised_sw": "golden_rail_active_raised_sw",
        "block/lily_pad": "waterlily",
        "block/sugar_cane": "reeds",
        "block/bookshelf": "bookshelf",
        "block/potted_brown_mushroom": "flower_pot_mushroom_brown",
        "block/potted_red_mushroom": "flower_pot_mushroom_red",
    }
    names.update({f"block/{name}": name for name in list(BAR_SIDES) + list(VINE_MODELS)})
    for mushroom in ("brown_mushroom", "red_mushroom"):
        for suffix in models3d.MUSHROOM_TURNS:
            names[f"block/{mushroom}{suffix}"] = f"{mushroom}{suffix}"
    for wood, (_, model_name) in DOORS.items():
        for half in ("top", "bottom"):
            # A closed left-hinged door shows the picture as it is, a right-hinged one mirrored - the two
            # models 1.8.9 has per half; its blockstate turns them for open doors
            names[f"block/{wood}_door_{half}_left"] = f"{model_name}_door_{half}"
            names[f"block/{wood}_door_{half}_right"] = f"{model_name}_door_{half}_rh"
    for wood, model_name in TRAPDOORS.items():
        for state in ("bottom", "top", "open"):
            names[f"block/{wood}_trapdoor_{state}"] = f"{model_name}_trapdoor_{state}"

    models = {legacy: legacy_model(made[modern]) for modern, legacy in names.items()}
    blockstates = {
        mushroom: {"variants": {"normal": [
            {"model": f"{mushroom}{suffix}", **({"y": angle} if angle else {})}
            for suffix in models3d.MUSHROOM_TURNS for angle in models3d.QUARTER_TURN_ANGLES]}}
        for mushroom in ("brown_mushroom", "red_mushroom")}
    return models, blockstates, item_models


def legacy_texture(value: str) -> str:
    # A reference to another entry, or a picture by its 1.8.9 name already
    if value.startswith(("#", "blocks/", "items/", "entity/")):
        return value
    name = value.split(":", 1)[-1]
    return "blocks/" + TEXTURES[name.removeprefix("block/")]


def legacy_model(data: dict) -> dict:
    """The model as 1.8.9 reads it: its pictures by that version's names, and without the parents that
    only carry display settings there (block/block, block/thin_block do not exist in 1.8.9, where the
    item models bring their own)."""
    data = copy.deepcopy(data)
    data.pop("parent", None)
    textures = {key: legacy_texture(value) for key, value in data["textures"].items()}
    # 1.8.9 does not follow one entry of a model pointing at another entry of the same model
    # ("particle": "#bottom" next to "bottom": ...) - it warns of an "upward reference" and shows the
    # missing texture, as the doors' breaking particles did. Such an entry gets the picture itself.
    data["textures"] = {key: textures.get(value[1:], value) if value.startswith("#") else value for key, value in textures.items()}
    for element in data["elements"]:
        for spec in element["faces"].values():
            spec["uv"] = inset(spec["uv"])
    return data


# How far a face's picture region is pulled in from its edges, in pixels of the picture.
UV_INSET = 0.02


def inset(uv: list) -> list:
    """The region a hair smaller on every side. Today's game does this by itself; 1.8.9 takes the region
    exactly as written, and at a face's very edge its rounding can land on the pixel next door. Next to
    a see-through pixel - around every door window - that left a line one screen pixel wide to look
    through, along the whole edge (own user report)."""
    u1, v1, u2, v2 = uv

    def pulled(a: float, b: float) -> tuple:
        if a == b:
            return a, b
        step = UV_INSET if a < b else -UV_INSET
        return round(a + step, 4), round(b - step, 4)

    (u1, u2), (v1, v2) = pulled(u1, u2), pulled(v1, v2)
    return [u1, v1, u2, v2]


def see_through_faces(name: str, data: dict, textures: TextureSet) -> list:
    """Faces that show a part of their picture with nothing in it - a hole in the model. Shapes cut from
    a mask can't have any; the hand-placed ones (rails, bars, sugar cane, bookshelf) name fixed picture
    regions, which have to be solid in both sets of pictures."""
    by_old_name = {old: new for new, old in TEXTURES.items()}
    problems = []
    for index, element in enumerate(data["elements"]):
        for face_name, spec in element["faces"].items():
            reference = data["textures"].get(spec["texture"][1:], "")
            if not reference.startswith("blocks/"):
                continue  # a template's picture comes from the child models
            image = textures.picture(by_old_name[reference.removeprefix("blocks/")])
            u1, v1, u2, v2 = [round(value) for value in spec["uv"]]  # as written, before the inset
            if abs(u2 - u1) == 16 and abs(v2 - v1) == 16:
                continue  # the whole picture, laid out flat as the game has it (the layer under a rail's 3D parts)
            columns = range(int(min(u1, u2)), max(int(min(u1, u2)) + 1, int(-(-max(u1, u2) // 1))))
            rows = range(int(min(v1, v2)), max(int(min(v1, v2)) + 1, int(-(-max(v1, v2) // 1))))
            pixels = [image.getpixel((min(u, 15), min(v, 15)))[3] for u in columns for v in rows]
            empty = sum(1 for alpha in pixels if alpha == 0)
            if empty:
                problems.append(f"{name} element {index} {face_name}: {empty} of {len(pixels)} pixels of {reference} {spec['uv']} are empty")
    return problems


def write(zf: zipfile.ZipFile, name: str, data: str) -> None:
    info = zipfile.ZipInfo(name, date_time=TIMESTAMP)
    info.compress_type = zipfile.ZIP_DEFLATED
    zf.writestr(info, data)


def build(sets: list, out_dir: pathlib.Path) -> list:
    out_dir.mkdir(parents=True, exist_ok=True)
    written = []
    for textures in sets:
        models, blockstates, item_models = generate(textures)
        problems = [problem for name, data in item_models.items() for problem in validate(f"item/{name}", data)]
        for name, data in models.items():
            problems += validate(name, data)
            # A vine model covering several sides has them meet in the block's corners; that is how the
            # Fabric versions' vines are assembled by their blockstate too
            if name not in VINE_MODELS or len(VINE_MODELS[name]) == 1:
                problems += overlapping_faces(name, data)
        if problems:
            raise SystemExit(f"Invalid models ({textures.name}):\n" + "\n".join(problems))
        for name, data in models.items():
            for warning in see_through_faces(name, data, textures):
                print(f"  note ({textures.name}): {warning}")
        path = out_dir / f"TNT-3D-Blocks-{textures.name}.zip"
        with zipfile.ZipFile(path, "w") as zf:
            pack = {"pack": {"pack_format": PACK_FORMAT, "description": DESCRIPTIONS[textures.name]}}
            write(zf, "pack.mcmeta", json.dumps(pack, indent=2, ensure_ascii=False))
            for name in sorted(models):
                write(zf, f"assets/minecraft/models/block/{name}.json", json.dumps(models[name], indent=1))
            for block in sorted(blockstates):
                write(zf, f"assets/minecraft/blockstates/{block}.json", json.dumps(blockstates[block], indent=1))
            for name in sorted(item_models):
                write(zf, f"assets/minecraft/models/item/{name}.json", json.dumps(item_models[name], indent=1))
            write(zf, FLAT_ITEMS_LIST, json.dumps({"items": sorted(list(item_models) + BLOCK_MODEL_ITEMS)}, indent=1))
        elements = sum(len(data.get("elements", [])) for data in list(models.values()) + list(item_models.values()))
        print(f"{path.name}: {len(models)} block models, {len(item_models)} item models, {elements} elements, {path.stat().st_size // 1024} KB")
        written.append(path)
    return written


def texture_sets(argv: list) -> list:
    jars = {"classic": LEGACY_JAR, "new": NEW_JAR}
    args = iter(argv)
    for arg in args:
        if arg == "--legacy-jar":
            jars["classic"] = pathlib.Path(next(args))
        elif arg == "--new-jar":
            jars["new"] = pathlib.Path(next(args))
        else:
            sys.exit(f"Unknown argument {arg}")
    return [TextureSet(name, jar) for name, jar in jars.items()]


if __name__ == "__main__":
    build(texture_sets(sys.argv[1:]), OUT)
