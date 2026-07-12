package com.acmecorp.acmeapp.feature.catalog.di

import com.acmecorp.acmeapp.feature.catalog.data.local.catalogDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val catalogPlatformModule: Module = module {
    single { catalogDatabase(androidContext(), get()) }
}
