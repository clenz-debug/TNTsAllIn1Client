"""Builds the TNT 3D Blocks resource pack (models3d.py) for each Minecraft version the client supports.

Usage: python build.py [output folder]   (default: dist/ next to this file)

Writes TNT-3D-Blocks-<version>.zip per version, plus TNT-3D-Bushes-<version>.zip with the bushes on
their own (the mod switches them separately) - same models for all versions. What differs:
pack.mcmeta (min/max_format is the current scheme, the old single pack_format only for pre-26.x
clients - the same split as the flat inventory icons pack, launcher/scripts/generate_flat_icons_pack.py)
and texture entries in 26.1's {"sprite": ...} form, which older clients only read as plain names.
"""
import json
import math
import pathlib
import sys
import zipfile

import entity_items
import models3d

PACK_FORMATS = {
    "1.21.10": 69,
    "1.21.11": 75,
    "26.1.2": 84,
    "26.3": 97,
}

DESCRIPTION = "§6TNT 3D-Blöcke\n§7Eigene 3D-Modelle für TNT's All-In-1 Client"
BUSHES_DESCRIPTION = "§6TNT 3D-Büsche\n§7Eigene 3D-Büsche für TNT's All-In-1 Client"

# Fixed timestamp, so an unchanged pack builds byte-identical (same SHA-1 in the mod bundle manifest).
TIMESTAMP = (2026, 1, 1, 0, 0, 0)


FACES = {"north", "south", "east", "west", "up", "down"}
# 1.21.11 still only accepts these element angles (26.x takes any angle)
ANGLES = {-45, -22.5, 0, 22.5, 45}


def validate(path: str, data: dict, any_rotation: bool = False) -> list:
    """What Minecraft would reject or silently drop in a model - checked before anything gets packed.
    any_rotation: a model only 26.x loads, which takes any angle and rotations around several axes."""
    problems = []
    for i, element in enumerate(data.get("elements", [])):
        where = f"{path} element {i}"
        for corner in (element["from"], element["to"]):
            if len(corner) != 3 or any(not -16 <= v <= 32 for v in corner):
                problems.append(f"{where}: position {corner} outside -16..32")
        rotation = element.get("rotation")
        if rotation and not any_rotation and (rotation["angle"] not in ANGLES or rotation["axis"] not in ("x", "y", "z")):
            problems.append(f"{where}: rotation {rotation}")
        if not element.get("faces"):
            problems.append(f"{where}: no faces")
        for name, face in element.get("faces", {}).items():
            if name not in FACES:
                problems.append(f"{where}: unknown face {name}")
            if any(not 0 <= v <= 16 for v in face.get("uv", [])):
                problems.append(f"{where} {name}: uv {face['uv']} outside 0..16")
            if face.get("rotation", 0) not in (0, 90, 180, 270):
                problems.append(f"{where} {name}: uv rotation {face['rotation']}")
            if face.get("cullface", "north") not in FACES:
                problems.append(f"{where} {name}: cullface {face['cullface']}")
            # Only variables: templates (rails, chains, lanterns, bars) get them from vanilla's child models
            if not face.get("texture", "").startswith("#"):
                problems.append(f"{where} {name}: texture {face.get('texture')!r} is not a #variable")
    return problems


# Which coordinate a face lies on, and whether at the element's "to" (True) or "from" (False) end
FACE_PLANES = {"north": (2, False), "south": (2, True), "west": (0, False), "east": (0, True), "down": (1, False), "up": (1, True)}


