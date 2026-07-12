import dev.detekt.gradle.Detekt
import dev.detekt.gradle.report.ReportMergeTask

plugins {
    listOf(
        libs.plugins.compose,
        libs.plugins.composeCompiler,
        libs.plugins.ksp,
        libs.plugins.room,
        libs.plugins.android.application,
        libs.plugins.kmp.library,
        libs.plugins.kotlin.multiplatform,
        libs.plugins.kotlin.serialization,
        // kmp-scaffold:firebase:begin
        libs.plugins.google.services,
        libs.plugins.firebase.crashlytics,
        libs.plugins.firebase.appdistribution,
        // kmp-scaffold:firebase:end
        libs.plugins.detekt,
        libs.plugins.ktlint,
    ).forEach {
        alias(it).apply(false)
    }
}

// Merge every module's detekt SARIF into one report for reviewdog.
val reportMerge = tasks.register<ReportMergeTask>("reportMerge") {
    output.set(layout.buildDirectory.file("reports/detekt/merged.sarif"))
}

// One task to run both static-analysis tools across every module.
val staticAnalysis = tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs detekt and ktlint across all modules."
}

subprojects {
    val subPath = path
    plugins.withId("dev.detekt") {
        tasks.withType<Detekt>().configureEach {
            val sarif = reports.sarif.outputLocation
            finalizedBy(reportMerge)
            reportMerge.configure { input.from(sarif) }
        }
        staticAnalysis.configure { dependsOn("$subPath:detekt") }
    }
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        staticAnalysis.configure { dependsOn("$subPath:ktlintCheck") }
    }
}
