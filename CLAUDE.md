# CLAUDE.md — working on the `kmp-cmp-scaffold` plugin

This repo **is a Claude Code plugin** that scaffolds new Kotlin Multiplatform + Compose
Multiplatform apps (Android + iOS) from a golden template, tailored to a user's org / name / domain.
You are editing the *generator*, not a generated app. End-user docs live in `README.md`; deep
internals + roadmap live in `docs/DEVELOPMENT.md` — **read that file before changing the generator.**

## What it does (one paragraph)

Hybrid generator: a sanitized **literal copy of a golden KMP/CMP repo** lives in `template/`; the
golden literals (`com.acmecorp.acmeapp`, `AcmeApp`, `Acme`/`acme.`, the display names,
`dummyjson.com`) act as tokens. `scripts/generate.py` copies `template/` into an empty folder and
replaces those literals with the user's identity (ordered longest-first to avoid substring
collisions), relocates package directories, strips Firebase regions unless enabled, and writes
`CLAUDE.md` / `docs/ARCHITECTURE.md` / `.claude/rules/`. Feature modules are tailored to the user's
domain by cloning an archetype (`scripts/feature_ops.py`, mechanical identifier rename) and then
wiring DI + navigation semantically (the `kmp-feature-author` agent, guided by the `kmp-blueprint`
skill).

## Layout

```
.claude-plugin/plugin.json + marketplace.json   manifest + single-plugin marketplace
commands/kmp-new.md                              entry command → kmp-scaffold skill
skills/kmp-scaffold/SKILL.md                     orchestration: interview→generate→tailor→verify→document
skills/kmp-blueprint/SKILL.md                    the conventions contract (copied into generated repos)
agents/kmp-feature-author.md                     clones an archetype + does semantic wiring + domain modeling
scripts/lib_tokens.py                            Identity model + ordered (golden→user) replacements
scripts/generate.py                              copy + replace + relocate + firebase-strip + finalize
scripts/feature_ops.py                           clone/remove a feature; add/remove a domain:x+data:x capability
scripts/verify.sh                                android assemble + JVM-host tests (best-effort)
tools/extract-template.py                        AUTHORING: rebuild template/ from the golden source
archetypes/capability/                           plugin-side domain:x + data:x archetype (NOT shipped in template/)
template/                                        curated lean copy of the golden project (no auth/session — see below)
```

The generated skeleton is deliberately **generic**: `:core:*` + `:feature:{home,catalog,profile}`,
a tabbed shell (no auth/session), Arrow typed errors (`AppError` + `catching`), Kermit logging with
a no-op `CrashReporter` default, and ktlint wired as a Gradle task. The domain/data **capability
split** isn't baked in — it's generated on demand via `feature_ops.py capability`.

## The golden source

`template/` is modeled on **a private production KMP/CMP app** (the author's golden source, not
part of this repo), but it is
**not a verbatim mirror** — it is a *curated lean* copy. The golden project carries app-specific
machinery the scaffold deliberately omits: Firebase-backed auth (`domain:auth` / `data:auth`,
GitLive, social sign-in), session state, and the session-gated nav shell. The scaffold keeps only
the generic architecture (modules, convention plugins, Arrow errors, logging interface, ktlint) and
ships a Firebase-free, runnable shell.

Consequently `tools/extract-template.py` is now a **reference aid, not a one-shot refresh** —
re-running it re-imports the golden project's auth/session/Firebase code that must then be stripped
again. To sync a specific convention from golden, port it by hand (read the golden file, drop the
Firebase/auth specifics) rather than wholesale re-extracting. Always read the golden repo when a
convention isn't fully covered by the blueprint.

When you do re-extract, you must afterwards: (1) strip `domain:auth`/`data:auth`/session + the
session nav shell; (2) re-add the **Firebase sentinels** (below); (3) keep the Firebase reporter
files (`FirebaseCrashReporter.kt`, `SwiftCrashReporter.swift`) in `FIREBASE_ONLY_FILES`.

## Invariants & gotchas (do not break these)

1. **Replacement is single-pass and order is load-bearing.** `apply_replacements` matches all
   tokens in ONE regex pass over the original text — replacement output is never re-tokenized
   (a user group like `com.acme` must not collide with the golden `acme.` prefix). Alternation is
   first-match-wins, so within `lib_tokens.content_replacements()` order still decides ties
   between tokens sharing a prefix (`AcmeApp` before `Acme`). Don't reorder casually, and never
   revert to sequential `str.replace`.
