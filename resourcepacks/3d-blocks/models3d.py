"""TNT 3D Blocks - our own 3D block models (own user request: replaces the Vanilla Tweaks selection,
whose terms don't allow passing it on unmodified). Every model here is written from scratch; the
textures are Minecraft's own, only referenced by name and never copied into the pack.

The models replace vanilla's model files under the same names - blockstates stay untouched, so every
block state (rail shapes, bar connections, chain axes, ...) keeps working exactly as in vanilla. Where
vanilla shares one template between several blocks (iron and copper bars, all chains, the rail shapes),
the template is what gets replaced, so every variant gets the 3D look at once.

The exceptions get blockstates of their own: amethyst and the mushrooms, to pick one of several turned
models per block (a random pick between models is something only a blockstate can do), and glow lichen and sculk
veins, whose vanilla blockstates turn the model with "uvlock" - that re-maps every face's texture to
where the face ends up in the world, which leaves the pixel-thin sides of our 3D pieces showing
see-through parts of the picture.

MODELS maps a model path ("block/ladder") to its JSON, BLOCKSTATES a block ("amethyst_cluster") to its
blockstate JSON, ITEMS an item ("sugar_cane") to its item definition; build.py writes them into the
pack, preview.py renders them. Coordinates are Minecraft's: 0..16 per block, x east, y up, z south.
"""
import copy
import entity_items
import math

MODELS: dict = {}
BLOCKSTATES: dict = {}

HALF = 0.5


def model(path: str, textures: dict, elements: list, ambient_occlusion: bool = False, parent: str | None = None) -> None:
    data = {"textures": textures, "elements": elements}
    if not ambient_occlusion:
        data = {"ambientocclusion": False, **data}
    if parent:
        data = {"parent": parent, **data}
    MODELS[path] = data


def face(uv, texture: str, rotation: int = 0, cullface: str | None = None) -> dict:
    spec = {"uv": [round(v, 4) for v in uv], "texture": texture}
    if rotation:
        spec["rotation"] = rotation
    if cullface:
        spec["cullface"] = cullface
    return spec


def box(frm, to, faces: dict, rotation: dict | None = None) -> dict:
    element = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


def solid(frm, to, uv, texture: str, rotation: dict | None = None, skip: tuple = ()) -> dict:
    """A box with the same texture region on every face - for small parts where the exact pixels don't matter."""
    return box(frm, to, {name: face(uv, texture) for name in ("north", "south", "east", "west", "up", "down") if name not in skip}, rotation)


# --- Ladder ---------------------------------------------------------------------------------------
# Two side rails standing off the wall with four rungs running through them. The texture has the rails
# in columns 2-3 and 12-13 and a rung every four rows (light top row, dark bottom row).

def ladder() -> None:
    t = "#texture"
    elements = []
    for x in (2, 12):
        u = 16 - (x + 2)  # the north face shows the texture mirrored (u = 16 - x)
        side = face([u, 0, u + 2, 16], t)
        elements.append(box([x, 0, 13], [x + 2, 16, 16], {
            "north": side,
            "south": face([u, 0, u + 2, 16], t, cullface="south"),
            "east": side,
            "west": side,
            "up": face([u, 0, u + 2, 3], t),
            "down": face([u, 13, u + 2, 16], t),
        }))
    for row in (1, 5, 9, 13):
        top, bottom = 16 - row, 16 - (row + 2)
        front = face([1, row, 15, row + 2], t)
        elements.append(box([1, bottom, 14], [15, top, 15], {
            "north": front,
            "south": front,
            "up": face([1, row, 15, row + 1], t),
            "down": face([1, row + 1, 15, row + 2], t),
            "east": face([1, row, 2, row + 2], t),
            "west": face([14, row, 15, row + 2], t),
        }))
    model("block/ladder", {"particle": "block/ladder", "texture": "block/ladder"}, elements)


# --- Rails ----------------------------------------------------------------------------------------
# Wooden ties with the two metal rails running on top of them. The texture has the ties at rows 1-2,
# 5-6, 9-10 and 13-14 and the rails in columns 2-3 and 12-13 - the same layout for normal, powered,
# detector and activator rails. Under the 3D parts the whole texture stays as a flat layer, so what
# only exists in the picture (a powered rail's gold, the detector plate) is still there.

RAIL_TIE_ROWS = (1, 5, 9, 13)
RAIL_COLUMNS = (2, 12)


# The detector rail's sensor plate in its picture: columns and rows 5-10, all solid
SENSOR = (5, 11)


def rail_parts(texture: str, base: float, rotation: dict | None = None, sensor: bool = False) -> list:
    """sensor: the detector rail - its sensor becomes one plate at the ties' height (own user request:
    with the ties at rows 5 and 9 running through it, it came out wavy, lower in the middle)."""
    elements = [box([0, base + 0.25, 0], [16, base + 0.25, 16], {
        "up": face([0, 0, 16, 16], texture),
        "down": face([0, 16, 16, 0], texture),
    }, rotation)]
    for row in RAIL_TIE_ROWS:
        # A tie crossing the sensor plate is only its two ends, left and right of the plate
        pieces = [(1, SENSOR[0]), (SENSOR[1], 15)] if sensor and SENSOR[0] <= row < SENSOR[1] else [(1, 15)]
        for x1, x2 in pieces:
            faces = {
                "up": face([x1, row, x2, row + 2], texture),
                "north": face([x1, row, x2, row + 1], texture),
                "south": face([x1, row + 1, x2, row + 2], texture),
            }
            if x2 == 15:
                faces["east"] = face([14, row, 15, row + 2], texture, rotation=90)
            if x1 == 1:
                faces["west"] = face([1, row, 2, row + 2], texture, rotation=90)
            elements.append(box([x1, base, row], [x2, base + 1, row + 2], faces, rotation))
    if sensor:
        low, high = SENSOR
        elements.append(box([low, base, low], [high, base + 1, high], {
            "up": face([low, low, high, high], texture),
            "north": face([low, low, high, low + 1], texture),
            "south": face([low, high - 1, high, high], texture),
            "east": face([high - 1, low, high, high], texture, rotation=90),
            "west": face([low, low, low + 1, high], texture, rotation=90),
        }, rotation))
    for column in RAIL_COLUMNS:
        length = face([column, 0, column + 2, 16], texture, rotation=90)
        elements.append(box([column, base, 0], [column + 2, base + 2, 16], {
            "up": face([column, 0, column + 2, 16], texture),
            "east": length,
            "west": length,
            "north": face([column, 0, column + 2, 2], texture),
            "south": face([column, 14, column + 2, 16], texture),
        }, rotation))
    return elements


def rail_flat() -> None:
    model("block/rail_flat", {"particle": "#rail"}, rail_parts("#rail", 0))


def rail_raised(path: str, angle: float, textures: dict | None = None, sensor: bool = False) -> None:
    # Vanilla's slope: the flat layer lifted to y 9 and tilted by 45 degrees around the block's middle,
    # stretched to reach from edge to edge. Our parts sit one pixel lower, so the ties rest on the slope.
    rotation = {"origin": [8, 9, 8], "axis": "x", "angle": angle, "rescale": True}
    model(path, textures or {"particle": "#rail"}, rail_parts("#rail", 8, rotation, sensor))


def detector_rails() -> None:
    """The detector rail's own models (vanilla gives it the shared rail templates) - for its sensor plate."""
    for suffix, picture in (("", "detector_rail"), ("_on", "detector_rail_on")):
        textures = {"particle": "#rail", "rail": f"minecraft:block/{picture}"}
        model(f"block/detector_rail{suffix}", textures, rail_parts("#rail", 0, sensor=True))
        rail_raised(f"block/detector_rail{suffix}_raised_ne", 45, textures, sensor=True)
        rail_raised(f"block/detector_rail{suffix}_raised_sw", -45, textures, sensor=True)


RAIL_HEIGHT = 2  # as on the straight rail: rails two pixels high, ties one
TIE_HEIGHT = 1


def rail_curved() -> None:
    """The curve from the south edge to the east edge (vanilla's rail_corner), pixel by pixel from its
    own picture: the grey metal pixels become the rails, the others the ties, as high as on the
    straight rail - so they meet the straight rail's rails exactly at the block edges.

    Before, the curve was short straight pieces turned along two arcs; 1.21.11 only turns in steps of
    22.5 degrees, and even after two fix rounds the inner rail kinked and the turned ties stuck out past
    the outer rail (own user reports). Built from the picture, nothing is turned any more.
    """
    elements = (floor_extrusion(mask_of("rail_corner_metal"), "#rail", RAIL_HEIGHT)
                + floor_extrusion(mask_of("rail_corner_wood"), "#rail", TIE_HEIGHT))
    model("block/rail_curved", {"particle": "#rail"}, elements)


# --- Chains ---------------------------------------------------------------------------------------
# Real interlocking links instead of two crossed pictures: they alternate between facing east-west and
# north-south, eight pixels apart. Where a link crosses into the next chain block, each block draws its
# half, so a column of chains is seamless and a single one looks like a cut piece of chain.

CHAIN_UVS = {"texture": "#texture", "side": ([0, 1, 6, 2], 90), "bar": [0, 3, 4, 4], "end": [0, 1, 1, 2]}
# Lanterns carry their own little chain in their texture, columns 11-13
LANTERN_UVS = {"texture": "#lantern", "side": ([11, 1, 12, 5], 0), "bar": [11, 1, 14, 2], "end": [11, 1, 12, 2]}


def chain_link(axis: str, bottom: float, top: float, clip_bottom: float = 0, clip_top: float = 16, uvs: dict = CHAIN_UVS) -> list:
    """A rectangular link 4 wide and (top - bottom) tall, made of 1-pixel bars, in the x-y ("x") or z-y ("z") plane.
    Only the part between clip_bottom and clip_top is built."""
    t = uvs["texture"]
    long_uv, long_rotation = uvs["side"]
    bar_uv = uvs["bar"]
    end_uv = uvs["end"]
    parts = []

    def span(a, b):
        return max(a, clip_bottom), min(b, clip_top)

    def at(lo, hi, y0, y1, depth_lo=7.5, depth_hi=8.5):
        if axis == "x":
            return [lo, y0, depth_lo], [hi, y1, depth_hi]
        return [depth_lo, y0, lo], [depth_hi, y1, hi]

    # Side bars
    for lo, hi in ((6, 7), (9, 10)):
        y0, y1 = span(bottom, top)
        if y1 > y0:
            frm, to = at(lo, hi, y0, y1)
            side = face(long_uv, t, rotation=long_rotation)
            parts.append(box(frm, to, {"north": side, "south": side, "east": side, "west": side,
                                       "up": face(end_uv, t), "down": face(end_uv, t)}))
    # Top and bottom bars
    for y0, y1 in ((bottom, bottom + 1), (top - 1, top)):
        if y0 >= clip_bottom and y1 <= clip_top:
            frm, to = at(7, 9, y0, y1)
            bar = face(bar_uv, t)
            parts.append(box(frm, to, {"north": bar, "south": bar, "east": bar, "west": bar, "up": bar, "down": bar}))
    return parts


def chain() -> None:
    elements = []
    elements += chain_link("x", 0, 6)
    elements += chain_link("x", 8, 14)
    elements += chain_link("z", 4, 10)
    elements += chain_link("z", -4, 2, clip_bottom=0)  # upper half of the link shared with the block below
    elements += chain_link("z", 12, 18, clip_top=16)  # lower half of the link shared with the block above
    model("block/template_chain", {"particle": "#texture"}, elements)


# --- Lanterns -------------------------------------------------------------------------------------
# Same links as the chain blocks instead of vanilla's two crossed pictures: a hanging lantern hangs
# from a link that is the lower half of the one a chain block above it ends with, so lantern and chain
# join up seamlessly; a standing lantern gets a small 3D handle. Body and cap as in vanilla.

def lantern_body(bottom: float) -> list:
    t = "#lantern"
    side, cap_side = face([0, 2, 6, 9], t), face([1, 0, 5, 2], t)
    body = box([5, bottom, 5], [11, bottom + 7, 11], {"north": side, "south": side, "east": side, "west": side,
                                                    "up": face([0, 9, 6, 15], t),
                                                    "down": face([0, 9, 6, 15], t, cullface="down" if bottom == 0 else None)})
    cap = box([6, bottom + 7, 6], [10, bottom + 9, 10], {"north": cap_side, "south": cap_side, "east": cap_side, "west": cap_side,
                                                       "up": face([1, 10, 5, 14], t), "down": face([1, 10, 5, 14], t)})
    return [body, cap]


def lanterns() -> None:
    hanging = lantern_body(1)
    # The links start at the cap's top (own user report: sunk into the cap, their outer sides lay exactly
    # on the cap's sides and flickered against them)
    hanging += chain_link("x", 8, 14, clip_bottom=10, uvs=LANTERN_UVS)
    hanging += chain_link("z", 12, 18, clip_top=16, uvs=LANTERN_UVS)  # lower half of a chain block's last link
    model("block/template_hanging_lantern", {"particle": "#lantern"}, hanging, ambient_occlusion=True, parent="block/block")
    standing = lantern_body(0)
    standing += chain_link("x", 7, 12, clip_bottom=9, uvs=LANTERN_UVS)
    model("block/template_lantern", {"particle": "#lantern"}, standing, ambient_occlusion=True, parent="block/block")


# --- Iron and copper bars ---------------------------------------------------------------------------
# Square rods instead of flat panes: a pole in the middle, one more rod in each connected half (where
# the texture has its rods, columns 2-3 and 12-13) and two thin cross bars holding them together.
# Vanilla's cap and post pieces aren't needed then - the middle pole is always there.

