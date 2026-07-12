import SwiftUI
import Shared
// kmp-scaffold:firebase:begin
import FirebaseCore
// kmp-scaffold:firebase:end

@main
struct iOSApp: App {
    init() {
        // kmp-scaffold:firebase:begin
        FirebaseApp.configure()
        // kmp-scaffold:firebase:end
        // Pass SwiftCrashReporter() instead of nil once Firebase is enabled.
        MainViewControllerKt.startKoinIos(crashReporter: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}
