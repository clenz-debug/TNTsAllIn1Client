"""Preview renderer for the 3D block models - draws small scenes with Minecraft's own textures, once
with the vanilla models and once with ours, side by side, so a model can be judged before it's ever
loaded in the game.

Reads models, blockstates and textures straight from a vanilla client jar; our models come from
models3d.py and take precedence over the jar's. Orthographic projection, nearest-pixel textures,
cutout transparency and Minecraft's per-direction face shading - close enough to the game to judge
shapes, not a replacement for looking at it in the game. Not simulated: a blockstate's "uvlock" (it hid
that vanilla's glow lichen blockstate broke our 3D pieces' sides) and the depth fighting of faces that
lie exactly on top of each other.

Usage: python preview.py <client.jar> <output folder> [scene ...]   ("items": our 3D items as the
inventory shows them)
"""
import json
import math
import pathlib
import sys
import zipfile

import numpy as np
from PIL import Image, ImageDraw

import models3d

FACE_NAMES = ("north", "south", "east", "west", "up", "down")
# Minecraft's directional face shading.
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
# Colors for tinted faces ("tintindex"): a plains biome's grass, and the lily pad's fixed color.
GRASS_TINT = np.array([0x91, 0xBD, 0x59], dtype=np.float32) / 255
BLOCK_TINTS = {"lily_pad": np.array([0x20, 0x80, 0x30], dtype=np.float32) / 255,
               "redstone_wire": np.array([1.0, 0.2, 0.0], dtype=np.float32)}  # full power


def normalize(ref: str) -> str:
    return ref.split(":", 1)[1] if ":" in ref else ref


class Assets:
    def __init__(self, jar_path: str, overrides: dict, blockstates: dict | None = None):
        self.jar = zipfile.ZipFile(jar_path)
        self.overrides = overrides
        self.blockstates = blockstates or {}
        self.textures: dict = {}

    def _json(self, path: str):
        return json.loads(self.jar.read(path).decode("utf-8"))

    def raw_model(self, ref: str) -> dict:
        path = normalize(ref)
        if path in self.overrides:
            return self.overrides[path]
        return self._json(f"assets/minecraft/models/{path}.json")

    def model(self, ref: str) -> dict:
        """Parent chain resolved: the nearest elements, textures merged child-over-parent."""
        chain = []
        current = ref
        while current:
            data = self.raw_model(current)
            chain.append(data)
            current = data.get("parent")
            if current and normalize(current).startswith("builtin/"):
                break
        textures: dict = {}
        elements = None
        for data in reversed(chain):
            textures.update(data.get("textures", {}))
        for data in chain:
            if "elements" in data:
                elements = data["elements"]
                break
        return {"textures": textures, "elements": elements or []}

    def display(self, ref: str) -> dict:
        """The nearest "display" in the model's parent chain."""
        current = ref
        while current and not normalize(current).startswith("builtin/"):
            data = self.raw_model(current)
            if "display" in data:
                return data["display"]
            current = data.get("parent")
        return {}

    def blockstate(self, block: str) -> dict:
        if block in self.blockstates:
            return self.blockstates[block]
        return self._json(f"assets/minecraft/blockstates/{block}.json")

    def texture(self, ref: str) -> np.ndarray:
        path = normalize(ref)
        if path not in self.textures:
            image = Image.open(self.jar.open(f"assets/minecraft/textures/{path}.png")).convert("RGBA")
            if image.height > image.width:
                image = image.crop((0, 0, image.width, image.width))  # first frame of an animation
            self.textures[path] = np.asarray(image, dtype=np.float32) / 255.0
        return self.textures[path]


def resolve_texture(ref, textures: dict) -> str:
    seen = 0
    while True:
        if isinstance(ref, dict):  # 26.1 form: {"sprite": ..., "force_translucent": ...}
            ref = ref.get("sprite", "")
        if not ref.startswith("#") or seen >= 10:
            return ref
        ref = textures.get(ref[1:], "")
        seen += 1


def matches(when: dict, state: dict) -> bool:
    if "OR" in when:
        return any(matches(part, state) for part in when["OR"])
    if "AND" in when:
        return all(matches(part, state) for part in when["AND"])
    for key, value in when.items():
        if str(state.get(key, "")) not in str(value).split("|"):
            return False
    return True


def pick(options, position) -> dict:
    """One entry of a blockstate's random list - fixed per block position, like the game (not the game's exact pick)."""
    if not isinstance(options, list):
        return options
    x, y, z = position
    return options[(x * 73856093 ^ y * 19349663 ^ z * 83492791) % len(options)]


