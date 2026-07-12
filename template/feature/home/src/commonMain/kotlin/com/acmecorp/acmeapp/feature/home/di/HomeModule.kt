package com.acmecorp.acmeapp.feature.home.di

import com.acmecorp.acmeapp.core.navigation.HomeNav
import com.acmecorp.acmeapp.feature.home.ui.main.HomeViewModel
import com.acmecorp.acmeapp.feature.home.ui.navigation.HomeNavImpl
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
    factory<HomeNav> { HomeNavImpl() }
}