def bars() -> None:
    t = "#bars"
    rod = [2, 0, 4, 16]
    pole = box([7, 0, 7], [9, 16, 9], {name: face([7, 0, 9, 16], t) for name in ("north", "south", "east", "west")}
               | {"up": face([7, 7, 9, 9], t), "down": face([7, 7, 9, 9], t)})
    model("block/template_bars_post_ends", {"particle": "#bars"}, [pole])
    for name in ("block/template_bars_post", "block/template_bars_cap", "block/template_bars_cap_alt"):
        model(name, {"particle": "#bars"}, [])

    def half(z_lo: float, z_hi: float, rod_z: float) -> list:
        parts = [box([7, 0, rod_z], [9, 16, rod_z + 2], {name: face(rod, t) for name in ("north", "south", "east", "west")}
                     | {"up": face([2, 0, 4, 2], t), "down": face([2, 14, 4, 16], t)})]
        for y in (2, 13):
            cross = face([2, 0, 3, z_hi - z_lo], t, rotation=90)
            parts.append(box([7.5, y, z_lo], [8.5, y + 1, z_hi], {"east": cross, "west": cross, "up": cross, "down": cross,
                                                                  "north": face([2, 0, 3, 1], t), "south": face([2, 0, 3, 1], t)}))
        return parts

    model("block/template_bars_side", {"particle": "#bars"}, half(0, 7, 2))
    model("block/template_bars_side_alt", {"particle": "#bars"}, half(9, 16, 12))


# --- Pointed dripstone ----------------------------------------------------------------------------
# Stacked square slices getting thinner towards the tip, following the outline of vanilla's picture
# of each piece - each slice shows the matching part of that picture on its sides. The "up" pieces are
# the same shapes upside down (their textures are the same pictures flipped).

# Per thickness, from the end that touches the block (row 0 of the "down" texture) outwards:
# (first texture row, last row + 1, slice width)
DRIPSTONE_SLICES = {
    "tip": [(0, 2, 7), (2, 5, 6), (5, 8, 4), (8, 10, 3), (10, 11, 2)],
    "frustum": [(0, 5, 12), (5, 8, 11), (8, 11, 10), (11, 14, 9), (14, 16, 8)],
    "middle": [(0, 4, 12), (4, 7, 11), (7, 11, 12), (11, 14, 11), (14, 16, 12)],
    "base": [(0, 4, 16), (4, 14, 14), (14, 16, 12)],
    # Where a hanging and a standing tip meet: thin all the way, ending at the other one's tip
    "tip_merge": [(0, 2, 8), (2, 5, 7), (5, 6, 6), (6, 12, 5), (12, 16, 3)],
}


# The sulfur spike (26.3): the same block in sulfur, its outline more jagged - slices after its own pictures.
SULFUR_SPIKE_SLICES = {
    "tip": [(0, 4, 9), (4, 5, 7), (5, 8, 5), (8, 11, 4), (11, 12, 2)],
    "frustum": [(0, 3, 12), (3, 5, 11), (5, 10, 9), (10, 16, 8)],
    "middle": [(0, 2, 12), (2, 8, 11), (8, 11, 13), (11, 13, 11), (13, 16, 12)],
    "base": [(0, 3, 16), (3, 4, 14), (4, 7, 12), (7, 11, 14), (11, 13, 11), (13, 16, 12)],
    "tip_merge": [(0, 4, 9), (4, 5, 7), (5, 8, 5), (8, 11, 4), (11, 14, 5), (14, 16, 3)],
}
SPIKES = {"pointed_dripstone": DRIPSTONE_SLICES, "sulfur_spike": SULFUR_SPIKE_SLICES}


def solid_columns(mask: list, rows) -> tuple:
    """(first, end) of the columns that are solid in every one of these rows."""
    columns = set.intersection(*({u for u in range(16) if mask[v][u] == "#"} for v in rows))
    assert columns and max(columns) - min(columns) + 1 == len(columns), rows
    return min(columns), max(columns) + 1


def dripstone_slices(texture: str, mask: list, slices: list, pointing_up: bool) -> list:
    """Sides and caps only ever show columns solid in all of the slice's rows (own user report: with the
    slice's full width, the picture's narrower rows left see-through slivers along the edges)."""
    elements = []
    for first, last, width in slices:
        lo, hi = 8 - width / 2, 8 + width / 2
        if pointing_up:
            y0, y1 = first, last
            rows = (16 - last, 16 - first)
        else:
            y0, y1 = 16 - last, 16 - first
            rows = (first, last)
        u1, u2 = solid_columns(mask, range(*rows))
        side = face([u1, rows[0], u2, rows[1]], texture)
        # Top and bottom: the slice's widest row stretched
        widest = rows[0] if not pointing_up else rows[1] - 1
        c1, c2 = solid_columns(mask, [widest])
        cap = face([c1, widest, c2, widest + 1], texture)
        elements.append(box([lo, y0, lo], [hi, y1, hi], {"north": side, "south": side, "east": side, "west": side,
                                                        "up": cap, "down": cap}))
    return elements


def dripstone() -> None:
    for spike, slice_table in SPIKES.items():
        for thickness, slices in slice_table.items():
            for direction in ("down", "up"):
                texture = f"minecraft:block/{spike}_{direction}_{thickness}"
                model(f"block/{spike}_{direction}_{thickness}", {"particle": texture, "cross": texture},
                      dripstone_slices("#cross", mask_of(f"{spike}_{direction}_{thickness}"), slices, direction == "up"),
                      ambient_occlusion=True)


# --- Pixel extrusion ------------------------------------------------------------------------------
# Flat things (glow lichen, sculk veins, lily pads, redstone dust) get real thickness: every solid pixel
# of the picture becomes part of a thin slab. Neighbouring pixels are merged into as few boxes as
# possible - runs along a row, then rows with the same run stacked into one rectangle. Which pixels are
# solid comes from texture_masks.py ("#" = solid), generated from vanilla's pictures by extract_masks.py.

from texture_masks import MASKS


def mask_of(texture: str) -> list:
    rows = MASKS[texture]
    return rows.split() if rows else ["#" * 16] * 16


def mask_rectangles(mask: list) -> list:
    """(first column, first row, end column, end row) rectangles covering every '#' of a 16x16 mask."""
    def runs(row: str) -> list:
        found, start = [], None
        for x, pixel in enumerate(row + "."):
            if pixel == "#" and start is None:
                start = x
            elif pixel != "#" and start is not None:
                found.append((start, x))
                start = None
        return found

    rectangles, open_runs = [], {}
    for row_index in range(len(mask) + 1):
        current = set(runs(mask[row_index])) if row_index < len(mask) else set()
        for run in list(open_runs):
            if run not in current:
                rectangles.append((run[0], open_runs.pop(run), run[1], row_index))
        for run in current:
            open_runs.setdefault(run, row_index)
    return sorted(rectangles)


def wall_extrusion(mask: list, texture: str, depth: float, tint: bool = False, start: float = 0) -> list:
    """The picture standing against the north side of the block, facing south, `depth` pixels thick
    (from `start` pixels in front of the side)."""
    def tinted(spec: dict) -> dict:
        return {**spec, "tintindex": 0} if tint else spec

    elements = []
    for x1, r1, x2, r2 in mask_rectangles(mask):
        elements.append(box([x1, 16 - r2, start], [x2, 16 - r1, start + depth], {
            "south": tinted(face([x1, r1, x2, r2], texture)),
            "north": tinted(face([x2, r1, x1, r2], texture)),
            "up": tinted(face([x1, r1, x2, r1 + 1], texture)),
            "down": tinted(face([x1, r2 - 1, x2, r2], texture)),
            "east": tinted(face([x2 - 1, r1, x2, r2], texture)),
            "west": tinted(face([x1, r1, x1 + 1, r2], texture)),
        }))
    return elements


def floor_extrusion(mask: list, texture: str, thickness: float, tint: bool = False) -> list:
    """The picture lying flat on the floor (rows run north to south), `thickness` pixels thick."""
    def tinted(spec: dict) -> dict:
        return {**spec, "tintindex": 0} if tint else spec

    elements = []
    for x1, r1, x2, r2 in mask_rectangles(mask):
        elements.append(box([x1, 0, r1], [x2, thickness, r2], {
            "up": tinted(face([x1, r1, x2, r2], texture)),
            "down": tinted(face([x1, r2, x2, r1], texture)),
            "north": tinted(face([x1, r1, x2, r1 + 1], texture)),
            "south": tinted(face([x1, r2 - 1, x2, r2], texture)),
            "east": tinted(face([x2 - 1, r1, x2, r2], texture, rotation=90)),
            "west": tinted(face([x1, r1, x1 + 1, r2], texture, rotation=270)),
        }))
    return elements


# The sides a wall growth can cover, and how vanilla turns its north-side model onto each
WALL_GROWTH_SIDES = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
VINE_SIDES = ("north", "east", "south", "west", "up")  # vines never hang on a block's underside
VINE_DEPTH = 0.75
VINE_LEAF_DEPTH = 0.75  # the lighter leaf pixels, on top of the rest


def wall_growth_blockstate(name: str, sides) -> None:
    """Vanilla's blockstate minus its "uvlock" (own user report: on every side but north the pieces'
    sides turned invisible, the pieces looked like floating flakes). With no side set at all (e.g. a
    placed-then-emptied state) vanilla shows every side, and so does this."""
    nothing = {side: "false" for side in sides}
    parts = []
    for side in sides:
        apply = {"model": f"minecraft:block/{name}", **WALL_GROWTH_SIDES[side]}
        parts += [{"when": {side: "true"}, "apply": apply}, {"when": nothing, "apply": apply}]
    BLOCKSTATES[name] = {"multipart": parts}


def wall_growths() -> None:
    # Applied once per covered side by the blockstate (turned there), like vanilla's flat version
    for name in ("glow_lichen", "sculk_vein"):
        model(f"block/{name}", {"particle": f"block/{name}", name: f"block/{name}"},
              wall_extrusion(mask_of(name), f"#{name}", 0.75))
        wall_growth_blockstate(name, WALL_GROWTH_SIDES)


def vine() -> None:
    """Vines like glow lichen, plus a second layer: the picture's lighter pixels - the leaves - stand
    out further than the dark stems between them (Vanilla Tweaks' 3D vines as the example). Tinted
    with the biome's foliage color like vanilla's."""
    leaves = wall_extrusion(mask_of("vine_light"), "#vine", VINE_LEAF_DEPTH, tint=True, start=VINE_DEPTH)
    for element in leaves:
        del element["faces"]["north"]  # lies on the layer below
    model("block/vine", {"particle": "block/vine", "vine": "block/vine"},
          wall_extrusion(mask_of("vine"), "#vine", VINE_DEPTH, tint=True) + leaves)
    wall_growth_blockstate("vine", VINE_SIDES)


# Weeping and twisting vines: real stalks instead of a crossed picture (own user request). Every solid
# stretch of the picture becomes a square piece of stalk - as deep as it is wide, standing where the
# picture has it - so the stalk winds exactly like the picture and joins up with the blocks above and
# below. The weeping vines' two strands are moved apart front to back, or from the side they'd look
# like one flat wall.
NETHER_VINES = {
    "weeping_vines": 1.5,  # how far the left strand moves south and the right one north
    "weeping_vines_plant": 1.5,
    "twisting_vines": 0,
    "twisting_vines_plant": 0,
}


def stalk(mask: list, texture: str, strand_offset: float) -> list:
    elements = []
    for x1, r1, x2, r2 in mask_rectangles(mask):
        width = x2 - x1
        middle = 8 + (strand_offset if (x1 + x2) / 2 < 8 else -strand_offset)
        side = face([x1, r1, x2, r2], texture)
        elements.append(box([x1, 16 - r2, middle - width / 2], [x2, 16 - r1, middle + width / 2], {
            "north": side, "south": side, "east": side, "west": side,
            "up": face([x1, r1, x2, r1 + 1], texture), "down": face([x1, r2 - 1, x2, r2], texture),
        }))
    return elements


def nether_vines() -> None:
    for name, strand_offset in NETHER_VINES.items():
        model(f"block/{name}", {"particle": f"minecraft:block/{name}", "cross": f"minecraft:block/{name}"},
              stalk(mask_of(name), "#cross", strand_offset))


def bamboo_sapling() -> None:
    """A freshly planted bamboo as a real little shoot, built like the nether vines' stalks (own user
    request: the flat crossed picture looked broken next to our 3D blocks)."""
    model("block/bamboo_sapling", {"particle": "minecraft:block/bamboo_stage0", "cross": "minecraft:block/bamboo_stage0"},
          stalk(mask_of("bamboo_stage0"), "#cross", 0))


def moved(elements: list, dx: float = 0, dy: float = 0, dz: float = 0) -> list:
    """The same elements shifted, turning points included."""
    shift = (dx, dy, dz)
    for element in elements:
        element["from"] = [round(v + d, 4) for v, d in zip(element["from"], shift)]
        element["to"] = [round(v + d, 4) for v, d in zip(element["to"], shift)]
        if "rotation" in element:  # a copy: elements may share one rotation (a crystal and its tip)
            element["rotation"] = {**element["rotation"],
                                   "origin": [round(v + d, 4) for v, d in zip(element["rotation"]["origin"], shift)]}
    return elements


# --- Bushes ---------------------------------------------------------------------------------------
# Round, stepped bushes instead of a crossed picture (own user request, Vanilla Tweaks' 3D bushes as
# the example; a single leaf box looked like a crate): three stacked tiers - narrower at the bottom and
# top, widest in the middle - each side showing the part of the picture at its height, with the gaps
# the picture has, and the crossed picture inside so the gaps show leaves behind them. Sweet berry
# bushes in all four stages, the bush (tinted with the grass color, like vanilla's) and the firefly
# bush: its denser lower part becomes the bush, its thin twigs peek a little above it from the
# crossed picture, and its glowing fireflies sit just outside the tiers' sides.

