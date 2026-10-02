"""Builds the TNT Dark Mode resource pack for each Minecraft version the client supports.

Usage: python build.py [output folder] [--jar <version>=<client jar>]...
       (default output: dist/ next to this file; default jars: Loom's cache, see find_jar)

Writes TNT-Dark-Mode-<version>.zip per version. Our own replacement for the "Default Dark Mode"
pack (CC-BY-NC-SA): nothing is taken from that pack. Every picture is made here from the game's own
GUI textures in the client jar, by recoloring their neutral greys with the palette below - so a new
Minecraft version gets its dark screens by running this again, new screens included.

What gets recolored: the container screens (inventory, chests, furnace, ...), their sprites, the
recipe book, the advancements window, the menu buttons and sliders (options, pause menu, ...) and
the hotbar's frame.
Only pixels that are fully opaque and neutral grey change.
Anything with a colour of its own - flames, arrows, paper, wood, gold frames, red crosses - stays as
it is, so the pictures keep reading the way they do in vanilla.

Vanilla draws the screens' labels ("Inventory", "Crafting") in a fixed dark grey that a resource pack
cannot change. The pack therefore carries assets/tntsallin1client/dark_mode.json with the colour the
labels should have instead; the client mod reads it and swaps the colour while the pack is active
(DarkModePack.java).
"""
import fnmatch
import io
import json
import pathlib
import sys
import zipfile

from PIL import Image

PACK_FORMATS = {
    "1.21.11": 75,
    "26.1.2": 84,
    "26.3": 97,
}

DESCRIPTION = "§6TNT Dark Mode\n§7Dunkle Inventare für TNT's All-In-1 Client"

# Fixed timestamp, so an unchanged pack builds byte-identical (same SHA-1 in the mod bundle manifest).
TIMESTAMP = (2026, 1, 1, 0, 0, 0)

GUI = "assets/minecraft/textures/gui/"

# The palette: vanilla grey level -> dark grey level, straight lines in between. The order of the
# levels stays the same, so every bevel keeps its light and shadow side.
#   0x00 outlines, 0x37 a slot's shadow edge, 0x55 a panel's shadow edge, 0x8B slots,
#   0xC6 panels, 0xFF highlights
PALETTE = [(0x00, 0x00), (0x37, 0x0E), (0x55, 0x16), (0x8B, 0x1A), (0xC6, 0x2B), (0xFF, 0x4A)]

# The menu buttons and sliders get a palette of their own: they sit on the blurred world rather than
# on a panel, and a button has to stay clearly lighter than a disabled one.
#   0x2C disabled buttons and slider tracks, 0x56 a button's shadow edge, 0x6F buttons,
#   0xAA a button's light edge, 0xFF the white frame around the hovered button (stays white)
WIDGET_PALETTE = [(0x00, 0x00), (0x2C, 0x1A), (0x56, 0x2A), (0x6F, 0x3A), (0xAA, 0x5C), (0xFF, 0xFF)]

# The hotbar: the buttons' greys, but without their jump to white at the top - the frame's light
# corner (0xCE) came out as a bright spot with it.
HOTBAR_PALETTE = WIDGET_PALETTE[:-1] + [(0xFF, 0x7A)]

# The empty-slot pictures (helmet, sword, ... outlines) are darker than the slot in vanilla. On a
# dark slot they have to be lighter than it instead, or they vanish: the darker the vanilla pixel,
# the lighter it gets.
SILHOUETTE_LIGHTEST = 0x58
SILHOUETTE_DARKEST = 0x3A

# The colour the mod draws container labels in while this pack is active (vanilla: #404040).
LABEL_COLOR = "#D0D0D0"

# How far apart a pixel's red, green and blue may be to still count as neutral grey.
MAX_CHROMA = 10

# Paths below textures/gui/ that are recolored.
INCLUDE = [
    "container/*.png",
    "container/creative_inventory/*.png",
    "sprites/container/*.png",
    "sprites/container/*/*.png",
    "sprites/recipe_book/*.png",
    "recipe_book.png",
    "advancements/window.png",
    "sprites/advancements/*.png",
    "sprites/widget/button*.png",
    "sprites/widget/slider*.png",
    "sprites/widget/cross_button*.png",
    "sprites/widget/locked_button*.png",
    "sprites/widget/unlocked_button*.png",
    "sprites/hud/hotbar.png",
    "sprites/hud/hotbar_offhand_*.png",
]

# Left exactly as they are:
#  - the white fill of progress arrows, the brewing bubbles and the villager's experience bar are
#    meant to stand out, and they do so best against the dark screens when they stay white
#  - a small picture that is an icon drawn in greys rather than a piece of a panel (the padlock on a
#    locked map) would just turn into a dark blob
KEEP = [
    "*progress*.png",
    "*/bubbles.png",
    "sprites/container/villager/experience_bar_*.png",
    "sprites/container/cartography_table/locked.png",
]

SILHOUETTES = ["sprites/container/slot/*.png"]

WIDGETS = ["sprites/widget/*.png"]

# The hotbar's grey frame sits on the world like the buttons do and gets the same greys. Its inside
# is half transparent and stays as it is (only fully opaque pixels change), and so does the
# selection frame.
HOTBAR = ["sprites/hud/hotbar*.png"]

# Buttons with an icon drawn in greys on them (the furnace on the recipe book's filter button): only
# the exact grey levels a panel is made of change, so the button gets dark and the icon stays.
ICON_BUTTONS = ["sprites/recipe_book/*filter*.png"]


def find_jar(version: str) -> pathlib.Path:
    """The client jar Loom downloaded for this version's mod project."""
    return pathlib.Path.home() / ".gradle" / "caches" / "fabric-loom" / version / "minecraft-client.jar"


