package com.acmecorp.acmeapp.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.acmecorp.acmeapp.core.common.AppConfig
import com.acmecorp.acmeapp.core.common.logging.CrashReporter
import com.acmecorp.acmeapp.core.common.logging.NoOpCrashReporter
import com.acmecorp.acmeapp.core.common.logging.initLogging
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

fun mainViewController(): UIViewController = ComposeUIViewController {
    App()
}

/** Pass a [CrashReporter] (e.g. SwiftCrashReporter) to forward release logs; null uses a no-op. */
@OptIn(ExperimentalNativeApi::class)
fun startKoinIos(crashReporter: CrashReporter? = null) {
    initLogging(crashReporter ?: NoOpCrashReporter(), Platform.isDebugBinary)
    val appConfigModule = module {
        single {
            AppConfig(
                baseUrl = infoPlistString("APP_BASE_URL"),
                environment = infoPlistString("APP_ENVIRONMENT"),
            )
        }
    }
    initKoin {
        modules(appConfigModule)
    }
}

private fun infoPlistString(key: String): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String
        ?: error("Missing Info.plist key: $key")
