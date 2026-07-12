package com.acmecorp.acmeapp.android

import android.app.Application
import android.content.pm.ApplicationInfo
import com.acmecorp.acmeapp.core.common.AppConfig
import com.acmecorp.acmeapp.core.common.logging.NoOpCrashReporter
import com.acmecorp.acmeapp.core.common.logging.initLogging
import com.acmecorp.acmeapp.shared.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module

class AcmeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // Swap NoOpCrashReporter() for FirebaseCrashReporter() once Firebase is enabled.
        initLogging(NoOpCrashReporter(), isDebug)
        initKoin {
            androidLogger()
            androidContext(this@AcmeApplication)
            val appConfigModule = module {
                single { AppConfig(FlavorConfig.BASE_URL, FlavorConfig.ENVIRONMENT) }
            }
            modules(appConfigModule)
        }
    }
}
