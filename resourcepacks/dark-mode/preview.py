"""Before/after picture of the dark mode pack, for a look at palette changes without starting the game.

Usage: python preview.py [version]   (default: the newest version in build.py)

Writes dist/preview-<version>.png: vanilla on the left, the built pack (dist/TNT-Dark-Mode-<version>.zip,
run build.py first) on the right, for the screens people see most. Labels are not shown - the game
draws those, in the colour from build.py's LABEL_COLOR.
"""
import io
import pathlib
import sys
import zipfile

from PIL import Image

import build

# (path below textures/gui/, the part of the picture the screen actually uses)
SCREENS = [
    ("container/inventory.png", (0, 0, 176, 166)),
    ("container/crafting_table.png", (0, 0, 176, 166)),
    ("container/furnace.png", (0, 0, 176, 166)),
    ("container/generic_54.png", (0, 0, 176, 222)),
    ("container/creative_inventory/tab_items.png", (0, 0, 195, 136)),
    ("container/villager.png", (0, 0, 276, 166)),
    ("container/enchanting_table.png", (0, 0, 176, 166)),
    ("container/anvil.png", (0, 0, 176, 166)),
    ("recipe_book.png", (0, 0, 149, 168)),
    ("advancements/window.png", (0, 0, 252, 140)),
    ("sprites/widget/button.png", (0, 0, 200, 20)),
    ("sprites/widget/button_highlighted.png", (0, 0, 200, 20)),
    ("sprites/widget/button_disabled.png", (0, 0, 200, 20)),
    ("sprites/widget/slider_handle.png", (0, 0, 8, 20)),
    ("sprites/recipe_book/filter_disabled.png", (0, 0, 26, 16)),
    ("sprites/recipe_book/furnace_filter_disabled.png", (0, 0, 26, 16)),
]
SCALE = 2
GAP = 12
BACKDROP = (58, 62, 70)


def crop(data: bytes, box: tuple) -> Image.Image:
    image = Image.open(io.BytesIO(data)).convert("RGBA")
    # Some sheets are 512 wide (villager) - the box is in the sheet's own pixels either way
    part = image.crop(box)
    return part.resize((part.width * SCALE, part.height * SCALE), Image.NEAREST)


def main(argv: list) -> None:
    version = argv[0] if argv else list(build.PACK_FORMATS)[-1]
    dist = pathlib.Path(__file__).parent / "dist"
    with zipfile.ZipFile(build.find_jar(version)) as jar, zipfile.ZipFile(dist / f"TNT-Dark-Mode-{version}.zip") as pack:
        pack_names = set(pack.namelist())
        rows = []
        for path, box in SCREENS:
            name = build.GUI + path
            vanilla = crop(jar.read(name), box)
            dark = crop(pack.read(name) if name in pack_names else jar.read(name), box)
            rows.append((vanilla, dark))
    width = GAP * 3 + max(v.width for v, _ in rows) * 2
    height = GAP + sum(v.height + GAP for v, _ in rows)
    sheet = Image.new("RGB", (width, height), BACKDROP)
    column = max(v.width for v, _ in rows)
    y = GAP
    for vanilla, dark in rows:
        sheet.paste(vanilla, (GAP, y), vanilla)
        sheet.paste(dark, (GAP * 2 + column, y), dark)
        y += vanilla.height + GAP
    out = dist / f"preview-{version}.png"
    sheet.save(out)
    print(out)


if __name__ == "__main__":
    main(sys.argv[1:])
