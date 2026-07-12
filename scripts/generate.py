#!/usr/bin/env python3
"""Generate a new KMP/CMP project from the bundled golden template.

Deterministic steps:
  1. copy template/ -> target  (dropping Firebase-only / iOS-only files per config)
  2. strip Firebase sentinel regions when Firebase is disabled
  3. apply ordered identity replacements to file contents
  4. relocate package directories to the chosen base package
  5. realize the requested feature set (see feature_ops.py) — default set is a no-op
  6. finalize: chmod gradlew, write CLAUDE.md / blueprint, optional git init

Usage:
    generate.py --target DIR --config config.json
    generate.py --target DIR --group com.acme --name FitTrack [--firebase] [--android-only]

The skill normally writes a config.json and calls the first form.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import stat
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from lib_tokens import Identity, apply_replacements, content_replacements, derive_identity, transform_relpath  # noqa: E402

PLUGIN_ROOT = Path(__file__).resolve().parent.parent
TEMPLATE_DIR = PLUGIN_ROOT / "template"

BINARY_EXTS = {
    ".jar", ".keystore", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico",
    ".ttf", ".otf", ".woff", ".woff2", ".bin", ".zip", ".so", ".a", ".dylib",
    ".class", ".pdf",
}

# Dropped (relative POSIX paths) when Firebase is disabled. Paths use the golden package
# (pre-relocation) because should_drop() matches on the template-relative path.
FIREBASE_ONLY_FILES = {
    ".github/workflows/distribute-stage-android.yml",
    ".github/workflows/distribute-prod-play.yml",
    ".github/workflows/distribute-ios-testflight.yml",
    "androidApp/src/prod/google-services.json",
    "androidApp/src/stage/google-services.json",
    "androidApp/src/main/kotlin/com/acmecorp/acmeapp/android/FirebaseCrashReporter.kt",
    "iosApp/config/prod/GoogleService-Info.plist",
    "iosApp/config/stage/GoogleService-Info.plist",
    "iosApp/iosApp/SwiftCrashReporter.swift",
}

# Dropped when iOS is disabled (android-only). The KMP iOS *targets* stay in the
# convention plugins so the project remains iOS-ready; only the Xcode app + iOS CI go.
IOS_ONLY_PREFIXES = ("iosApp/",)
IOS_ONLY_FILES = {
    ".github/workflows/ios-build.yml",
    ".github/workflows/distribute-ios-testflight.yml",
}

FIREBASE_MARKER = "kmp-scaffold:firebase:"


def is_binary(path: Path) -> bool:
    return path.suffix.lower() in BINARY_EXTS


def strip_firebase(text: str, firebase_enabled: bool) -> str:
    """Always remove the sentinel marker lines; drop the wrapped body only when
    Firebase is disabled."""
    if FIREBASE_MARKER not in text:
        return text
    out: list[str] = []
    dropping = False
    for line in text.split("\n"):
        if FIREBASE_MARKER + "begin" in line:
            dropping = not firebase_enabled
            continue
        if FIREBASE_MARKER + "end" in line:
            dropping = False
            continue
        if dropping:
            continue
        out.append(line)
    return "\n".join(out)


def should_drop(rel: str, firebase: bool, android_only: bool) -> bool:
    if not firebase and rel in FIREBASE_ONLY_FILES:
        return True
    if android_only and (rel in IOS_ONLY_FILES or rel.startswith(IOS_ONLY_PREFIXES)):
        return True
    return False


def copy_tree(target: Path, idn: Identity, firebase: bool, android_only: bool) -> int:
    repls = content_replacements(idn)
    written = 0
    for dirpath, _dirnames, filenames in os.walk(TEMPLATE_DIR):
        for fn in filenames:
            abs_path = Path(dirpath) / fn
            rel = abs_path.relative_to(TEMPLATE_DIR).as_posix()
            if should_drop(rel, firebase, android_only):
                continue
            dest = target / transform_relpath(rel, idn)
            dest.parent.mkdir(parents=True, exist_ok=True)
            if is_binary(abs_path):
                shutil.copy2(abs_path, dest)
            else:
                text = abs_path.read_text(encoding="utf-8")
                text = strip_firebase(text, firebase)
                text = apply_replacements(text, repls)
                dest.write_text(text, encoding="utf-8")
            written += 1
    return written


def make_executable(path: Path) -> None:
    if path.exists():
        path.chmod(path.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def write_blueprint(target: Path, idn: Identity) -> None:
    """Copy the conventions blueprint into the generated repo (docs + CLAUDE.md) so
    future Claude sessions there extend the project the same way."""
    blueprint_src = PLUGIN_ROOT / "skills" / "kmp-blueprint" / "SKILL.md"
    docs = target / "docs"
    docs.mkdir(parents=True, exist_ok=True)
    if blueprint_src.exists():
        text = apply_replacements(blueprint_src.read_text(encoding="utf-8"), content_replacements(idn))
        (docs / "ARCHITECTURE.md").write_text(text, encoding="utf-8")

    claude_md = target / "CLAUDE.md"
    if not claude_md.exists():
        claude_md.write_text(
            f"# {idn.project_name} — project guide for Claude\n\n"
            f"KMP / Compose Multiplatform app (Android + iOS). Base package "
            f"`{idn.base_pkg}`.\n\n"
            "## Conventions (read `docs/ARCHITECTURE.md` for the full blueprint)\n\n"
            "- Modular: `:core:*` (shared infra) + `:feature:*` (screens). Features depend on "
            "`:core:*` only, never on each other.\n"
            "- Convention plugins in `build-logic` (`"
            f"{idn.prefix_lower}.kmp.library`, `{idn.prefix_lower}.kmp.compose`, "
            f"`{idn.prefix_lower}.feature`, `{idn.prefix_lower}.android.application`, "
            f"`{idn.prefix_lower}.room`).\n"
            "- Koin DI: every `module { }` lives in its own file inside a `di` package — never "
            "beside a ViewModel/class. Register feature modules in `shared/.../Koin.kt#appModules()`.\n"
            "- Cross-feature navigation goes through the `:core:navigation` facade: each feature "
            "exposes an `XNav` interface (route keys stay `internal` to the feature).\n"
            "- ViewModels: plain `ViewModel` + a single `StateFlow<UiState>`; no base class, no "
            "`Result` wrapper. Screens collect with `collectAsStateWithLifecycle()` and get the VM "
            "via `koinViewModel()`.\n"
            "- Domain/data split: `UseCase → Repository → DataSource` (remote = Ktor, local = Room). "
            "A cross-cutting capability is a separate `:domain:x` + `:data:x` pair.\n"
            "- Errors: fallible ops return `Either<AppError, T>` (Arrow); exceptions are caught only at "
            "the data boundary via `catching {}` in `:core:common`. ViewModels `.fold` into UiState.\n"
            "- Logging: `initLogging(crashReporter, isDebug)` (Kermit) — default `NoOpCrashReporter`; "
            "swap in a Firebase reporter when enabled.\n\n"
            "## Add a feature\n\n"
            "Ask Claude: \"add a feature called <name>\" — it clones the `catalog` (full) or `home` "
            "(minimal) archetype, adapts the domain, and wires DI + navigation. See the \"Adding a "
            "feature\" section of `docs/ARCHITECTURE.md`.\n\n"
            "## Build & run\n\n"
            "```bash\n"
            "./gradlew :androidApp:assembleProdDebug   # Android APK\n"
            "./gradlew check                            # unit tests (JVM host)\n"
            "./gradlew staticAnalysis                   # detekt + ktlint\n"
            "```\n",
            encoding="utf-8",
        )


def seed_memories(target: Path) -> None:
    mem = target / ".claude" / "memory"
    mem.mkdir(parents=True, exist_ok=True)
    (mem / "koin-modules-separate-file.md").write_text(
        "---\nname: koin-modules-separate-file\n"
        "description: Koin module { } definitions live in their own file in a di package\n"
        "metadata:\n  type: project\n---\n\n"
        "Never put a `module { }` beside a ViewModel/class. Each Koin module is its own file in a "
        "`di` package. Aggregate feature modules in `shared/.../Koin.kt#appModules()`.\n",
        encoding="utf-8",
    )
    (mem / "cross-feature-navigation.md").write_text(
        "---\nname: cross-feature-navigation\n"
        "description: Features navigate via the core navigation interface facade\n"
        "metadata:\n  type: project\n---\n\n"
        "Cross-feature navigation goes through `:core:navigation`. Each feature exposes an `XNav` "
        "interface + `XNavImpl`; route keys stay `internal` to the feature module. The shell never "
        "sees another feature's route key.\n",
        encoding="utf-8",
    )
    (mem / "domain-data-capability-split.md").write_text(
        "---\nname: domain-data-capability-split\n"
        "description: Cross-cutting capabilities split into :domain:x + :data:x; features stay single-module\n"
        "metadata:\n  type: project\n---\n\n"
        "A feature is one module (domain/data as packages inside it, e.g. `catalog`). A cross-cutting "
        "capability used by multiple features is split into `:domain:x` (model, repository interface, "
        "use cases, `XError : AppError`) + `:data:x` (implementation). Fallible ops return "
        "`Either<AppError, T>` via `catching {}`; nothing throws above the data boundary.\n",
        encoding="utf-8",
    )


def git_init(target: Path) -> None:
    try:
        subprocess.run(["git", "init", "-q"], cwd=target, check=True)
        subprocess.run(["git", "add", "-A"], cwd=target, check=True)
        subprocess.run(
            ["git", "commit", "-q", "-m", "Initial scaffold from kmp-cmp-scaffold"],
            cwd=target, check=True,
            env={**os.environ, "GIT_AUTHOR_NAME": "kmp-cmp-scaffold",
                 "GIT_AUTHOR_EMAIL": "scaffold@local", "GIT_COMMITTER_NAME": "kmp-cmp-scaffold",
                 "GIT_COMMITTER_EMAIL": "scaffold@local"},
        )
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(f"warning: git init/commit skipped: {e}", file=sys.stderr)


def load_config(args: argparse.Namespace) -> dict:
    if args.config:
        cfg = json.loads(Path(args.config).read_text(encoding="utf-8"))
    else:
        cfg = {}
    # CLI overrides.
    for key in ("group", "slug", "prefix", "app_display", "base_url"):
        v = getattr(args, key, None)
        if v:
            cfg[key] = v
    if args.name:
        cfg["app_name"] = args.name
    if args.firebase:
        cfg["firebase"] = True
    if args.android_only:
        cfg["android_only"] = True
    if args.no_git:
        cfg["git_init"] = False
    return cfg


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--target", required=True, help="output directory (created if missing; must be empty)")
    ap.add_argument("--config", help="path to a JSON config")
    ap.add_argument("--group", help="reverse-domain org, e.g. com.acme")
    ap.add_argument("--name", help="app name, e.g. FitTrack")
    ap.add_argument("--slug")
    ap.add_argument("--prefix")
    ap.add_argument("--app-display", dest="app_display")
    ap.add_argument("--base-url", dest="base_url")
    ap.add_argument("--firebase", action="store_true")
    ap.add_argument("--android-only", action="store_true")
    ap.add_argument("--no-git", action="store_true")
    args = ap.parse_args()

    cfg = load_config(args)
    if not cfg.get("group") or not cfg.get("app_name"):
        print("error: 'group' and 'app_name' (--group / --name) are required", file=sys.stderr)
        return 2

    idn = derive_identity(
        group=cfg["group"],
        app_name=cfg["app_name"],
        base_url=cfg.get("base_url") or "https://dummyjson.com/",
        slug=cfg.get("slug"),
        prefix=cfg.get("prefix"),
        app_display=cfg.get("app_display"),
    )
    firebase = bool(cfg.get("firebase", False))
    android_only = bool(cfg.get("android_only", False))

    target = Path(args.target).resolve()
    if target.exists() and any(target.iterdir()):
        print(f"error: target is not empty: {target}", file=sys.stderr)
        return 2
    target.mkdir(parents=True, exist_ok=True)

    if not TEMPLATE_DIR.is_dir():
        print(f"error: template dir missing: {TEMPLATE_DIR}", file=sys.stderr)
        return 2

    written = copy_tree(target, idn, firebase, android_only)

    # features: None / "default" -> keep the reference set as-is (exact replica).
    features = cfg.get("features")
    if features not in (None, "default"):
        import feature_ops
        feature_ops.realize(target, idn, features)

    make_executable(target / "gradlew")
    write_blueprint(target, idn)
    seed_memories(target)
    if cfg.get("git_init", True):
        git_init(target)

    print(f"Generated {idn.project_name} at {target}")
    print(f"  base package : {idn.base_pkg}")
    print(f"  plugin prefix: {idn.prefix_lower}.*")
    print(f"  firebase     : {'on' if firebase else 'off'}")
    print(f"  targets      : {'android-only' if android_only else 'android + ios'}")
    print(f"  files written: {written}")
    if firebase:
        print("  NOTE: replace placeholder google-services.json / GoogleService-Info.plist with real Firebase config,")
        print("        then wire the Firebase crash reporter (FirebaseCrashReporter / SwiftCrashReporter) into")
        print("        the Application + iOSApp.swift in place of the no-op default.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