def state_models(assets: Assets, block: str, state: dict, position=(0, 0, 0)) -> list:
    data = assets.blockstate(block)
    chosen = []
    if "variants" in data:
        for key, variant in data["variants"].items():
            conditions = dict(part.split("=") for part in key.split(",")) if key else {}
            if all(str(state.get(k)) == v for k, v in conditions.items()):
                chosen.append(pick(variant, position))
                break
    else:
        for part in data["multipart"]:
            if "when" not in part or matches(part["when"], state):
                apply = part["apply"]
                chosen.append(pick(apply, position))
    return chosen


# --- Geometry --------------------------------------------------------------------------------------

def face_corners(face: str, frm, to):
    """The face's four corners (top-left, top-right, bottom-right, bottom-left as seen from outside) and its default UV."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if face == "north":
        return [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)], [16 - x2, 16 - y2, 16 - x1, 16 - y1]
    if face == "south":
        return [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)], [x1, 16 - y2, x2, 16 - y1]
    if face == "east":
        return [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)], [16 - z2, 16 - y2, 16 - z1, 16 - y1]
    if face == "west":
        return [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)], [z1, 16 - y2, z2, 16 - y1]
    if face == "up":
        return [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)], [x1, z1, x2, z2]
    return [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)], [x1, 16 - z2, x2, 16 - z1]


def axis_rotation(axis: str, degrees: float) -> np.ndarray:
    """Right-handed rotation about +axis - the direction of an element's own "rotation"."""
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def element_transform(rotation):
    if not rotation:
        return lambda p: np.asarray(p, dtype=np.float64)
    origin = np.asarray(rotation["origin"], dtype=np.float64)
    if "angle" not in rotation:
        # 26.3's rotation around several axes: around x, then y, then z
        matrix = axis_rotation("z", rotation["z"]) @ axis_rotation("y", rotation["y"]) @ axis_rotation("x", rotation["x"])
        return lambda p: origin + matrix @ (np.asarray(p, dtype=np.float64) - origin)
    matrix = axis_rotation(rotation["axis"], rotation["angle"])
    scale = np.ones(3)
    if rotation.get("rescale"):
        factor = 1 / math.cos(math.radians(abs(rotation["angle"])))
        for i, axis in enumerate("xyz"):
            if axis != rotation["axis"]:
                scale[i] = factor
    return lambda p: origin + scale * (matrix @ (np.asarray(p, dtype=np.float64) - origin))


def blockstate_transform(x_rot: int, y_rot: int):
    """Blockstate "x"/"y": clockwise looking along +axis, x applied first - the opposite direction of element rotations."""
    center = np.array([8.0, 8.0, 8.0])
    matrix = axis_rotation("y", -y_rot) @ axis_rotation("x", -x_rot)
    return lambda p: center + matrix @ (p - center)


class Quad:
    __slots__ = ("points", "uvs", "texture", "shade", "tint", "normal")

    def __init__(self, points, uvs, texture, shade, tint, normal):
        self.points, self.uvs, self.texture, self.shade, self.tint, self.normal = points, uvs, texture, shade, tint, normal


