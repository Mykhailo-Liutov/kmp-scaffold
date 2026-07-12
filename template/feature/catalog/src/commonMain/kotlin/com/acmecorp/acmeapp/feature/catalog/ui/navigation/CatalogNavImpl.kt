package com.acmecorp.acmeapp.feature.catalog.ui.navigation

import com.acmecorp.acmeapp.core.navigation.CatalogNav
import com.acmecorp.acmeapp.core.navigation.NavRoute

class CatalogNavImpl : CatalogNav {
    override fun route(): NavRoute = CatalogRoute
}
