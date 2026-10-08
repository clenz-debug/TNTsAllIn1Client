"""Builds the TNT Dark Mode resource pack for each Minecraft version the client supports.

Usage: python build.py [output folder] [--jar <version>=<client jar>]...
       (default output: dist/ next to this file; default jars: Loom's cache, see find_jar)

Writes TNT-Dark-Mode-<version>.zip per version. Our own replacement for the "Default Dark Mode"
pack (CC-BY-NC-SA): nothing is taken from that pack. Every picture is made here from the game's own
GUI textures in the client jar, by recoloring their neutral greys with the palette below - so a new
Minecraft version gets its dark screens by running this again, new screens included.

Versions before 1.20.2 (pack format 18) keep what the newer ones have as single sprites in a few big
sheets - buttons and hotbar in widgets.png, a furnace's flame and arrow beside its panel. For those
SHEET_RULES below work on parts of a picture; the same rules, for the same reason, as the launcher's
dark mode for the versions without a mod loader (launcher/src/main/launch/legacyDarkModePack.ts).

What gets recolored: the container screens (inventory, chests, furnace, ...), their sprites, the
recipe book, the advancements window, the menu buttons and sliders (options, pause menu, ...) and
the hotbar's frame.
Only pixels that are fully opaque and neutral grey change.
Anything with a colour of its own - flames, arrows, paper, wood, gold frames, red crosses - stays as
it is, so the pictures keep reading the way they do in vanilla. The exceptions are listed in
COLOR_SWAPS.

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
    "1.14.4": 4,
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

# Single colours swapped as they are, per picture - for the few places where "only neutral greys
# change" leaves a bright patch. The creative inventory's "destroy item" slot has a reddish fill
# that stayed as bright as in vanilla (own user report): now a dark red. Its cross is a neutral grey
# and goes through the palette like everything else - nearly black, darker than the fill as in
# vanilla (a lighter cross was tried and turned down).
COLOR_SWAPS = {
    "container/creative_inventory/tab_inventory.png": {
        (0xAB, 0x7F, 0x7F): (0x4A, 0x24, 0x24),
    },
}


# From this pack format on the GUI is single sprites (INCLUDE and friends above); before it, sheets.
SPRITES_SINCE = 18

TEXTURES = "assets/minecraft/textures/"

# Sheet versions: (path below textures/, palette, regions, colour swaps) - first match wins.
# A region is (left, top, right, bottom, palette or None, tinted); right and bottom are exclusive, the
# first region a pixel lies in counts, None leaves it as it is. "tinted" is for a part that is
# coloured rather than grey: every pixel is darkened by as much as a grey of its brightness would be.
BESIDE_PANEL = (176, 0, 256, 256, None, False)
SHEET_RULES = [
    # The flame, the bubbles and the progress arrows lie right of the panel and are meant to stand out - white stays white.
    ("gui/container/furnace.png", "panel", [BESIDE_PANEL], {}),
    ("gui/container/blast_furnace.png", "panel", [BESIDE_PANEL], {}),
    ("gui/container/smoker.png", "panel", [BESIDE_PANEL], {}),
    ("gui/container/brewing_stand.png", "panel", [BESIDE_PANEL], {}),
    # Below the panel: the padlock of a locked map, an icon drawn in greys that would turn into a dark blob.
    ("gui/container/cartography_table.png", "panel", [(0, 214, 256, 228, None, False)], {}),
    # Below the panel: the villager's experience bar, which stays as it is like the progress arrows.
    ("gui/container/villager2.png", "panel", [(0, 181, 512, 196, None, False)], {}),
    # The "destroy item" slot's reddish fill would stay as bright as it is - a dark red, as in COLOR_SWAPS.
    ("gui/container/creative_inventory/tab_inventory.png", "panel", [], {(0xAB, 0x7F, 0x7F): (0x4A, 0x24, 0x24)}),
    # Slots with little pictures drawn in greys on them, for the statistics screen - not a container.
    ("gui/container/stats_icons.png", None, [], {}),
    ("gui/container/*.png", "panel", [], {}),
    ("gui/container/creative_inventory/*.png", "panel", [], {}),
    # Right of the book: buttons with an icon drawn in greys on them - only a panel's exact greys change there.
    ("gui/recipe_book.png", "panel", [(150, 0, 256, 256, "panel_only", False)], {}),
    ("gui/advancements/window.png", "panel", [], {}),
    ("gui/advancements/widgets.png", "panel", [], {}),
    ("gui/advancements/tabs.png", "panel", [], {}),
    ("gui/widgets.png", "widget", [
        # The white frame around the selected hotbar slot.
        (0, 22, 24, 46, None, False),
        # The hotbar's frame and the off-hand slot beside it; the half see-through inside is no opaque grey and stays anyway.
        (0, 0, 182, 22, "hotbar", False),
        (24, 22, 82, 46, "hotbar", False),
        # A button under the cursor is bluish in these versions.
        (0, 86, 200, 106, "widget", True),
    ], {}),
    ("gui/spectator_widgets.png", "hotbar", [(0, 22, 24, 46, None, False)], {}),
    # The helmet, chestplate, ... outlines in the empty armor slots.
    ("item/empty_armor_slot_*.png", "silhouette", [], {}),
]


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


SHEET_MAPS = {
    "panel": CURVE.__getitem__,
    "widget": WIDGET_CURVE.__getitem__,
    "hotbar": HOTBAR_CURVE.__getitem__,
    "silhouette": silhouette,
    "panel_only": panel_only,
}


def sheet_rule_for(relative: str):
    """The rule for the picture at this path (below textures/) in a sheet version, or None."""
    return next((rule for rule in SHEET_RULES if fnmatch.fnmatchcase(relative, rule[0])), None)


def recolor_sheet(data: bytes, rule: tuple) -> bytes | None:
    """A sheet with its greys run through its rule's palettes, part by part - None if no pixel changed."""
    _, default, regions, swaps = rule
    image = Image.open(io.BytesIO(data)).convert("RGBA")
    pixels = image.load()
    changed = False
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a != 255:
                continue
            region = next((region for region in regions if region[0] <= x < region[2] and region[1] <= y < region[3]), None)
            name = region[4] if region else default
            if name is None:
                continue
            if (r, g, b) in swaps:
                pixels[x, y] = (*swaps[(r, g, b)], a)
                changed = True
                continue
            level_map = SHEET_MAPS[name]
            level = round((r + g + b) / 3)
            if region and region[5]:
                factor = 1 if level == 0 else level_map(level) / level
                new = (round(r * factor), round(g * factor), round(b * factor))
            else:
                if max(r, g, b) - min(r, g, b) > MAX_CHROMA:
                    continue
                new = (level_map(level),) * 3
            if new != (r, g, b):
                pixels[x, y] = (*new, a)
                changed = True
    if not changed:
        return None
    out = io.BytesIO()
    image.save(out, "PNG", optimize=True)
    return out.getvalue()