def face_outline(element: dict, name: str):
    """(key, plane, outline) of a face for comparing with others. A face across its element's turning axis
    stays flat in the block's own coordinates, so it gets its real turned outline there (key "block");
    any other turned face only compares with faces of elements turned exactly the same way."""
    axis, at_to = FACE_PLANES[name]
    plane = round((element["to"] if at_to else element["from"])[axis], 4)
    u_axis, v_axis = [a for a in range(3) if a != axis]
    lo, hi = element["from"], element["to"]
    corners = [(lo[u_axis], lo[v_axis]), (hi[u_axis], lo[v_axis]), (hi[u_axis], hi[v_axis]), (lo[u_axis], hi[v_axis])]
    rotation = element.get("rotation")
    if not rotation or not rotation.get("angle"):
        return "block", plane, corners
    turning_axis = "xyz".index(rotation["axis"])
    if turning_axis != axis:
        return (rotation["axis"], rotation["angle"], tuple(rotation["origin"])), plane, corners
    # Minecraft turns elements right-handed about +axis; in the (u, v) plane of the two other axes that
    # is counter-clockwise for x (y, z) and z (x, y), clockwise for y (x, z)
    radians = math.radians(rotation["angle"]) * (-1 if axis == 1 else 1)
    cos, sin = math.cos(radians), math.sin(radians)
    scale = 1 / math.cos(math.radians(rotation["angle"])) if rotation.get("rescale") else 1
    ou, ov = rotation["origin"][u_axis], rotation["origin"][v_axis]
    turned = [(ou + scale * (cos * (u - ou) - sin * (v - ov)), ov + scale * (sin * (u - ou) + cos * (v - ov))) for u, v in corners]
    return "block", plane, turned


def overlap_area(first: list, second: list) -> float:
    """Area shared by two convex outlines (Sutherland-Hodgman clipping)."""
    def counter_clockwise(outline):
        doubled = sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(outline, outline[1:] + outline[:1]))
        return outline if doubled > 0 else outline[::-1]

    def left_of(p, a, b):
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-9

    def crossing(p, q, a, b):
        den = (p[0] - q[0]) * (a[1] - b[1]) - (p[1] - q[1]) * (a[0] - b[0])
        t = ((p[0] - a[0]) * (a[1] - b[1]) - (p[1] - a[1]) * (a[0] - b[0])) / den
        return p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])

    result, clipper = counter_clockwise(first), counter_clockwise(second)
    for a, b in zip(clipper, clipper[1:] + clipper[:1]):
        points, result = result, []
        for p, q in zip(points, points[1:] + points[:1]):
            if left_of(q, a, b):
                if not left_of(p, a, b):
                    result.append(crossing(p, q, a, b))
                result.append(q)
            elif left_of(p, a, b):
                result.append(crossing(p, q, a, b))
        if not result:
            return 0.0
    return abs(sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(result, result[1:] + result[:1]))) / 2


def overlapping_faces(path: str, data: dict) -> list:
    """Faces turned the same way that lie on top of each other - the game can't tell which is in front
    and they flicker (own user reports: lantern caps, the rail curve's pieces, found in-game)."""
    faces = []
    for index, element in enumerate(data.get("elements", [])):
        for name in element.get("faces", {}):
            faces.append((name, *face_outline(element, name), index))
    problems = []
    for i, (name, key, plane, outline, index) in enumerate(faces):
        for other_name, other_key, other_plane, other_outline, other_index in faces[i + 1:]:
            if (name, key, plane) != (other_name, other_key, other_plane) or index == other_index:
                continue
            if overlap_area(outline, other_outline) > 1e-4:
                problems.append(f"{path}: {name} faces of elements {index} and {other_index} overlap at {plane}")
    return problems


def for_version(data: dict, pack_format: int) -> dict:
    """Before 26.1 a model's texture entry is only a name - {"sprite": ..., "force_translucent": ...}
    entries become just the sprite's name there. From 26.1 on, some entity textures have moved.
    26.3 dropped an element's "shade": false for "shade_direction_override": "up" (as in its own models)."""
    if pack_format >= 97 and "elements" in data:
        data = {**data, "elements": [
            {**{key: value for key, value in element.items() if key != "shade"}, "shade_direction_override": "up"}
            if element.get("shade") is False else element
            for element in data["elements"]]}
    renames = entity_items.texture_renames(pack_format)
    if pack_format >= 84:
        textures = {key: renames.get(value, value) if isinstance(value, str) else value
                    for key, value in data.get("textures", {}).items()}
    else:
        textures = {key: value["sprite"] if isinstance(value, dict) else value for key, value in data.get("textures", {}).items()}
    return {**data, "textures": textures} if "textures" in data else data


