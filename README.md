# kmp-cmp-scaffold

A Claude Code plugin that scaffolds a new **Kotlin Multiplatform + Compose Multiplatform** app
(Android + iOS) from a production-grade golden template — tailored to your organization, app name,
and domain. The generated project **compiles and runs out of the box with zero external accounts**.

It reproduces the golden architecture: `build-logic` convention plugins, a Gradle version catalog,
a modular `:core:*` + `:feature:*` layout, Koin DI (modules in `di` packages), a cross-feature
navigation facade in `:core:navigation`, plain `ViewModel + StateFlow`, Arrow typed errors
(`Either<AppError, T>` via a `catching` boundary), Kermit logging with a pluggable `CrashReporter`,
Ktor + Room, detekt + ktlint (Gradle tasks) + gitleaks CI, and an XcodeGen-driven iOS app. The
generated app is a **generic, runnable shell** — no auth or session is baked in (those are
app-specific); the domain/data **capability split** is generated on demand.

## Install

From GitHub:

```
/plugin marketplace add Mykhailo-Liutov/kmp-cmp-scaffold
/plugin install kmp-cmp-scaffold
```

Or from a local clone:

```
/plugin marketplace add /path/to/kmp-cmp-scaffold
/plugin install kmp-cmp-scaffold
```

## Use

In an **empty folder**, run:

```
/kmp-cmp-scaffold:kmp-new
```

(Interactive sessions may also resolve the short form `/kmp-new`; the fully qualified name
always works, including in headless `claude -p` runs.)

Answer a short interview (app name, organization, what the app is about, Android-only vs Android+iOS,
Firebase on/off). The plugin generates a compiling project, adapts feature modules to your domain,
verifies the Android build, and prints next steps. You can later ask Claude to "add a feature
called X" at any time.

## How it works

Hybrid: a **deterministic generator** lays down the invariant skeleton; **Claude** tailors the
variable parts.

- `template/` — a sanitized *literal* copy of the golden project (the golden literals act as tokens).
- `scripts/generate.py` — copies `template/` into the target, then: strips Firebase regions (unless
  enabled), applies ordered identity replacements to file **contents**, relocates package
  **directories** to the chosen base package, optionally realizes a feature manifest, and writes
  `CLAUDE.md` / `docs/ARCHITECTURE.md` / seed memories. Produces an exact compiling replica for the
  default feature set.
- `scripts/feature_ops.py` — mechanical module ops: `clone` an archetype (`catalog` = full
  clean-arch, `home` = minimal) into a new feature; `remove` a feature; `capability` scaffolds a
  `domain:x` + `data:x` split (from `archetypes/capability/`); `remove-capability`. Identifiers are
  renamed and re-tokenized to the project's identity.
- `skills/kmp-scaffold` — orchestrates interview → generate → tailor → verify → document.
- `skills/kmp-blueprint` — the conventions reference (also copied into each generated repo as
  `docs/ARCHITECTURE.md`).
- `agents/kmp-feature-author` — clones an archetype and does the semantic wiring (DI + navigation
  facade + tab/flow) and domain modeling, following the blueprint.
- `scripts/verify.sh` — best-effort Android assemble + JVM-host tests.

Identity mapping (golden → yours), applied longest-first to avoid substring collisions:

| golden | becomes |
|---|---|
| `com.acmecorp.acmeapp` | `<org>.<slug>` |
| `AcmeApp` | PascalCase project name |
| `Acme` / `acme.` | class prefix / plugin-id prefix |
| `Acme App` / `Acme App Stage` | display names |

## Firebase

Off by default — the generator strips the Firebase plugins, deps, iOS SPM package, GoogleService
config, and distribute workflows (marked in `template/` with `# kmp-scaffold:firebase:begin/end`
sentinels). When enabled, those regions are kept and **placeholder** `google-services.json` /
`GoogleService-Info.plist` are emitted for you to replace with real Firebase config.

## Maintaining the template

`template/` is a **curated lean** copy of the golden source, not a verbatim mirror: it omits the
golden project's app-specific machinery (Firebase auth, session, social sign-in) and keeps only the
generic architecture. So `tools/extract-template.py` is a **reference aid, not a one-shot refresh** —
re-running it re-imports that machinery, which must then be re-stripped. To sync a single convention
from golden, port it by hand (drop the Firebase/auth specifics). Details + the Firebase sentinel
checklist live in `docs/DEVELOPMENT.md`.

## Status

**Beta.** The deterministic generator is gate-tested (generated projects build and pass JVM-host
tests for several identities, Firebase on/off, feature/capability ops), but the full interactive
interview → agent-wiring flow is young — expect rough edges, and treat generated CI/distribution
lanes as stubs to finish. Feature `remove` is not build-preserving (it prints a cleanup checklist).

## Limitations / roadmap

- **Android-only** mode drops the Xcode app + iOS CI but keeps the KMP iOS targets (the project stays
  iOS-ready). Full iOS-target removal is not yet automated.
- iOS build verification is macOS-only (needs Xcode + `xcodegen`).
- Play / TestFlight distribution lanes ship as stubs (as in the golden template).
- Trimming optional `:core` modules (network/database) for apps that don't need them is not
  yet automated.

## Requirements

JDK 17 (auto-provisioned by the Gradle foojay resolver), an Android SDK (`ANDROID_HOME` or
`local.properties`), Python 3, and — for iOS — macOS + Xcode + `xcodegen`.

## License

[Apache 2.0](LICENSE)