2. **Firebase regions are stripped by sentinel comments**, not pattern matching. Five template files
   wrap their Firebase-only lines in `kmp-scaffold:firebase:begin` … `:end`
   (`#` for toml/yaml, `//` for kt/gradle/swift): root `build.gradle.kts`,
   `androidApp/build.gradle.kts`, `gradle/libs.versions.toml`, `iosApp/project.yml`,
   `iosApp/iosApp/iOSApp.swift` (the manifest lives in `FIREBASE_SENTINEL_FILES` in `generate.py`;
   generation FAILS if the set drifts or a region is unmatched/nested — after re-extraction,
   re-add the sentinels AND keep that manifest in sync).
   `generate.py` always deletes the marker lines; deletes the wrapped body only when Firebase is off.
   Whole Firebase-only **files** (dropped when Firebase is off) are listed in `FIREBASE_ONLY_FILES`
   in `generate.py` — incl. `androidApp/.../FirebaseCrashReporter.kt` and
   `iosApp/iosApp/SwiftCrashReporter.swift` (the entry points use `NoOpCrashReporter` by default).
3. **`generate.py` is deterministic and must keep producing a compiling replica** for the default
   feature set. The acceptance test is the gate (below).
4. **Feature wiring is split**: `feature_ops.py clone` does the mechanical 20-file rename;
   the *semantic* wiring (which is a tab vs a top-level flow, cross-feature callbacks) is the agent's
   job because it needs judgment. Don't try to make `generate.py` regenerate `RootNavHost`/`MainRoute`.
5. **Scripts reference paths via `__file__` / `${CLAUDE_PLUGIN_ROOT}`** — keep them relocatable.

## Adding a feature to a *generated* project (the 6 wiring touch-points)

`settings.gradle.kts` (include) · `shared/build.gradle.kts` (api projects.feature.x) ·
`shared/.../Koin.kt` (import + `appModules()`) · `core/navigation/<Class>Nav.kt` (facade) +
`NavigationRoutes.kt` (field) · `shared/.../navigation/MainRoute.kt` (tab) **or**
`shared/.../RootNavHost.kt` (top-level flow). Full checklist: `skills/kmp-blueprint/SKILL.md`.

A **capability** (`domain:x` + `data:x`) is scaffolded by `feature_ops.py capability --capability x
--noun N` (clones `archetypes/capability/`), then wired: `settings.gradle.kts` (both includes) ·
`shared/build.gradle.kts` (`implementation(projects.data.x)`) · `shared/.../Koin.kt`
(`xDomainModule` + `xDataModule`).

## How to test (acceptance gate)

Generate into a temp dir and build with real Gradle. Needs JDK 17 (foojay auto-provisions),
an Android SDK, and network.

```bash
TMP=$(mktemp -d)/SampleApp
python3 scripts/generate.py --target "$TMP" --group com.example --name SampleApp --no-git
echo "sdk.dir=$HOME/Library/Android/sdk" > "$TMP/local.properties"
( cd "$TMP" && ./gradlew :androidApp:assembleProdDebug && ./gradlew testAndroidHostTest testProdDebugUnitTest )

# sanity: no stray golden literals should remain in a generated project (dummyjson is expected)
grep -rIl -e acmecorp -e acmeapp -e AcmeApp -e Acme "$TMP" | grep -v gradle-wrapper.jar   # expect empty

# REGRESSION (identity collides with golden tokens): `com.acme` must survive the
# replacement pass un-cascaded — packages stay com.acme.fittrack, dirs match.
TMP2=$(mktemp -d)/FitTrack
python3 scripts/generate.py --target "$TMP2" --group com.acme --name FitTrack --no-git
grep -rn "com.ft.fittrack" "$TMP2" && echo "CASCADE BUG" || echo "ok: no cascade"
test -d "$TMP2/shared/src/commonMain/kotlin/com/acme/fittrack" && echo "ok: dirs match"
# the app class must become <Prefix>Application, never "<Name>lication"
test -f "$TMP2/androidApp/src/main/kotlin/com/acme/fittrack/android/FtApplication.kt" && echo "ok: FtApplication"
grep -rEn "class \w*lication" "$TMP2" --include='*.kt' | grep -v "Application" && echo "LICATION BUG" || echo "ok: no lication"

# feature clone + (manual/agent) wire, then rebuild
python3 scripts/feature_ops.py clone --target "$TMP" --archetype catalog --feature events --noun Event
```

## Status

Validated on Gradle 9.4.1 + Android SDK: generated replica (Firebase off) builds
(`:androidApp:assembleProdDebug`, APK) and JVM-host tests pass; `./gradlew staticAnalysis` (detekt +
ktlint) is green; a `capability billing --noun Invoice` split, wired in, compiles into the build;
Firebase on/off file + sentinel handling verified. See `docs/DEVELOPMENT.md` for the roadmap
(Android-only iOS-target stripping, optional `:core` trimming, live interview run, Play/TestFlight).
