package com.acmecorp.acmeapp.core.common

import kotlinx.coroutines.CoroutineDispatcher

class DispatcherProvider(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)

expect fun defaultDispatcherProvider(): DispatcherProvider
