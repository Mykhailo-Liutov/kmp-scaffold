package com.acmecorp.acmeapp.core.common.logging

/** Platform crash sink. The skeleton binds [NoOpCrashReporter]; swap in a real one (e.g. Firebase). */
interface CrashReporter {
    fun log(message: String)

    fun recordException(throwable: Throwable)
}
