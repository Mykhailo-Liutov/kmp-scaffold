package com.acmecorp.acmeapp.shared

import com.acmecorp.acmeapp.core.common.commonModule
import com.acmecorp.acmeapp.core.navigation.di.coreNavigationModule
import com.acmecorp.acmeapp.core.network.networkModule
import com.acmecorp.acmeapp.feature.catalog.di.catalogModule
import com.acmecorp.acmeapp.feature.home.di.homeModule
import com.acmecorp.acmeapp.feature.profile.di.profileModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun appModules(): List<Module> = listOf(
    commonModule,
    networkModule,
    coreNavigationModule,
    homeModule,
    catalogModule,
    profileModule,
)

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(appModules())
}
