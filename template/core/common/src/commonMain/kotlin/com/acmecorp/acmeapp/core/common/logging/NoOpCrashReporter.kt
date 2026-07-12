package com.acmecorp.acmeapp.core.common.logging

/** Default reporter — discards everything. Replace with a Firebase-backed one when enabled. */
class NoOpCrashReporter : CrashReporter {
    override fun log(message: String) = Unit

    override fun recordException(throwable: Throwable) = Unit
}
