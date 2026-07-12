package com.acmecorp.acmeapp.feature.home.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acmecorp.acmeapp.core.navigation.NavRoute
import com.acmecorp.acmeapp.feature.home.ui.main.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
internal data object HomeRoute : NavRoute

/** The feature owns its destination; cross-feature navigation is delegated to the caller. */
fun NavGraphBuilder.homeGraph(onOpenCatalog: () -> Unit) {
    composable<HomeRoute> {
        HomeScreen(onOpenCatalog = onOpenCatalog)
    }
}
