package com.acmecorp.acmeapp.core.common.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

/** Verbose console logging in debug; in release, also forward to [crashReporter]. */
fun initLogging(
    crashReporter: CrashReporter,
    isDebug: Boolean,
) {
    Logger.setMinSeverity(if (isDebug) Severity.Verbose else Severity.Info)
    Logger.setLogWriters(
        buildList {
            add(platformLogWriter())
            if (!isDebug) add(CrashlyticsLogWriter(crashReporter))
        },
    )
}
