package com.acmecorp.acmeapp.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Aggregated cross-feature navigation contracts. Built by Koin from each feature's
 * binding (internal constructor — only `coreNavigationModule` may create it) and
 * exposed to the Compose tree via [LocalNavigationRoutes].
 */
class NavigationRoutes internal constructor(
    val homeNav: HomeNav,
    val catalogNav: CatalogNav,
    val profileNav: ProfileNav,
)

val LocalNavigationRoutes = staticCompositionLocalOf<NavigationRoutes> {
    error("LocalNavigationRoutes not provided")
}