BUSHES = {
    "bush": {"tint": True, "top_lower": 2},  # top tier two pixels lower (own user request)
    # Rows above body_top_row: thin twigs; the crossed picture starts at cross_top_row so they only
    # peek a little over the body (own user feedback: rising the full height they stuck out too far)
    "firefly_bush": {"emissive": "firefly_bush_emissive", "body_top_row": 4, "cross_top_row": 2},
    **{f"sweet_berry_bush_stage{stage}": {"top_lower": 2} for stage in range(4)},
    "red_shrub": {"top_lower": 2},  # 26.3
}
BUSH_MAX_HALF = 7  # a pixel in from the block's sides, so neighbouring bushes don't merge into a hedge
BUSH_TIERS = ((0.3, 2), (0.75, 0), (1.0, 2))  # (top as share of the height, pixels in from the widest)
GLOW_OUT = 0.05    # the fireflies' layer in front of the leaves (on them, both would flicker)
# The bushes and their items go into a pack of their own (build.py), so the mod menu can switch them
# on their own (own user request)
BUSH_MODELS = {f"block/{name}" for name in BUSHES} | {"item/bush", "item/firefly_bush", "item/red_shrub"}


def bush_box(mask: list) -> tuple:
    """(half width, top row) of the square box around the picture's solid pixels."""
    columns = [x for row in mask for x, pixel in enumerate(row) if pixel == "#"]
    top = next(index for index, row in enumerate(mask) if "#" in row)
    return min(BUSH_MAX_HALF, max(8 - min(columns), max(columns) + 1 - 8)), top


def densest_window(mask: list, size: int) -> list:
    """uv of the size x size square of the picture with the most solid pixels - the tiers' tops
    (own user feedback: cut from the picture's edge like the sides, they looked odd from above)."""
    def solid(u: int, v: int) -> int:
        return sum(row[u:u + size].count("#") for row in mask[v:v + size])

    u, v = max(((u, v) for v in range(17 - size) for u in range(17 - size)), key=lambda corner: solid(*corner))
    return [u, v, u + size, v + size]


def bushes() -> None:
    for name, spec in BUSHES.items():
        mask = mask_of(name)
        widest, top = bush_box(mask)
        cross_top = max(top, spec.get("cross_top_row", 0))
        plant_height = 16 - cross_top
        body_height = 16 - max(top, spec.get("body_top_row", 0))
        extra = {"tintindex": 0} if spec.get("tint") else {}

        def leaves(uv: list, texture: str = "#cross") -> dict:
            return {**face(uv, texture), **extra}

        elements, glowing = [], []
        bottom = 0
        for index, (share, inset) in enumerate(BUSH_TIERS):
            tier_top = round(body_height * share)
            if index == len(BUSH_TIERS) - 1:
                tier_top = max(bottom + 1, tier_top - spec.get("top_lower", 0))  # at least a pixel thick
            half = max(1, widest - inset)
            low, high = 8 - half, 8 + half
            side_uv = [low, 16 - tier_top, high, 16 - bottom]
            top_uv = densest_window(mask, 2 * half)  # a closed leaf cover, not the ragged edge of a side
            faces = {side: leaves(side_uv) for side in ("north", "east", "south", "west")}
            faces["up"] = leaves(top_uv)
            if index == 1:  # the widest tier's underside shows around the narrower one below it
                faces["down"] = leaves(top_uv)
            elements.append(box([low, bottom, low], [high, tier_top, high], faces))
            # The sides' backs: faces only show from the front, so through one side's gaps the far
            # side was missing (own user report). Mirrored, facing in, on the same planes as the sides.
            back_uv = [side_uv[2], side_uv[1], side_uv[0], side_uv[3]]
            elements += [
                box([low, bottom, high], [high, tier_top, high], {"north": leaves(back_uv)}),
                box([low, bottom, low], [high, tier_top, low], {"south": leaves(back_uv)}),
                box([high, bottom, low], [high, tier_top, high], {"west": leaves(back_uv)}),
                box([low, bottom, low], [low, tier_top, high], {"east": leaves(back_uv)}),
            ]
            if "emissive" in spec:
                glowing.append(box([low - GLOW_OUT, bottom, low - GLOW_OUT], [high + GLOW_OUT, tier_top, high + GLOW_OUT],
                                   {side: face(side_uv, "#cross_emissive") for side in ("north", "east", "south", "west")}))
            bottom = tier_top
        # The crossed picture inside, as tall as the whole plant (the firefly bush's twigs included)
        low, high = 8 - widest, 8 + widest
        cross_uv = [low, cross_top, high, 16]
        turned = {"origin": [8, 0, 8], "axis": "y", "angle": 45}
        elements += [
            box([low, 0, 8], [high, plant_height, 8], {"north": leaves(cross_uv), "south": leaves(cross_uv)}, dict(turned)),
            box([8, 0, low], [8, plant_height, high], {"east": leaves(cross_uv), "west": leaves(cross_uv)}, dict(turned)),
        ]
        textures = {"particle": f"minecraft:block/{name}", "cross": f"minecraft:block/{name}"}
        if "emissive" in spec:
            textures["cross_emissive"] = f"minecraft:block/{spec['emissive']}"
            for glow in glowing:
                glow["light_emission"] = 15
            elements += glowing
        model(f"block/{name}", textures, elements)


# --- Stonecutter and calibrated sculk sensor ------------------------------------------------------
# The stonecutter's saw blade as a solid, pixel-thick blade (its outline is the same in every frame of
# the animated picture, so the edges never show see-through pixels). The calibrated sculk sensor's
# amethyst as real crystals like our amethyst clusters' - a tall one with a tip and two small ones
# leaning outwards (a single stepped column following the picture didn't convince in-game); the body
# and its four tendrils are shaped like vanilla's.

SAW_THICKNESS = 1.0
# Same form as AMETHYST's entries; sides from the picture's solid middle, tips from its top rows
SENSOR_CRYSTALS = {"body": [5, 7, 10, 15], "tip": [6, 5, 9, 7], "crystals": [
    (8, 8, 4, 9, None, 2), (11.5, 8, 2, 5, "+x", 1), (4.5, 8, 2, 4, "-x", 1)]}


def stonecutter() -> None:
    s = "#side"
    body = box([0, 0, 0], [16, 9, 16], {
        "down": face([0, 0, 16, 16], "#bottom", cullface="down"), "up": face([0, 0, 16, 16], "#top"),
        **{side: face([0, 7, 16, 16], s, cullface=side) for side in ("north", "east", "south", "west")}})
    # The picture's blade rows 9-15 stand on the body, as in vanilla's flat version
    blade = moved(wall_extrusion(mask_of("stonecutter_saw"), "#saw", SAW_THICKNESS), dy=9, dz=8 - SAW_THICKNESS / 2)
    model("block/stonecutter", {"particle": "block/stonecutter_bottom", "bottom": "block/stonecutter_bottom",
                                "top": "block/stonecutter_top", "side": "block/stonecutter_side", "saw": "block/stonecutter_saw"},
          [body] + blade, ambient_occlusion=True, parent="block/block")


def calibrated_sculk_sensor() -> None:
    body = box([0, 0, 0], [16, 8, 16], {
        "north": face([0, 8, 16, 16], "#side"), "east": face([0, 8, 16, 16], "#side"),
        "south": face([0, 8, 16, 16], "#calibrated_side"), "west": face([0, 8, 16, 16], "#side"),
        "up": face([0, 0, 16, 16], "#top"), "down": face([0, 0, 16, 16], "#bottom", cullface="down")})
    tendrils = []
    for x1, x2, z, angle in ((-1, 7, 3, 45), (9, 17, 3, -45), (9, 17, 13, 45), (-1, 7, 13, -45)):
        left = x1 < 0
        front, back = ([4, 8, 12, 16], [12, 8, 4, 16]) if left else ([12, 8, 4, 16], [4, 8, 12, 16])
        tendrils.append(box([x1, 8, z], [x2, 16, z], {"north": face(front, "#tendrils"), "south": face(back, "#tendrils")},
                            {"origin": [3 if left else 13, 12, z], "axis": "y", "angle": angle}))
    crystal = moved(crystal_elements(SENSOR_CRYSTALS, 0, "#amethyst"), dy=8)  # standing on the body
    model("block/calibrated_sculk_sensor", {
        "amethyst": "block/calibrated_sculk_sensor_amethyst", "bottom": "block/sculk_sensor_bottom",
        "side": "block/sculk_sensor_side", "calibrated_side": "block/calibrated_sculk_sensor_input_side",
        "tendrils": "block/sculk_sensor_tendril_inactive", "top": "block/calibrated_sculk_sensor_top",
        "particle": "block/sculk_sensor_bottom"}, [body] + tendrils + crystal, ambient_occlusion=True, parent="block/block")


def lily_pad() -> None:
    model("block/lily_pad", {"particle": "block/lily_pad", "texture": "block/lily_pad"},
          floor_extrusion(mask_of("lily_pad"), "#texture", 0.75, tint=True))


# --- Redstone dust --------------------------------------------------------------------------------
# Every grain of dust a little heap instead of a picture painted on the floor, still tinted with the
# power level's color. The dot where lines meet stands a bit higher than the lines, so the two never
# share a surface (overlapping surfaces at the same height flicker).
#
# 26.1 draws redstone dust translucent and says so in the model (force_translucent), as vanilla's do;
# build.py turns these texture entries back into plain names for 1.21.11, which doesn't know that form.

DUST_LINE_HEIGHT = 0.5
DUST_DOT_HEIGHT = 0.75
EMPTY_ROWS = ["." * 16] * 8


def redstone_dust() -> None:
    def dust(path: str, picture: str, elements: list) -> None:
        model(path, {"particle": "block/redstone_dust_dot",
                     "line": {"sprite": f"block/{picture}", "force_translucent": True}}, elements)

    dust("block/redstone_dust_dot", "redstone_dust_dot",
         floor_extrusion(mask_of("redstone_dust_dot"), "#line", DUST_DOT_HEIGHT, tint=True))
    # A connection to one side: the half of the line picture towards that side (the blockstate turns it)
    for suffix in ("0", "1"):
        picture = f"redstone_dust_line{suffix}"
        line = mask_of(picture)
        dust(f"block/redstone_dust_side{suffix}", picture,
             floor_extrusion(line[:8] + EMPTY_ROWS, "#line", DUST_LINE_HEIGHT, tint=True))
        dust(f"block/redstone_dust_side_alt{suffix}", picture,
             floor_extrusion(EMPTY_ROWS + line[8:], "#line", DUST_LINE_HEIGHT, tint=True))
    # Running up a wall
    dust("block/redstone_dust_up", "redstone_dust_line1",
         wall_extrusion(mask_of("redstone_dust_line1"), "#line", DUST_LINE_HEIGHT, tint=True))


# --- Amethyst -------------------------------------------------------------------------------------
# Real crystals instead of a crossed picture: a tall one in the middle, smaller ones around it leaning
# outwards. Their sides show the solid middle of each stage's own picture, their tips its top pixels.
# Modelled growing upwards; the blockstate turns them for every other side, as in vanilla. Unshaded like
# vanilla's crossed picture: the crystals glow, and shaded they came out a dull grey-purple.

# (x, z, width, height, lean: "+x"/"-x"/"+z"/"-z"/None, tip height)
AMETHYST = {
    "amethyst_cluster": {"body": [5, 3, 10, 13], "tip": [6, 2, 9, 3], "crystals": [
        (8, 8, 4, 11, None, 2), (12.5, 8, 3, 7, "+x", 1), (3.5, 8, 3, 6, "-x", 1),
        (8, 12.5, 3, 5, "+z", 1), (8, 3.5, 3, 6, "-z", 1)]},
    "large_amethyst_bud": {"body": [6, 8, 11, 14], "tip": [7, 7, 10, 8], "crystals": [
        (8, 8, 4, 7, None, 2), (12, 8.5, 3, 4, "+x", 1), (4, 7.5, 3, 4, "-x", 1)]},
    "medium_amethyst_bud": {"body": [4, 12, 12, 16], "tip": [7, 10, 10, 11], "crystals": [
        (8, 8, 3, 5, None, 1), (11, 9, 2, 3, "+x", 1), (5, 7, 2, 3, "-x", 1)]},
    "small_amethyst_bud": {"body": [4, 14, 12, 16], "tip": [7, 12, 10, 13], "crystals": [
        (8, 8, 3, 3, None, 1), (10.5, 9.5, 2, 2, None, 0), (5.5, 6.5, 2, 2, None, 0)]},
}
LEANS = {"+x": ("z", -22.5), "-x": ("z", 22.5), "+z": ("x", 22.5), "-z": ("x", -22.5)}
# Where a lean points after a quarter turn about the crystal's own axis (x, z -> 16 - z, x)
LEAN_AFTER_QUARTER_TURN = {"+x": "+z", "+z": "-x", "-x": "-z", "-z": "+x"}
# Every group comes in four turns (own user request: they all looked the same way). A blockstate's
# x/y can't turn a crystal about its own axis on a wall (that axis lies flat there), so the turns are
# separate models, and our blockstates let Minecraft pick one of the four per block position.
QUARTER_TURNS = (0, 1, 2, 3)


def turned_crystal(crystal: tuple, quarter_turns: int) -> tuple:
    x, z, width, height, lean, tip = crystal
    for _ in range(quarter_turns):
        x, z = 16 - z, x
        lean = LEAN_AFTER_QUARTER_TURN.get(lean)
    return x, z, width, height, lean, tip


def amethyst_model_name(name: str, quarter_turns: int) -> str:
    return f"block/{name}" if quarter_turns == 0 else f"block/{name}_turned_{90 * quarter_turns}"


def amethyst() -> None:
    for name, spec in AMETHYST.items():
        for quarter_turns in QUARTER_TURNS:
            amethyst_group(name, spec, quarter_turns)
        # Vanilla's own facing rotations, each with a free pick of the four turns
        facings = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180},
                   "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
        BLOCKSTATES[name] = {"variants": {
            f"facing={facing}": [{"model": f"minecraft:{amethyst_model_name(name, turns)}", **rotation} for turns in QUARTER_TURNS]
            for facing, rotation in facings.items()
        }}


def amethyst_group(name: str, spec: dict, quarter_turns: int) -> None:
    model(amethyst_model_name(name, quarter_turns), {"particle": f"minecraft:block/{name}", "cross": f"minecraft:block/{name}"},
          crystal_elements(spec, quarter_turns, "#cross"))


