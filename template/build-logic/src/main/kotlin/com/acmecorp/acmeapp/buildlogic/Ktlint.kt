package com.acmecorp.acmeapp.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Applies ktlint, pinned to the same version as the CI workflow. Never fails the build
 * (ignoreFailures) — findings are surfaced on PRs by reviewdog from the SARIF report.
 */
internal fun Project.configureKtlint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        version.set(libs.versions.ktlint.get())
        ignoreFailures.set(true)
        reporters {
            reporter(ReporterType.SARIF)
        }
    }
}
