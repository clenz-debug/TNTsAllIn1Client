"""Builds the TNT 3D Blocks pack for the Fabric versions before item definitions (1.21.4) - so far
1.14.4 and 1.21.1: the same 3D models as build.py makes for the newer ones, for the blocks that version
has, under the names it asks for.

Usage: python early.py [output folder] [--jar <version>=<client jar>] [--new-jar <newest version's client jar>]
       (default output: dist/ next to this file; default jars: Loom's cache)

Writes TNT-3D-Blocks-<version>.zip, and for the versions in BUSHES_APART the bushes as a pack of
their own (TNT-3D-Bushes-<version>.zip), as build.py does. These versions are past "the flattening" (blocks, pictures and
most model files already have today's names), so nearly everything is models3d.py's output as it is.
What differs, and what this file does about it:

- Blocks that came later (chains, amethyst, dripstone, copper, ...) are left out: a model stays only
  if every picture it wears and the model it builds on exist in that version's client jar.
- A door is four models in 1.14.4 (bottom and top, each as it is and mirrored for the other hinge
  side; the blockstate turns them for an open door) where today's versions have sixteen - the two
  closed ones per half are taken under those names. 1.21.1 has today's sixteen.
- The chain block and its picture were just "chain" before there were copper chains.
- Where today's versions share one template between several blocks and build.py replaces the
  template (bars, lanterns), these versions have the block's models stand alone: each one is put
  together from the template and the pictures today's version of that model names.
- A vine is one model per combination of sides in 1.14.4, not parts a blockstate assembles: built
  from the same piece by legacy.py's code, which makes the same models for 1.8.9. Where the game's
  own blockstate already assembles it from parts (1.21.1), ours is used as it is.
- No item definitions: an item's model is just models/item/<name>.json. No atlas file in 1.14.4
  either - a picture a model names is put on the block atlas without being told; versions that have
  the file get build.py's, for the entity pictures the entity-shaped items wear.

The items of these blocks are 3D models too. These versions have one model per item, for every place
it is shown in, so the "3D items in inventory & hand" switch is the mod's (Items3d.java in
mod/1.14.4): the pack lists the items it changes in assets/tntsallin1client/flat_items.json and
carries, in our own namespace, a copy of the model the game would show for each without this pack
(read from the client jar: the model and everything it builds on), which the mod shows in those
places while the switch is off. The same file names the items that are lit from the front in the
inventory (the campfire, whose upright fire looks dark in the side light a 3D item gets there) -
today's versions have a model setting for that, these don't - and the items tinted like grass (the
sugar cane), which today's versions say in the item definition.
"""
import copy
import json
import pathlib
import re
import sys
import zipfile

import entity_items
import legacy
import models3d
from build import BUSHES_DESCRIPTION, DESCRIPTION, TIMESTAMP, for_version, overlapping_faces, validate

PACK_FORMATS = {"1.14.4": 4, "1.21.1": 34}
# Versions whose mod offers the bushes as a switch of their own, like the newest ones
BUSHES_APART = {"1.21.1"}
LOOM = pathlib.Path.home() / ".gradle" / "caches" / "fabric-loom"
NEW_VERSION = "26.3"

NAMESPACE = "tntsallin1client"
FLAT_ITEMS_LIST = f"assets/{NAMESPACE}/flat_items.json"
FLAT_FOLDER = "flat/"
MODELS_DIR = "assets/minecraft/models/"

# Blocks build.py gives a blockstate of its own, which here keep the game's: the vine's picks one
# model per combination of sides in these versions (see vine_models).
GAME_BLOCKSTATES = {"vine"}

# A model's name in these versions -> its name today, where that changed
RENAMED_SINCE = {"block/hanging_lantern": "block/lantern_hanging", "block/chain": "block/iron_chain"}
# The same for item models (today's name -> the name in these versions) and for pictures
ITEM_MODELS_BEFORE = {"item/iron_chain": "item/chain"}
PICTURES_BEFORE = {"block/iron_chain": "block/chain"}
ATLAS_FILE = "assets/minecraft/atlases/blocks.json"

