package com.acmecorp.acmeapp.feature.profile.ui.navigation

import com.acmecorp.acmeapp.core.navigation.NavRoute
import com.acmecorp.acmeapp.core.navigation.ProfileNav

class ProfileNavImpl : ProfileNav {
    override fun route(): NavRoute = ProfileRoute
}
