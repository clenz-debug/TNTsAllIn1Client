"""
Generates the "TNT Flat Inventory Icons" resource pack for one Minecraft version (own user request:
3D item models from the bundled 3D pack look wrong in the inventory; later: the hand should show
the same item the inventory shows). The 3D pack is our own TNT 3D Blocks (resourcepacks/3d-blocks),
which replaced Vanilla Tweaks.

Since 1.21.4 an item definition (assets/minecraft/items/<id>.json) can pick its model per display
context. For every item whose look the 3D pack changes - directly via its item definition, or
indirectly through any item/block model in the item's model chain - this pack overrides the item
definition with:
    in the GUI and in a hand  ->  the untouched vanilla model (copied into our own namespace, so
    (first and third person)      the 3D pack's overrides of the same model paths can't reach it) -
                                  also where that is a block in vanilla too, like a trapdoor (own
                                  user report: it stayed the 3D pack's model in the hand)
    anything else             ->  exactly what the 3D pack would have used (3D on the ground, in
                                  item frames, on the head)
The pack sits directly above the 3D pack (pinned by the mod), below the user's own packs, and is
switched by the mod menu's "3D items in inventory & hand" toggle.

Usage (re-run whenever the bundled 3D packs change; several 3D packs comma-separated, lowest first -
the bushes have a pack of their own):
    python generate_flat_icons_pack.py <vanilla client jar> <3D pack zip>[,<zip>...] <pack format> <out zip>
"""
import json
import sys
import zipfile

NAMESPACE = "tntsallin1client"
PREFIX = "assets/minecraft/"
VANILLA_CONTEXTS = ["gui", "firstperson_lefthand", "firstperson_righthand", "thirdperson_lefthand", "thirdperson_righthand"]


def normalize(model_id):
    return model_id if ":" in model_id else f"minecraft:{model_id}"


def model_path(model_id):
    namespace, path = normalize(model_id).split(":", 1)
    return f"assets/{namespace}/models/{path}.json"


def flat_id(model_id):
    """minecraft:item/oak_door -> tntsallin1client:flat/item/oak_door"""
    return f"{NAMESPACE}:flat/{normalize(model_id).split(':', 1)[1]}"


def is_builtin(model_id):
    return normalize(model_id).split(":", 1)[1].startswith("builtin/")


def format_applies(entry, pack_format):
    formats = entry.get("formats")
    if isinstance(formats, dict):
        low, high = formats.get("min_inclusive"), formats.get("max_inclusive")
    elif isinstance(formats, list):
        low, high = formats[0], formats[-1]
    elif isinstance(formats, int):
        low = high = formats
    else:
        low, high = entry.get("min_format"), entry.get("max_format")
    low = low[0] if isinstance(low, list) else low
    high = high[0] if isinstance(high, list) else high
    return low is not None and high is not None and low <= pack_format <= high


def effective_pack_files(pack, pack_format):
    """The 3D pack's files as the game sees them: base, then every applicable overlay on top, in order
    - as (zip, name inside it)."""
    names = pack.namelist()
    files = {name: (pack, name) for name in names if name.startswith(PREFIX) and not name.endswith("/")}
    mcmeta = json.loads(pack.read("pack.mcmeta"))
    for entry in mcmeta.get("overlays", {}).get("entries", []):
        if not format_applies(entry, pack_format):
            continue
        directory = entry["directory"].rstrip("/") + "/"
        for name in names:
            if name.startswith(directory + PREFIX) and not name.endswith("/"):
                files[name[len(directory):]] = (pack, name)
    return files


def model_refs(node, found):
    """Every model id an item definition references (plain models, and special models' base)."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key in ("model", "base") and isinstance(value, str):
                found.add(normalize(value))
            else:
                model_refs(value, found)
    elif isinstance(node, list):
        for value in node:
            model_refs(value, found)
    return found


def remap_refs(node):
    if isinstance(node, dict):
        return {
            key: (flat_id(value) if key in ("model", "base") and isinstance(value, str) else remap_refs(value))
            for key, value in node.items()
        }
    if isinstance(node, list):
        return [remap_refs(value) for value in node]
    return node


def model_closure(jar, model_id, seen):
    """The model plus its whole parent chain, from the vanilla jar."""
    if model_id in seen or is_builtin(model_id):
        return
    path = model_path(model_id)
    if path not in jar.NameToInfo:
        return
    seen[model_id] = json.loads(jar.read(path))
    parent = seen[model_id].get("parent")
    if isinstance(parent, str):
        model_closure(jar, normalize(parent), seen)


def main():
    jar_path, pack_paths, pack_format, out_path = sys.argv[1], sys.argv[2].split(","), int(sys.argv[3]), sys.argv[4]
    packs = [zipfile.ZipFile(path) for path in pack_paths]
    with zipfile.ZipFile(jar_path) as jar:
        pack_files = {}
        for pack in packs:  # a higher pack's files win, like in the game
            pack_files.update(effective_pack_files(pack, pack_format))
        overridden_models = {
            "minecraft:" + name[len(PREFIX + "models/"):-len(".json")]
            for name in pack_files
            if name.startswith(PREFIX + "models/") and name.endswith(".json")
        }
        pack_items = {
            name[len(PREFIX + "items/"):-len(".json")]: json.loads(pack_files[name][0].read(pack_files[name][1]))
            for name in pack_files
            if name.startswith(PREFIX + "items/") and name.endswith(".json")
        }

        out_items = {}
        out_models = {}
        for name in jar.namelist():
            if not (name.startswith(PREFIX + "items/") and name.endswith(".json")):
                continue
            item_id = name[len(PREFIX + "items/"):-len(".json")]
            vanilla_definition = json.loads(jar.read(name))
            closure = {}
            for ref in model_refs(vanilla_definition.get("model"), set()):
                model_closure(jar, ref, closure)
            if item_id not in pack_items and not (closure.keys() & overridden_models):
                continue

            for model_id, model in closure.items():
                copy = dict(model)
                if isinstance(copy.get("parent"), str) and not is_builtin(copy["parent"]):
                    copy["parent"] = flat_id(copy["parent"])
                out_models[model_path(flat_id(model_id))] = copy

            source = pack_items.get(item_id, vanilla_definition)
            definition = {key: value for key, value in source.items() if key != "model"}
            definition["model"] = {
                "type": "minecraft:select",
                "property": "minecraft:display_context",
                "cases": [{"when": VANILLA_CONTEXTS, "model": remap_refs(vanilla_definition["model"])}],
                "fallback": source["model"],
            }
            out_items[f"{PREFIX}items/{item_id}.json"] = definition

    # min/max_format is the current scheme; the old single pack_format only for pre-26.x clients
    # (the same split as resourcepacks/3d-blocks/build.py).
    mcmeta = {
        "pack": {
            "min_format": pack_format,
            "max_format": pack_format,
            "description": "§6TNT Flat Inventory Icons\n§7Flache Items in Inventar und Hand",
        }
    }
    if pack_format < 84:
        mcmeta["pack"]["pack_format"] = pack_format
    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as out:
        out.writestr("pack.mcmeta", json.dumps(mcmeta, indent=2, ensure_ascii=False))
        for path, data in sorted({**out_items, **out_models}.items()):
            out.writestr(path, json.dumps(data, indent=2))
    print(f"{out_path}: {len(out_items)} items, {len(out_models)} models")
    print(" ".join(sorted(p.rsplit("/", 1)[1][:-5] for p in out_items)))


if __name__ == "__main__":
    main()
