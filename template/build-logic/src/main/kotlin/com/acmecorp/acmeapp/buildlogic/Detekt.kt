package com.acmecorp.acmeapp.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Applies detekt with default rules. Never fails the build (ignoreFailures) — findings are
 * surfaced on PRs by reviewdog from the SARIF report instead.
 */
internal fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")

    extensions.configure<DetektExtension> {
        parallel.set(true)
        ignoreFailures.set(true)
        source.setFrom(layout.projectDirectory.dir("src"))
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            sarif.required.set(true)
        }
    }
}
