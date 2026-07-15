# Acme App — KMP / Compose Multiplatform skeleton

A minimal, multi-module Kotlin Multiplatform + Compose Multiplatform skeleton (Android + iOS).
Deliberately low-boilerplate: **no base classes, no custom `Result` wrapper**, plain
`ViewModel` + `StateFlow`, and a `UseCase → Repository → DataSource` domain/data split.

## Module graph

```
build-logic/            included build — convention plugins (acme.kmp.library / .compose / .feature / .android.application)
:shared                 KMP umbrella — App(), RootNavHost, Koin aggregation, iOS framework (baseName "Shared")
:androidApp             Android launcher — AcmeApplication (initKoin) + MainActivity
iosApp/                 Xcode app — consumes the Shared framework (see below)

:core:common            DispatcherProvider, AppConfig, commonModule
:core:designsystem      AcmeTheme
:core:ui                AppScaffold (compose helpers — not a base class)
:core:network           Ktor HttpClient (expect/actual engine), networkModule
:core:database          Room runtime + buildDefault() helper

:feature:home           placeholder feature
:feature:catalog        reference feature — remote list + Room cache, full vertical
```

Rules: features depend on `:core:*` only, never on each other; cross-feature navigation is owned
by `:shared` (features expose `NavGraphBuilder.xGraph(onNavigate…)` and emit callbacks).

## Reference feature layout (`:feature:catalog`)

```
domain/{model,repository,usecase}
data/{remote/dto, local/{entity,dao}, mapper, repository}
ui/{list, navigation}
di
```

- ViewModel: plain `ViewModel`, single `StateFlow<CatalogUiState>`; errors are a `String?` field.
- UseCase: one per operation (`operator fun invoke`), depends on the repository interface.
- Repository + DataSources: interfaces + impls; remote = Ktor, local = Room.
- Mappers: extension functions in `data/mapper`.

## Adding a feature

1. `include(":feature:x")` in `settings.gradle.kts`.
2. `feature/x/build.gradle.kts`:
   ```kotlin
   plugins { id("acme.feature") }
   kotlin { androidLibrary { namespace = "com.acmecorp.acmeapp.feature.x" } }
   ```
3. Add `xRoute` + `NavGraphBuilder.xGraph(...)`, screen, VM, `xModule` (Koin).
4. Register `xModule` in `shared/.../Koin.kt#appModules()`, then wire the destination: a **tab** goes
   in `MainScreen` (add a `MainTab` + call `xGraph(...)`), a **top-level flow** goes in `RootNavHost`.

## Build & run

```bash
./gradlew :androidApp:assembleProdDebug      # Android (flavors: prod / stage)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # iOS shared framework
./gradlew check                              # unit tests (JVM host)
```

Tests run on the JVM (android host) via `testAndroidHostTest`; iOS test execution is disabled
project-wide in the `acme.kmp.library` convention, which already enables host tests for every module
(`withHostTest {}`), so a new module with `commonTest` needs no extra opt-in.

> **⚠️ Demo endpoint:** ALL flavors — **including prod** — ship pointing at the placeholder API
> `https://dummyjson.com/`. Replace `BASE_URL` in `androidApp/src/{prod,stage}/…/FlavorConfig.kt`
> and `APP_BASE_URL` in `iosApp/project.yml` with your real endpoints **before any release build**.

## iOS

The Xcode project is generated from `iosApp/project.yml` via [xcodegen](https://github.com/yonsm/XcodeGen)
(the `.xcodeproj` is gitignored). Swift glue lives in `iosApp/iosApp/`.

```bash
brew install xcodegen          # once
cd iosApp && xcodegen generate # creates AcmeApp.xcodeproj
open AcmeApp.xcodeproj
```

`project.yml` already wires a pre-build script (`:shared:embedAndSignAppleFrameworkForXcode`)
and the framework search paths, so the `Shared` framework builds and links automatically.
Headless build check:

```bash
xcodebuild -project iosApp/AcmeApp.xcodeproj -scheme AcmeApp-prod \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO build
```
