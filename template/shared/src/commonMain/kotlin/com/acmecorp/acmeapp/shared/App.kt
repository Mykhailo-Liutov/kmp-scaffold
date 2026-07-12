package com.acmecorp.acmeapp.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.acmecorp.acmeapp.core.designsystem.AcmeTheme
import com.acmecorp.acmeapp.core.navigation.LocalNavigationRoutes
import com.acmecorp.acmeapp.core.navigation.NavigationRoutes
import org.koin.compose.koinInject

@Composable
fun App() {
    AcmeTheme {
        CompositionLocalProvider(
            LocalNavigationRoutes provides koinInject<NavigationRoutes>(),
        ) {
            RootNavHost()
        }
    }
}
