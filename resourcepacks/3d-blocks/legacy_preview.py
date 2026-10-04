"""Preview renderer for the 1.8.9 packs (legacy.py): small scenes drawn once with that version's own
models and once with ours, side by side, for both sets of pictures - so a model can be judged before
it is loaded in the game. The drawing itself is preview.py's; see there for what it does not show
(a blockstate's "uvlock", faces lying exactly on top of each other).

Usage: python legacy_preview.py [output folder] [scene ...] [--legacy-jar <jar>] [--new-jar <jar>]
       (default output: previews-legacy/ next to this file)
"""
import io
import pathlib
import sys

import numpy as np
from PIL import Image

import legacy
import preview


class LegacyAssets(preview.Assets):
    """The 1.8.9 game's models and blockstates, wearing one set of pictures."""

    def __init__(self, textures: legacy.TextureSet, classic: legacy.TextureSet, overrides: dict, blockstates: dict):
        super().__init__(str(classic.jar.filename), {f"block/{name}": data for name, data in overrides.items()}, blockstates)
        self.set = textures
        self.modern_names = {old: new for new, old in legacy.TEXTURES.items()}

    def raw_model(self, ref: str) -> dict:
        # That version's blockstates name a model without its folder
        return super().raw_model(ref if "/" in ref else f"block/{ref}")

    def blockstate(self, block: str) -> dict:
        data = super().blockstate(block)
        # A block without states has the one variant "normal"
        return {"variants": {("" if key == "normal" else key): value for key, value in data["variants"].items()}}

    def texture(self, ref: str) -> np.ndarray:
        path = preview.normalize(ref)
        old_name = path.removeprefix("blocks/")
        if self.set.name == "new" and old_name in self.modern_names:
            if path not in self.textures:
                self.textures[path] = np.asarray(self.set.picture(self.modern_names[old_name]), dtype=np.float32) / 255.0
            return self.textures[path]
        return super().texture(ref)


def floor(blocks, block, x_range, z_range, y=-1):
    for x in x_range:
        for z in z_range:
            blocks.append((block, {}, (x, y, z)))


def scene_ladder():
    blocks = []
    for y in range(3):
        for x in range(2):
            blocks.append(("stone", {}, (x, y, 1)))
        blocks.append(("ladder", {"facing": "north"}, (0, y, 0)))
    blocks.append(("ladder", {"facing": "north"}, (1, 0, 0)))
    floor(blocks, "stone", range(-1, 3), range(-1, 2))
    return blocks, 200, 25


def scene_rails():
    blocks = []
    floor(blocks, "stone", range(0, 6), range(0, 5))
    for z in range(0, 3):
        blocks.append(("rail", {"shape": "north_south"}, (0, 0, z)))
    blocks.append(("rail", {"shape": "north_east"}, (0, 0, 3)))
    blocks.append(("rail", {"shape": "east_west"}, (1, 0, 3)))
    blocks.append(("golden_rail", {"shape": "north_south", "powered": "true"}, (2, 0, 0)))
    blocks.append(("golden_rail", {"shape": "north_south", "powered": "false"}, (2, 0, 1)))
    blocks.append(("detector_rail", {"shape": "north_south", "powered": "false"}, (3, 0, 0)))
    blocks.append(("activator_rail", {"shape": "north_south", "powered": "false"}, (3, 0, 1)))
    blocks.append(("rail", {"shape": "ascending_north"}, (5, 0, 2)))
    blocks.append(("stone", {}, (5, 0, 1)))
    blocks.append(("rail", {"shape": "north_south"}, (5, 1, 1)))
    return blocks, 35, 35


def bars_state(**sides):
    return {side: str(sides.get(side, False)).lower() for side in ("north", "east", "south", "west")}


def scene_bars():
    blocks = []
    floor(blocks, "stone", range(0, 6), range(0, 4))
    for x in range(0, 3):
        blocks.append(("iron_bars", bars_state(east=x < 2, west=x > 0), (x, 0, 1)))
    blocks.append(("iron_bars", bars_state(), (4, 0, 0)))
    blocks.append(("iron_bars", bars_state(south=True), (4, 0, 2)))
    blocks.append(("iron_bars", bars_state(north=True, east=True), (4, 0, 3)))
    blocks.append(("iron_bars", bars_state(west=True, north=True, south=True), (5, 0, 3)))
    return blocks, 30, 30


def vine_state(**sides):
    return {side: str(sides.get(side, False)).lower() for side in ("north", "east", "south", "west", "up")}


