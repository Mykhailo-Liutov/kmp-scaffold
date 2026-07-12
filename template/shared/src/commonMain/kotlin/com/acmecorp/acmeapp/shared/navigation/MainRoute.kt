package com.acmecorp.acmeapp.shared.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.acmecorp.acmeapp.core.navigation.LocalNavigationRoutes
import com.acmecorp.acmeapp.core.navigation.NavRoute
import com.acmecorp.acmeapp.feature.catalog.ui.navigation.catalogGraph
import com.acmecorp.acmeapp.feature.home.ui.navigation.homeGraph
import com.acmecorp.acmeapp.feature.profile.ui.navigation.profileGraph
import kotlinx.serialization.Serializable

@Serializable
internal data object MainRoute : NavRoute

private data class MainTab(val route: NavRoute, val label: String)

/** App shell: bottom-nav bar over the tab graphs, each keeping its own back stack. */
@Composable
fun MainScreen() {
    val routes = LocalNavigationRoutes.current
    val tabNav = rememberNavController()

    // Routes come from the navigation facade — the shell never sees a feature's route key.
    val tabs = listOf(
        MainTab(routes.homeNav.route(), "Home"),
        MainTab(routes.catalogNav.route(), "Catalog"),
        MainTab(routes.profileNav.route(), "Profile"),
    )

    Scaffold(
        bottomBar = {
            val backStackEntry by tabNav.currentBackStackEntryAsState()
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy
                        ?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tabNav.switchTab(tab.route) },
                        icon = { Text(tab.label.first().toString()) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNav,
            startDestination = routes.homeNav.route(),
            modifier = Modifier.padding(padding),
        ) {
            homeGraph(onOpenCatalog = { tabNav.switchTab(routes.catalogNav.route()) })
            catalogGraph(onItemClick = { /* detail screen not part of the skeleton */ })
            profileGraph()
        }
    }
}

/** Standard bottom-nav switch: single instance per tab, state saved/restored. */
private fun NavController.switchTab(route: NavRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
