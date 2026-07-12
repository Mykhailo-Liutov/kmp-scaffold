package com.acmecorp.acmeapp.feature.home.ui.navigation

import com.acmecorp.acmeapp.core.navigation.HomeNav
import com.acmecorp.acmeapp.core.navigation.NavRoute

class HomeNavImpl : HomeNav {
    override fun route(): NavRoute = HomeRoute
}