def crystal_elements(spec: dict, quarter_turns: int, t: str) -> list:
    """Unshaded crystals growing up from y 0, as listed in an AMETHYST-style spec."""
    elements = []
    for crystal in spec["crystals"]:
        x, z, width, height, lean, tip = turned_crystal(crystal, quarter_turns)
        rotation = None
        if lean:
            axis, angle = LEANS[lean]
            rotation = {"origin": [x, 0, z], "axis": axis, "angle": angle}
        half = width / 2
        side = face(spec["body"], t)
        elements.append(box([x - half, 0, z - half], [x + half, height, z + half],
                            {"north": side, "south": side, "east": side, "west": side,
                             "up": face(spec["tip"], t), "down": face(spec["body"], t)}, rotation))
        if tip:
            point = max(1.0, width - 2) / 2
            tip_face = face(spec["tip"], t)
            elements.append(box([x - point, height, z - point], [x + point, height + tip, z + point],
                                {"north": tip_face, "south": tip_face, "east": tip_face, "west": tip_face,
                                 "up": tip_face}, rotation))
    for element in elements:
        element["shade"] = False
    return elements


# --- Sugar cane -----------------------------------------------------------------------------------
# Four round-ish stalks with a few leaves instead of a crossed picture. Stalks take their look from
# the picture's four stalk columns; like vanilla, everything is tinted with the biome's grass color.
# Unshaded like vanilla's crossed plants - shaded, the cane came out darker than the grass around it.

SUGAR_CANE_STALKS = [
    # (x, z, picture column of the stalk, leaves: (bottom y, direction))
    (3.5, 4.5, 1, [(6, "-x"), (13, "-z")]),
    (11.5, 3.5, 5, [(11, "+z"), (2, "+x")]),
    (4.5, 11.5, 9, [(4, "-z"), (10, "+x")]),
    (12, 12, 13, [(8, "+x"), (1, "+z")]),
]
LEAF_UV = [7, 3, 8, 5]


def sugar_cane() -> None:
    t = "#cross"

    def tinted(uv, rotation: int = 0) -> dict:
        return {**face(uv, t, rotation=rotation), "tintindex": 0}

    elements = []
    for x, z, column, leaves in SUGAR_CANE_STALKS:
        side = tinted([column, 0, column + 2, 16])
        elements.append(box([x - 1, 0, z - 1], [x + 1, 16, z + 1],
                            {"north": side, "south": side, "east": side, "west": side,
                             "up": tinted([column, 0, column + 2, 2]), "down": tinted([column, 14, column + 2, 16])}))
        for bottom, direction in leaves:
            dx, dz = {"-x": (-1, 0), "+x": (1, 0), "-z": (0, -1), "+z": (0, 1)}[direction]
            # 1.5 pixels out from the stalk's side, half a pixel thin, two pixels tall
            lx, lz = x + dx * 1.75, z + dz * 1.75
            size_x, size_z = (1.5, 0.5) if dx else (0.5, 1.5)
            leaf_face = tinted(LEAF_UV)
            elements.append(box([lx - size_x / 2, bottom, lz - size_z / 2], [lx + size_x / 2, bottom + 2, lz + size_z / 2],
                                {"north": leaf_face, "south": leaf_face, "east": leaf_face, "west": leaf_face,
                                 "up": leaf_face, "down": leaf_face}))
    for element in elements:
        element["shade"] = False
    model("block/sugar_cane", {"particle": "minecraft:block/sugar_cane", "cross": "minecraft:block/sugar_cane"}, elements)


# --- Mushrooms and fungi --------------------------------------------------------------------------
# Blocky little mushrooms instead of a crossed picture: a short stem under a thick cap, the red one and
# the crimson fungus with a smaller block on top (own user request, Vanilla Tweaks' 3D mushrooms as the
# example). The plant pictures are too small to wrap around a cap, so caps and stems wear the big
# blocks' pictures - the huge mushrooms' caps, stem and gills, the nether wart blocks and stems. The
# fungi's glowing spots are little bumps cut from the orange pixels of their own plant pictures.
# Each plant comes straight and turned by 45 degrees, and each of those in four quarter turns (the
# pictures on the faces turn along) - our blockstates pick one per block; in a flower pot it stands straight.

# (stem width, stem height, cap tiers [(width, height), ...] from the bottom up, bumps [(x1, z1, x2, z2,
#  tier)] relative to the block's middle, sitting on that tier's top) - all centered in the block
MUSHROOMS = {
    "brown_mushroom": {
        "textures": {"cap": "brown_mushroom_block", "stem": "mushroom_stem", "gills": "mushroom_block_inside"},
        "plant": (2, 3, [(6, 3)], [])},
    "red_mushroom": {
        "textures": {"cap": "red_mushroom_block", "stem": "mushroom_stem", "gills": "mushroom_block_inside"},
        "plant": (2, 3, [(6, 3), (4, 2)], [])},
    "crimson_fungus": {
        "textures": {"cap": "nether_wart_block", "stem": "crimson_stem", "gills": "nether_wart_block", "spots": "crimson_fungus"},
        "plant": (2, 4, [(8, 3), (6, 3)], [(3, -1, 4, 1, 0), (-2, 3, 0, 4, 0), (-2, -2, 0, -1, 1), (1, 0, 2, 2, 1)])},
    "warped_fungus": {
        "textures": {"cap": "warped_wart_block", "stem": "warped_stem", "gills": "warped_wart_block", "spots": "warped_fungus"},
        "plant": (2, 5, [(8, 3)], [(-3, 1, -1, 2, 0), (1, -2, 2, 0, 0), (2, 2, 3, 3, 0), (-2, -3, -1, -2, 0)])},
}
# Where each picture's details sit - the red cap's white dots and the fungi's orange spots; faces are
# cut from around them in turn. Plain pictures just take a few spread-out spots. The fungi's centers are
# picked so that 1 and 2 pixel wide cuts both land on orange pixels only.
PICTURE_SPOTS = {
    "red_mushroom_block": [(4, 4), (13, 8), (8, 13)],
    "crimson_fungus": [(6.5, 6.5), (9, 7.5)],
    "warped_fungus": [(10, 8.5), (7, 10)],
}
PLAIN_SPOTS = [(5, 5), (11, 6), (6, 11), (11, 11)]
QUARTER_TURN_ANGLES = (0, 90, 180, 270)
MUSHROOM_TURNS = {"": None, "_turned": {"origin": [8, 0, 8], "axis": "y", "angle": 45}}


def picture_window(texture: str, turn: int, width: float, height: float) -> list:
    """A width x height uv region around the picture's turn-th spot, kept inside the picture."""
    spots = PICTURE_SPOTS.get(texture, PLAIN_SPOTS)
    center_u, center_v = spots[turn % len(spots)]
    u = min(max(math.floor(center_u - width / 2), 0), 16 - width)
    v = min(max(math.floor(center_v - height / 2), 0), 16 - height)
    return [u, v, u + width, v + height]


def mushroom_plant(plant: tuple, textures: dict, lift: float, on_ground: bool, rotation: dict | None = None) -> list:
    stem_width, stem_height, tiers, bumps = plant
    turn = 0

    def cut(texture: str, width: float, height: float) -> list:
        nonlocal turn
        turn += 1
        return picture_window(textures[texture], turn, width, height)

    def sides(texture: str, width: float, height: float) -> dict:
        return {name: face(cut(texture, width, height), f"#{texture}") for name in ("north", "east", "south", "west")}

    half = stem_width / 2
    stem_faces = sides("stem", stem_width, stem_height)
    if on_ground:
        stem_faces["down"] = face(cut("stem", stem_width, stem_width), "#stem", cullface="down")
    elements = [box([8 - half, lift, 8 - half], [8 + half, lift + stem_height, 8 + half], stem_faces, rotation)]
    bottom = lift + stem_height
    tops = []
    for index, (width, height) in enumerate(tiers):
        half = width / 2
        faces = {**sides("cap", width, height), "up": face(cut("cap", width, width), "#cap")}
        if index == 0:  # the tiers above sit on the one below, only the lowest one's underside shows
            faces["down"] = face(cut("gills", width, width), "#gills")
        elements.append(box([8 - half, bottom, 8 - half], [8 + half, bottom + height, 8 + half], faces, rotation))
        bottom += height
        tops.append(bottom)
    for x1, z1, x2, z2, tier in bumps:
        width, depth = x2 - x1, z2 - z1
        faces = {name: face(cut("spots", width if name in ("north", "south") else depth, 1), "#spots")
                 for name in ("north", "east", "south", "west")}
        faces["up"] = face(cut("spots", width, depth), "#spots")
        elements.append(box([8 + x1, tops[tier], 8 + z1], [8 + x2, tops[tier] + 1, 8 + z2], faces, rotation))
    return elements


def mushrooms() -> None:
    for name, spec in MUSHROOMS.items():
        textures = {key: f"minecraft:block/{texture}" for key, texture in spec["textures"].items()}
        textures["particle"] = f"minecraft:block/{name}"
        for suffix, rotation in MUSHROOM_TURNS.items():
            model(f"block/{name}{suffix}", textures, mushroom_plant(spec["plant"], spec["textures"], 0, True, rotation))
        BLOCKSTATES[name] = {"variants": {"": [
            {"model": f"minecraft:block/{name}{suffix}", **({"y": angle} if angle else {})}
            for suffix in MUSHROOM_TURNS for angle in QUARTER_TURN_ANGLES]}}
        # Potted: the pot, with the plant standing straight on its soil (4 pixels up)
        elements = flower_pot() + mushroom_plant(spec["plant"], spec["textures"], 4, False)
        model(f"block/potted_{name}", {**textures, "flowerpot": "minecraft:block/flower_pot", "dirt": "minecraft:block/dirt"}, elements)


def flower_pot() -> list:
    """The flower pot's four walls and its soil, the same shape as vanilla's pot."""
    t = "#flowerpot"
    wall_side = [5, 10, 11, 16]
    return [
        box([5, 0, 5], [6, 6, 11], {"down": face([5, 5, 6, 11], t, cullface="down"), "up": face([5, 5, 6, 11], t),
                                    "north": face([10, 10, 11, 16], t), "south": face([5, 10, 6, 16], t),
                                    "west": face(wall_side, t), "east": face(wall_side, t)}),
        box([10, 0, 5], [11, 6, 11], {"down": face([10, 5, 11, 11], t, cullface="down"), "up": face([10, 5, 11, 11], t),
                                      "north": face([5, 10, 6, 16], t), "south": face([10, 10, 11, 16], t),
                                      "west": face(wall_side, t), "east": face(wall_side, t)}),
        box([6, 0, 5], [10, 6, 6], {"down": face([6, 10, 10, 11], t, cullface="down"), "up": face([6, 5, 10, 6], t),
                                    "north": face([6, 10, 10, 16], t), "south": face([6, 10, 10, 16], t)}),
        box([6, 0, 10], [10, 6, 11], {"down": face([6, 5, 10, 6], t, cullface="down"), "up": face([6, 10, 10, 11], t),
                                      "north": face([6, 10, 10, 16], t), "south": face([6, 10, 10, 16], t)}),
        box([6, 0, 6], [10, 4, 10], {"down": face([6, 12, 10, 16], t, cullface="down"), "up": face([6, 6, 10, 10], "#dirt")}),
    ]


# --- Bookshelves ----------------------------------------------------------------------------------
# The books sit a pixel deeper than the wooden frame (top, middle shelf, bottom and the four corner
# posts), on all four sides - as if they were really standing in a shelf.

def bookshelf() -> None:
    side, end = "#side", "#end"
    elements = [box([1, 1, 1], [15, 15, 15], {name: face([1, 1, 15, 15], side, cullface=name) for name in ("north", "south", "east", "west")})]
    for y1, y2, rows in ((15, 16, [0, 0, 16, 1]), (7, 9, [0, 7, 16, 9]), (0, 1, [0, 15, 16, 16])):
        faces = {name: face(rows, side, cullface=name) for name in ("north", "south", "east", "west")}
        faces["up"] = face([0, 0, 16, 16], end, cullface="up" if y2 == 16 else None)
        faces["down"] = face([0, 0, 16, 16], end, cullface="down" if y1 == 0 else None)
        elements.append(box([0, y1, 0], [16, y2, 16], faces))
    # Corner posts between the shelves - not through the middle one, where their sides lay on its sides and flickered
    for x in (0, 15):
        for z in (0, 15):
            for y1, y2 in ((1, 7), (9, 15)):
                uv = [0, 16 - y2, 1, 16 - y1]
                elements.append(box([x, y1, z], [x + 1, y2, z + 1], {
                    "north": face(uv, side, cullface="north" if z == 0 else None),
                    "south": face(uv, side, cullface="south" if z == 15 else None),
                    "west": face(uv, side, cullface="west" if x == 0 else None),
                    "east": face(uv, side, cullface="east" if x == 15 else None),
                }))
    # block/block: the bookshelf item is drawn with this model, and needs a full block's display transforms
    model("block/bookshelf", {"particle": "minecraft:block/bookshelf", "end": "minecraft:block/oak_planks", "side": "minecraft:block/bookshelf"},
          elements, ambient_occlusion=True, parent="block/block")


# The six slots on a chiseled bookshelf's front: (slot, x from, x to, y from, y to, picture u from, u to, v from, v to)
CHISELED_SLOTS = [
    ("top_left", 10, 16, 8, 16, 0, 6, 0, 8), ("top_mid", 5, 10, 8, 16, 6, 11, 0, 8), ("top_right", 0, 5, 8, 16, 11, 16, 0, 8),
    ("bottom_left", 10, 16, 0, 8, 0, 6, 8, 16), ("bottom_mid", 5, 10, 0, 8, 6, 11, 8, 16), ("bottom_right", 0, 5, 0, 8, 11, 16, 8, 16),
]