def model_quads(assets: Assets, model_ref: str, x_rot=0, y_rot=0, offset=(0, 0, 0), tint=GRASS_TINT) -> list:
    model = assets.model(model_ref)
    placed = blockstate_transform(x_rot, y_rot)
    quads = []
    for element in model["elements"]:
        transform = element_transform(element.get("rotation"))
        for face, spec in element.get("faces", {}).items():
            corners, default_uv = face_corners(face, element["from"], element["to"])
            u1, v1, u2, v2 = spec.get("uv", default_uv)
            corner_uvs = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
            steps = (spec.get("rotation", 0) // 90) % 4
            uvs = [corner_uvs[(i - steps) % 4] for i in range(4)]
            points = [placed(transform(c)) + np.asarray(offset, dtype=np.float64) * 16 for c in corners]
            # Outward: the corners run clockwise as seen from outside
            normal = np.cross(points[3] - points[0], points[1] - points[0])
            length = np.linalg.norm(normal)
            if length < 1e-9:
                normal = np.zeros(3)
            else:
                normal = normal / length
            shade = 1.0
            if element.get("shade", True):
                if normal[1] > 0.5:
                    shade = 1.0
                elif normal[1] < -0.5:
                    shade = 0.5
                elif abs(normal[2]) >= abs(normal[0]):
                    shade = 0.8
                else:
                    shade = 0.6
            texture_ref = resolve_texture(spec["texture"], model["textures"])
            if not texture_ref:
                continue
            quads.append(Quad(points, uvs, texture_ref, shade, tint if "tintindex" in spec else None, normal))
    return quads


def gui_quads(assets: Assets, model_ref: str, position) -> list:
    """The model with its "gui" display transform (translate, then rotate x-y-z, then scale, about
    the item space's middle), seen face-on from the south like an inventory slot."""
    gui = assets.display(model_ref).get("gui", {})
    rx, ry, rz = gui.get("rotation", [0, 0, 0])
    matrix = axis_rotation("x", rx) @ axis_rotation("y", ry) @ axis_rotation("z", rz)
    scale = np.asarray(gui.get("scale", [1, 1, 1]), dtype=np.float64)
    translation = np.asarray(gui.get("translation", [0, 0, 0]), dtype=np.float64)
    center = np.array([8.0, 8.0, 8.0])
    offset = np.asarray(position, dtype=np.float64) * 16
    quads = model_quads(assets, model_ref)
    for quad in quads:
        quad.points = [center + translation + matrix @ (scale * (point - center)) + offset for point in quad.points]
        quad.normal = matrix @ quad.normal
    return quads


# --- Rendering -------------------------------------------------------------------------------------

def render(assets: Assets, blocks: list, yaw: float, pitch: float, scale: float, size=(560, 420)) -> Image.Image:
    """blocks: (block id, state dict, (x, y, z) block position). yaw/pitch in degrees."""
    quads = []
    for block, state, position in blocks:
        if block.startswith("model:"):  # a model on its own, e.g. an item's
            quads += model_quads(assets, block[len("model:"):], offset=position)
            continue
        if block.startswith("gui:"):  # an item model as the inventory shows it
            quads += gui_quads(assets, block[len("gui:"):], position)
            continue
        for variant in state_models(assets, block, state, position):
            quads += model_quads(assets, variant["model"], variant.get("x", 0), variant.get("y", 0), position,
                                 BLOCK_TINTS.get(block, GRASS_TINT))

    # Camera looks down -z in view space, so a larger view z is closer. Yaw 0 looks at south faces,
    # yaw 180 at north faces; pitch tilts it down from above.
    view = axis_rotation("x", pitch) @ axis_rotation("y", yaw)
    projected = [np.array([view @ p for p in quad.points]) for quad in quads]
    # Framed by the blocks' cubes, not the models - both halves of a comparison get the same scale.
    cube = [np.array([dx, dy, dz], dtype=np.float64) * 16 for dx in (0, 1) for dy in (0, 1) for dz in (0, 1)]
    frame = np.array([view @ (np.asarray(position, dtype=np.float64) * 16 + corner)
                      for _, _, position in blocks for corner in cube])
    min_xy = frame[:, :2].min(axis=0)
    max_xy = frame[:, :2].max(axis=0)
    width, height = size
    center = (min_xy + max_xy) / 2
    fit = min((width - 40) / max(max_xy[0] - min_xy[0], 1), (height - 40) / max(max_xy[1] - min_xy[1], 1))
    scale = min(scale, fit)

    color = np.zeros((height, width, 3), dtype=np.float32)
    color[:] = (0.53, 0.72, 0.96)
    depth = np.full((height, width), -np.inf, dtype=np.float32)
    ys, xs = np.mgrid[0:height, 0:width]
    px = xs + 0.5
    py = ys + 0.5

    for quad, pts in zip(quads, projected):
        # Minecraft draws blocks with back faces culled - a face turned away from the camera is not drawn
        if (view @ quad.normal)[2] <= 1e-6:
            continue
        screen = np.empty((4, 3))
        screen[:, 0] = (pts[:, 0] - center[0]) * scale + width / 2
        screen[:, 1] = -(pts[:, 1] - center[1]) * scale + height / 2
        screen[:, 2] = pts[:, 2]
        texture = assets.texture(quad.texture)
        tex_h, tex_w = texture.shape[:2]
        uvs = np.array(quad.uvs, dtype=np.float64)
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = screen[list(tri)]
            ua, ub, uc = uvs[list(tri)]
            x0, x1 = int(max(min(a[0], b[0], c[0]), 0)), int(min(max(a[0], b[0], c[0]) + 1, width))
            y0, y1 = int(max(min(a[1], b[1], c[1]), 0)), int(min(max(a[1], b[1], c[1]) + 1, height))
            if x0 >= x1 or y0 >= y1:
                continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            sx = px[y0:y1, x0:x1]
            sy = py[y0:y1, x0:x1]
            w0 = ((b[1] - c[1]) * (sx - c[0]) + (c[0] - b[0]) * (sy - c[1])) / den
            w1 = ((c[1] - a[1]) * (sx - c[0]) + (a[0] - c[0]) * (sy - c[1])) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            z = w0 * a[2] + w1 * b[2] + w2 * c[2]
            u = w0 * ua[0] + w1 * ub[0] + w2 * uc[0]
            v = w0 * ua[1] + w1 * ub[1] + w2 * uc[1]
            tu = np.clip((u / 16 * tex_w).astype(int), 0, tex_w - 1)
            tv = np.clip((v / 16 * tex_h).astype(int), 0, tex_h - 1)
            sample = texture[tv, tu]
            region_depth = depth[y0:y1, x0:x1]
            visible = inside & (sample[..., 3] > 0.1) & (z > region_depth - 1e-4)
            region_depth[visible] = z[visible]
            region_color = color[y0:y1, x0:x1]
            rgb = sample[..., :3] if quad.tint is None else sample[..., :3] * quad.tint
            region_color[visible] = rgb[visible] * quad.shade
    image = Image.fromarray((np.clip(color, 0, 1) * 255).astype(np.uint8), "RGB")
    return image


def side_by_side(title: str, vanilla: Image.Image, ours: Image.Image) -> Image.Image:
    gap = 12
    out = Image.new("RGB", (vanilla.width + ours.width + gap * 3, vanilla.height + 44), (40, 40, 48))
    out.paste(vanilla, (gap, 34))
    out.paste(ours, (vanilla.width + gap * 2, 34))
    draw = ImageDraw.Draw(out)
    draw.text((gap, 8), f"{title} - Vanilla", fill=(255, 255, 255))
    draw.text((vanilla.width + gap * 2, 8), f"{title} - TNT 3D", fill=(255, 255, 255))
    return out


# --- Scenes ----------------------------------------------------------------------------------------

def floor(blocks, block, x_range, z_range, y=0):
    for x in x_range:
        for z in z_range:
            blocks.append((block, {}, (x, y, z)))


def scene_ladder():
    blocks = []
    for y in range(3):
        for x in range(2):
            blocks.append(("stone", {}, (x, y, 1)))
    for y in range(3):
        blocks.append(("ladder", {"facing": "north", "waterlogged": "false"}, (0, y, 0)))
    blocks.append(("ladder", {"facing": "north", "waterlogged": "false"}, (1, 0, 0)))
    floor(blocks, "grass_block", range(-1, 3), range(-1, 2), y=-1)
    return blocks, 200, 25


def scene_rails():
    blocks = []
    floor(blocks, "stone", range(0, 6), range(0, 5), y=-1)
    for z in range(0, 3):
        blocks.append(("rail", {"shape": "north_south"}, (0, 0, z)))
    blocks.append(("rail", {"shape": "north_east"}, (0, 0, 3)))
    blocks.append(("rail", {"shape": "east_west"}, (1, 0, 3)))
    blocks.append(("powered_rail", {"shape": "north_south", "powered": "true"}, (2, 0, 0)))
    blocks.append(("powered_rail", {"shape": "north_south", "powered": "false"}, (2, 0, 1)))
    blocks.append(("detector_rail", {"shape": "north_south", "powered": "false"}, (3, 0, 0)))
    blocks.append(("activator_rail", {"shape": "north_south", "powered": "false"}, (3, 0, 1)))
    blocks.append(("rail", {"shape": "ascending_north"}, (5, 0, 2)))
    blocks.append(("stone", {}, (5, 0, 1)))
    blocks.append(("rail", {"shape": "north_south"}, (5, 1, 1)))
    return blocks, 35, 35


def scene_bars():
    blocks = []
    floor(blocks, "stone", range(0, 6), range(0, 3), y=-1)
    row = [("iron_bars", x) for x in range(0, 3)] + [("copper_bars", x) for x in range(3, 5)]
    for block, x in row:
        blocks.append((block, {"east": str(x < 4).lower(), "west": str(x > 0).lower(), "north": "false", "south": "false", "waterlogged": "false"}, (x, 0, 1)))
    blocks.append(("iron_bars", {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"}, (1, 0, 2)))
    blocks.append(("iron_bars", {"east": "false", "west": "false", "north": "false", "south": "true", "waterlogged": "false"}, (5, 0, 1)))
    return blocks, 30, 25


def scene_chains():
    blocks = []
    floor(blocks, "stone", range(0, 4), range(0, 2), y=3)
    for y in range(0, 3):
        blocks.append(("iron_chain", {"axis": "y", "waterlogged": "false"}, (0, y, 0)))
        blocks.append(("copper_chain", {"axis": "y", "waterlogged": "false"}, (2, y, 0)))
    blocks.append(("iron_chain", {"axis": "x", "waterlogged": "false"}, (1, 1, 1)))
    blocks.append(("iron_chain", {"axis": "z", "waterlogged": "false"}, (3, 1, 1)))
    return blocks, 30, 20


def scene_lanterns():
    blocks = []
    floor(blocks, "stone", range(0, 4), range(0, 2), y=4)
    floor(blocks, "stone", range(0, 4), range(0, 2), y=-1)
    blocks.append(("iron_chain", {"axis": "y", "waterlogged": "false"}, (0, 3, 0)))
    blocks.append(("lantern", {"hanging": "true", "waterlogged": "false"}, (0, 2, 0)))
    blocks.append(("soul_lantern", {"hanging": "true", "waterlogged": "false"}, (1, 3, 0)))
    blocks.append(("copper_chain", {"axis": "y", "waterlogged": "false"}, (2, 3, 0)))
    blocks.append(("copper_chain", {"axis": "y", "waterlogged": "false"}, (2, 2, 0)))
    blocks.append(("copper_lantern", {"hanging": "true", "waterlogged": "false"}, (2, 1, 0)))
    blocks.append(("lantern", {"hanging": "false", "waterlogged": "false"}, (3, 0, 1)))
    blocks.append(("soul_lantern", {"hanging": "false", "waterlogged": "false"}, (1, 0, 1)))
    return blocks, 25, 15


def scene_dripstone(spike: str = "pointed_dripstone", ground: str = "dripstone_block"):
    blocks = []
    floor(blocks, ground, range(0, 3), range(0, 2), y=4)
    floor(blocks, ground, range(0, 3), range(0, 2), y=-1)
    down = ["base", "middle", "frustum", "tip"]
    for i, thickness in enumerate(down):
        blocks.append((spike, {"thickness": thickness, "vertical_direction": "down", "waterlogged": "false"}, (0, 3 - i, 0)))
    for i, thickness in enumerate(["base", "frustum", "tip"]):
        blocks.append((spike, {"thickness": thickness, "vertical_direction": "up", "waterlogged": "false"}, (2, i, 1)))
    blocks.append((spike, {"thickness": "tip_merge", "vertical_direction": "down", "waterlogged": "false"}, (1, 3, 1)))
    return blocks, 30, 15


def scene_sulfur_spike():
    """26.3 and later only - the block doesn't exist in older jars."""
    return scene_dripstone("sulfur_spike", "sulfur")


def scene_curve():
    blocks = []
    floor(blocks, "stone", range(0, 3), range(0, 3), y=-1)
    blocks.append(("rail", {"shape": "north_south"}, (0, 0, 0)))
    blocks.append(("rail", {"shape": "north_east"}, (0, 0, 1)))
    blocks.append(("rail", {"shape": "east_west"}, (1, 0, 1)))
    blocks.append(("rail", {"shape": "south_west"}, (2, 0, 1)))
    blocks.append(("rail", {"shape": "north_south"}, (2, 0, 2)))
    return blocks, 20, 55


def scene_amethyst():
    blocks = []
    floor(blocks, "amethyst_block", range(0, 4), range(0, 4), y=-1)
    floor(blocks, "amethyst_block", range(0, 4), range(-1, 0), y=0)
    stages = ["amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud"]
    for x, stage in enumerate(stages):
        for z in range(1, 4):
            blocks.append((stage, {"facing": "up", "waterlogged": "false"}, (x, 0, z)))
        blocks.append((stage, {"facing": "south", "waterlogged": "false"}, (x, 0, 0)))
    return blocks, 25, 35


def wall_growth_state(**sides):
    state = {side: "false" for side in ("north", "south", "east", "west", "up", "down")}
    state.update({side: str(value).lower() for side, value in sides.items()})
    state["waterlogged"] = "false"
    return state


def scene_lichen():
    blocks = []
    floor(blocks, "stone", range(0, 4), range(0, 3), y=-1)
    for x in range(0, 4):
        for y in range(0, 2):
            blocks.append(("deepslate" if x < 2 else "sculk", {}, (x, y, 0)))
    blocks.append(("glow_lichen", wall_growth_state(north=True), (0, 0, 1)))
    blocks.append(("glow_lichen", wall_growth_state(north=True, down=True), (1, 0, 1)))
    blocks.append(("glow_lichen", wall_growth_state(north=True), (1, 1, 1)))
    blocks.append(("sculk_vein", wall_growth_state(north=True, down=True), (2, 0, 1)))
    blocks.append(("sculk_vein", wall_growth_state(north=True), (3, 1, 1)))
    blocks.append(("sculk_vein", wall_growth_state(down=True), (3, 0, 2)))
    return blocks, 20, 25


def scene_lily_pad():
    blocks = []
    floor(blocks, "blue_concrete", range(0, 3), range(0, 2), y=-1)
    blocks.append(("lily_pad", {}, (0, 0, 0)))
    blocks.append(("lily_pad", {}, (2, 0, 1)))
    blocks.append(("lily_pad", {}, (1, 0, 1)))
    return blocks, 20, 40


def scene_sugar_cane():
    blocks = []
    floor(blocks, "sand", range(0, 3), range(0, 2), y=-1)
    for y in range(0, 3):
        blocks.append(("sugar_cane", {"age": "0"}, (0, y, 0)))
    for y in range(0, 2):
        blocks.append(("sugar_cane", {"age": "0"}, (2, y, 1)))
    return blocks, 25, 15


def vine_state(**sides):
    return {side: str(sides.get(side, False)).lower() for side in models3d.VINE_SIDES}


def scene_vines():
    blocks = []
    for x in range(3):
        for y in range(3):
            blocks.append(("stone", {}, (x, y, -1)))
            blocks.append(("vine", vine_state(north=True), (x, y, 0)))
    blocks.append(("stone", {}, (3, 2, 0)))  # a corner: vines on two sides, and one under a ceiling
    blocks.append(("vine", vine_state(north=True, east=True), (2, 2, 0)))
    blocks.append(("stone", {}, (4, 3, 1)))
    blocks.append(("vine", vine_state(up=True, west=True), (4, 2, 1)))
    blocks.append(("stone", {}, (3, 2, 1)))
    return blocks, 25, 25


def scene_nether_vines():
    blocks = []
    for x in range(3):  # weeping vines hang from a ceiling, twisting ones grow from the floor
        blocks.append(("netherrack", {}, (x, 4, 0)))
        blocks.append(("netherrack", {}, (x + 4, -1, 0)))
    for x, length in ((0, 4), (1, 2), (2, 3)):
        for y in range(4 - length, 4):
            blocks.append(("weeping_vines" if y == 4 - length else "weeping_vines_plant", {}, (x, y, 0)))
        for y in range(length):
            blocks.append(("twisting_vines" if y == length - 1 else "twisting_vines_plant", {}, (x + 4, y, 0)))
    return blocks, 25, 15


def scene_bushes():
    blocks = []
    floor(blocks, "moss_block", range(0, 4), range(0, 2), y=-1)
    for age in range(4):
        blocks.append(("sweet_berry_bush", {"age": str(age)}, (age, 0, 0)))
    for x, name in enumerate(("bush", "bush", "firefly_bush")):
        blocks.append((name, {}, (x, 0, 1)))
    return blocks, 25, 25


def scene_workstations():
    return [("stonecutter", {"facing": "north"}, (0, 0, 0)),
            ("calibrated_sculk_sensor", {"facing": "north", "sculk_sensor_phase": "inactive", "power": "0", "waterlogged": "false"}, (2, 0, 0))], 25, 25


def scene_mushrooms():
    blocks = []
    for i, name in enumerate(("brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus")):
        floor(blocks, "crimson_nylium" if name == "crimson_fungus" else "warped_nylium" if name == "warped_fungus" else "podzol",
              range(i * 2, i * 2 + 2), range(0, 3), y=-1)
        for x, z in ((i * 2, 0), (i * 2 + 1, 1), (i * 2, 2)):
            blocks.append((name, {}, (x, 0, z)))
        blocks.append((f"potted_{name}", {}, (i * 2 + 1, 0, 3)))
    return blocks, 25, 30


def chiseled_state(facing, *occupied):
    state = {f"slot_{slot}_occupied": str(slot in occupied).lower() for slot in range(6)}
    state["facing"] = facing
    return state


def scene_bookshelves():
    blocks = []
    floor(blocks, "oak_planks", range(0, 4), range(0, 2), y=-1)
    for x in range(0, 2):
        for y in range(0, 2):
            blocks.append(("bookshelf", {}, (x, y, 1)))
    blocks.append(("chiseled_bookshelf", chiseled_state("south", 0, 1, 2, 3, 4, 5), (2, 0, 1)))
    blocks.append(("chiseled_bookshelf", chiseled_state("south", 0, 2, 4), (3, 0, 1)))
    blocks.append(("chiseled_bookshelf", chiseled_state("south"), (2, 1, 1)))
    blocks.append(("bookshelf", {}, (3, 1, 1)))
    return blocks, 40, 25


def door(blocks, name, position, facing="north", hinge="left", is_open=False):
    x, y, z = position
    for half, dy in (("lower", 0), ("upper", 1)):
        state = {"facing": facing, "half": half, "hinge": hinge, "open": str(is_open).lower(), "powered": "false"}
        blocks.append((f"{name}_door", state, (x, y + dy, z)))


def scene_doors():
    blocks = []
    floor(blocks, "stone", range(0, 5), range(0, 2), y=-1)
    door(blocks, "oak", (0, 0, 0))
    door(blocks, "acacia", (1, 0, 0), hinge="right")
    door(blocks, "cherry", (2, 0, 0))
    door(blocks, "iron", (3, 0, 0), is_open=True)
    door(blocks, "copper", (4, 0, 0), hinge="right", is_open=True)
    return blocks, 30, 20


def scene_doors_back():
    blocks, _, _ = scene_doors()
    return blocks, 210, 20


def trapdoor_state(facing="north", half="bottom", is_open=False):
    return {"facing": facing, "half": half, "open": str(is_open).lower(), "powered": "false", "waterlogged": "false"}


def scene_trapdoors():
    blocks = []
    floor(blocks, "stone", range(0, 4), range(0, 3), y=-1)
    blocks.append(("oak_trapdoor", trapdoor_state(), (0, 0, 0)))
    blocks.append(("warped_trapdoor", trapdoor_state(facing="east"), (1, 0, 0)))
    blocks.append(("copper_trapdoor", trapdoor_state(half="top"), (2, 0, 0)))
    blocks.append(("jungle_trapdoor", trapdoor_state(is_open=True), (0, 0, 2)))
    blocks.append(("iron_trapdoor", trapdoor_state(facing="west", is_open=True), (1, 0, 2)))
    blocks.append(("bamboo_trapdoor", trapdoor_state(facing="south", is_open=True), (2, 0, 2)))
    blocks.append(("spruce_trapdoor", trapdoor_state(facing="south"), (3, 0, 1)))
    return blocks, 25, 40


def scene_poplar():
    """26.3 and later only: the poplar door (closed and open), its trapdoor (lying and open) and the red shrub."""
    blocks = []
    floor(blocks, "stone", range(0, 5), range(0, 2), y=-1)
    door(blocks, "poplar", (0, 0, 0))
    door(blocks, "poplar", (1, 0, 0), hinge="right", is_open=True)
    blocks.append(("poplar_trapdoor", trapdoor_state(), (2, 0, 1)))
    blocks.append(("poplar_trapdoor", trapdoor_state(facing="south", is_open=True), (3, 0, 0)))
    blocks.append(("red_shrub", {}, (4, 0, 1)))
    return blocks, 30, 20


def scene_hanging_signs():
    """26.3 and later only: hanging signs under a ceiling (straight, turned, attached in the middle) and on a wall."""
    blocks = []
    floor(blocks, "oak_planks", range(0, 4), range(0, 2), y=1)
    blocks.append(("oak_hanging_sign", {"attached": "false", "rotation": "0", "waterlogged": "false"}, (0, 0, 1)))
    blocks.append(("spruce_hanging_sign", {"attached": "false", "rotation": "2", "waterlogged": "false"}, (1, 0, 1)))
    blocks.append(("birch_hanging_sign", {"attached": "true", "rotation": "0", "waterlogged": "false"}, (2, 0, 1)))
    blocks.append(("cherry_hanging_sign", {"attached": "true", "rotation": "1", "waterlogged": "false"}, (3, 0, 1)))
    blocks.append(("oak_planks", {}, (5, 0, 0)))
    blocks.append(("poplar_wall_hanging_sign", {"facing": "south", "waterlogged": "false"}, (5, 0, 1)))
    return blocks, 25, 10


def wire(**sides):
    state = {side: "none" for side in ("north", "south", "east", "west")}
    state.update(sides)
    state["power"] = "15"
    return state


def scene_redstone():
    blocks = []
    floor(blocks, "stone", range(0, 5), range(0, 3), y=-1)
    blocks.append(("stone", {}, (4, 0, 0)))
    blocks.append(("redstone_wire", wire(east="side", south="side"), (0, 0, 0)))
    for x in range(1, 3):
        blocks.append(("redstone_wire", wire(east="side", west="side"), (x, 0, 0)))
    blocks.append(("redstone_wire", wire(west="side", east="up"), (3, 0, 0)))
    blocks.append(("redstone_wire", wire(north="side", south="side", east="side"), (0, 0, 1)))
    blocks.append(("redstone_wire", wire(west="side"), (1, 0, 1)))
    blocks.append(("redstone_wire", wire(north="side"), (0, 0, 2)))
    blocks.append(("redstone_wire", wire(), (2, 0, 2)))
    blocks.append(("redstone_wire", wire(north="side", south="side", east="side", west="side"), (3, 0, 2)))
    return blocks, 25, 45


# Stand-ins on the vanilla side for item models that are flat pictures there: a one pixel thin
# plate (close enough for comparing).
VANILLA_STAND_INS = {
    "item/mace": {"textures": {"layer0": "minecraft:item/mace"}, "elements": [
        {"from": [0, 0, 7.5], "to": [16, 16, 8.5], "faces": {
            "south": {"uv": [0, 0, 16, 16], "texture": "#layer0"}, "north": {"uv": [16, 0, 0, 16], "texture": "#layer0"}}}]},
}


def scene_mace():
    # Vanilla's side shows the flat picture as a thin plate, the way it's drawn in the hand
    return [("model:item/mace", {}, (0, 0, 0))], 20, 15


SCENES = {
    "ladder": scene_ladder,
    "rails": scene_rails,
    "curve": scene_curve,
    "bars": scene_bars,
    "chains": scene_chains,
    "lanterns": scene_lanterns,
    "dripstone": scene_dripstone,
    "sulfur_spike": scene_sulfur_spike,
    "poplar": scene_poplar,
    "hanging_signs": scene_hanging_signs,
    "amethyst": scene_amethyst,
    "lichen": scene_lichen,
    "lily_pad": scene_lily_pad,
    "sugar_cane": scene_sugar_cane,
    "mushrooms": scene_mushrooms,
    "vines": scene_vines,
    "nether_vines": scene_nether_vines,
    "bushes": scene_bushes,
    "workstations": scene_workstations,
    "bookshelves": scene_bookshelves,
    "doors": scene_doors,
    "doors_back": scene_doors_back,
    "trapdoors": scene_trapdoors,
    "redstone": scene_redstone,
    "mace": scene_mace,
}


def item_sheet(assets: Assets, out_dir: pathlib.Path) -> pathlib.Path:
    """Every 3D item of ours as the inventory shows it, eight to a row (vanilla's flat pictures have
    no 3D shape to compare with)."""
    items = sorted(path[len("item/"):] for path in models3d.MODELS if path.startswith("item/"))
    columns = 8
    blocks = [(f"gui:item/{item}", {}, (1.1 * (index % columns), -1.1 * (index // columns), 0)) for index, item in enumerate(items)]
    rows = (len(items) + columns - 1) // columns
    image = render(assets, blocks, 0, 0, scale=4, size=(columns * 72 + 40, rows * 72 + 40))
    draw = ImageDraw.Draw(image)
    for index, item in enumerate(items):
        draw.text((20 + (index % columns) * 72 * 1.0, 22 + (index // columns) * 72 + 58), item[:12], fill=(20, 20, 30))
    path = out_dir / "items.png"
    image.save(path)
    return path


def main():
    jar, out_dir = sys.argv[1], pathlib.Path(sys.argv[2])
    names = sys.argv[3:] or list(SCENES) + ["items"]
    out_dir.mkdir(parents=True, exist_ok=True)
    vanilla = Assets(jar, VANILLA_STAND_INS)
    # Models only newer games get (models3d.MODELS_SINCE) count when the jar is new enough
    with zipfile.ZipFile(jar) as archive:
        pack_format = json.loads(archive.read("version.json")).get("pack_version", {}).get("resource_major", 0)
    newer = {path: model for since, models in models3d.MODELS_SINCE.items() if pack_format >= since for path, model in models.items()}
    ours = Assets(jar, {**models3d.MODELS, **newer}, models3d.BLOCKSTATES)
    for name in names:
        if name == "items":
            print(item_sheet(ours, out_dir))
            continue
        blocks, yaw, pitch = SCENES[name]()
        left = render(vanilla, blocks, yaw, pitch, scale=6)
        right = render(ours, blocks, yaw, pitch, scale=6)
        path = out_dir / f"{name}.png"
        side_by_side(name, left, right).save(path)
        print(path)


if __name__ == "__main__":
    main()
