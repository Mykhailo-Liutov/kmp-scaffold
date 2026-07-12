package com.acmecorp.acmeapp.core.network

import com.acmecorp.acmeapp.core.common.AppConfig
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient(httpClientEngine(), get<AppConfig>().baseUrl) }
}
