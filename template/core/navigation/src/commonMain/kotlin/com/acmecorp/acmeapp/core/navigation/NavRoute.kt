package com.acmecorp.acmeapp.core.navigation

/**
 * Marker for a navigation destination key returned across features. Plain, not
 * sealed: route keys live in separate Gradle modules and sealed hierarchies can't
 * span modules. Returning [NavRoute] instead of `Any` stops callers passing
 * arbitrary objects into `navigate()`.
 */
interface NavRoute
