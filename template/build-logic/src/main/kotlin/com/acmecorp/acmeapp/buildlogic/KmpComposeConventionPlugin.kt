package com.acmecorp.acmeapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.compose)
            apply(libs.plugins.composeCompiler)
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.apply {
                commonMain.dependencies {
                    implementation(libs.compose.runtime)
                    implementation(libs.compose.foundation)
                    implementation(libs.compose.ui)
                    implementation(libs.compose.material3)
                    implementation(libs.compose.ui.tooling.preview)
                    implementation(libs.compose.lifecycle.runtime)
                    implementation(libs.compose.lifecycle.viewmodel)
                }
            }
        }
    }
}