# Blocks that only exist from this pack format on - their models stay out of older versions' packs.
NEW_BLOCKS = {"sulfur_spike": 97, "poplar": 97, "red_shrub": 97}


def in_version(model_path: str, pack_format: int) -> bool:
    """26.3 turned signs into plain block models with textures of their own (block/<wood>_sign): the
    entity textures our 3D sign items are cut from are gone there, so those items keep their flat
    vanilla look in 26.3 until they are rebuilt on the new models."""
    if any(block in model_path and pack_format < since for block, since in NEW_BLOCKS.items()):
        return False
    return not (pack_format >= 97 and model_path.startswith("item/") and model_path.endswith("_sign"))


def mcmeta(pack_format: int, description: str = DESCRIPTION) -> dict:
    pack = {"min_format": pack_format, "max_format": pack_format, "description": description}
    if pack_format < 84:
        pack["pack_format"] = pack_format
    return {"pack": pack}


def write(zf: zipfile.ZipFile, name: str, data: str) -> None:
    info = zipfile.ZipInfo(name, date_time=TIMESTAMP)
    info.compress_type = zipfile.ZIP_DEFLATED
    zf.writestr(info, data)


def build(out_dir: pathlib.Path) -> list:
    problems = [problem for path, data in models3d.MODELS.items() for problem in validate(path, data) + overlapping_faces(path, data)]
    problems += [problem for models in models3d.MODELS_SINCE.values() for path, data in models.items()
                 for problem in validate(path, data, any_rotation=True)]
    if problems:
        raise SystemExit("Invalid models:\n" + "\n".join(problems))
    out_dir.mkdir(parents=True, exist_ok=True)
    written = []
    main_models = sorted(set(models3d.MODELS) - models3d.BUSH_MODELS)
    for version, pack_format in PACK_FORMATS.items():
        path = out_dir / f"TNT-3D-Blocks-{version}.zip"
        with zipfile.ZipFile(path, "w") as zf:
            write(zf, "pack.mcmeta", json.dumps(mcmeta(pack_format), indent=2, ensure_ascii=False))
            for model_path in main_models:
                if not in_version(model_path, pack_format):
                    continue
                data = for_version(models3d.MODELS[model_path], pack_format)
                write(zf, f"assets/minecraft/models/{model_path}.json", json.dumps(data, indent=1))
            for since, models in sorted(models3d.MODELS_SINCE.items()):
                for model_path in sorted(models) if pack_format >= since else ():
                    write(zf, f"assets/minecraft/models/{model_path}.json", json.dumps(for_version(models[model_path], pack_format), indent=1))
            for block in sorted(models3d.BLOCKSTATES):
                write(zf, f"assets/minecraft/blockstates/{block}.json", json.dumps(models3d.BLOCKSTATES[block], indent=1))
            # Entity textures for the entity-shaped items, added to the block texture atlas
            write(zf, "assets/minecraft/atlases/blocks.json", json.dumps(entity_items.atlas_sources(pack_format), indent=1))
            for item in sorted(models3d.ITEMS):
                write(zf, f"assets/minecraft/items/{item}.json", json.dumps(models3d.ITEMS[item], indent=1))
        written.append(path)
        path = out_dir / f"TNT-3D-Bushes-{version}.zip"
        with zipfile.ZipFile(path, "w") as zf:
            write(zf, "pack.mcmeta", json.dumps(mcmeta(pack_format, BUSHES_DESCRIPTION), indent=2, ensure_ascii=False))
            for model_path in sorted(models3d.BUSH_MODELS):
                if not in_version(model_path, pack_format):
                    continue
                data = for_version(models3d.MODELS[model_path], pack_format)
                write(zf, f"assets/minecraft/models/{model_path}.json", json.dumps(data, indent=1))
        written.append(path)
    return written


if __name__ == "__main__":
    target = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).parent / "dist"
    for path in build(target):
        print(path)
