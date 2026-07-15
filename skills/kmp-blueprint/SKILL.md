---
name: kmp-blueprint
description: Architecture conventions for the KMP/CMP golden template — module graph, convention plugins, Koin DI layout, the cross-feature navigation facade, ViewModel/Screen pattern, Arrow typed errors, logging/crash reporting, the domain/data capability split, Room KMP setup, and the step-by-step "add a feature" / "add a capability" checklists. Use when generating, extending, or reviewing a project scaffolded by kmp-scaffold, or when adding/modifying a feature in such a project.
---

# KMP / CMP architecture blueprint

This is the contract every module follows. A generated project ships a copy of this file
as `docs/ARCHITECTURE.md` (with the project's real package/prefix substituted). Identifiers
below use the template's golden names (`com.acmecorp.acmeapp`, `acme.*`, `Acme*`); in a
generated project read the equivalent real names.

## Module graph

```
build-logic/        included build — convention plugins (acme.kmp.library / .kmp.compose / .feature / .android.application / .room)
:shared             KMP umbrella — App(), RootNavHost, Koin aggregation, iOS framework (baseName "Shared")
:androidApp         Android launcher — AcmeApplication (initLogging + initKoin) + MainActivity
iosApp/             Xcode app (generated from project.yml via xcodegen) — consumes the Shared framework

:core:common        DispatcherProvider, AppConfig, AppScope, error (AppError + catching), logging (CrashReporter + initLogging), commonModule
:core:designsystem  AcmeTheme (Material3)
:core:ui            AppScaffold + shared composables (NOT base classes)
:core:network       Ktor HttpClient (expect/actual engine), networkModule
:core:database      Room runtime + buildDefault() helpers
:core:navigation    NavRoute marker, per-feature XNav facades, NavigationRoutes, coreNavigationModule

:feature:home       minimal feature (archetype for simple screens)
:feature:catalog    full feature — remote list + Room cache + Arrow-typed errors (archetype for clean-arch features)
:feature:profile    profile tab

:domain:<x> / :data:<x>   a capability split — added on demand (see "Add a capability"); none ship by default
```

**Dependency rule:** features depend on `:core:*` (and, optionally, a capability's `:domain:<x>`) —
**never on each other**. Cross-feature navigation is owned by `:shared`; features expose
`NavGraphBuilder.xGraph(onNavigate…)` and emit callbacks. The shell decides where callbacks go.
The skeleton ships **no** auth/session — it's a generic, runnable shell.

## Convention plugins (build-logic)

Apply one id, get a fully configured module. Never re-declare targets/SDKs in a module.

- `acme.kmp.library` — KMP base: Android (AGP KMP library) + iosArm64 + iosSimulatorArm64, JVM
  toolchain 17, commonTest deps (kotlin-test, coroutines-test, turbine), JVM-host tests, and
  `configureDetekt()` + `configureKtlint()`.
- `acme.kmp.compose` — adds Compose runtime/foundation/ui/material3 + lifecycle to commonMain.
- `acme.feature` — `acme.kmp.library` + `acme.kmp.compose` + serialization + auto-depends on
  `:core:common`, `:core:designsystem`, `:core:ui`, plus compose-navigation + koin.
- `acme.android.application` — Android app (compileSdk/minSdk/targetSdk, version code/name, compose,
  detekt + ktlint).
- `acme.room` — KSP + Room, schema dir `$projectDir/schemas`, per-target room-compiler. Apply on a
  module that already declares targets (i.e. after `acme.feature`).

A feature `build.gradle.kts` is therefore tiny:
```kotlin
plugins { id("acme.feature") }       // + id("acme.room") if it has a Room database
kotlin { androidLibrary { namespace = "com.acmecorp.acmeapp.feature.x" } }
```

## Koin DI

- Every `module { }` lives in **its own file inside a `di` package** — never beside a ViewModel
  or other class.
- Providers: `single`/`singleOf` for stateful collaborators, `factoryOf(::UseCase)` for use cases,
  `viewModelOf(::XViewModel)` for ViewModels, `factory<XNav> { XNavImpl() }` for the nav facade.
- A module needing a platform-built dependency (e.g. a Room DB) declares
  `expect val xPlatformModule: Module` and `includes(xPlatformModule)`; androidMain/iosMain supply
  the `actual`.
- Aggregate in `shared/.../Koin.kt`:
  ```kotlin
  fun appModules(): List<Module> = listOf(commonModule, networkModule, coreNavigationModule,
      homeModule, catalogModule, profileModule, /* + new feature & capability modules */)
  ```
  `initKoin {}` starts Koin; the Android `AcmeApplication` and iOS `startKoinIos()` add only the
  `AppConfig` module (base URL + environment) on top.

## Navigation (the facade pattern)

Route keys stay **`internal` to each feature**; other features/shell only see an interface.

1. `:core:navigation` defines the marker and one interface per feature:
   ```kotlin
   interface NavRoute                       // opaque destination key
   interface CatalogNav { fun route(): NavRoute }   // facade — add fun detail(id): NavRoute later
   ```
2. The feature owns its route key + graph (route key is `internal`, `@Serializable`):
   ```kotlin
   @Serializable internal data object CatalogRoute : NavRoute
   fun NavGraphBuilder.catalogGraph(onItemClick: (Int) -> Unit) {
       composable<CatalogRoute> { CatalogListScreen(onItemClick = onItemClick) }
   }
   ```
3. The feature implements the facade, returning its internal key:
   ```kotlin
   class CatalogNavImpl : CatalogNav { override fun route(): NavRoute = CatalogRoute }
   ```
   bound in the feature's Koin module: `factory<CatalogNav> { CatalogNavImpl() }`.
4. `NavigationRoutes` aggregates all facades (Koin builds it; `internal` constructor) and is exposed
   via `LocalNavigationRoutes`. The shell reads `routes.catalogNav.route()` — never a route key.
5. `:shared` assembles the host: `RootNavHost` starts directly at `MainRoute` → `MainScreen`, the
   tabbed shell (home/catalog/profile), each tab a feature `xGraph`. The skeleton ships no top-level
   flows; **add a top-level flow** (e.g. onboarding, a detail stack) as a sibling graph in
   `RootNavHost` and navigate to it from the shell.

Tab (home, catalog, profile) → a `MainTab` entry + `xGraph` in `MainScreen`. Top-level flow → a
graph in `RootNavHost`.

## ViewModel + Screen

- Plain `androidx.lifecycle.ViewModel`. **No base class, no Result/UiState wrapper.** Loading and
  error are ordinary fields on a single state data class.
  ```kotlin
  data class CatalogUiState(val products: List<Product> = emptyList(),
                            val isLoading: Boolean = false, val errorMessage: String? = null)
  class CatalogListViewModel(getProducts: GetProductsUseCase, private val refresh: RefreshProductsUseCase) : ViewModel() {
      private val _state = MutableStateFlow(CatalogUiState(isLoading = true))
      val state: StateFlow<CatalogUiState> = _state.asStateFlow()
      // collect use-case flows in init via launchIn(viewModelScope); mutate with _state.update {}
  }
  ```
- Screen: an entry composable with the VM as a defaulted `koinViewModel()` param, state via
  `collectAsStateWithLifecycle()`, navigation surfaced as callbacks, wrapped in `AppScaffold`.

## Errors — Arrow typed results

- Fallible operations return `Either<AppError, T>` (Arrow), not exceptions. The **only** place a
  throwing SDK call (Ktor, Room, …) is caught is the data boundary, via the `catching(mapError) { … }`
  primitive in `:core:common`. Above that boundary, nothing throws.
  ```kotlin
  override suspend fun refresh(): Either<AppError, Unit> = catching({ CatalogError(it) }) {
      val products = remote.fetchProducts().map { it.toDomain() }
      local.replaceAll(products)
  }
  ```
- Each capability/feature subclasses `AppError` in its own domain layer (e.g. `CatalogError`).
- ViewModels `.fold(ifLeft = { error -> … }, ifRight = { … })` the result into their UiState.
  Observing a cache stays a plain `Flow<…>` (not fallible — no `Either`).

## Logging & crash reporting

- `initLogging(crashReporter, isDebug)` (in `:core:common`) configures Kermit: verbose in debug; in
  release it also forwards logs to a `CrashReporter`. Android `AcmeApplication` and iOS
  `startKoinIos()` call it before `initKoin`.
- The default is `NoOpCrashReporter` (no external accounts). `FirebaseCrashReporter` (Android) and
  `SwiftCrashReporter` (iOS) ship as **Firebase-only** files — drop them in place of the no-op once
  Firebase is enabled.

## Domain / data split — two forms

1. **In-feature (single module).** A feature with a data layer keeps `domain/` + `data/` *packages*
   inside its one feature module (see `catalog`). Use for feature-local data:
   ```
   domain/{model, repository (interface), usecase (operator fun invoke), <X>Error}
   data/{remote/dto (Ktor + @Serializable), local/{entity, dao} (Room), mapper (extension fns), repository (impl)}
   ui/{list, navigation}
   di
   ```
   Repository impl observes the local cache and refreshes from remote (behind `catching`); mappers are
   extension functions (`Dto.toDomain()`, `Entity.toDomain()`, `Domain.toEntity()`). Tests use fakes +
   Turbine on the JVM host.
2. **Capability split (separate modules `:domain:<x>` + `:data:<x>`).** A cross-cutting business
   capability used by more than one feature. `:domain:<x>` holds the model, repository interface, use
   cases, and error type; `:data:<x>` holds the implementation + its Koin module. Features (and the
   shell) depend on `:domain:<x>` only. See "Add a capability".

## Room (KMP)

```kotlin
@Database(entities = [ProductEntity::class], version = 1)
@ConstructedBy(CatalogDatabaseConstructor::class)
abstract class CatalogDatabase : RoomDatabase() { abstract fun productDao(): ProductDao }

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object CatalogDatabaseConstructor : RoomDatabaseConstructor<CatalogDatabase> { override fun initialize(): CatalogDatabase }
```
The module applies `acme.room`; the platform Koin module builds the DB (Android needs a Context;
iOS uses a documents path). Schemas are written under `feature/x/schemas/`.

Two builders in `:core:database`: `buildDefault` (safe — a schema change without a migration fails
at open) and `buildDestructiveCache` (drops ALL tables on schema change — disposable caches only,
like the catalog's remote-list cache). Any database holding user data must use `buildDefault` and
ship explicit `Migration`s when its `version` bumps.

## Adding a feature (checklist)

1. **Scaffold the module** — ask Claude to "add a feature called events" (it clones the `catalog`
   full-data or `home` screen-only archetype and renames every identifier), or copy an existing
   `feature/<x>/**` and rename its identifiers by hand. Either way you get `feature/events/**`.
2. **settings.gradle.kts** — add `include(":feature:events")`.
3. **shared/build.gradle.kts** — add `api(projects.feature.events)`.
4. **core/navigation** — the cloned module references `EventsNav`; create the facade
   `EventsNav.kt` (`interface EventsNav { fun route(): NavRoute }`) and add `val eventsNav: EventsNav`
   to `NavigationRoutes` (`coreNavigationModule` builds it automatically via `factoryOf`).
5. **shared/.../Koin.kt** — import `eventsModule` and add it to `appModules()`.
6. **Wire the destination** — for a tab: add a `MainTab(routes.eventsNav.route(), "Events")` + call
   `eventsGraph(...)` in `MainScreen`. For a top-level flow: add `eventsGraph(...)` in `RootNavHost`.
7. **Adapt the domain** — rename/extend the model fields, DTO, entity, DAO query, mappers, use cases,
   and screen to the real data. Return `Either<AppError, …>` from fallible repository ops via
   `catching`. Point `AppConfig.baseUrl` / the DTO at the real API.
8. `./gradlew :androidApp:assembleProdDebug` and `./gradlew check` to verify.

## Adding a capability (domain/data split)

Ask Claude to "add a capability called billing" — it clones the domain/data archetype, creating
`domain/billing/**` (model, `BillingRepository`, use cases, `BillingError`, `billingDomainModule`) +
`data/billing/**` (`InMemoryBillingRepository` stub, `billingDataModule`), tokenized to the project's
identity. Then wire:

1. **settings.gradle.kts** — `include(":domain:billing")` + `include(":data:billing")`.
2. **shared/build.gradle.kts** — `implementation(projects.data.billing)` (binds the impl).
3. **shared/.../Koin.kt** — import + add `billingDomainModule` and `billingDataModule` to `appModules()`.
4. **Consume** — a feature that uses the capability adds `implementation(projects.domain.billing)` and
   injects the use cases.
5. **Back the stub** — replace `InMemoryBillingRepository` with a real `:data:billing` implementation
   (Ktor/Room/SDK) behind `catching`.

## Build & run

```bash
./gradlew :androidApp:assembleProdDebug                      # Android APK (prod flavor, debug)
./gradlew check                                              # unit tests (JVM host)
./gradlew staticAnalysis                                     # detekt + ktlint across all modules
./gradlew ktlintFormat                                       # auto-format
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64        # iOS shared framework
cd iosApp && xcodegen generate && open AcmeApp.xcodeproj  # iOS app (needs macOS + xcodegen)
```
Tests run on the JVM (android host) via `testAndroidHostTest`; iOS test execution is disabled in
the `acme.kmp.library` convention. A module with `commonTest` gets host tests via the convention's
`withHostTest {}` (already on).
