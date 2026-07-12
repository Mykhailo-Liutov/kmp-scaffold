package com.acmecorp.acmeapp.feature.profile.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.acmecorp.acmeapp.core.navigation.NavRoute
import com.acmecorp.acmeapp.feature.profile.ui.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
internal data object ProfileRoute : NavRoute

/** A bottom-nav tab (single destination), not a sub-flow. */
fun NavGraphBuilder.profileGraph() {
    composable<ProfileRoute> {
        ProfileScreen()
    }
}
