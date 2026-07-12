package com.acmecorp.acmeapp.feature.profile.di

import com.acmecorp.acmeapp.core.navigation.ProfileNav
import com.acmecorp.acmeapp.feature.profile.ui.ProfileViewModel
import com.acmecorp.acmeapp.feature.profile.ui.navigation.ProfileNavImpl
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
    factory<ProfileNav> { ProfileNavImpl() }
}
