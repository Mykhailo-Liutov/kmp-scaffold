package com.acmecorp.acmeapp.core.navigation.di

import com.acmecorp.acmeapp.core.navigation.NavigationRoutes
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val coreNavigationModule = module {
    factoryOf(::NavigationRoutes)
}