def chiseled_bookshelf() -> None:
    """Every slot becomes a real wooden compartment: empty ones deep, filled ones with the books standing
    a little way in. The frame stays flush with the front, and side-by-side slots are parted by a board
    one pixel wide - where the picture has its dark gap between the book stacks (columns 5 and 10);
    without it, the two compartments' walls met in a divider of no thickness at all. Boards and inner
    walls are wood, taken from the picture's own frame (column 0, rows 0/7/8/15)."""
    for state, texture_name, depth in (("empty", "chiseled_bookshelf_empty", 5), ("occupied", "chiseled_bookshelf_occupied", 1)):
        t = "#texture"

        def front(frm, to, uv) -> dict:
            # Everything, the compartment included, is hidden behind a solid block in front - hence cullface north throughout
            return box(frm, to, {"north": face(uv, t, cullface="north")})

        for slot, x1, x2, y1, y2, u1, u2, v1, v2 in CHISELED_SLOTS:
            # The compartment: inside the frame (outer columns 0/15, rows 0/7/8/15) and this slot's board (its last column)
            iu1 = u1 + 1 if u1 == 0 else u1
            iu2 = u2 - 1
            iv1, iv2 = v1 + 1, v2 - 1
            ix1, ix2, iy1, iy2 = 16 - iu2, 16 - iu1, 16 - iv2, 16 - iv1
            wood_column = [0, iv1, 1, iv2]
            elements = [
                front([ix1, iy1, depth], [ix2, iy2, depth], [iu1, iv1, iu2, iv2]),  # back: books or empty shelf
                front([x1, iy2, 0], [x2, y2, 0], [u1, v1, u2, iv1]),               # frame above
                front([x1, y1, 0], [x2, iy1, 0], [u1, iv2, u2, v2]),               # frame below
                # The slot's last column: the outer frame for the rightmost slot, otherwise the board
                front([x1, iy1, 0], [ix1, iy2, 0], [iu2, iv1, u2, iv2] if u2 == 16 else wood_column),
            ]
            if u1 == 0:  # the leftmost slot also has the outer frame on its other side
                elements.append(front([ix2, iy1, 0], [x2, iy2, 0], [u1, iv1, iu1, iv2]))
            # The compartment's wooden walls, from the front to the back
            elements.append(box([ix1, iy2, 0], [ix2, iy2, depth], {"down": face([iu1, v1, iu2, v1 + 1], t, cullface="north")}))
            elements.append(box([ix1, iy1, 0], [ix2, iy1, depth], {"up": face([iu1, v2 - 1, iu2, v2], t, cullface="north")}))
            elements.append(box([ix1, iy1, 0], [ix1, iy2, depth], {"east": face(wood_column, t, cullface="north")}))
            elements.append(box([ix2, iy1, 0], [ix2, iy2, depth], {"west": face(wood_column, t, cullface="north")}))
            model(f"block/chiseled_bookshelf_{state}_slot_{slot}",
                  {"particle": f"minecraft:block/{texture_name}", "texture": f"minecraft:block/{texture_name}"}, elements)


# --- Doors and trapdoors --------------------------------------------------------------------------
# Built like real ones: a frame around the edge at full thickness (3 pixels, as in vanilla), the
# filling in between set back half a pixel on both sides, and every window a real opening with walls
# instead of a see-through spot on a solid slab. Windows differ per wood type, so every door and
# trapdoor gets models of its own, made from which pixels of its picture are solid (texture_masks.py).
#
# The shape is worked out on the picture itself - u across, v down, t through the thickness (0..3) -
# and then placed into the block by a mapping per model (door hinge side, open/closed, trapdoor
# position), so the same picture pixel always ends up at the same spot on both sides.

FRAME = (0.0, 3.0)
PANEL = (0.5, 2.5)

# The corners Minecraft hands a face's UV corners to (top-left, top-right, bottom-right, bottom-left
# as seen from outside) - the order preview.py draws faces with, checked against vanilla's doors.
FACE_NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def face_corner_order(name: str, frm, to) -> list:
    x1, y1, z1 = frm
    x2, y2, z2 = to
    return {
        "north": [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)],
        "south": [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)],
        "east": [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)],
        "west": [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)],
        "up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
        "down": [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)],
    }[name]


def flat_face(corners: list, uvs: list, normal: tuple, texture: str, cull: bool) -> dict:
    """A single flat face given by its four corners and the picture coordinates wanted at each - as a
    zero-thickness element, with the UV rectangle and UV rotation that make Minecraft map it just so."""
    corners = [tuple(round(c, 4) for c in corner) for corner in corners]
    name = next(face_name for face_name, vector in FACE_NORMALS.items() if vector == normal)
    xs, ys, zs = zip(*corners)
    frm, to = (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))
    wanted = dict(zip(corners, uvs))
    at_corner = [wanted[corner] for corner in face_corner_order(name, frm, to)]
    for steps in range(4):
        # Rotated by `steps` quarter turns, corner i shows UV corner (i - steps)
        (u1, v1), (u2, v1b), (u2b, v2), (u1b, v2b) = [at_corner[(k + steps) % 4] for k in range(4)]
        if v1 == v1b and u2 == u2b and v2 == v2b and u1 == u1b:
            return box(frm, to, {name: face([u1, v1, u2, v2], texture, rotation=90 * steps, cullface=name if cull else None)})
    raise ValueError(f"no UV rotation maps {uvs} onto {corners}")


def thickness_ranges(mask_rows: list, thickness_at) -> list:
    """Per pixel: the thickness it spans (thickness_at(u, v)), None where the picture is see-through."""
    return [[thickness_at(u, v) if mask_rows[v][u] == "#" else None for u in range(16)] for v in range(16)]


# Pictures made of upright planks with dark gaps between them (own user request: on the spruce door and
# trapdoor every plank should stand out on its own): the gaps are set back deeper than a filling, the
# planks keep the full thickness.
PLANK_GAP = (0.75, 2.25)
SPRUCE_GAPS = (3, 6, 9, 12)
PLANK_GAPS = {"spruce_door_top": SPRUCE_GAPS, "spruce_door_bottom": SPRUCE_GAPS, "spruce_trapdoor": SPRUCE_GAPS}

# Pictures that paint their own sunken panels - shadow along the top and left edge, light along the
# bottom and right (own user requests: dark oak, then oak): exactly those panels are set back, the rest
# of the door comes out to the frame's full thickness. Panels as (first column, first row, end column,
# end row). Where oak paints two panels right next to each other, the light and the dark line between
# them are read as the two sides of a ridge and stay raised - so each panel sinks in on its own.
PANELS = {
    "oak_door_top": [(2, 2, 7, 6), (9, 2, 14, 6), (2, 8, 7, 12), (9, 8, 14, 12), (2, 13, 7, 16), (9, 13, 14, 16)],
    "oak_door_bottom": [(2, 0, 7, 2), (9, 0, 14, 2), (2, 3, 7, 8), (9, 3, 14, 8), (2, 9, 7, 14), (9, 9, 14, 14)],
    "dark_oak_door_top": [(2, 2, 7, 15), (9, 2, 14, 15)],
    "dark_oak_door_bottom": [(2, 1, 7, 14), (9, 1, 14, 14)],
    "dark_oak_trapdoor": [(1, 1, 7, 7), (9, 1, 15, 7), (1, 9, 7, 15), (9, 9, 15, 15)],
}

# Metal on top of the wood (own user request): handles, hinges, and the iron bands of spruce and pale
# oak stand out a quarter pixel from both faces, whatever the shape underneath. Picked by hand from each
# picture's metal pixels (grey, brass on dark oak, pale yellow on bamboo, a lighter wood bar on
# mangrove); the trapdoors of oak, dark oak, mangrove, cherry, bamboo, crimson, warped, iron and copper
# have none painted.
FITTING = (-0.25, 3.25)


def pixel_rows(rows, columns=range(16)) -> set:
    return {(u, v) for v in rows for u in columns}


HINGES_TOP = {(0, 4), (0, 5), (0, 15)}
HINGES_BOTTOM = {(0, 0), (0, 10), (0, 11)}
LATCH = {(11, 14), (12, 14), (13, 14), (11, 15)}  # oak, jungle and iron: the latch just above the halves' seam
# Spruce: its bands run across the planks only (the gaps between stay wood), on the door not over the dark edge column
SPRUCE_DOOR_PLANKS = [u for u in range(15) if u not in SPRUCE_GAPS]
SPRUCE_TRAPDOOR_PLANKS = [u for u in range(16) if u not in SPRUCE_GAPS]
COPPER_TOP = HINGES_TOP | {(11, 15), (12, 15)}
COPPER_BOTTOM = HINGES_BOTTOM | {(11, 0), (12, 0), (10, 1), (13, 1), (10, 2), (13, 2), (10, 3), (13, 3)} | pixel_rows((4, 5), range(10, 14))
FITTINGS = {
    "oak_door_top": HINGES_TOP | LATCH,
    "oak_door_bottom": HINGES_BOTTOM,
    "spruce_door_top": pixel_rows((4, 15), SPRUCE_DOOR_PLANKS) | {(0, 5)},
    "spruce_door_bottom": pixel_rows((10,), SPRUCE_DOOR_PLANKS) | {(0, 0), (0, 11)}
                          | {(11, 0), (12, 0), (10, 1), (13, 1), (10, 2), (13, 2), (11, 3), (12, 3)},
    "spruce_trapdoor": pixel_rows((3, 12), SPRUCE_TRAPDOOR_PLANKS),
    "birch_door_top": HINGES_TOP | {(14, 14), (14, 15)},
    "birch_door_bottom": HINGES_BOTTOM,
    "birch_trapdoor": pixel_rows((14,), range(6, 10)),
    "jungle_door_top": HINGES_TOP | LATCH,
    "jungle_door_bottom": {(0, 0), (0, 9), (0, 10)},
    "jungle_trapdoor": pixel_rows((14,), range(5, 11)),
    "acacia_door_top": HINGES_TOP | {(12, 15), (13, 15), (14, 15)},
    "acacia_door_bottom": HINGES_BOTTOM | {(14, 0)},
    "acacia_trapdoor": pixel_rows((14, 15), range(5, 11)),
    "dark_oak_door_top": HINGES_TOP | {(11, 15), (12, 15), (13, 15)},
    "dark_oak_door_bottom": HINGES_BOTTOM | {(11, 0)},
    # Mangrove: an upright bar handle across the halves' seam, fixed at both ends
    "mangrove_door_top": HINGES_TOP | {(12, 13), (13, 13), (13, 14), (13, 15)},
    "mangrove_door_bottom": HINGES_BOTTOM | {(13, 0), (13, 1), (13, 2), (12, 2)},
    "bamboo_door_top": HINGES_TOP,
    "bamboo_door_bottom": HINGES_BOTTOM | {(11, 0), (12, 0), (10, 1), (13, 1), (10, 2), (13, 2), (11, 3), (12, 3)},
    "cherry_door_top": HINGES_TOP,
    "cherry_door_bottom": HINGES_BOTTOM | {(7, 3), (8, 3), (6, 4), (9, 4), (6, 5), (9, 5), (7, 6), (8, 6)},
    # Pale oak: a metal band across each half, with a bracket above and below, and a loop handle
    "pale_oak_door_top": pixel_rows((4, 5), range(13)) | {(9, 3), (10, 3), (9, 6), (10, 6), (0, 15), (11, 15), (12, 15)},
    "pale_oak_door_bottom": pixel_rows((10, 11), range(13)) | {(9, 9), (10, 9), (9, 12), (10, 12), (0, 0)}
                            | {(11, 0), (12, 0), (10, 1), (13, 1), (10, 2), (13, 2)} | pixel_rows((3,), range(10, 14)),
    "pale_oak_trapdoor": {(7, 13), (8, 13), (6, 14), (7, 14), (8, 14), (9, 14)},
    # Poplar (26.3): the usual hinges, a small dark handle in the corner; the trapdoor a bracket handle
    "poplar_door_top": HINGES_TOP | {(13, 14), (14, 14), (14, 15)},
    "poplar_door_bottom": HINGES_BOTTOM | {(14, 0), (13, 1), (14, 1)},
    "poplar_trapdoor": pixel_rows((13,), range(6, 10)) | {(6, 14), (9, 14)},
    # Crimson and warped: a U-shaped handle across the halves' seam - two posts, the bar below them
    "crimson_door_top": HINGES_TOP | {(10, 14), (10, 15), (14, 14), (14, 15)},
    "crimson_door_bottom": HINGES_BOTTOM | {(11, 0), (12, 0), (13, 0)},
    "warped_door_top": HINGES_TOP | {(10, 14), (10, 15), (14, 14), (14, 15)},
    "warped_door_bottom": HINGES_BOTTOM | {(11, 0), (12, 0), (13, 0)},
    "iron_door_top": HINGES_TOP | LATCH,
    "iron_door_bottom": HINGES_BOTTOM,
    **{f"{copper}_door_top": COPPER_TOP for copper in ("copper", "exposed_copper", "weathered_copper", "oxidized_copper")},
    **{f"{copper}_door_bottom": COPPER_BOTTOM for copper in ("copper", "exposed_copper", "weathered_copper", "oxidized_copper")},
}


def shape_of(picture: str, is_frame):
    """How thick each solid pixel of a door/trapdoor picture is: its metal on top, then planks, panels, or
    the usual frame with a set-back filling."""
    fittings = FITTINGS.get(picture, set())
    gaps = PLANK_GAPS.get(picture)
    panels = PANELS.get(picture)

    def thickness_at(u, v):
        if (u, v) in fittings:
            return FITTING
        if gaps is not None:
            return PLANK_GAP if u in gaps else FRAME
        if panels is not None:
            return PANEL if any(u1 <= u < u2 and v1 <= v < v2 for u1, v1, u2, v2 in panels) else FRAME
        return FRAME if is_frame(u, v) else PANEL
    return thickness_at


def uncovered(own: tuple, neighbour) -> list:
    """The parts of a pixel's side not covered by the neighbouring pixel's thickness."""
    if neighbour is None:
        return [own]
    parts = []
    if own[0] < neighbour[0]:
        parts.append((own[0], min(own[1], neighbour[0])))
    if neighbour[1] < own[1]:
        parts.append((max(own[0], neighbour[1]), own[1]))
    return parts


