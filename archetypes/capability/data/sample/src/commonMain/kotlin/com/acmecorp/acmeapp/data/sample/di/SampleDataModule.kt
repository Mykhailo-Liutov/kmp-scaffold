package com.acmecorp.acmeapp.data.sample.di

import com.acmecorp.acmeapp.data.sample.InMemorySampleRepository
import com.acmecorp.acmeapp.domain.sample.SampleRepository
import org.koin.dsl.module

val sampleDataModule = module {
    single<SampleRepository> { InMemorySampleRepository() }
}