def scene_vines():
    blocks = []
    for x in range(3):
        for y in range(3):
            blocks.append(("stone", {}, (x, y, -1)))
            blocks.append(("vine", vine_state(north=True), (x, y, 0)))
    blocks.append(("stone", {}, (3, 2, 0)))
    blocks.append(("vine", vine_state(north=True, east=True), (2, 2, 0)))
    blocks.append(("stone", {}, (4, 3, 1)))
    blocks.append(("vine", vine_state(up=True, west=True), (4, 2, 1)))
    blocks.append(("stone", {}, (3, 2, 1)))
    return blocks, 25, 25


def scene_plants():
    blocks = []
    floor(blocks, "stone", range(0, 6), range(0, 3))
    blocks.append(("waterlily", {}, (0, 0, 0)))
    blocks.append(("waterlily", {}, (0, 0, 1)))
    for y in range(2):
        blocks.append(("reeds", {}, (1, y, 1)))
    blocks.append(("brown_mushroom", {}, (2, 0, 1)))
    blocks.append(("red_mushroom", {}, (3, 0, 1)))
    blocks.append(("red_mushroom", {}, (3, 0, 2)))
    blocks.append(("flower_pot", {"contents": "mushroom_red"}, (4, 0, 1)))
    blocks.append(("flower_pot", {"contents": "mushroom_brown"}, (4, 0, 2)))
    blocks.append(("bookshelf", {}, (5, 0, 1)))
    return blocks, 30, 30


def door(blocks, name, position, facing="north", hinge="left", is_open=False):
    x, y, z = position
    for half, dy in (("lower", 0), ("upper", 1)):
        blocks.append((name, {"facing": facing, "half": half, "hinge": hinge, "open": str(is_open).lower()}, (x, y + dy, z)))


def scene_doors():
    blocks = []
    floor(blocks, "stone", range(0, 7), range(0, 2))
    door(blocks, "wooden_door", (0, 0, 0))
    door(blocks, "spruce_door", (1, 0, 0), hinge="right")
    door(blocks, "birch_door", (2, 0, 0))
    door(blocks, "jungle_door", (3, 0, 0))
    door(blocks, "acacia_door", (4, 0, 0), is_open=True)
    door(blocks, "dark_oak_door", (5, 0, 0))
    door(blocks, "iron_door", (6, 0, 0), hinge="right", is_open=True)
    return blocks, 30, 20


def scene_doors_back():
    blocks, _, _ = scene_doors()
    return blocks, 210, 20


def trapdoor_state(facing="north", half="bottom", is_open=False):
    return {"facing": facing, "half": half, "open": str(is_open).lower()}


def scene_trapdoors():
    blocks = []
    floor(blocks, "stone", range(0, 4), range(0, 3))
    blocks.append(("trapdoor", trapdoor_state(), (0, 0, 0)))
    blocks.append(("iron_trapdoor", trapdoor_state(facing="east"), (1, 0, 0)))
    blocks.append(("trapdoor", trapdoor_state(half="top"), (2, 0, 0)))
    blocks.append(("trapdoor", trapdoor_state(is_open=True), (0, 0, 2)))
    blocks.append(("iron_trapdoor", trapdoor_state(facing="west", is_open=True), (1, 0, 2)))
    blocks.append(("trapdoor", trapdoor_state(facing="south", is_open=True), (2, 0, 2)))
    return blocks, 25, 40


SCENES = {
    "ladder": scene_ladder,
    "rails": scene_rails,
    "bars": scene_bars,
    "vines": scene_vines,
    "plants": scene_plants,
    "doors": scene_doors,
    "doors_back": scene_doors_back,
    "trapdoors": scene_trapdoors,
}


def main(argv: list) -> None:
    out_dir = pathlib.Path(__file__).parent / "previews-legacy"
    names, jar_args = [], []
    args = iter(argv)
    for arg in args:
        if arg in ("--legacy-jar", "--new-jar"):
            jar_args += [arg, next(args)]
        elif arg in SCENES:
            names.append(arg)
        else:
            out_dir = pathlib.Path(arg)
    out_dir.mkdir(parents=True, exist_ok=True)
    sets = legacy.texture_sets(jar_args)
    classic = sets[0]
    for textures in sets:
        models, blockstates, _ = legacy.generate(textures)
        vanilla = LegacyAssets(textures, classic, {}, {})
        ours = LegacyAssets(textures, classic, models, blockstates)
        for name in names or list(SCENES):
            blocks, yaw, pitch = SCENES[name]()
            left = preview.render(vanilla, blocks, yaw, pitch, scale=6)
            right = preview.render(ours, blocks, yaw, pitch, scale=6)
            path = out_dir / f"{textures.name}-{name}.png"
            preview.side_by_side(f"{name} ({textures.name})", left, right).save(path)
            print(path)


if __name__ == "__main__":
    main(sys.argv[1:])