DOOR = re.compile(r"block/(\w+_door)_(top|bottom)_(left|right)(_open)?")
# A closed left-hinged door shows the picture as it is, a right-hinged one mirrored
DOOR_SUFFIX = {"left": "", "right": "_hinge"}


def plain(reference: str) -> str:
    return reference.split(":", 1)[-1]


class Jar:
    def __init__(self, path: pathlib.Path):
        if not path.is_file():
            sys.exit(f"No client jar at {path} - build that mod once, or pass --jar / --new-jar")
        self.zip = zipfile.ZipFile(path)
        self.names = set(self.zip.namelist())

    def has_model(self, name: str) -> bool:
        return f"{MODELS_DIR}{name}.json" in self.names

    def model(self, name: str) -> dict:
        return json.loads(self.zip.read(f"{MODELS_DIR}{name}.json"))

    def has_picture(self, reference: str) -> bool:
        return f"assets/minecraft/textures/{plain(reference)}.png" in self.names

    def blockstate_models(self) -> set:
        found = set()

        def walk(node):
            if isinstance(node, dict):
                if isinstance(node.get("model"), str):
                    found.add(plain(node["model"]))
                for value in node.values():
                    walk(value)
            elif isinstance(node, list):
                for value in node:
                    walk(value)

        for name in self.names:
            if name.startswith("assets/minecraft/blockstates/") and name.endswith(".json"):
                walk(json.loads(self.zip.read(name)))
        return found

    def has_blockstate(self, block: str) -> bool:
        return f"assets/minecraft/blockstates/{block}.json" in self.names

    def assembles_vine(self) -> bool:
        """Whether the vine's blockstate puts it together from parts, as today's does."""
        return "multipart" in json.loads(self.zip.read("assets/minecraft/blockstates/vine.json"))


def vine_models(jar_path: pathlib.Path) -> dict:
    """The vine, one model per combination of sides - legacy.py's, shaped after this version's picture."""
    saved = models3d.MODELS
    try:
        models3d.MODELS = {}
        legacy.vines(legacy.TextureSet("new", jar_path))
        made = models3d.MODELS
    finally:
        models3d.MODELS = saved
    return {path: data for path, data in made.items() if path != f"block/{legacy.VINE_ITEM}"}


def candidates(pack_format: int, old: Jar, new: Jar, jar_path: pathlib.Path) -> dict:
    """Every model that might go into the pack, by the name this version knows it under."""
    made = {}
    for path, data in models3d.MODELS.items():
        door = DOOR.fullmatch(path)
        if door and not old.has_model(f"block/{door.group(1)}_bottom_left"):
            if door.group(4):
                continue  # an open door is the closed one turned
            path = f"block/{door.group(1)}_{door.group(2)}{DOOR_SUFFIX[door.group(3)]}"
        before = ITEM_MODELS_BEFORE.get(path)
        if before and old.has_model(before) and not old.has_model(path):
            path = before
        made[path] = for_version(data, pack_format)

    # A block's own model where today's is a child of a template build.py replaces
    for name in sorted(old.blockstate_models()):
        today = RENAMED_SINCE.get(name, name)
        if name in made or not new.has_model(today):
            continue
        child = new.model(today)
        parent = plain(child.get("parent", ""))
        if parent in models3d.MODELS and not old.has_model(parent):
            template = for_version(models3d.MODELS[parent], pack_format)
            textures = {key: plain(value["sprite"] if isinstance(value, dict) else value) for key, value in child.get("textures", {}).items()}
            made[name] = {**copy.deepcopy(template), "textures": {**template.get("textures", {}), **textures}}

    if not old.assembles_vine():
        made.update(vine_models(jar_path))
    return {path: without_own_references(pictures_of(data, old)) for path, data in made.items()}


def pictures_of(data: dict, old: Jar) -> dict:
    """A picture this version has under an earlier name is asked for by that name."""
    textures = data.get("textures")
    if not textures or not any(plain(value) in PICTURES_BEFORE for value in textures.values()):
        return data

    def before(value: str) -> str:
        earlier = PICTURES_BEFORE.get(plain(value))
        return earlier if earlier and old.has_picture(earlier) and not old.has_picture(value) else value

    return {**data, "textures": {key: before(value) for key, value in textures.items()}}


