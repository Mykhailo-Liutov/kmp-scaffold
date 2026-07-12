package com.acmecorp.acmeapp.core.navigation

interface CatalogNav {
    fun route(): NavRoute
    // Future cross-feature push, e.g. fun detail(id: Int): NavRoute
}
