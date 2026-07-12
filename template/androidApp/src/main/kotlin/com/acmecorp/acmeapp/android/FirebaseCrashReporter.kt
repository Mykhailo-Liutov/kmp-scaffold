package com.acmecorp.acmeapp.android

import com.acmecorp.acmeapp.core.common.logging.CrashReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Firebase-backed [CrashReporter]. Wire it in [AcmeApplication]: `initLogging(FirebaseCrashReporter(), …)`. */
class FirebaseCrashReporter : CrashReporter {
    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    override fun log(message: String) = crashlytics.log(message)

    override fun recordException(throwable: Throwable) = crashlytics.recordException(throwable)
}
