#!/usr/bin/env python3
"""Mechanical feature operations on an already-generated project.

These are the deterministic, token-level transforms. The *semantic* wiring (adding a
tab vs a top-level flow, cross-feature callbacks) is bespoke per app and is handled by
the kmp-feature-author agent following docs/ARCHITECTURE.md — not here.

clone_feature  copy an archetype module (catalog=full clean-arch, home=minimal) into a
               new feature, renaming every identifier (module segment, class prefix,
               domain noun) across paths and contents.
remove_feature delete a feature module and strip its *simple* aggregation references.

Both return a checklist of follow-up wiring for the agent / user.
"""

from __future__ import annotations

import argparse
import re
import shutil
import sys
from pathlib import Path

from lib_tokens import apply_replacements, content_replacements, derive_identity, transform_relpath

BINARY_EXTS = {".jar", ".keystore", ".png", ".jpg", ".jpeg", ".webp", ".ico", ".bin"}

# Identifiers each archetype renames. Order matters: PascalCase before lowercase (the
# lowercase token is a substring of the PascalCase one).
ARCHETYPES = {
    "catalog": {"class": "Catalog", "module": "catalog", "noun": "Product", "noun_lower": "product"},
    "home": {"class": "Home", "module": "home", "noun": None, "noun_lower": None},
}

# The capability archetype (a domain:x + data:x split) ships with the plugin, NOT inside
# template/, so generated projects don't carry a sample capability. Its files use the golden
# identity; add_capability() re-tokenizes them to the target project's identity.
CAPABILITY_SRC = Path(__file__).resolve().parent.parent / "archetypes" / "capability"
CAPABILITY_ARCHETYPE = {"class": "Sample", "module": "sample", "noun": "Item", "noun_lower": "item"}


def _pascal(name: str) -> str:
    parts = [p for p in re.split(r"[^A-Za-z0-9]+", name) if p]
    if len(parts) == 1 and any(c.isupper() for c in parts[0][1:]):
        return parts[0][0].upper() + parts[0][1:]
    return "".join(p[:1].upper() + p[1:] for p in parts) or "Feature"


