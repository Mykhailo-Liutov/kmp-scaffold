package com.acmecorp.acmeapp.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.acmecorp.acmeapp.shared.navigation.MainRoute
import com.acmecorp.acmeapp.shared.navigation.MainScreen

/**
 * The umbrella owns cross-feature navigation. The skeleton ships a single top-level
 * destination (the tabbed shell); add further top-level flows as sibling graphs here.
 */
@Composable
fun RootNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MainRoute) {
        composable<MainRoute> { MainScreen() }
    }
}
