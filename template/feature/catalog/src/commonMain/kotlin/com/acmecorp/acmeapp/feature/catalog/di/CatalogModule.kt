package com.acmecorp.acmeapp.feature.catalog.di

import com.acmecorp.acmeapp.feature.catalog.data.local.CatalogDatabase
import com.acmecorp.acmeapp.feature.catalog.data.local.CatalogLocalDataSource
import com.acmecorp.acmeapp.feature.catalog.data.local.CatalogLocalDataSourceImpl
import com.acmecorp.acmeapp.feature.catalog.data.remote.CatalogRemoteDataSource
import com.acmecorp.acmeapp.feature.catalog.data.remote.CatalogRemoteDataSourceImpl
import com.acmecorp.acmeapp.feature.catalog.data.repository.CatalogRepositoryImpl
import com.acmecorp.acmeapp.feature.catalog.domain.repository.CatalogRepository
import com.acmecorp.acmeapp.feature.catalog.domain.usecase.GetProductsUseCase
import com.acmecorp.acmeapp.core.navigation.CatalogNav
import com.acmecorp.acmeapp.feature.catalog.domain.usecase.RefreshProductsUseCase
import com.acmecorp.acmeapp.feature.catalog.ui.list.CatalogListViewModel
import com.acmecorp.acmeapp.feature.catalog.ui.navigation.CatalogNavImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Platform module supplies the CatalogDatabase (built differently per platform).
expect val catalogPlatformModule: Module

val catalogModule = module {
    includes(catalogPlatformModule)

    single<CatalogRemoteDataSource> { CatalogRemoteDataSourceImpl(get()) }
    single<CatalogLocalDataSource> { CatalogLocalDataSourceImpl(get<CatalogDatabase>().productDao()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }

    factoryOf(::GetProductsUseCase)
    factoryOf(::RefreshProductsUseCase)

    viewModelOf(::CatalogListViewModel)

    factory<CatalogNav> { CatalogNavImpl() }
}
