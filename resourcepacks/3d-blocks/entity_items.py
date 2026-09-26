"""3D items shaped like their entities: boats and rafts (with and without chest), minecarts, signs,
hanging signs and the armor stand (own user request: 3D in the inventory, like Vanilla Tweaks had).

Their pictures are entity textures, laid out for the entity models' boxes. This module rebuilds those
boxes as block-model elements: every box is given the way the game's entity model defines it (texture
offset, corner, size, and its part's position and turn), and each face gets exactly the piece of the
picture the game would put on it - worked out the same way the game's ModelPart does it. Only numbers
are taken from the game's model definitions; no textures are copied.

Entity textures aren't in the block texture atlas, so the pack adds them there (ATLAS_SOURCES, written
by build.py); two of them moved in 26.1 (TEXTURE_RENAMES).
"""
import math

# Entity model space: y points down, and the game draws entities turned half round the z axis
# (x and y flipped) - that turn, applied here, gives block-model space (x east, y up, z south).
FACES = ("down", "up", "west", "north", "east", "south")
FACE_VECTORS = {"down": (0, -1, 0), "up": (0, 1, 0), "west": (-1, 0, 0), "north": (0, 0, -1),
                "east": (1, 0, 0), "south": (0, 0, 1)}  # the game's directions (the polygons' normals)


def rotated(vector, rotation):
    """A part's turn (x, y, z radians), applied like the game's rotationZYX: x first, then y, then z."""
    x, y, z = vector
    rx, ry, rz = rotation
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return x, y, z


def to_block(point, flip: bool):
    x, y, z = point
    return (-x, -y, z) if flip else (x, y, z)


