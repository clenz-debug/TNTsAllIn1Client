"""Checks every mixin of one mod version against that version's Minecraft jar, without starting the game.

Usage: python check_mixins.py <version>      e.g. python check_mixins.py 26.3
       (run from mod/; the version's mod must have been built once, so Loom has the jar)

A mixin whose target method, field or injection point no longer exists only fails when the game
starts. This reads the mixin sources and looks every target up with javap:
  - the class in @Mixin
  - every method named in method = "..."
  - every @At(target = "...") call or field access - and that it really occurs in that method
  - every @Shadow, @Accessor and @Invoker member
  - that an @Inject handler takes the target method's parameters, and a @Redirect handler the
    redirected call's (compared by type name)
Targets in other mods' classes (Sodium, e4mc, ...) are not in the Minecraft jar and are listed as
"not checked". Works for versions with Mojang's own names (26.x); 1.21.11's jar is remapped by Loom
and sits elsewhere.
"""
import pathlib
import re
import subprocess
import sys

version = sys.argv[1]
JAR = pathlib.Path.home() / ".gradle" / "caches" / "fabric-loom" / version / "minecraft-merged.jar"
SRC = pathlib.Path(__file__).parent / version / "src" / "main" / "java"

_cache = {}
BRIDGES = set()


def javap(binary_name: str):
    """(members, supertypes, code) of a class, or None if it is not in the Minecraft jar."""
    if binary_name in _cache:
        return _cache[binary_name]
    result = subprocess.run(["javap", "-cp", str(JAR), "-p", "-v", binary_name.replace("/", ".")],
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0 or "class not found" in result.stderr.lower() or not result.stdout.strip():
        _cache[binary_name] = None
        return None
    lines = result.stdout.splitlines()
    members = {}   # name -> set of descriptors
    code = {}      # (name, descriptor) -> body text
    supertypes = []
    header = next((l for l in lines if re.match(r"^(\w+ )*(class|interface|enum) ", l)), "")
    for part in re.findall(r"(?:extends|implements)\s+(.+?)(?=\s+implements\b|\s*\{|$)", header):
        for name in re.sub(r"<[^<>]*(<[^<>]*>[^<>]*)*>", "", part).split(","):
            supertypes.append(name.strip())
    current = None
    for i, line in enumerate(lines):
        if line.startswith("    descriptor: "):
            descriptor = line[len("    descriptor: "):].strip()
            declaration = lines[i - 1].strip().rstrip(";")
            if "(" in declaration:
                name = declaration[:declaration.index("(")].split()[-1]
                if "." in name or name == binary_name.split("/")[-1].split("$")[-1]:
                    name = "<init>"
            elif declaration == "static {}":
                name = "<clinit>"
            else:
                name = declaration.split()[-1]
            members.setdefault(name, set()).add(descriptor)
            current = (name, descriptor)
            code[current] = []
            # The compiler's own bridge methods (generic overrides) share the name - Mixin skips them
            if i + 1 < len(lines) and "ACC_BRIDGE" in lines[i + 1]:
                BRIDGES.add((binary_name, name, descriptor))
        elif current is not None:
            code[current].append(line)
    _cache[binary_name] = (members, supertypes, {k: "\n".join(v) for k, v in code.items()})
    return _cache[binary_name]


def declares(binary_name: str, name: str, descriptor, seen=None):
    """Whether the class or one of its supertypes has the member - None if a class is outside the jar."""
    seen = seen or set()
    if binary_name in seen:
        return False
    seen.add(binary_name)
    info = javap(binary_name)
    if info is None:
        return None
    members, supertypes, _ = info
    if name in members and (descriptor is None or descriptor in members[name]):
        return True
    unknown = False
    for parent in supertypes:
        found = declares(parent.replace(".", "/"), name, descriptor, seen)
        if found:
            return True
        unknown = unknown or found is None
    return None if unknown else False


PRIMITIVES = {"I": "int", "F": "float", "D": "double", "Z": "boolean", "J": "long", "B": "byte", "S": "short", "C": "char"}


def descriptor_types(descriptor: str) -> list:
    """Simple type names of a method descriptor's parameters: (ILa/b/C$D;[F)V -> [int, D, float[]]."""
    types = []
    for array, primitive, cls in re.findall(r"(\[*)(?:([IFDZJBSC])|L([^;]+);)", descriptor[1:descriptor.index(")")]):
        base = PRIMITIVES[primitive] if primitive else re.split(r"[/$]", cls)[-1]
        types.append(base + "[]" * len(array))
    return types


def handler_types(parameters: str) -> list:
    """Simple type names of a handler's parameters as written in the source, up to the callback."""
    depth = 0
    parts = [""]
    for ch in parameters:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        elif ch == "," and depth == 0:
            parts.append("")
        elif depth == 0:
            parts[-1] += ch
    types = []
    for part in parts:
        words = re.sub(r"@\w+(\([^)]*\))?", " ", part).replace("final ", " ").split()
        if len(words) < 2:
            continue
        simple = words[-2].split(".")[-1]
        if simple.startswith("CallbackInfo"):
            break
        types.append(simple)
    return types


def resolve(simple: str, imports: dict, package: str) -> str:
    """Binary name (a/b/C$D) of a class named in the source as C or C.D."""
    head, *inner = simple.split(".")
    full = imports.get(head, package + "." + head)
    return full.replace(".", "/") + "".join("$" + part for part in inner)


problems = []
unchecked = []
checked = 0

for path in sorted(SRC.rglob("mixin/*.java")):
    source = path.read_text(encoding="utf-8")
    name = path.stem
    package = re.search(r"^package ([\w.]+);", source, re.M).group(1)
    imports = {m.group(1).split(".")[-1]: m.group(1) for m in re.finditer(r"^import ([\w.]+);", source, re.M)}
    mixin = re.search(r"@Mixin\(([^)]*)\)", source)
    if not mixin:
        continue
    targets = [resolve(c, imports, package) for c in re.findall(r"([\w.]+)\.class", mixin.group(1))]
    targets += [t.replace(".", "/") for t in re.findall(r'"([\w.$]+)"', mixin.group(1))]

    for target in targets:
        info = javap(target)
        if info is None:
            unchecked.append(f"{name}: class {target} is not in the Minecraft jar")
            continue
        members, _, code = info

        # method = "..." of every injector, with the @At targets that belong to it
        for annotation in re.finditer(r"@(Inject|Redirect|ModifyVariable|ModifyArg|ModifyArgs|ModifyConstant|ModifyReturnValue|WrapOperation|ModifyExpressionValue|WrapWithCondition)\((.*?)\)\s*\n\s*(?:private|public|protected)[^(]*\(([^{]*)\)\s*(?:throws [\w., ]+)?\{", source, re.S):
            kind = annotation.group(1)
            body = annotation.group(2)
            handler = handler_types(annotation.group(3))
            method_names = re.findall(r'"([^"]+)"', re.search(r"method\s*=\s*(\{[^}]*\}|\"[^\"]*\")", body).group(1))
            at_targets = re.findall(r'target\s*=\s*"([^"]+)"', body)
            for method in method_names:
                checked += 1
                method_name, _, descriptor = method.partition("(")
                descriptor = "(" + descriptor if descriptor else None
                if method_name not in members or (descriptor and descriptor not in members[method_name]):
                    problems.append(f"{name}: no method {method} in {target}")
                    continue
                bodies = [text for (n, d), text in code.items() if n == method_name and (descriptor is None or d == descriptor)]
                candidates = [descriptor_types(d) for d in members[method_name]
                              if (descriptor is None or d == descriptor) and (target, method_name, d) not in BRIDGES]
                # A bare method name means every method of that name - the handler has to fit them all
                if kind == "Inject" and handler and any(handler != candidate for candidate in candidates):
                    problems.append(f"{name}: handler for {method_name} takes ({', '.join(handler)}), the method takes ({' | '.join(', '.join(c) for c in candidates)})")
                elif kind != "Inject" and descriptor is None and len(candidates) > 1:
                    problems.append(f"{name}: {method_name} names {len(candidates)} methods in {target.split('/')[-1]} - @{kind} needs the full descriptor")
                for at in at_targets:
                    checked += 1
                    m = re.match(r"L([^;]+);([^(:]+)(\(.*|:.*)", at)
                    if not m:
                        problems.append(f"{name}: cannot read @At target {at}")
                        continue
                    owner, member, rest = m.groups()
                    printed = '"<init>"' if member == "<init>" else member  # javap's spelling of a constructor
                    reference = f"{printed}:{rest}" if rest.startswith("(") else f"{printed}:{rest[1:]}"
                    qualified = f"{owner}.{reference}"
                    used = any(qualified in text or (owner == target and re.search(r"// \w+ " + re.escape(reference), text)) for text in bodies)
                    if kind == "Redirect" and used and rest.startswith("("):
                        call = descriptor_types(rest)
                        if call and call not in (handler[:len(call)], handler[1:1 + len(call)]):
                            problems.append(f"{name}: redirect handler for {member} takes ({', '.join(handler)}), the call takes ({', '.join(call)})")
                    if not used:
                        exists = declares(owner, member, rest if rest.startswith("(") else rest[1:])
                        why = "the class is not in the Minecraft jar" if exists is None else ("it exists, but is not used there" if exists else "it does not exist")
                        problems.append(f"{name}: {method_name} has no {owner.split('/')[-1]}.{member}{rest if rest.startswith('(') else ''} - {why}")

        # @Shadow / @Accessor / @Invoker members
        for m in re.finditer(r"@(Shadow|Accessor|Invoker)(?:\(([^)]*)\))?[^;{]*?\b(\w+)\s*(\(|;|=)", source):
            kind, argument, declared, after = m.groups()
            checked += 1
            explicit = re.search(r'"([^"]+)"', argument or "")
            member = explicit.group(1) if explicit else declared
            if not explicit and kind != "Shadow":
                stripped = re.sub(r"^(get|set|is|invoke|call)", "", declared)
                member = stripped[:1].lower() + stripped[1:]
            if member not in members and declares(target, member, None) is not True:
                problems.append(f"{name}: @{kind} {member} not in {target}")

print(f"{version}: {checked} targets checked, {len(problems)} problems, {len(unchecked)} not checked")
for line in problems:
    print("  PROBLEM  ", line)
for line in unchecked:
    print("  unchecked", line)
sys.exit(1 if problems else 0)
