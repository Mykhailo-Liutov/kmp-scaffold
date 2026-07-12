package com.acmecorp.acmeapp.core.common.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/** Forwards Kermit logs (Info+) to a [CrashReporter]; records throwables at Error+. */
class CrashlyticsLogWriter(
    private val reporter: CrashReporter,
) : LogWriter() {
    override fun isLoggable(
        tag: String,
        severity: Severity,
    ): Boolean = severity >= Severity.Info

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        reporter.log("$severity $tag: $message")
        if (severity >= Severity.Error && throwable != null) {
            reporter.recordException(throwable)
        }
    }
}