def within_slab(a: float, b: float) -> list:
    """A side at the picture's edge, split into (from, to, lies on the block's side): only the part
    within the slab's own thickness lies against the neighbouring block and may disappear with it. What a
    fitting adds beyond the slab's two faces stands in the open beside that block - hidden along with
    the rest, it left a hinge open at the back, and one could look through the door there (own user
    report, seen with a door standing next to a wall)."""
    parts = []
    if a < FRAME[0]:
        parts.append((a, min(b, FRAME[0]), False))
    if b > FRAME[0] and a < FRAME[1]:
        parts.append((max(a, FRAME[0]), min(b, FRAME[1]), True))
    if b > FRAME[1]:
        parts.append((max(a, FRAME[1]), b, False))
    return parts


def slab_quads(ranges: list, beyond) -> list:
    """The faces of a picture-shaped slab, in picture space: (corners (u, v, t), picture UVs, outward
    direction, lies on the block's outside). beyond(u, v) describes a pixel past the picture's edge:
    (its thickness or None, whether that edge is the block's outside)."""
    quads = []
    # Both flat sides: one rectangle per run of pixels with the same thickness
    for thickness in {r for row in ranges for r in row if r}:
        layer = ["".join("#" if ranges[v][u] == thickness else "." for u in range(16)) for v in range(16)]
        for u1, v1, u2, v2 in mask_rectangles(layer):
            uvs = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
            for t, direction in ((thickness[0], (0, 0, -1)), (thickness[1], (0, 0, 1))):
                # Whether this side is the block's outside depends on the placement - place_slab decides
                quads.append(([(u1, v1, t), (u2, v1, t), (u2, v2, t), (u1, v2, t)], uvs, direction, None))

    # Sides: wherever a pixel's side isn't covered by its neighbour (openings, the frame's step, the edge)
    runs: dict = {}
    for v in range(16):
        for u in range(16):
            own = ranges[v][u]
            if not own:
                continue
            for du, dv in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nu, nv = u + du, v + dv
                neighbour, outside = (ranges[nv][nu], False) if 0 <= nu < 16 and 0 <= nv < 16 else beyond(nu, nv)
                for a, b in uncovered(own, neighbour):
                    line, along = (u, v) if du else (v, u)
                    for a, b, on_block_side in within_slab(a, b) if outside else [(a, b, False)]:
                        runs.setdefault((du, dv, line, a, b, on_block_side), []).append(along)
    for (du, dv, line, a, b, outside), positions in runs.items():
        # Neighbouring pixels along the same edge become one face
        spans = []
        for position in sorted(positions):
            if spans and spans[-1][1] == position:
                spans[-1][1] = position + 1
            else:
                spans.append([position, position + 1])
        for p1, p2 in spans:
            if du:  # a side facing along u, at the pixel column's left or right edge, running down v
                edge = line if du < 0 else line + 1
                corners = [(edge, p1, a), (edge, p1, b), (edge, p2, b), (edge, p2, a)]
                uvs = [(line, p1), (line + 1, p1), (line + 1, p2), (line, p2)]
            else:  # a side facing along v, at the pixel row's top or bottom edge, running across u
                edge = line if dv < 0 else line + 1
                corners = [(p1, edge, a), (p2, edge, a), (p2, edge, b), (p1, edge, b)]
                uvs = [(p1, line), (p2, line), (p2, line + 1), (p1, line + 1)]
            quads.append((corners, uvs, (du, dv, 0), outside))
    return quads


def place_slab(quads: list, place, texture: str, outside_t: tuple) -> list:
    """Picture-space faces placed into the block. place(u, v, t) gives block coordinates; outside_t says
    which of the slab's flat sides (t = 0, t = 3) lies on the block's outside - faces there, and the set-back
    filling behind them, disappear against a solid neighbour like vanilla's."""
    elements = []
    for corners, uvs, direction, outside in quads:
        placed = [place(*corner) for corner in corners]
        origin = place(*corners[0])
        # Outward direction: move a step from a corner along the picture-space direction and see where it lands
        step = place(corners[0][0] + direction[0], corners[0][1] + direction[1], corners[0][2] + direction[2])
        normal = tuple(round(s - o) for s, o in zip(step, origin))
        if direction[2]:
            outside = outside_t[0] if direction[2] < 0 else outside_t[1]
        elements.append(flat_face(placed, uvs, normal, texture, outside))
    return elements


DOORS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar", "bamboo",
         "crimson", "warped", "iron", "copper", "exposed_copper", "weathered_copper", "oxidized_copper"]
# Trapdoors whose picture turns with the trapdoor's facing (vanilla's "orientable" templates)
ORIENTABLE_TRAPDOORS = {"acacia", "bamboo", "birch", "cherry", "crimson", "jungle", "mangrove", "pale_oak", "poplar", "spruce", "warped"}


def doors() -> None:
    for door in DOORS:
        top_mask, bottom_mask = mask_of(f"{door}_door_top"), mask_of(f"{door}_door_bottom")
        top = thickness_ranges(top_mask, shape_of(f"{door}_door_top", lambda u, v: u in (0, 15) or v == 0))
        bottom = thickness_ranges(bottom_mask, shape_of(f"{door}_door_bottom", lambda u, v: u in (0, 15) or v == 15))

        def top_beyond(u, v):
            return (bottom[0][u], False) if v == 16 else (None, True)

        def bottom_beyond(u, v):
            return (top[15][u], False) if v == -1 else (None, True)

        halves = {"top": slab_quads(top, top_beyond), "bottom": slab_quads(bottom, bottom_beyond)}
        textures = {"bottom": f"minecraft:block/{door}_door_bottom", "top": f"minecraft:block/{door}_door_top"}
        for half, quads in halves.items():
            for hinge in ("left", "right"):
                for is_open in (False, True):
                    # Vanilla's door models show the picture with u running along z for a closed
                    # left-hinged or an open right-hinged door, mirrored for the other two
                    along_z = (hinge == "left") != is_open

                    def place(u, v, t, along_z=along_z):
                        return (t, 16 - v, u if along_z else 16 - u)

                    name = f"block/{door}_door_{half}_{hinge}{'_open' if is_open else ''}"
                    model(name, {**textures, "particle": f"#{half}"}, place_slab(quads, place, f"#{half}", (True, False)))


def trapdoors() -> None:
    for wood in DOORS:
        texture = f"{wood}_trapdoor"
        ranges = thickness_ranges(mask_of(texture), shape_of(texture, lambda u, v: u in (0, 15) or v in (0, 15)))
        quads = slab_quads(ranges, lambda u, v: (None, True))
        orientable = wood in ORIENTABLE_TRAPDOORS
        # Lying down, the picture's v runs along z (backwards for orientable ones, as in vanilla); swung
        # open against the block's south side, it runs down from the top edge (up for orientable ones)
        placements = {
            "bottom": (lambda u, v, t: (u, t, 16 - v if orientable else v), (True, False)),
            "top": (lambda u, v, t: (u, 13 + t, 16 - v if orientable else v), (False, True)),
            "open": (lambda u, v, t: (u, v if orientable else 16 - v, 16 - t), (True, False)),
        }
        for state, (place, outside_t) in placements.items():
            # The item is drawn with the bottom model - it needs thin_block's display transforms
            model(f"block/{texture}_{state}", {"particle": f"minecraft:block/{texture}", "texture": f"minecraft:block/{texture}"},
                  place_slab(quads, place, "#texture", outside_t), ambient_occlusion=True,
                  parent="block/thin_block" if state == "bottom" else None)


ladder()
rail_flat()
rail_raised("block/template_rail_raised_ne", 45)
rail_raised("block/template_rail_raised_sw", -45)
rail_curved()
detector_rails()
chain()
lanterns()
bars()
dripstone()
wall_growths()
vine()
nether_vines()
bamboo_sapling()
bushes()
stonecutter()
calibrated_sculk_sensor()
lily_pad()
amethyst()
sugar_cane()
mushrooms()
bookshelf()
chiseled_bookshelf()
# --- Mace -----------------------------------------------------------------------------------------
# A real mace - breeze rod handle, steel pommel, a heavy head with spikes on every side. Replaces the
# item model itself, so it's 3D everywhere; the inventory shows the flat picture through the mod's
# "TNT Flat Inventory Icons" pack (launcher/scripts/generate_flat_icons_pack.py), like every other
# 3D item. Built upright, then leaned 45 degrees to lie along the picture's diagonal, so vanilla's
# ways of holding and showing the mace (its display transforms) fit it unchanged.
#
# Colors come from single spots of the mace picture: its steel and breeze rod pixels.

MACE_LEAN = {"origin": [8, 8, 8], "axis": "z", "angle": -45}
MACE_STEEL = [7, 2, 12, 7]       # the head's light-to-dark steel, all solid
MACE_POMMEL = [0, 13, 3, 16]
MACE_DARK = [12, 6, 13, 7]       # darkest steel, for spike tips
MACE_SPIKE = [12, 5, 15, 7]
MACE_COLLAR = [9, 8, 10, 9]
# Breeze rod: one row of the picture's handle - dark blue, light lilac, deep blue - drawn along its length
MACE_ROD = [3, 11, 6, 12]
MACE_HEAD_MIDDLE = 12.5


def mace() -> None:
    t = "#layer0"
    elements = [
        solid([7, -2.5, 7], [9, -0.5, 9], MACE_POMMEL, t, MACE_LEAN),
        solid([7.25, -0.5, 7.25], [8.75, 9, 8.75], MACE_ROD, t, MACE_LEAN, skip=("up", "down")),
        solid([6.75, 9, 6.75], [9.25, 9.5, 9.25], MACE_COLLAR, t, MACE_LEAN),
        solid([5, 9.5, 5], [11, 15.5, 11], MACE_STEEL, t, MACE_LEAN),
        solid([6, 15.5, 6], [10, 16.5, 10], [7, 2, 11, 6], t, MACE_LEAN),
        # Top spike
        solid([7, 16.5, 7], [9, 18, 9], MACE_SPIKE, t, MACE_LEAN),
        solid([7.5, 18, 7.5], [8.5, 19, 8.5], MACE_DARK, t, MACE_LEAN),
    ]
    # A spike on each side of the head: a base and a thinner point
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        def span(center: float, direction: int, near: float, far: float, half: float) -> tuple:
            if direction > 0:
                return center + near, center + far
            if direction < 0:
                return center - far, center - near
            return center - half, center + half

        for near, far, half, uv in ((3, 4.5, 1.0, MACE_SPIKE), (4.5, 5.5, 0.5, MACE_DARK)):
            x1, x2 = span(8, dx, near, far, half)
            z1, z2 = span(8, dz, near, far, half)
            elements.append(solid([x1, MACE_HEAD_MIDDLE - half, z1], [x2, MACE_HEAD_MIDDLE + half, z2], uv, t, MACE_LEAN))
    display = {
        # vanilla's handheld_mace
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 1], "scale": [1, 1, 1]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 1], "scale": [1, 1, 1]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [0, 3, 0.8], "scale": [0.9, 0.9, 0.9]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [0, 3, 0.8], "scale": [0.9, 0.9, 0.9]},
        # vanilla's item/generated: on the ground, on the head, in item frames (the inventory shows it
        # face-on, like the flat picture)
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    }
    MODELS["item/mace"] = {"gui_light": "front", "textures": {"layer0": "minecraft:item/mace", "particle": "#layer0"},
                           "elements": elements, "display": display}


doors()
trapdoors()
redstone_dust()
mace()


# --- 3D items ------------------------------------------------------------------------------------
# The items of our 3D blocks as 3D models too, in the hand, on the ground, in item frames and - unless
# the mod menu's "3D items in inventory" is off - in the inventory (own user request: Vanilla Tweaks
# had that, switchable). The switch needs nothing here: the flat inventory icons pack
# (launcher/scripts/generate_flat_icons_pack.py) picks up every item whose model this pack changes.
# Each item model is its block's 3D model moved into the middle of the item's space, shown in the
# inventory as big as a block would be; flat things (wall growths, the ladder) are held like a flat
# item, everything else like a block.

