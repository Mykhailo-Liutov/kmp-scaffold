import FirebaseCrashlytics
import Foundation
import Shared

/// Firebase-backed CrashReporter. Wire it in iOSApp.swift: `startKoinIos(crashReporter: SwiftCrashReporter())`.
final class SwiftCrashReporter: CrashReporter {
    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordException(throwable: KotlinThrowable) {
        let error = NSError(
            domain: String(describing: type(of: throwable)),
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: throwable.message ?? "Unknown Kotlin exception"]
        )
        Crashlytics.crashlytics().record(error: error)
    }
}
