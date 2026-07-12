package com.acmecorp.acmeapp.domain.sample.di

import com.acmecorp.acmeapp.domain.sample.usecase.ObserveItemsUseCase
import com.acmecorp.acmeapp.domain.sample.usecase.RefreshItemsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val sampleDomainModule = module {
    factoryOf(::ObserveItemsUseCase)
    factoryOf(::RefreshItemsUseCase)
}
