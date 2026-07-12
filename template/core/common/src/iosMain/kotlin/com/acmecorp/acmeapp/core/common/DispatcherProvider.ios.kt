package com.acmecorp.acmeapp.core.common

import kotlinx.coroutines.Dispatchers

actual fun defaultDispatcherProvider(): DispatcherProvider = DispatcherProvider(
    main = Dispatchers.Main,
    io = Dispatchers.Default,
    default = Dispatchers.Default,
)