def face_corners(face: str, frm, to) -> list:
    """The face's corners top-left, top-right, bottom-right, bottom-left, as the game orders a block
    model face's UV corners (the same as preview.py's)."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    return {
        "north": [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)],
        "south": [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)],
        "east": [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)],
        "west": [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)],
        "up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
        "down": [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)],
    }[face]


def cube_polygons(uv, origin, size, mirror: bool):
    """The game's ModelPart.Cube: per direction the four corners (entity model space) and their
    texture pixel coordinates."""
    u, v = uv
    w, h, d = size
    f, g, hz = origin
    s, t, uz = f + w, g + h, hz + d
    if mirror:
        f, s = s, f
    v1, v2, v3, v4 = (f, g, hz), (s, g, hz), (s, t, hz), (f, t, hz)
    v5, v6, v7, v8 = (f, g, uz), (s, g, uz), (s, t, uz), (f, t, uz)
    cu = [u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w]
    cv = [v, v + d, v + d + h]
    polygons = {
        "down": ([v6, v5, v1, v2], cu[1], cv[0], cu[2], cv[1]),
        "up": ([v3, v4, v8, v7], cu[2], cv[1], cu[3], cv[0]),
        "west": ([v1, v5, v8, v4], cu[0], cv[1], cu[1], cv[2]),
        "north": ([v2, v1, v4, v3], cu[1], cv[1], cu[2], cv[2]),
        "east": ([v6, v2, v3, v7], cu[2], cv[1], cu[4], cv[2]),
        "south": ([v5, v6, v7, v8], cu[4], cv[1], cu[5], cv[2]),
    }
    result = {}
    for direction, (corners, u1, pv1, u2, pv2) in polygons.items():
        uvs = [(u2, pv1), (u1, pv1), (u1, pv2), (u2, pv2)]
        pairs = list(zip(corners, uvs))
        if mirror:
            pairs.reverse()
            direction = {"west": "east", "east": "west"}.get(direction, direction)
        result[direction] = pairs
    return result


def face_uv(pairs, face: str, frm, to, texture_size):
    """The block-model face (uv 0..16 and uv turn) that puts the same picture pixels on the same
    corners as the entity polygon (corners already in block space), or None if the face has no area."""
    width, height = texture_size
    corners = face_corners(face, frm, to)
    if len({tuple(round(c, 3) for c in corner) for corner in corners}) < 4:
        return None

    def uv_at(corner):
        for position, uv in pairs:
            if all(abs(a - b) < 1e-3 for a, b in zip(position, corner)):
                return uv
        raise ValueError(f"no polygon corner at {corner}")

    uvs = [uv_at(corner) for corner in corners]  # at top-left, top-right, bottom-right, bottom-left
    for steps in range(4):
        c = [uvs[(k + steps) % 4] for k in range(4)]
        if c[0][1] == c[1][1] and c[1][0] == c[2][0] and c[2][1] == c[3][1] and c[3][0] == c[0][0]:
            uv = [c[0][0] * 16 / width, c[0][1] * 16 / height, c[2][0] * 16 / width, c[2][1] * 16 / height]
            return [round(value, 4) for value in uv], steps * 90
    raise ValueError(f"{face}: picture corners {uvs} don't form a rectangle")


def part_elements(part: dict, texture: str, texture_size, flip: bool = True) -> list:
    """Block-model elements for one entity model part:
    {"offset": (x, y, z), "rotation": (x, y, z radians, quarter turns only), "cubes": [(uv, origin,
    size, mirror), ...]}."""
    offset = part.get("offset", (0, 0, 0))
    rotation = part.get("rotation", (0, 0, 0))
    elements = []
    inset = part.get("inset", 0)
    for uv, origin, size, mirror in part["cubes"]:
        placed = {}
        for direction, pairs in cube_polygons(uv, origin, size, mirror).items():
            block_pairs = []
            for position, picture_uv in pairs:
                turned = rotated(position, rotation)
                entity = tuple(a + b for a, b in zip(turned, offset))
                block_pairs.append((tuple(round(c, 4) for c in to_block(entity, flip)), picture_uv))
            placed[direction] = block_pairs
        points = [p for pairs in placed.values() for p, _ in pairs]
        frm = [min(p[i] for p in points) for i in range(3)]
        to = [max(p[i] for p in points) for i in range(3)]
        faces = {}
        # Pictures are placed on the full box first; "inset" then pulls the box in a hair, for parts
        # the game lets overlap a neighbour (their faces would lie on the neighbour's and flicker)
        for direction, pairs in placed.items():
            name = placed_face(pairs, frm, to) or max(FACES, key=lambda f: sum(
                a * b for a, b in zip(block_vector(f), to_block(rotated(FACE_VECTORS[direction], rotation), flip))))
            result = face_uv(pairs, name, frm, to, texture_size)
            if result:
                uv_rect, turn = result
                spec = {"uv": uv_rect, "texture": texture}
                if turn:
                    spec["rotation"] = turn
                faces[name] = spec
        if "both_sides" in part:  # a flat part whose picture is only on one side: mirrored onto the other
            front, back = part["both_sides"]
            u1, v1, u2, v2 = faces[front]["uv"]
            faces[back] = {**faces[front], "uv": [u2, v1, u1, v2]}
        elements.append({"from": [round(v + inset, 4) for v in frm], "to": [round(v - inset, 4) for v in to], "faces": faces})
    return elements


def placed_face(pairs, frm, to):
    """Which side of the box the polygon lies on, from where its corners are - None for a box without
    thickness there (both sides on one plane; the polygon's normal decides then)."""
    for axis, (low_face, high_face) in enumerate((("west", "east"), ("down", "up"), ("north", "south"))):
        values = {round(position[axis], 3) for position, _ in pairs}
        if len(values) == 1 and abs(frm[axis] - to[axis]) > 1e-6:
            return high_face if abs(values.pop() - to[axis]) < 1e-3 else low_face
    return None


def block_vector(face: str):
    return {"down": (0, -1, 0), "up": (0, 1, 0), "west": (-1, 0, 0), "north": (0, 0, -1),
            "east": (1, 0, 0), "south": (0, 0, 1)}[face]


def model_elements(parts: list, texture: str, texture_size, flip: bool = True) -> list:
    return [element for part in parts for element in part_elements(part, texture, texture_size, flip)]


# --- The entity models (numbers from the game's own layer definitions, 1.21.11 = 26.1.2) -----------
HALF_PI, PI = math.pi / 2, math.pi

BOAT = [
    {"offset": (0, 3, 1), "rotation": (HALF_PI, 0, 0), "cubes": [((0, 0), (-14, -9, -3), (28, 16, 3), False)]},       # bottom
    {"offset": (-15, 4, 4), "rotation": (0, 3 * HALF_PI, 0), "cubes": [((0, 19), (-13, -7, -1), (18, 6, 2), False)]},  # back
    {"offset": (15, 4, 0), "rotation": (0, HALF_PI, 0), "cubes": [((0, 27), (-8, -7, -1), (16, 6, 2), False)]},        # front
    {"offset": (0, 4, -9), "rotation": (0, PI, 0), "cubes": [((0, 35), (-14, -7, -1), (28, 6, 2), False)]},            # right
    {"offset": (0, 4, 9), "cubes": [((0, 43), (-14, -7, -1), (28, 6, 2), False)]},                                     # left
]
RAFT = [
    {"offset": (0, -2.1, 1), "rotation": (1.5708, 0, 0),
     "cubes": [((0, 0), (-14, -11, -4), (28, 20, 4), False), ((0, 0), (-14, -9, -8), (28, 16, 4), False)]},
]


def boat_chest(lift: float) -> list:
    """The chest on a chest boat (lift 0) or chest raft (lift -5.1, it sits higher)."""
    return [
        {"offset": (-2, -5 + lift, -6), "rotation": (0, -HALF_PI, 0), "cubes": [((0, 76), (0, 0, 0), (12, 8, 12), False)]},
        {"offset": (-2, -9 + lift, -6), "rotation": (0, -HALF_PI, 0), "cubes": [((0, 59), (0, 0, 0), (12, 4, 12), False)]},
        {"offset": (-1, -6 + lift, -1), "rotation": (0, -HALF_PI, 0), "cubes": [((0, 59), (0, 0, 0), (2, 4, 1), False)]},
    ]


MINECART = [
    {"offset": (0, 4, 0), "rotation": (HALF_PI, 0, 0), "cubes": [((0, 10), (-10, -8, -1), (20, 16, 2), False)]},       # bottom
    {"offset": (-9, 4, 0), "rotation": (0, 3 * HALF_PI, 0), "cubes": [((0, 0), (-8, -9, -1), (16, 8, 2), False)]},     # back
    {"offset": (9, 4, 0), "rotation": (0, HALF_PI, 0), "cubes": [((0, 0), (-8, -9, -1), (16, 8, 2), False)]},          # front
    {"offset": (0, 4, -7), "rotation": (0, PI, 0), "cubes": [((0, 0), (-8, -9, -1), (16, 8, 2), False)]},             # left
    {"offset": (0, 4, 7), "cubes": [((0, 0), (-8, -9, -1), (16, 8, 2), False)]},                                       # right
]
SIGN = [
    {"cubes": [((0, 0), (-12, -14, -1), (24, 12, 2), False)]},   # board
    {"cubes": [((0, 14), (-1, -2, -1), (2, 14, 2), False)]},     # stick
]
HANGING_SIGN = [
    {"cubes": [((0, 12), (-7, 0, -1), (14, 10, 2), False)]},     # board
    {"cubes": [((0, 0), (-8, -6, -2), (16, 2, 4), False)]},      # plank
    # the chains in a V, flat towards the front (the game's wall-hanging version) - the crossed ones
    # of the ceiling version are edge-on in the inventory and the board seemed to float
    {"both_sides": ("north", "south"), "cubes": [((14, 6), (-6, -6, 0), (12, 6, 0), False)]},
]
ARMOR_STAND = [
    {"offset": (0, 1, 0), "cubes": [((0, 0), (-1, -7, -1), (2, 7, 2), False)]},          # head (neck)
    {"cubes": [((0, 26), (-6, 0, -1.5), (12, 3, 3), False)]},                              # body (shoulders)
    {"offset": (-5, 2, 0), "inset": 0.01, "cubes": [((24, 0), (-2, -2, -1), (2, 12, 2), False)]},  # right arm
    {"offset": (5, 2, 0), "inset": 0.01, "cubes": [((32, 16), (0, -2, -1), (2, 12, 2), True)]},    # left arm
    {"offset": (-1.9, 12, 0), "cubes": [((8, 0), (-1, 0, -1), (2, 11, 2), False)]},      # right leg
    {"offset": (1.9, 12, 0), "cubes": [((40, 16), (-1, 0, -1), (2, 11, 2), True)]},      # left leg
    {"cubes": [((16, 0), (-3, 3, -1), (2, 7, 2), False)]},                                 # right body stick
    {"cubes": [((48, 16), (1, 3, -1), (2, 7, 2), False)]},                                 # left body stick
    {"cubes": [((0, 48), (-4, 10, -1), (8, 2, 2), False)]},                                # shoulder stick
    {"offset": (0, 12, 0), "cubes": [((0, 32), (-6, 11, -6), (12, 1, 12), False)]},      # base plate
]

# The chest (block entity) on a chest minecart; its model is set up the right way up, not flipped
CHEST = [
    {"cubes": [((0, 19), (1, 0, 1), (14, 10, 14), False)]},                       # bottom
    {"offset": (0, 9, 1), "inset": 0.01, "cubes": [((0, 0), (1, 0, 0), (14, 5, 14), False)]},  # lid
    {"offset": (0, 9, 1), "cubes": [((0, 0), (7, -2, 14), (2, 4, 1), False)]},    # lock
]
CHEST_FLIP = False

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"]
SIGN_WOODS = WOODS + ["bamboo", "crimson", "warped"]
MINECART_TEXTURE = "minecraft:entity/minecart"
ARMOR_STAND_TEXTURE = "minecraft:entity/armorstand/wood"
# Texture names that moved in 26.1 (pack format 84)
TEXTURE_RENAMES = {84: {MINECART_TEXTURE: "minecraft:entity/minecart/minecart",
                        ARMOR_STAND_TEXTURE: "minecraft:entity/armorstand/armorstand"}}


def atlas_sources(pack_format: int) -> dict:
    """Additions to the block texture atlas (the game merges every pack's atlas sources)."""
    renames = TEXTURE_RENAMES.get(pack_format, {})
    singles = [renames.get(name, name) for name in (MINECART_TEXTURE, ARMOR_STAND_TEXTURE)]
    return {"sources": [
        {"type": "minecraft:directory", "source": "entity/boat", "prefix": "entity/boat/"},
        {"type": "minecraft:directory", "source": "entity/chest_boat", "prefix": "entity/chest_boat/"},
        {"type": "minecraft:directory", "source": "entity/signs", "prefix": "entity/signs/"},
        *({"type": "minecraft:single", "resource": name} for name in singles + ["minecraft:entity/chest/normal"]),
    ]}
