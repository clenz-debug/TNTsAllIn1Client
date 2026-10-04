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
here from the same pieces. Not covered: redstone dust (other pictures and models before 1.16) and
the 3D items (every item stays the flat picture it is in this version).
"""
import copy
import io
import json
import pathlib
import sys
import zipfile

from PIL import Image

import models3d
from build import TIMESTAMP, overlapping_faces, validate

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "mod" / "1.8.9" / "src" / "main" / "resources" / "assets" / "tntsallin1client" / "packs"
LEGACY_JAR = ROOT / "mod" / "1.8.9" / "build" / "minecraft" / "minecraft-1.8.9-named.jar"
NEW_VERSION = "26.3"
NEW_JAR = pathlib.Path.home() / ".gradle" / "caches" / "fabric-loom" / NEW_VERSION / "minecraft-client.jar"

# Resource pack format of 1.6.1 to 1.8.9
PACK_FORMAT = 1
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
    for name, sides in VINE_MODELS.items():
        elements = []
        for side in sides:
            # The side lying on the block the vine grows on is the slab's t = 0
            elements += models3d.place_slab(quads, VINE_PLACES[side], "#vine", (True, False))
        for element in elements:
            for spec in element["faces"].values():
                spec["tintindex"] = 0
        models3d.model(f"block/{name}", {"particle": "block/vine", "vine": "block/vine"}, elements)


# --- Putting a pack together --------------------------------------------------------------------------

def generate(textures: TextureSet) -> tuple:
    """(models, blockstates) for 1.8.9, shaped after this set of pictures - by the names that version uses."""
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
    return models, blockstates


def legacy_texture(value: str) -> str:
    if value.startswith("#"):
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
        models, blockstates = generate(textures)
        problems = []
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
        elements = sum(len(data["elements"]) for data in models.values())
        print(f"{path.name}: {len(models)} models, {elements} elements, {path.stat().st_size // 1024} KB")
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
