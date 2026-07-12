package com.acmecorp.acmeapp.core.navigation

/**
 * Cross-feature navigation contract for the Home feature. Returns the destination
 * as an opaque [NavRoute] so callers never see the feature's internal route key.
 */
interface HomeNav {
    fun route(): NavRoute
}