def without_own_references(data: dict) -> dict:
    """These versions do not follow one entry of a model pointing at another entry of the same model
    ("particle": "#bottom" next to "bottom": ...): the breaking particles of doors, bars and lanterns
    showed the missing texture (own user report - as 1.8.9 did, see legacy.py). Such an entry gets the
    picture itself; one that points at an entry a child model brings stays."""
    textures = data.get("textures")
    if not textures:
        return data
    return {**data, "textures": {key: textures.get(value[1:], value) if value.startswith("#") else value
                                 for key, value in textures.items()}}


def fitting(made: dict, old: Jar) -> dict:
    """What of it this version can show: its pictures and the model it builds on have to be there."""
    kept = dict(made)
    while True:
        dropped = [
            path for path, data in kept.items()
            if any(not value.startswith("#") and not old.has_picture(value) for value in data.get("textures", {}).values())
            or ("parent" in data and plain(data["parent"]) not in kept and not old.has_model(plain(data["parent"])))
        ]
        if not dropped:
            return kept
        for path in dropped:
            del kept[path]


def in_use(kept: dict, blockstates: dict, old: Jar) -> dict:
    """Without the models nothing in this version asks for: it replaces one of the game's, a
    blockstate of ours names it, or a model that stays builds on it."""
    named = {plain(variant["model"]) for state in blockstates.values() for variants in state.get("variants", {}).values()
             for variant in (variants if isinstance(variants, list) else [variants])}
    used = {path for path in kept if old.has_model(path) or path in named}
    while True:
        more = {plain(kept[path]["parent"]) for path in used if "parent" in kept[path]} & set(kept) - used
        if not more:
            return {path: kept[path] for path in sorted(used)}
        used |= more


def flat_copies(models: dict, old: Jar) -> tuple:
    """(items whose look the pack changes, the models the game shows for them without it - under our
    own names, where the pack's files of the same names can't reach them)."""
    def chain(name: str) -> list:
        found = []
        while name and not name.startswith("builtin/") and old.has_model(name):
            found.append(name)
            name = plain(old.model(name).get("parent", ""))
        return found

    def flat_name(name: str) -> str:
        folder, _, rest = name.partition("/")
        return f"{folder}/{FLAT_FOLDER}{rest}"

    items, copies = [], {}
    for file in sorted(old.names):
        if not file.startswith(MODELS_DIR + "item/") or not file.endswith(".json"):
            continue
        item = file[len(MODELS_DIR + "item/"):-len(".json")]
        models_of_item = chain(f"item/{item}")
        if not any(name in models for name in models_of_item):
            continue
        items.append(item)
        for name in models_of_item:
            data = old.model(name)
            parent = plain(data.get("parent", ""))
            if parent and not parent.startswith("builtin/"):
                data["parent"] = f"{NAMESPACE}:{flat_name(parent)}"
            copies[flat_name(name)] = data
    return items, copies


def write(zf: zipfile.ZipFile, name: str, data: str) -> None:
    info = zipfile.ZipInfo(name, date_time=TIMESTAMP)
    info.compress_type = zipfile.ZIP_DEFLATED
    zf.writestr(info, data)