def _module(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", name.lower()) or "feature"


def _is_binary(p: Path) -> bool:
    return p.suffix.lower() in BINARY_EXTS


def _renames(archetype: str, new_feature: str, new_noun: str | None) -> list[tuple[str, str]]:
    a = ARCHETYPES[archetype]
    new_class = _pascal(new_feature)
    new_module = _module(new_feature)
    pairs = [(a["class"], new_class), (a["module"].upper(), new_module.upper()), (a["module"], new_module)]
    if a["noun"] and new_noun:
        new_noun_pascal = _pascal(new_noun)
        pairs.append((a["noun"], new_noun_pascal))
        pairs.append((a["noun_lower"], new_noun_pascal.lower()))
    # Longest source first to avoid a shorter token rewriting a substring of a longer one.
    pairs.sort(key=lambda p: len(p[0]), reverse=True)
    return pairs


def _apply(text: str, pairs: list[tuple[str, str]]) -> str:
    for old, new in pairs:
        text = text.replace(old, new)
    return text


def clone_feature(target: Path, archetype: str, new_feature: str, new_noun: str | None = None) -> list[str]:
    if archetype not in ARCHETYPES:
        raise ValueError(f"unknown archetype: {archetype} (choose from {sorted(ARCHETYPES)})")
    src = target / "feature" / archetype
    if not src.is_dir():
        raise FileNotFoundError(f"archetype module not found: {src}")
    new_module = _module(new_feature)
    dest_root = target / "feature" / new_module
    if dest_root.exists():
        raise FileExistsError(f"feature already exists: {dest_root}")

    pairs = _renames(archetype, new_feature, new_noun)
    created: list[str] = []
    for path in sorted(src.rglob("*")):
        if path.is_dir():
            continue
        rel = path.relative_to(src).as_posix()
        new_rel = _apply(rel, pairs)
        dest = dest_root / new_rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if _is_binary(path):
            shutil.copy2(path, dest)
        else:
            dest.write_text(_apply(path.read_text(encoding="utf-8"), pairs), encoding="utf-8")
        created.append(str(dest.relative_to(target)))
    return created


def _strip_lines(path: Path, predicate) -> bool:
    if not path.exists():
        return False
    lines = path.read_text(encoding="utf-8").split("\n")
    kept = [ln for ln in lines if not predicate(ln)]
    if len(kept) != len(lines):
        path.write_text("\n".join(kept), encoding="utf-8")
        return True
    return False


def _find_pkg_root(target: Path) -> str:
    # base package path, e.g. com/acme/fittrack — inferred from the shared module.
    for p in (target / "shared" / "src" / "commonMain" / "kotlin").rglob("Koin.kt"):
        return p.parent.parent.relative_to(target / "shared" / "src" / "commonMain" / "kotlin").as_posix()
    return ""


def remove_feature(target: Path, feature: str) -> list[str]:
    module = _module(feature)
    cls = _pascal(feature)
    notes: list[str] = []
    dest = target / "feature" / module
    if dest.exists():
        shutil.rmtree(dest)
        notes.append(f"deleted feature/{module}")

    # settings.gradle.kts: include(":feature:x")
    if _strip_lines(target / "settings.gradle.kts", lambda ln: f'":feature:{module}"' in ln):
        notes.append("stripped settings include")
    # shared/build.gradle.kts: api(projects.feature.x)
    if _strip_lines(target / "shared" / "build.gradle.kts", lambda ln: f"projects.feature.{module}" in ln):
        notes.append("stripped shared api(projects.feature)")

    # shared Koin.kt: import + appModules() entry
    koin = next((target / "shared" / "src" / "commonMain" / "kotlin").rglob("Koin.kt"), None)
    if koin and _strip_lines(koin, lambda ln: f".feature.{module}.di." in ln or re.match(rf"\s*{module}Module,\s*$", ln)):
        notes.append("stripped Koin import + appModules entry")

    # core/navigation: NavigationRoutes field + the XNav facade file
    nav_dir = next(((target / "core" / "navigation" / "src" / "commonMain" / "kotlin").rglob("NavigationRoutes.kt")), None)
    if nav_dir:
        _strip_lines(nav_dir, lambda ln: re.search(rf"\bval {module}Nav\s*:\s*{cls}Nav\b", ln) is not None)
        facade = nav_dir.parent / f"{cls}Nav.kt"
        if facade.exists():
            facade.unlink()
            notes.append(f"deleted core/navigation/{cls}Nav.kt + NavigationRoutes field")

    notes.append(
        f"REVIEW MANUALLY: remove any '{module}Graph(...)' / '{module}Nav' tab or flow wiring in "
        "shared/.../RootNavHost.kt and shared/.../navigation/MainRoute.kt, plus cross-feature "
        "callbacks that referenced it."
    )
    return notes


def _infer_prefix(target: Path) -> str:
    # Plugin-id prefix, e.g. the `ft` in id("ft.kmp.library"), read from any module build file.
    pat = re.compile(r'id\("([a-z][a-z0-9]*)\.kmp\.library"\)')
    for bf in target.rglob("build.gradle.kts"):
        m = pat.search(bf.read_text(encoding="utf-8"))
        if m:
            return m.group(1)
    return "app"


def _infer_identity(target: Path):
    """Reconstruct the generated project's identity (base package + plugin prefix) by reading
    it back out of the project, so a plugin archetype can be re-tokenized into it."""
    pkg_path = _find_pkg_root(target)
    if not pkg_path:
        raise FileNotFoundError("could not locate the base package (shared/.../Koin.kt) in target")
    parts = pkg_path.split("/")
    group, slug = ".".join(parts[:-1]), parts[-1]
    return derive_identity(group=group, app_name=slug, slug=slug, prefix=_infer_prefix(target))


def _capability_renames(capability: str, noun: str | None) -> list[tuple[str, str]]:
    a = CAPABILITY_ARCHETYPE
    new_class, new_module = _pascal(capability), _module(capability)
    pairs = [(a["class"], new_class), (a["module"].upper(), new_module.upper()), (a["module"], new_module)]
    if noun:
        np = _pascal(noun)
        pairs.append((a["noun"], np))
        pairs.append((a["noun_lower"], np.lower()))
    pairs.sort(key=lambda p: len(p[0]), reverse=True)
    return pairs


def add_capability(target: Path, capability: str, new_noun: str | None = None) -> list[str]:
    """Scaffold a domain:<x> + data:<x> capability split from the plugin archetype, tokenized
    to the target's identity. Aggregation wiring is left to the caller (see checklist)."""
    if not CAPABILITY_SRC.is_dir():
        raise FileNotFoundError(f"capability archetype missing: {CAPABILITY_SRC}")
    module = _module(capability)
    for layer in ("domain", "data"):
        dest = target / layer / module
        if dest.exists():
            raise FileExistsError(f"capability module already exists: {dest}")

    idn = _infer_identity(target)
    repls = content_replacements(idn)
    name_pairs = _capability_renames(capability, new_noun)
    created: list[str] = []
    for path in sorted(CAPABILITY_SRC.rglob("*")):
        if path.is_dir():
            continue
        rel = path.relative_to(CAPABILITY_SRC).as_posix()
        rel = _apply(transform_relpath(rel, idn), name_pairs)
        dest = target / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if _is_binary(path):
            shutil.copy2(path, dest)
        else:
            text = apply_replacements(path.read_text(encoding="utf-8"), repls)
            dest.write_text(_apply(text, name_pairs), encoding="utf-8")
        created.append(str(dest.relative_to(target)))
    return created


def remove_capability(target: Path, capability: str) -> list[str]:
    module = _module(capability)
    notes: list[str] = []
    for layer in ("domain", "data"):
        dest = target / layer / module
        if dest.exists():
            shutil.rmtree(dest)
            notes.append(f"deleted {layer}/{module}")
        if _strip_lines(target / "settings.gradle.kts", lambda ln, la=layer: f'":{la}:{module}"' in ln):
            notes.append(f"stripped settings include :{layer}:{module}")
        if _strip_lines(
            target / "shared" / "build.gradle.kts",
            lambda ln, la=layer: f"projects.{la}.{module}" in ln,
        ):
            notes.append(f"stripped shared dependency on projects.{layer}.{module}")

    koin = next((target / "shared" / "src" / "commonMain" / "kotlin").rglob("Koin.kt"), None)
    if koin and _strip_lines(
        koin,
        lambda ln: f".domain.{module}.di." in ln or f".data.{module}.di." in ln
        or re.match(rf"\s*{module}(Domain|Data)Module,\s*$", ln) is not None,
    ):
        notes.append("stripped Koin imports + appModules entries")
    return notes


def realize(target: Path, identity, features) -> None:
    """Entry used by generate.py when a feature manifest is supplied. Each op is
    {"op": "clone", "archetype": ..., "feature": ..., "noun": ...},
    {"op": "remove", "feature": ...}, or
    {"op": "capability", "capability": ..., "noun": ...}. Wiring is left to the agent."""
    for op in features or []:
        kind = op.get("op")
        if kind == "clone":
            clone_feature(target, op["archetype"], op["feature"], op.get("noun"))
        elif kind == "remove":
            remove_feature(target, op["feature"])
        elif kind == "capability":
            add_capability(target, op["capability"], op.get("noun"))


def main() -> int:
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("clone", help="clone an archetype into a new feature")
    c.add_argument("--target", required=True)
    c.add_argument("--archetype", required=True, choices=sorted(ARCHETYPES))
    c.add_argument("--feature", required=True, help="new feature module name (lowercase), e.g. events")
    c.add_argument("--noun", help="new domain noun for full archetype, e.g. Event")
    r = sub.add_parser("remove", help="remove a feature module + simple references")
    r.add_argument("--target", required=True)
    r.add_argument("--feature", required=True)
    cap = sub.add_parser("capability", help="scaffold a domain:x + data:x capability split")
    cap.add_argument("--target", required=True)
    cap.add_argument("--capability", required=True, help="capability module name (lowercase), e.g. billing")
    cap.add_argument("--noun", help="domain model noun, e.g. Invoice")
    rc = sub.add_parser("remove-capability", help="remove a capability split + simple references")
    rc.add_argument("--target", required=True)
    rc.add_argument("--capability", required=True)
    args = ap.parse_args()

    target = Path(args.target).resolve()
    if args.cmd == "clone":
        created = clone_feature(target, args.archetype, args.feature, args.noun)
        print(f"cloned {args.archetype} -> feature/{_module(args.feature)} ({len(created)} files)")
        for f in created:
            print(f"  {f}")
        print("\nWIRE UP (agent / manual): settings include, shared api(projects.feature), "
              "Koin appModules(), core/navigation/<Class>Nav facade + NavigationRoutes field, "
              "and a tab/graph in MainRoute.kt or RootNavHost.kt. See docs/ARCHITECTURE.md.")
    elif args.cmd == "capability":
        module = _module(args.capability)
        created = add_capability(target, args.capability, args.noun)
        print(f"scaffolded capability -> domain/{module} + data/{module} ({len(created)} files)")
        for f in created:
            print(f"  {f}")
        print(f"\nWIRE UP (agent / manual): settings include :domain:{module} + :data:{module}, "
              f"shared implementation(projects.data.{module}), Koin appModules() add {module}DomainModule "
              f"+ {module}DataModule, and inject the use cases where the capability is consumed. "
              "See docs/ARCHITECTURE.md.")
    elif args.cmd == "remove-capability":
        for note in remove_capability(target, args.capability):
            print(f"  {note}")
    else:
        for note in remove_feature(target, args.feature):
            print(f"  {note}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
