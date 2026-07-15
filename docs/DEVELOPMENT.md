# Development notes — `kmp-cmp-scaffold`

Deep internals + rationale + roadmap for the generator. Pair with `CLAUDE.md` (quick orientation)
and `README.md` (end-user docs).

## Goal

Let someone — possibly not a mobile expert — drop into an empty folder, run
`/kmp-cmp-scaffold:kmp-new`, answer a
short interview, and get the *generic architecture* of a private production KMP/CMP app (the
golden source), tailored to their org / name / domain, that
**compiles and runs with zero external accounts**. "Generic" = the golden project's app-specific
machinery (Firebase auth, session, social sign-in, the session-gated nav shell) is deliberately
**left out**; the scaffold ships the reusable architecture (modules, convention plugins, Arrow
typed errors, logging interface, ktlint) + a tabbed shell.

## Why this design (hybrid)

| Part | Strategy | Why |
|---|---|---|
| Invariant skeleton (convention plugins, version catalog, Gradle wrapper, CI, core modules, app entry points, `project.yml`) | copy a literal template + string-replace | intricate + version-pinned; regenerating from prose breaks compilation |
| Identity (org, name, prefix, package) | deterministic ordered replacement | must be exact and reproducible |
| Which features + their domain (models, nouns, fields) | clone archetype (mechanical) + Claude tailors | the one place an LLM genuinely adds value |
| Cross-feature nav wiring (tab vs flow, callbacks) | Claude (agent), guided by blueprint | bespoke per app; a regex regenerator would be wrong |

Pure script can't model a domain; pure-LLM generation drifts and may not compile. Hybrid puts
determinism where correctness matters and judgment where it matters.

## template/ — how it's stored

A **curated, sanitized copy** of the golden repo. The golden literals act as tokens (no
`__PLACEHOLDER__` indirection — simpler, and `template/` stays diffable against the source). It is
**not a verbatim mirror**: the golden project's app-specific machinery is intentionally absent —
`domain:auth` / `data:auth` (Firebase + GitLive + social sign-in), session/`SessionState`, and the
session-gated nav shell (`RouterRoute`/`RouterViewModel`). What's kept is generic: `:core:*`,
`:feature:{home,catalog,profile}`, Arrow primitives + logging interface in `:core:common`, ktlint as
a Gradle task, and a tabbed `RootNavHost` that starts straight at the shell.

`tools/extract-template.py` was the original bootstrap (sanitized literal copy: excludes `build/
.gradle/ .git/ .idea/ .kotlin/ .claude/ captures/ xcuserdata/ Pods/ vendor/`, the generated
`*.xcodeproj/`, `local.properties`, `keystore.properties`, `release.jks`, `*.jks`/`*.keystore` —
keeps the committed `debug.keystore` — and `.DS_Store`; replaces real Firebase config with
placeholders; copies binaries byte-for-byte). Because `template/` now diverges from golden on
purpose, **re-extraction is a reference aid, not a refresh**: it re-imports the auth/session/Firebase
code, which must then be re-stripped and the Firebase sentinels re-added (below). Prefer porting a
single convention by hand.

## Token map (golden → user), in `lib_tokens.content_replacements()`

Applied in this order (longest / most-specific first):

1. `com.acmecorp.acmeapp` → `<group>.<slug>`
2. `Acme App Stage` → `<display> Stage`
3. `Acme App` → `<display>`
4. `AcmeApp` → `<ProjectName>`   (Gradle rootProject.name, Xcode name, scheme prefix)
5. `Acme` → `<Prefix>`               (class prefix: `AcmeTheme`, `AcmeApplication`)
6. `acme.` → `<prefix>.`             (plugin ids: `acme.kmp.library`, `acme.feature`, `acme.room`, …)
7. `acmeapp` → `<slug>`          (residual: keyAlias, schema dir segment)
8. `acmecorp` → `<group last segment>`
9. `https://dummyjson.com/` → `<base_url>` (only if changed; default keeps dummyjson as a live demo)