def build(version: str, pack_format: int, jar_path: pathlib.Path, new_jar_path: pathlib.Path, out_dir: pathlib.Path) -> pathlib.Path:
    old, new = Jar(jar_path), Jar(new_jar_path)
    made = candidates(pack_format, old, new, jar_path)
    kept = fitting(made, old)
    game_blockstates = set() if old.assembles_vine() else GAME_BLOCKSTATES
    blockstates = {block: state for block, state in models3d.BLOCKSTATES.items()
                   if old.has_blockstate(block) and block not in game_blockstates
                   and all(plain(variant["model"]) in kept for variants in state.get("variants", {}).values()
                           for variant in (variants if isinstance(variants, list) else [variants]))}
    models = in_use(kept, blockstates, old)

    problems = []
    for path, data in models.items():
        problems += validate(path, data)
        # A vine model covering several sides has them meet in the block's corners, as legacy.py notes
        if not (path.startswith("block/vine_") and len(legacy.VINE_MODELS.get(path.removeprefix("block/"), "")) > 1):
            problems += overlapping_faces(path, data)
    if problems:
        raise SystemExit(f"Invalid models ({version}):\n" + "\n".join(problems))

    items, copies = flat_copies(models, old)
    out_dir.mkdir(parents=True, exist_ok=True)
    bushes = {name: models.pop(name) for name in sorted(models3d.BUSH_MODELS & set(models))} if version in BUSHES_APART else {}
    if bushes:
        bushes_path = out_dir / f"TNT-3D-Bushes-{version}.zip"
        with zipfile.ZipFile(bushes_path, "w") as zf:
            pack = {"pack": {"pack_format": pack_format, "description": BUSHES_DESCRIPTION}}
            write(zf, "pack.mcmeta", json.dumps(pack, indent=2, ensure_ascii=False))
            for name in sorted(bushes):
                write(zf, f"{MODELS_DIR}{name}.json", json.dumps(bushes[name], indent=1))
        print(f"{bushes_path.name}: {len(bushes)} models, {bushes_path.stat().st_size // 1024} KB")
    path = out_dir / f"TNT-3D-Blocks-{version}.zip"
    with zipfile.ZipFile(path, "w") as zf:
        pack = {"pack": {"pack_format": pack_format, "description": DESCRIPTION}}
        write(zf, "pack.mcmeta", json.dumps(pack, indent=2, ensure_ascii=False))
        for name in sorted(models):
            write(zf, f"{MODELS_DIR}{name}.json", json.dumps(models[name], indent=1))
        if ATLAS_FILE in old.names:
            # Entity textures for the entity-shaped items, added to the block texture atlas
            write(zf, ATLAS_FILE, json.dumps(entity_items.atlas_sources(pack_format), indent=1))
        for block in sorted(blockstates):
            write(zf, f"assets/minecraft/blockstates/{block}.json", json.dumps(blockstates[block], indent=1))
        for name in sorted(copies):
            write(zf, f"assets/{NAMESPACE}/models/{name}.json", json.dumps(copies[name], indent=1))
        # Items build.py has lit from the front in the inventory ("gui_light", which these versions
        # don't know): named here, for the mod to draw them the way the game draws its flat items
        front_lit = sorted(name.removeprefix("item/") for name, data in models.items()
                           if name.startswith("item/") and data.get("gui_light") == "front")
        # Items build.py tints like grass through their item definition (the sugar cane, whose 3D item
        # wears the block's picture - pale without the tint the placed block gets): [temperature, downfall]
        grass_tinted = {
            item: [tint["temperature"], tint["downfall"]]
            for item, definition in sorted(models3d.ITEMS.items()) if f"item/{item}" in models
            for tint in definition["model"].get("tints", []) if tint["type"] == "minecraft:grass"}
        write(zf, FLAT_ITEMS_LIST, json.dumps({"items": items, "front_lit": front_lit, "grass_tinted": grass_tinted}, indent=1))
    blocks = sum(1 for name in models if name.startswith("block/"))
    print(f"{path.name}: {blocks} block models, {len(models) - blocks} item models, {len(blockstates)} blockstates, "
          f"{len(items)} items with a flat copy, {path.stat().st_size // 1024} KB")
    return path


def main(argv: list) -> None:
    jars = {}
    new_jar = LOOM / NEW_VERSION / "minecraft-client.jar"
    target = pathlib.Path(__file__).parent / "dist"
    args = iter(argv)
    for arg in args:
        if arg == "--jar":
            version, _, jar = next(args).partition("=")
            jars[version] = pathlib.Path(jar)
        elif arg == "--new-jar":
            new_jar = pathlib.Path(next(args))
        else:
            target = pathlib.Path(arg)
    for version, pack_format in PACK_FORMATS.items():
        build(version, pack_format, jars.get(version, LOOM / version / "minecraft-client.jar"), new_jar, target)


if __name__ == "__main__":
    main(sys.argv[1:])
