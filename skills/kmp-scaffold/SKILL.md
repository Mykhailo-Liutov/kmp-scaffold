---
name: kmp-scaffold
description: Scaffold a new Kotlin Multiplatform + Compose Multiplatform app (Android + iOS) into the current empty folder, tailored to the user's organization, app name, and domain. Runs a short interview, generates a compiling project from the golden template, clones/adapts feature modules to the user's domain, verifies the build, and documents next steps. Use when the user wants to create/start a new KMP, CMP, Kotlin Multiplatform, or Compose Multiplatform mobile project, or runs /kmp-new.
---

# Scaffold a KMP/CMP project

Generate a new Android+iOS app from the bundled golden template, customized to what the user
needs. Assume the user may not be a mobile expert: explain choices in plain terms, lead with
sensible defaults, and never block on jargon.

Scripts live under `${CLAUDE_PLUGIN_ROOT}/scripts`. The conventions reference is the
`kmp-blueprint` skill (`${CLAUDE_PLUGIN_ROOT}/skills/kmp-blueprint/SKILL.md`) — read it before
adapting any feature.

## 1. Confirm the target

The target is the current working directory. Check it is empty (ignore `.git`, `.DS_Store`).
If it is **not** empty, stop and ask the user for an empty folder — do not overwrite files.

## 2. Interview

Use **AskUserQuestion**. Offer a one-tap fast path ("use sensible defaults"). Collect:

- **App name** (human, e.g. "Fit Track"). Derive and *show* the user: PascalCase project name
  (`FitTrack`), package slug (`fittrack`), Gradle plugin prefix (`ft`). Let them override.
- **Organization** as a reverse-domain (e.g. `com.acme`). Base package = `<org>.<slug>`.
- **What is the app about?** (free text). From this, propose **1–3 domain features** and which is
  the primary list screen.
- **Targets**: Android + iOS (default) or Android-only.
- **Firebase**: off (default — builds with zero external accounts) or on (analytics + crashlytics +
  app-distribution; needs the user to drop in real `google-services.json` / `GoogleService-Info.plist`
  later).
- **Keep a working reference feature?** Default yes — keep the `catalog` module (a live remote-list +
  Room-cache example pointing at dummyjson.com) renamed to the user's primary domain noun, so the
  first build shows real data.

Do not ask about things with obvious defaults (minSdk, Gradle version, etc.). Confirm the derived
identity once before generating.

## 3. Generate the skeleton

Write a config JSON to a scratch path and run the generator:

```bash
python3 "${CLAUDE_PLUGIN_ROOT}/scripts/generate.py" --target "$PWD" \
  --group "<org>" --name "<App Name>" [--firebase] [--android-only] [--no-git]
```

(or pass `--config config.json` with keys: `group, app_name, slug, prefix, app_display, base_url,
firebase, android_only, git_init`). This produces a **compiling** Android+iOS project with the full
`:core:*` set and the reference features, all re-namespaced to the user's identity, Firebase regions
stripped unless enabled, plus `CLAUDE.md`, `docs/ARCHITECTURE.md`, and `.claude/rules/`.

## 4. Tailor to the domain

Adapt the generated project to the user's app. Prefer **additive** changes (low risk):

- **Primary domain feature**: rename the kept `catalog` to the user's primary noun, OR clone a fresh
  feature. To clone:
  ```bash
  python3 "${CLAUDE_PLUGIN_ROOT}/scripts/feature_ops.py" clone --target "$PWD" \
    --archetype catalog --feature events --noun Event     # full clean-arch (remote+Room)
  python3 "${CLAUDE_PLUGIN_ROOT}/scripts/feature_ops.py" clone --target "$PWD" \
    --archetype home --feature dashboard                  # minimal screen, no data layer
  ```
- **Add a cross-cutting capability** (a `domain:x` + `data:x` split shared by features) when the
  domain calls for one:
  ```bash
  python3 "${CLAUDE_PLUGIN_ROOT}/scripts/feature_ops.py" capability --target "$PWD" \
    --capability billing --noun Invoice
  ```
- **Wire each new/renamed feature or capability** by delegating to the **kmp-feature-author** agent
  (pass the name, kind/archetype, target dir, and a short domain description). The agent edits
  `settings.gradle.kts`, `shared/build.gradle.kts`, `shared/.../Koin.kt`, the `:core:navigation`
  facade + `NavigationRoutes` (features) or the `domain/data` includes (capabilities), and the
  tab/flow in `MainScreen`/`RootNavHost`, then adapts the domain — following `kmp-blueprint`. For one
  simple feature you may do this inline instead of spawning the agent.
- **Remove** reference features the user doesn't want:
  ```bash
  python3 "${CLAUDE_PLUGIN_ROOT}/scripts/feature_ops.py" remove --target "$PWD" --feature profile
  ```
  then follow the printed checklist to clean the `MainScreen`/`RootNavHost` wiring.

Keep the conventions: Koin modules in `di` packages, route keys `internal`, plain `ViewModel +
StateFlow`, fallible data ops return `Either<AppError, …>` via `catching`. When in doubt, mirror the
`catalog`/`home` modules.

**After renaming or removing `catalog`, sweep the leftovers**: the old `core/navigation/CatalogNav.kt`
facade, the `feature/catalog/schemas/` dir (rename its database-qualified subdir too), and the
`Catalog`/`Product` running-example wording in the generated `CLAUDE.md`, `docs/ARCHITECTURE.md`, and
`.claude/rules/` files (the blueprint copy in `ARCHITECTURE.md` uses `Catalog`/`Product` as its
example — repoint it at the new primary feature). Keep the Ktor remote seam intact when tailoring — if there is no real API yet,
keep the DTO + mapper + `HttpClient` structure and back it with sample data behind the same
interface, so the reference architecture survives.

## 5. Verify

```bash
bash "${CLAUDE_PLUGIN_ROOT}/scripts/verify.sh" "$PWD"
```

This assembles the Android prod-debug APK and runs JVM-host tests. If it fails, read the Gradle
output, fix the wiring (usually a missing `appModules()` entry, a missing `XNav` facade, or a
dangling cross-feature callback), and re-run. iOS verification is macOS-only — print the xcodegen +
xcodebuild hint from the script output; don't attempt it on Linux.

## 6. Finish

**Commit the tailoring** (generation auto-committed the raw scaffold): `git add -A && git commit -m
"Tailor scaffold to <domain>"` — the user should not land on a dirty tree.

Tell the user, concisely:
- where the project is and its base package;
- how to open/run it (Android Studio → run the `prodDebug` variant; or `./gradlew
  :androidApp:assembleProdDebug`);
- for iOS (if enabled): `brew install xcodegen && cd iosApp && xcodegen generate && open *.xcodeproj`;
- that `CLAUDE.md` + `docs/ARCHITECTURE.md` document the conventions, and they can ask Claude to
  "add a feature called X" any time;
- if Firebase is on: replace the placeholder `google-services.json` / `GoogleService-Info.plist`.
