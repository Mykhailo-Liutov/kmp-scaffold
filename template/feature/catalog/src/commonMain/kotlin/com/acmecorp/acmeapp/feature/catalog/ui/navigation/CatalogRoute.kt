package com.acmecorp.acmeapp.feature.catalog.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acmecorp.acmeapp.core.navigation.NavRoute
import com.acmecorp.acmeapp.feature.catalog.ui.list.CatalogListScreen
import kotlinx.serialization.Serializable

@Serializable
internal data object CatalogRoute : NavRoute

fun NavGraphBuilder.catalogGraph(onItemClick: (Int) -> Unit) {
    composable<CatalogRoute> {
        CatalogListScreen(onItemClick = onItemClick)
    }
}
