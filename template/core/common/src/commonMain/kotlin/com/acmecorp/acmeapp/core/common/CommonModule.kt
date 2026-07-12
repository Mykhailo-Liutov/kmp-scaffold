package com.acmecorp.acmeapp.core.common

import co.touchlab.kermit.Logger
import org.koin.dsl.module

val commonModule = module {
    single { defaultDispatcherProvider() }
    single { AppScope(get()) }
    single { Logger.withTag("Acme") }
}