Why the order: `Acme` is a substring of `AcmeApp` (so #4 before #5); `acmeapp` is a substring of
the dotted package (#1 before #7); the display strings (#2/#3) before the camel name (#4).

**Directory packages** are relocated separately (`transform_relpath`): the contiguous segment chain
`com/acmecorp/acmeapp` → the new base-package path; then the dotted replacements catch the
Room **schema dir** name (`com.acmecorp.acmeapp.feature.catalog…`) and any identity literal in
a file name.

Identity derivation (`derive_identity`): `slug` = lowercased alnum of the app name; `ProjectName` =
PascalCase; `prefix` = uppercase-initials of the name (≥2) else first 3 of slug; `base_pkg` =
`group.slug`.

## Firebase strip (sentinels)

`generate.py` doesn't pattern-match Firebase — it removes lines between
`kmp-scaffold:firebase:begin` and `kmp-scaffold:firebase:end` (any comment syntax). It **always**
drops the two marker lines; it drops the **wrapped body only when Firebase is disabled** (default).

Sentinels were hand-added to these five template files (regenerate-and-re-add after `extract`):

- `build.gradle.kts` — the 3 firebase/google-services plugin aliases in the root plugins list.
- `androidApp/build.gradle.kts` — the 3 plugin `alias(...)` lines, the `firebaseAppDistribution { }`
  block, and `platform(libs.firebase.bom)` + `libs.bundles.firebase`.
- `gradle/libs.versions.toml` — the firebase `[versions]`, `[plugins]`, `[libraries]`, and the
  `firebase` bundle.
- `iosApp/project.yml` — the `packages.Firebase` block, the target `dependencies:` (3 Firebase
  products), and the GoogleService-selection `postBuildScripts`.
- `iosApp/iosApp/iOSApp.swift` — `import FirebaseCore` and `FirebaseApp.configure()`.

Whole files dropped when Firebase is off (`FIREBASE_ONLY_FILES` in `generate.py`):
the 3 `distribute-*` workflows, both `google-services.json`, both `GoogleService-Info.plist`, and the
Firebase crash reporters `androidApp/.../FirebaseCrashReporter.kt` + `iosApp/iosApp/SwiftCrashReporter.swift`.
The app entry points (`AcmeApplication`, `startKoinIos`) call `initLogging(NoOpCrashReporter(), …)` by
default; with Firebase on, swap in the Firebase reporter (one-line edit, noted by the generator).
When Firebase is **on**, placeholders are kept and a "replace with real config" note is printed.

## Android-only mode

`--android-only` drops `iosApp/**` + `ios-build.yml` + `distribute-ios-testflight.yml`, but **keeps
the KMP iOS targets** in the convention plugins (project stays iOS-ready, Android still builds).
Full iOS-target removal (editing `KmpLibraryConventionPlugin`, `shared` framework block, deleting
`iosMain` + expect/actual) is **not yet automated** — see roadmap.

## feature_ops.py

`clone --archetype {catalog|home} --feature <name> [--noun <Noun>]`: copies `feature/<archetype>` →
`feature/<name>`, renaming identifiers across paths + contents. Ordered renames (PascalCase before
lowercase; uppercase const form too): `Catalog→<Class>`, `CATALOG→<UPPER>`, `catalog→<module>`, and
for the full archetype `Product→<Noun>`, `product→<noun>`. Verified consistent: e.g.
`EventsDatabase.eventDao()`, table `events`, `EventDao`/`EventEntity`/`EventDto` all align.

`remove --feature <name>`: deletes the module + strips the *simple* aggregation references (settings
include, shared api, Koin import+entry, `NavigationRoutes` field, deletes the `<Class>Nav` facade) and
prints a checklist for the bespoke `MainRoute`/`RootNavHost` cleanup.

`capability --capability <name> [--noun <Noun>]`: scaffolds a `domain:<name>` + `data:<name>` split
from `archetypes/capability/` (shipped with the plugin, **not** in `template/`, so generated projects
don't carry a sample capability). Unlike `clone` (which copies an in-project module already in the
user's identity), the archetype uses the golden identity, so `add_capability` re-tokenizes it: it
reads the target's identity back out of the project (`_infer_identity` — base package from
`shared/.../Koin.kt`, plugin prefix from any `id("…​.kmp.library")`), applies
`lib_tokens.content_replacements`, then the `Sample→<Class>` / `Item→<Noun>` renames. The archetype's
`InMemory…Repository` is a Firebase-free stub returning `Either<AppError, Unit>`. `remove-capability`
strips both modules + settings includes + shared dep + Koin entries.

Semantic wiring of a clone/capability is **not** in this script — the `kmp-feature-author` agent does it.

## Test evidence (what's been validated)

Real Gradle 9.4.1 builds on macOS, Android SDK present:

1. `generate.py` replica (`com.acme` / `FitTrack`, Firebase off) → `BUILD SUCCESSFUL`, APK; +
   `testAndroidHostTest` passes (catalog's Arrow-typed tests survive the rename). No stray golden
   literals; package dirs relocated; build-logic plugin ids `ft.*`; Firebase cleanly stripped.
2. `feature_ops.py capability --capability billing --noun Invoice`, wired into settings + shared +
   Koin → compiles as part of `:androidApp:assembleProdDebug`.
3. `./gradlew staticAnalysis` (detekt + ktlint across all modules) → `BUILD SUCCESSFUL`.
4. Firebase **on** generate → crash-reporter files kept, Firebase regions retained, no leftover
   sentinel markers; Firebase **off** → those whole files dropped.

Re-run #1 as the regression gate after any generator change (commands in `CLAUDE.md`).

## Roadmap / open items

- ~~**Live interview run**~~ DONE (2026-07-12): headless `claude -p "/kmp-cmp-scaffold:kmp-new ..."
  --plugin-dir` run generated a travel-log app (`com.demoworks` / TripLog), the agent renamed
  catalog→trips with a real Trip domain + tests, and the Android build + JVM-host tests passed.
  Note: only the *fully qualified* command name resolves in headless runs.
- **Full Android-only**: strip KMP iOS targets when iOS isn't wanted (for non-mac users).
- **Optional `:core` trimming**: let apps without networking/DB drop `:core:network` / `:core:database`
  (mind `FeatureConventionPlugin`'s mandatory deps: common/designsystem/ui).
- **More archetypes**: a form/detail feature beyond minimal/full (the `capability` domain/data split
  archetype already exists).
- **Distribution**: Play (`gradle-play-publisher`/`fastlane supply`) + TestFlight lanes (golden ships
  these stubbed).
- **`generate.py` feature manifest**: `realize()` exists but the skill currently prefers
  clone-then-agent-wire; flesh out manifest-driven generation if a fully headless path is wanted.
- **Transactional generation**: generate into a staging dir, validate, then rename into place, so a
  failed run never leaves a half-written target that blocks retry.
- **Escaping-aware display values**: `app_display`/`base_url` are inserted verbatim into Kotlin, XML,
  YAML and plist contexts; either restrict the charset harder or add context-aware escaping.
- **Immutable feature archetypes**: `clone` currently copies `feature/catalog|home` from the *target*
  project, so user edits mutate the archetype; ship plugin-side copies like the capability archetype.
- **Deterministic standard wiring**: settings include / shared dep / Koin entry / nav facade+field are
  mechanical once the tab-vs-flow decision is made — script them, leave only semantics to the agent.
- **Generator test matrix in CI**: identities incl. collisions/keywords/unicode, Firebase on/off,
  android-only, all feature ops, malformed sentinels, golden-token leak scan.
- **Template hardening from generated-app reviews** (TripLog/RecipeBox, 2026-07): feature-level
  typed errors instead of one generic `<X>Error` string; a failure strategy for the observe/`Flow`
  read path (refresh path is typed, reads aren't); single-flight `refresh()` in the ViewModel;
  iOS Room db under Application Support via `NSFileManager` (not `NSHomeDirectory()`); a real
  detail destination behind the list `onItemClick` (currently a stubbed no-op affordance).
- **Prefix collision check**: `derive_identity` could collide two-word names to the same prefix;
  consider validating/uniquifying.
- **Bookworm review (2026-07-15, codex full review of a headless `/kmp-new` run)** — build was green;
  these are template/generator hardening items, not regressions. Triaged as generator fixes unless noted:
  - *[High]* `core/database/RoomBuilders.kt` defaults to `fallbackToDestructiveMigration(dropAllTables =
    true)` — silent data loss on a missing migration once features persist real user data. Gate
    destructive recreation to debug, or document the DB as a disposable cache only.
  - *[Med]* Shipped `docs/ARCHITECTURE.md` is a verbatim copy of the blueprint, so it instructs the
    *generated* repo's user to run `scripts/feature_ops.py clone|capability` — but no `scripts/` dir is
    shipped in a generated project. The blueprint→ARCHITECTURE.md handoff must swap plugin-internal script
    calls for "ask Claude to add a feature called X". That same copy still teaches `Catalog*`/`Product*`
    after a catalog→feature rename — extend the skill's post-rename sweep to `docs/ARCHITECTURE.md` + seed
    memories, not just `CLAUDE.md`.
  - *[Med]* `core/navigation/NavigationRoutes.kt` is a `data class … internal constructor` → Kotlin flags
    the more-visible generated `copy()`; becomes an error at language version 2.5. Make it a plain class
    (no data-class semantics needed) or align copy visibility.
  - *[Med]* `androidApp/build.gradle.kts` release-signing gate keys on `taskNames.any {
    it.contains("Release") }` (aggregate/custom tasks can still build an unsigned release) and reads
    `storeFile = file(keystoreProps…)` relative to `androidApp/`, not the root that holds
    `keystore.properties`. Validate the release *variant* at execution time and resolve with
    `rootProject.file(...)`.
  - *[Med]* CI `ios-build.yml` + `distribute-ios-testflight.yml` hardcode `runs-on: macstadium` (an
    undocumented custom runner). Default to `macos-latest` or document the requirement; golden ships these.
  - *[Med, partly upstream]* iOS `project.yml` `deploymentTarget: 15.0` while Skiko's ICU object is built
    for iOS-sim 17.2 (end-to-end Xcode build warns). Verify the real supported iOS floor for the pinned
    Compose Multiplatform and align the target.
  - *[Med]* iOS is ARM-only with native tests disabled — add `iosX64` + KSP wiring so Room integration
    tests can run on CI simulators (ties into the Android-only / target-matrix work above).
  - *[Low]* `README.md` onboarding says wire new tabs into `RootNavHost` and manually enable host tests,
    but tabs live in `MainScreen` and the convention plugin enables host tests globally — fix the drift.
  - *[Low]* `shared/build.gradle.kts` uses `api(projects.feature.*)` (exports every feature to Android
    consumers) and public Koin entry points leak into the ObjC `Shared.framework` — prefer `implementation`
    and `internal`/`@HiddenFromObjC` unless the types are genuinely public API.
  - *[Low]* Root `staticAnalysis` iterates `subprojects {}`, so the included `build-logic` build is never
    linted despite the "all modules" claim — apply detekt/ktlint there too.
  - Reconfirms the "Template hardening from generated-app reviews" bullet above: observe/`Flow` read path
    still uncaught, one generic `<X>Error` conflates remote vs local failures, list `onItemClick` is a
    no-op affordance.
  - *Not a generator bug (by design):* the catalog→books rename stays cosmetic at the data seam (still
    loads DummyJSON `products`, persists `price`). The reference points at dummyjson intentionally;
    optionally have the tailoring agent reshape the DTO/domain fields to the domain noun, or state the
    placeholder plainly in the generated docs.