# How vanilla holds a block (block/block's transforms)
BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}
# How vanilla holds a flat item (item/generated's transforms); in the inventory turned a little, so
# the 3D pieces' depth shows
FLAT_DISPLAY = {
    "gui": {"rotation": [15, -25, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
# Lying flat (lily pad): tipped towards the viewer in the inventory, held like a block
FLOOR_DISPLAY = {**BLOCK_DISPLAY, "gui": {"rotation": [60, 45, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}
# Rails: in the inventory like vanilla's picture - rails running up the slot, filling it - but tipped
# back a little so the ties and rails show their height (own user feedback: tipped over diagonally
# like the lily pad, they didn't look right)
RAIL_DISPLAY = {**BLOCK_DISPLAY, "gui": {"rotation": [65, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}
# Doors (two blocks tall): the door's face turned towards the viewer, everything smaller
DOOR_DISPLAY = {
    "gui": {"rotation": [15, -70, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
}
GUI_FILL = 16    # how much of the inventory slot the item's longest side may fill, at the gui scale below
DOOR_GUI_FILL = 24  # doors nearly the slot's full height (own user feedback: squeezed to a block's, too small)
GUI_MAX_SCALE = 1.2

COPPER_STAGES = ["copper", "exposed_copper", "weathered_copper", "oxidized_copper"]
# item -> (our block model, textures a vanilla child model would fill in, display)
ITEM_MODELS = {
    **{name: (f"block/{name}", {}, BLOCK_DISPLAY) for name in (
        "amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud",
        "brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus", "bush", "firefly_bush", "red_shrub", "sugar_cane")},
    "weeping_vines": ("block/weeping_vines_plant", {}, BLOCK_DISPLAY),
    "twisting_vines": ("block/twisting_vines_plant", {}, BLOCK_DISPLAY),
    "pointed_dripstone": ("block/pointed_dripstone_up_tip", {}, BLOCK_DISPLAY),
    "sulfur_spike": ("block/sulfur_spike_up_tip", {}, BLOCK_DISPLAY),
    **{name: ("block/template_lantern", {"lantern": f"minecraft:block/{name}"}, BLOCK_DISPLAY)
       for name in ["lantern", "soul_lantern"] + [f"{stage}_lantern" for stage in COPPER_STAGES]},
    **{name: ("block/template_chain", {"texture": f"minecraft:block/{name}"}, BLOCK_DISPLAY)
       for name in ["iron_chain"] + [f"{stage}_chain" for stage in COPPER_STAGES]},
    **{name: ("block/rail_flat", {"rail": f"minecraft:block/{name}"}, RAIL_DISPLAY)
       for name in ("rail", "powered_rail", "activator_rail")},
    "detector_rail": ("block/detector_rail", {}, RAIL_DISPLAY),
    "lily_pad": ("block/lily_pad", {}, FLOOR_DISPLAY),
    **{name: (f"block/{name}", {}, FLAT_DISPLAY) for name in ("glow_lichen", "sculk_vein", "vine", "ladder")},
}
# Items of blocks that already are 3D in vanilla (own user request): the item points at vanilla's block
# model, only moved into the middle of the slot and scaled like a block - no vanilla geometry copied.
# The block models' bounds (from the 1.21.11 and 26.1.2 jars, the same in both): (low, high).
VANILLA_3D_ITEMS = {
    "lever": ("block/lever", (5, 0, 4), (11, 11, 12)),
    "tripwire_hook": ("block/tripwire_hook", (6, 1, 7.9), (10, 9, 16)),
    "repeater": ("block/repeater_1tick", (0, 0, 0), (16, 7, 16)),
    "comparator": ("block/comparator", (0, 0, 0), (16, 7, 16)),
    "torch": ("block/torch", (7, 0, 7), (9, 10, 9)),
    "soul_torch": ("block/soul_torch", (7, 0, 7), (9, 10, 9)),
    "copper_torch": ("block/copper_torch", (7, 0, 7), (9, 10, 9)),
    "redstone_torch": ("block/redstone_torch", (6.5, 0, 6.5), (9.5, 10.5, 9.5)),
    "campfire": ("block/campfire", (0, 0, 0), (16, 17, 16)),
    "soul_campfire": ("block/soul_campfire", (0, 0, 0), (16, 17, 16)),
    # Second round (own user request): what Vanilla Tweaks' "3D Tiles" had
    "cauldron": ("block/cauldron", (0, 0, 0), (16, 16, 16)),
    "hopper": ("block/hopper", (0, 0, 0), (16, 16, 16)),
    "brewing_stand": ("block/brewing_stand", (1, 0, 1), (15, 14, 15)),
    "flower_pot": ("block/flower_pot", (5, 0, 5), (11, 6, 11)),
    "cake": ("block/cake", (1, 0, 1), (15, 8, 15)),
    "sea_pickle": ("block/sea_pickle", (6, 0, 6), (10, 8.7, 10)),
    "turtle_egg": ("block/turtle_egg", (5, 0, 4), (9, 7, 8)),
    "sniffer_egg": ("block/sniffer_egg_not_cracked", (1, 0, 2), (15, 16, 14)),
    "item_frame": ("block/item_frame", (2, 2, 15), (14, 14, 16)),
    "glow_item_frame": ("block/glow_item_frame", (2, 2, 15), (14, 14, 16)),
    **{f"{color}candle": (f"block/{color}candle_one_candle", (7, 0, 7), (9, 7, 9)) for color in [""] + [f"{c}_" for c in (
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black")]},
}
# The tripwire hook and the item frames hang on a wall facing north: turned round so their front faces the slot
VANILLA_3D_GUI_TURN = {"tripwire_hook": [20, 160, 0], "item_frame": [20, 160, 0], "glow_item_frame": [20, 160, 0]}
# Lit from the front in the inventory, like flat items (own user feedback: the campfires' upright
# fire got little of the inventory's side light and looked dark there; gui_light only acts there)
VANILLA_3D_FRONT_LIT = {"campfire", "soul_campfire"}


def turned(rotation: list, vector: list) -> list:
    """vector turned by a display rotation (x, then y, then z degrees, right-handed like the game's)."""
    x, y, z = vector
    rx, ry, rz = (math.radians(angle) for angle in rotation)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return [x, y, z]


def vanilla_3d_item(parent: str, low, high, gui_rotation: list) -> dict:
    """An item model on a vanilla block model, its middle moved to the slot's middle (translation is
    applied after turning and scaling, so the offset is turned and scaled first)."""
    longest = max(h - l for l, h in zip(low, high))
    scale = round(min(GUI_MAX_SCALE, BLOCK_DISPLAY["gui"]["scale"][0] * GUI_FILL / longest), 3)
    offset = turned(gui_rotation, [scale * ((l + h) / 2 - 8) for l, h in zip(low, high)])
    gui = {"rotation": gui_rotation, "translation": [round(-offset[0], 3), round(-offset[1], 3), 0], "scale": [scale] * 3}
    return {"parent": f"minecraft:{parent}", "display": {**BLOCK_DISPLAY, "gui": gui}}


# Items whose flat vanilla picture carries its own color while our 3D model is tinted like the block:
# their item definitions get the block's tint (sugar cane: plains grass)
ITEM_TINTS = {"sugar_cane": [{"type": "minecraft:grass", "temperature": 0.8, "downfall": 0.4}]}
ITEMS: dict = {}


def item_model(elements: list, textures: dict, display: dict, fill: float = GUI_FILL) -> dict:
    """Copies of the elements moved into the middle of the item's space; in the inventory scaled so
    the longest side fills the slot like a block does."""
    elements = copy.deepcopy(elements)
    low = [min(e["from"][i] for e in elements) for i in range(3)]
    high = [max(e["to"][i] for e in elements) for i in range(3)]
    moved(elements, *(8 - (low[i] + high[i]) / 2 for i in range(3)))
    longest = max(high[i] - low[i] for i in range(3))
    gui = display["gui"]
    scale = round(min(GUI_MAX_SCALE, gui["scale"][0] * fill / longest), 3)
    return {"textures": textures, "elements": elements, "display": {**display, "gui": {**gui, "scale": [scale] * 3}}}


def items_3d() -> None:
    for item, (block_model, textures, display) in ITEM_MODELS.items():
        block = MODELS[block_model]
        MODELS[f"item/{item}"] = item_model(block["elements"], {**block["textures"], **textures}, display)
    # Doors: the whole door, both halves on top of each other
    for door in DOORS:
        bottom, top = MODELS[f"block/{door}_door_bottom_left"], MODELS[f"block/{door}_door_top_left"]
        elements = bottom["elements"] + moved(copy.deepcopy(top["elements"]), dy=16)
        MODELS[f"item/{door}_door"] = item_model(elements, {**top["textures"], **bottom["textures"]}, DOOR_DISPLAY, DOOR_GUI_FILL)
    # Bars: a straight piece, the post with both sides
    for bars in ["iron"] + COPPER_STAGES:
        texture = f"minecraft:block/{bars}_bars"
        elements = [e for part in ("post_ends", "side", "side_alt") for e in MODELS[f"block/template_bars_{part}"]["elements"]]
        MODELS[f"item/{bars}_bars"] = item_model(elements, {"particle": texture, "bars": texture, "edge": texture}, BLOCK_DISPLAY)
    # Bamboo: the stalk with a few leaves, as if it had grown (own user feedback: a bare thin stick
    # didn't look good) - the stalk's faces cut from its picture like vanilla's, the leaves crossed
    side = face([0, 0, 2, 16], "#stalk")
    elements = [
        box([7, 0, 7], [9, 16, 9], {"north": side, "south": side, "east": side, "west": side,
                                    "up": face([13, 0, 15, 2], "#stalk"), "down": face([13, 4, 15, 6], "#stalk")}),
        box([0.8, 0, 8], [15.2, 16, 8], {"north": face([0, 0, 16, 16], "#leaves"), "south": face([0, 0, 16, 16], "#leaves")}),
        box([8, 0, 0.8], [8, 16, 15.2], {"east": face([0, 0, 16, 16], "#leaves"), "west": face([0, 0, 16, 16], "#leaves")}),
    ]
    MODELS["item/bamboo"] = item_model(elements, {"particle": "minecraft:block/bamboo_stalk", "stalk": "minecraft:block/bamboo_stalk",
                                                  "leaves": "minecraft:block/bamboo_small_leaves"}, BLOCK_DISPLAY)
    for item, (parent, low, high) in VANILLA_3D_ITEMS.items():
        MODELS[f"item/{item}"] = vanilla_3d_item(parent, low, high, VANILLA_3D_GUI_TURN.get(item, BLOCK_DISPLAY["gui"]["rotation"]))
        if item in VANILLA_3D_FRONT_LIT:
            MODELS[f"item/{item}"]["gui_light"] = "front"
    # Glass panes: a straight piece of pane, glass on its faces, the pane's edge picture around it -
    # see-through like vanilla's panes (26.1 marks their pictures translucent, older versions ignore it)
    for color in [""] + [f"{c}_stained_" for c in (
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black")]:
        glass = "glass" if not color else f"{color}glass"
        pane = {"sprite": f"minecraft:block/{glass}", "force_translucent": True}
        edge = {"sprite": f"minecraft:block/{glass}_pane_top", "force_translucent": True}
        edge_face = face([7, 0, 9, 16], "#edge")
        elements = [box([7, 0, 0], [9, 16, 16], {"east": face([0, 0, 16, 16], "#pane"), "west": face([0, 0, 16, 16], "#pane"),
                                                  "north": edge_face, "south": edge_face, "up": edge_face, "down": edge_face})]
        MODELS[f"item/{glass}_pane"] = item_model(elements, {"particle": pane, "pane": pane, "edge": edge}, BLOCK_DISPLAY)
    for item, tints in ITEM_TINTS.items():
        ITEMS[item] = {"model": {"type": "minecraft:model", "model": f"minecraft:item/{item}", "tints": tints}}


items_3d()


# --- Entity-shaped 3D items ----------------------------------------------------------------------
# Boats, rafts, minecarts, signs, hanging signs and the armor stand, rebuilt from their entity models
# by entity_items.py. A minecart's load is a block made small, like the game draws it (three quarters
# of a block): the same pictures on smaller boxes.

CART_LOAD_SCALE = 0.75
# How much of the slot signs and the armor stand fill (own user feedback: at the full slot, 16, they
# came out bigger than the items around them)
SIGN_GUI_FILL = 12
ARMOR_STAND_GUI_FILL = 13


def cube(textures: dict) -> list:
    """A full block: {face: texture variable}."""
    return [box([0, 0, 0], [16, 16, 16], {side: face([0, 0, 16, 16], texture) for side, texture in textures.items()})]


def hopper_shape() -> list:
    """A hopper, simplified: the wide top (its picture's open middle shows the bowl's floor a pixel
    below), the narrow middle, the spout."""
    outside, inside, top = "#hopper_outside", "#hopper_inside", "#hopper_top"
    return [
        box([0, 10, 0], [16, 16, 16], {"up": face([0, 0, 16, 16], top), "down": face([0, 0, 16, 16], outside),
                                       **{side: face([0, 0, 16, 6], outside) for side in ("north", "east", "south", "west")}}),
        box([2, 10, 2], [14, 11, 14], {"up": face([2, 2, 14, 14], inside)}),
        box([4, 4, 4], [12, 10, 12], {"down": face([4, 4, 12, 12], outside),
                                      **{side: face([4, 6, 12, 12], outside) for side in ("north", "east", "south", "west")}}),
        box([6, 0, 6], [10, 4, 10], {"down": face([6, 6, 10, 10], outside),
                                     **{side: face([6, 12, 10, 16], outside) for side in ("north", "east", "south", "west")}}),
    ]


CART_LOADS = {
    "chest_minecart": None,  # the chest: built from the chest's entity model below
    "furnace_minecart": ({"furnace_front": "minecraft:block/furnace_front", "furnace_side": "minecraft:block/furnace_side",
                          "furnace_top": "minecraft:block/furnace_top"},
                         lambda: cube({"north": "#furnace_front", "south": "#furnace_side", "east": "#furnace_side",
                                       "west": "#furnace_side", "up": "#furnace_top", "down": "#furnace_top"})),
    "tnt_minecart": ({"tnt_side": "minecraft:block/tnt_side", "tnt_top": "minecraft:block/tnt_top", "tnt_bottom": "minecraft:block/tnt_bottom"},
                     lambda: cube({**{side: "#tnt_side" for side in ("north", "east", "south", "west")}, "up": "#tnt_top", "down": "#tnt_bottom"})),
    "command_block_minecart": ({"command_front": "minecraft:block/command_block_front", "command_back": "minecraft:block/command_block_back",
                                "command_side": "minecraft:block/command_block_side"},
                               lambda: cube({"north": "#command_front", "south": "#command_back", "east": "#command_side",
                                             "west": "#command_side", "up": "#command_side", "down": "#command_side"})),
    "hopper_minecart": ({"hopper_outside": "minecraft:block/hopper_outside", "hopper_inside": "minecraft:block/hopper_inside",
                         "hopper_top": "minecraft:block/hopper_top"}, hopper_shape),
}


def scaled(elements: list, factor: float, anchor) -> list:
    """The elements shrunk towards anchor (their pictures shrink along)."""
    for element in elements:
        for key in ("from", "to"):
            element[key] = [round(a + (v - a) * factor, 4) for v, a in zip(element[key], anchor)]
        if "rotation" in element:
            element["rotation"] = {**element["rotation"], "origin": [round(a + (v - a) * factor, 4)
                                                                     for v, a in zip(element["rotation"]["origin"], anchor)]}
    return elements


def entity_items_3d() -> None:
    e = entity_items
    for wood in e.WOODS:
        MODELS[f"item/{wood}_boat"] = item_model(e.model_elements(e.BOAT, "#texture", (128, 64)),
                                                 {"texture": f"minecraft:entity/boat/{wood}", "particle": f"minecraft:block/{wood}_planks"}, BLOCK_DISPLAY)
        MODELS[f"item/{wood}_chest_boat"] = item_model(e.model_elements(e.BOAT + e.boat_chest(0), "#texture", (128, 128)),
                                                       {"texture": f"minecraft:entity/chest_boat/{wood}", "particle": f"minecraft:block/{wood}_planks"}, BLOCK_DISPLAY)
    MODELS["item/bamboo_raft"] = item_model(e.model_elements(e.RAFT, "#texture", (128, 64)),
                                            {"texture": "minecraft:entity/boat/bamboo", "particle": "minecraft:block/bamboo_planks"}, BLOCK_DISPLAY)
    MODELS["item/bamboo_chest_raft"] = item_model(e.model_elements(e.RAFT + e.boat_chest(-5.1), "#texture", (128, 128)),
                                                  {"texture": "minecraft:entity/chest_boat/bamboo", "particle": "minecraft:block/bamboo_planks"}, BLOCK_DISPLAY)
    for wood in e.SIGN_WOODS:
        planks = f"minecraft:block/{wood}_planks" if wood != "bamboo" else "minecraft:block/bamboo_planks"
        MODELS[f"item/{wood}_sign"] = item_model(e.model_elements(e.SIGN, "#texture", (64, 32)),
                                                 {"texture": f"minecraft:entity/signs/{wood}", "particle": planks}, FLAT_DISPLAY, SIGN_GUI_FILL)
        MODELS[f"item/{wood}_hanging_sign"] = item_model(e.model_elements(e.HANGING_SIGN, "#texture", (64, 32)),
                                                         {"texture": f"minecraft:entity/signs/hanging/{wood}", "particle": planks}, FLAT_DISPLAY, SIGN_GUI_FILL)
    # The bell as it stands on the floor, between its two stone posts under the wooden bar (own user
    # request) - posts and bar where the placed bell's block model has them
    stand = [box([x1, 0, 6], [x2, 16, 10], {**{side: face([0, 1, 2 if side in ("north", "south") else 4, 16], "#post")
                                              for side in ("north", "east", "south", "west")},
                                           "up": face([0, 0, 2, 4], "#post"), "down": face([0, 0, 2, 4], "#post", cullface="down")})
             for x1, x2 in ((0, 2), (14, 16))]
    stand.append(box([2, 13, 7], [14, 15, 9], {side: face([2, 3, 14, 5], "#bar") for side in ("north", "south", "up", "down")}))
    MODELS["item/bell"] = item_model(stand + e.model_elements(e.BELL, "#texture", (32, 32), flip=False),
                                     {"texture": "minecraft:entity/bell/bell_body", "post": "minecraft:block/stone",
                                      "bar": "minecraft:block/dark_oak_planks", "particle": "minecraft:block/gold_block"},
                                     {**BLOCK_DISPLAY, "gui": {**BLOCK_DISPLAY["gui"], "rotation": [20, 30, 0]}})  # the stand's front towards the slot
    MODELS["item/end_crystal"] = item_model(e.model_elements(e.END_CRYSTAL, "#texture", (64, 32), flip=False),
                                            {"texture": e.END_CRYSTAL_TEXTURE, "particle": "minecraft:block/obsidian"}, BLOCK_DISPLAY)
    MODELS["item/armor_stand"] = item_model(e.model_elements(e.ARMOR_STAND, "#texture", (64, 64)),
                                            {"texture": e.ARMOR_STAND_TEXTURE, "particle": "minecraft:block/oak_planks"}, FLAT_DISPLAY, ARMOR_STAND_GUI_FILL)
    # Minecarts: the cart, and on its floor the load
    cart = e.model_elements(e.MINECART, "#cart", (64, 32))
    floor = max(el["to"][1] for el in cart if el["to"][1] - el["from"][1] <= 2.01)  # top of the bottom plate
    middle = [(min(el["from"][i] for el in cart) + max(el["to"][i] for el in cart)) / 2 for i in range(3)]
    cart_textures = {"cart": e.MINECART_TEXTURE, "particle": "minecraft:item/minecart"}
    MODELS["item/minecart"] = item_model(cart, cart_textures, BLOCK_DISPLAY)
    size = 16 * CART_LOAD_SCALE
    for item, load in CART_LOADS.items():
        if load is None:
            textures = {"chest": "minecraft:entity/chest/normal"}
            elements = e.model_elements(e.CHEST, "#chest", (64, 64), flip=e.CHEST_FLIP)
        else:
            textures, shape = load
            elements = shape()
        low = [min(el["from"][i] for el in elements) for i in range(3)]
        high = [max(el["to"][i] for el in elements) for i in range(3)]
        scaled(elements, size / max(h - l for l, h in zip(low, high)), low)
        moved(elements, middle[0] - low[0] - size / 2, floor - low[1], middle[2] - low[2] - size / 2)
        MODELS[f"item/{item}"] = item_model(copy.deepcopy(cart) + elements, {**cart_textures, **textures}, BLOCK_DISPLAY)


entity_items_3d()

# --- Hanging signs from 26.3 on -------------------------------------------------------------------
# Since 26.3 a hanging sign is a plain block model - before, the game drew it in code, and so did our
# mod for its 3D chains (HangingSignChains.java in mod/1.21.11 and mod/26.1.2). There the pack takes
# over: the sign templates again, with the same chain links made of small bars in place of the flat
# crossed chain pictures. Every wood's models are children of these templates, so all of them follow.
# Only in packs for 26.3 and later (MODELS_SINCE, by resource pack format): older games never load
# these models, and only 26.x takes element rotations by any angle and around several axes.

MODELS_SINCE: dict = {97: {}}

SIGN_CHAIN_TEXTURE = "minecraft:block/iron_chain"
# Solid pixels of the chain picture: a lighter one for a link's long sides, a darker one for its ends
SIGN_CHAIN_SIDE = [1, 1, 2, 2]
SIGN_CHAIN_END = [0, 3, 1, 4]
SIGN_PIVOT = [8, 0, 8]
SIGN_CEILING = [8, 16, 8]  # where the two legs of an attached sign's chains meet
SIGN_NOTCH_INSET = 0.1


def chain_link(x: float, top: float, height: float, facing_front: bool, clip: float = 16) -> list:
    """One link hanging down from `top`: two bars a pixel apart with a pixel-sized bar across each end -
    seen from the sign's front (facing_front) or turned a quarter. Nothing is drawn above `clip`."""
    low = top - height
    upper = min(top, clip)
    if facing_front:
        sides = [([x - 1.5, low, 7.5], [x - 0.5, upper, 8.5]), ([x + 0.5, low, 7.5], [x + 1.5, upper, 8.5])]
        hidden = ("east", "west")  # the end bars' faces that lie against the side bars
    else:
        sides = [([x - 0.5, low, 6.5], [x + 0.5, upper, 7.5]), ([x - 0.5, low, 8.5], [x + 0.5, upper, 9.5])]
        hidden = ("north", "south")
    elements = [solid(frm, to, SIGN_CHAIN_SIDE, "#chain") for frm, to in sides]
    for end_top in (top, low + 1):
        if end_top <= clip:
            elements.append(solid([x - 0.5, end_top - 1, 7.5], [x + 0.5, end_top, 8.5], SIGN_CHAIN_END, "#chain", skip=hidden))
    return elements


def euler_zyx(matrix: list) -> dict:
    """The x, y, z angles of the game's several-axes element rotation (it turns around x, then y, then z)."""
    y = -math.asin(max(-1.0, min(1.0, matrix[2][0])))
    x = math.atan2(matrix[2][1], matrix[2][2])
    z = math.atan2(matrix[1][0], matrix[0][0])
    return {name: round(math.degrees(angle), 4) for name, angle in (("x", x), ("y", y), ("z", z))}


def sign_turn(turn: float) -> dict | None:
    """The whole sign turned around its middle - how the templates for the in-between rotations differ."""
    return {"angle": turn, "axis": "y", "origin": SIGN_PIVOT} if turn else None


def leg_turn(lean: float, turn: float) -> dict:
    """A chain leg leaning sideways from the ceiling's middle, then turned along with the sign."""
    if not turn:
        return {"angle": lean, "axis": "z", "origin": SIGN_CEILING}
    cl, sl = math.cos(math.radians(lean)), math.sin(math.radians(lean))
    ct, st = math.cos(math.radians(turn)), math.sin(math.radians(turn))
    lean_matrix = [[cl, -sl, 0], [sl, cl, 0], [0, 0, 1]]
    turn_matrix = [[ct, 0, st], [0, 1, 0], [-st, 0, ct]]
    both = [[sum(turn_matrix[i][k] * lean_matrix[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return {**euler_zyx(both), "origin": SIGN_CEILING}


def turned_all(elements: list, rotation: dict | None) -> list:
    for element in elements:
        if rotation:
            element["rotation"] = dict(rotation)
    return elements


def sign_board() -> dict:
    """The board, where and with the picture regions the game's own hanging sign has."""
    return box([1, 0, 7], [15, 10, 9], {
        "north": face([9, 8, 16, 13], "#all"), "east": face([8, 8, 9, 13], "#all"),
        "south": face([1, 8, 8, 13], "#all"), "west": face([0, 8, 1, 13], "#all"),
        "up": face([1, 7, 8, 8], "#all"), "down": face([1, 13, 8, 14], "#all", cullface="down")})


def side_chains(clip: float = 16) -> list:
    """A chain of two links from the ceiling to the board near each end, the links hooked into each other."""
    return [element for x in (3, 13)
            for element in chain_link(x, 16, 4, True, clip) + chain_link(x, 14, 4, False, clip)]


def middle_chains(turn: float) -> list:
    """Two legs from the middle of the ceiling down to the board's corners, three links each."""
    elements = []
    for lean in (45, -45):
        # Each leg starts half a pixel below the spot both lean around, with a link seen from its narrow
        # side: so the two only touch along an edge. Starting at that spot with links facing the front,
        # they crossed each other - three pixels wide, flickering through one another and sticking up
        # into a chain block above (own user report).
        # The link in the middle faces the front and is long enough to show its hole between the other two's ends
        leg = chain_link(8, 15.5, 4, False) + chain_link(8, 13.5, 5, True) + chain_link(8, 10.5, 3.5, False)
        elements += turned_all(leg, leg_turn(lean, turn))
    # The notch the two legs leave between them under the ceiling: filled by a piece a little thinner
    # than the links, so its faces lie behind theirs and not in the same plane
    notch = solid([7.3, 15.3, 6.5 + SIGN_NOTCH_INSET], [8.7, 16, 9.5 - SIGN_NOTCH_INSET], SIGN_CHAIN_SIDE, "#chain")
    elements += turned_all([notch], sign_turn(turn))
    return elements


def sign_template(name: str, elements: list) -> None:
    MODELS_SINCE[97][f"block/{name}"] = {"parent": "block/block", "ambientocclusion": False,
                                         "textures": {"chain": SIGN_CHAIN_TEXTURE}, "elements": elements}


def hanging_signs() -> None:
    for step in range(4):
        turn = -22.5 * step
        sign_template(f"template_hanging_sign_rot_{step}", turned_all([sign_board()] + side_chains(), sign_turn(turn)))
        sign_template(f"template_attached_hanging_sign_rot_{step}",
                      turned_all([sign_board()], sign_turn(turn)) + middle_chains(turn))
    # On a wall: the bar it hangs from, the chains start at its underside
    bar = box([0, 14, 6], [16, 16, 10], {
        "north": face([0, 3.5, 8, 4.5], "#all"), "east": face([8, 2, 10, 3], "#all", cullface="east"),
        "south": face([0, 2, 8, 3], "#all"), "west": face([8, 3.5, 10, 4.5], "#all", cullface="west"),
        "up": face([0, 0, 8, 2], "#all", cullface="up"), "down": face([8, 4.5, 0, 6.5], "#all")})
    sign_template("template_wall_hanging_sign", [sign_board(), bar] + side_chains(clip=14))


def standing_sign() -> list:
    """A sign on its post, where and with the picture regions the game's own has since 26.3."""
    third = 16 / 12  # one pixel of the sign's picture: its board is 12 of them wide and fills the block's width
    post = box([8 - third / 2, 0, 8 - third / 2], [8 + third / 2, 7 * third, 8 + third / 2], {
        "north": face([14, 8, 15, 15], "#all"), "east": face([15, 0, 16, 7], "#all"),
        "south": face([14, 0, 15, 7], "#all"), "west": face([15, 8, 16, 15], "#all"),
        "down": face([14, 15, 15, 16], "#all")})
    board = box([0, 7 * third, 8 - third / 2], [16, 13 * third, 8 + third / 2], {
        "north": face([0, 8, 12, 14], "#all"), "east": face([12, 1, 13, 7], "#all"),
        "south": face([0, 1, 12, 7], "#all"), "west": face([12, 8, 13, 14], "#all"),
        "up": face([0, 0, 12, 1], "#all"), "down": face([0, 14, 12, 15], "#all")})
    return [post, board]


def sign_items() -> None:
    """The sign items in 3D for 26.3 and later: the entity pictures the older versions' items are cut
    from (entity_items.py) are gone there, the signs' pictures now are block textures."""
    for wood in entity_items.SIGN_WOODS:
        planks = f"minecraft:block/{wood}_planks"
        MODELS_SINCE[97][f"item/{wood}_sign"] = item_model(
            standing_sign(), {"all": f"minecraft:block/{wood}_sign", "particle": planks}, FLAT_DISPLAY, SIGN_GUI_FILL)
        MODELS_SINCE[97][f"item/{wood}_hanging_sign"] = item_model(
            [sign_board()] + side_chains(),
            {"all": f"minecraft:block/{wood}_hanging_sign", "chain": SIGN_CHAIN_TEXTURE, "particle": planks}, FLAT_DISPLAY, SIGN_GUI_FILL)


hanging_signs()
sign_items()