def color_swaps_for(relative: str) -> dict:
    """The single colours swapped in the picture at this path (below textures/gui/)."""
    return next((swaps for pattern, swaps in COLOR_SWAPS.items() if fnmatch.fnmatchcase(relative, pattern)), {})


def recolor(data: bytes, level_map, swaps: dict | None = None) -> bytes | None:
    """The picture with its neutral greys run through level_map and the colours in swaps replaced -
    None if no pixel changed."""
    swaps = swaps or {}
    image = Image.open(io.BytesIO(data)).convert("RGBA")
    pixels = image.load()
    changed = False
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a == 255 and (r, g, b) in swaps:
                pixels[x, y] = (*swaps[(r, g, b)], a)
                changed = True
                continue
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
    if pack_format < SPRITES_SINCE:
        # The form these versions know - they have no use for a range of formats.
        return {"pack": {"pack_format": pack_format, "description": DESCRIPTION}}
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
        for name in sorted(names) if pack_format < SPRITES_SINCE else ():
            if not name.startswith(TEXTURES) or not name.endswith(".png"):
                continue
            rule = sheet_rule_for(name[len(TEXTURES):])
            if rule is None or rule[1] is None:
                continue
            data = recolor_sheet(source.read(name), rule)
            if data is not None:
                write(zf, name, data)
                recolored += 1
        for name in sorted(names) if pack_format >= SPRITES_SINCE else ():
            if not name.startswith(GUI) or not name.endswith(".png"):
                continue
            relative = name[len(GUI):]
            if not matches(relative, INCLUDE) or matches(relative, KEEP):
                continue
            data = recolor(source.read(name), level_map_for(relative), color_swaps_for(relative))
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