def matches(path: str, patterns: list) -> bool:
    return any(fnmatch.fnmatchcase(path, pattern) for pattern in patterns)


def curve(palette: list) -> list:
    """A palette as a table: index = vanilla grey level, value = dark grey level."""
    table = []
    for level in range(256):
        for (from_low, to_low), (from_high, to_high) in zip(palette, palette[1:]):
            if from_low <= level <= from_high:
                table.append(round(to_low + (to_high - to_low) * (level - from_low) / (from_high - from_low)))
                break
    return table


CURVE = curve(PALETTE)
WIDGET_CURVE = curve(WIDGET_PALETTE)
HOTBAR_CURVE = curve(HOTBAR_PALETTE)
PANEL_LEVELS = {level for level, _ in PALETTE}


def silhouette(level: int) -> int:
    return round(SILHOUETTE_LIGHTEST - (SILHOUETTE_LIGHTEST - SILHOUETTE_DARKEST) * level / 255)


def panel_only(level: int) -> int:
    return CURVE[level] if level in PANEL_LEVELS else level


def level_map_for(relative: str):
    """How the greys of the picture at this path (below textures/gui/) are recolored."""
    if matches(relative, SILHOUETTES):
        return silhouette
    if matches(relative, HOTBAR):
        return HOTBAR_CURVE.__getitem__
    if matches(relative, WIDGETS):
        return WIDGET_CURVE.__getitem__
    if matches(relative, ICON_BUTTONS):
        return panel_only
    return CURVE.__getitem__


def recolor(data: bytes, level_map) -> bytes | None:
    """The picture with its neutral greys run through level_map - None if no pixel changed."""
    image = Image.open(io.BytesIO(data)).convert("RGBA")
    pixels = image.load()
    changed = False
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a != 255 or max(r, g, b) - min(r, g, b) > MAX_CHROMA:
                continue
            level = level_map(round((r + g + b) / 3))
            if (level, level, level) != (r, g, b):
                pixels[x, y] = (level, level, level, a)
                changed = True
    if not changed:
        return None
    out = io.BytesIO()
    image.save(out, "PNG", optimize=True)
    return out.getvalue()


def pack_icon() -> bytes:
    """pack.png: a dark panel with a 3x3 grid of slots, in the pack's own palette."""
    panel, slot, light, shadow = CURVE[0xC6], CURVE[0x8B], CURVE[0xFF], CURVE[0x37]
    size = 64
    image = Image.new("RGB", (size, size), (panel,) * 3)
    pixels = image.load()
    for i in range(size):
        for edge in (0, 1):
            pixels[i, edge] = pixels[edge, i] = (light,) * 3
            pixels[i, size - 1 - edge] = pixels[size - 1 - edge, i] = (CURVE[0x55],) * 3
    for row in range(3):
        for column in range(3):
            left, top = 8 + column * 16, 8 + row * 16
            for y in range(16):
                for x in range(16):
                    if x == 0 or y == 0:
                        level = shadow
                    elif x == 15 or y == 15:
                        level = light
                    else:
                        level = slot
                    pixels[left + x, top + y] = (level,) * 3
    out = io.BytesIO()
    image.save(out, "PNG", optimize=True)
    return out.getvalue()


def mcmeta(pack_format: int) -> dict:
    pack = {"min_format": pack_format, "max_format": pack_format, "description": DESCRIPTION}
    if pack_format < 84:
        pack["pack_format"] = pack_format
    return {"pack": pack}


def write(zf: zipfile.ZipFile, name: str, data) -> None:
    info = zipfile.ZipInfo(name, TIMESTAMP)
    info.compress_type = zipfile.ZIP_DEFLATED
    zf.writestr(info, data)


def build(version: str, pack_format: int, jar: pathlib.Path, target: pathlib.Path) -> pathlib.Path:
    path = target / f"TNT-Dark-Mode-{version}.zip"
    recolored = 0
    with zipfile.ZipFile(jar) as source, zipfile.ZipFile(path, "w") as zf:
        write(zf, "pack.mcmeta", json.dumps(mcmeta(pack_format), indent=2, ensure_ascii=False))
        write(zf, "pack.png", pack_icon())
        write(zf, "assets/tntsallin1client/dark_mode.json", json.dumps({"label_color": LABEL_COLOR}, indent=2))
        names = set(source.namelist())
        for name in sorted(names):
            if not name.startswith(GUI) or not name.endswith(".png"):
                continue
            relative = name[len(GUI):]
            if not matches(relative, INCLUDE) or matches(relative, KEEP):
                continue
            data = recolor(source.read(name), level_map_for(relative))
            if data is None:
                continue
            write(zf, name, data)
            # A sprite's scaling rules (nine-slice borders etc.) are read from the pack its picture
            # comes from, so they have to travel with it.
            if name + ".mcmeta" in names:
                write(zf, name + ".mcmeta", source.read(name + ".mcmeta"))
            recolored += 1
    print(f"{path.name}: {recolored} pictures recolored")
    return path


def main(argv: list) -> None:
    jars = {}
    target = pathlib.Path(__file__).parent / "dist"
    args = iter(argv)
    for arg in args:
        if arg == "--jar":
            version, _, jar = next(args).partition("=")
            jars[version] = pathlib.Path(jar)
        else:
            target = pathlib.Path(arg)
    target.mkdir(parents=True, exist_ok=True)
    for version, pack_format in PACK_FORMATS.items():
        jar = jars.get(version, find_jar(version))
        if not jar.is_file():
            sys.exit(f"No client jar for {version} at {jar} - build that mod once, or pass --jar {version}=<path>")
        build(version, pack_format, jar, target)


if __name__ == "__main__":
    main(sys.argv[1:])
