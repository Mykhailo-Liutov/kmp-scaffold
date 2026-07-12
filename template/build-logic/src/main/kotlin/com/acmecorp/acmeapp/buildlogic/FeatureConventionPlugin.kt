package com.acmecorp.acmeapp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("acme.kmp.library")
            apply("acme.kmp.compose")
            apply(libs.plugins.kotlin.serialization)
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.apply {
                commonMain.dependencies {
                    implementation(project(":core:common"))
                    implementation(project(":core:designsystem"))
                    implementation(project(":core:ui"))

                    implementation(libs.compose.navigation)
                    implementation(libs.koin.core)
                    implementation(libs.koin.compose)
                    implementation(libs.koin.compose.viewmodel)
                    implementation(libs.koin.compose.viewmodel.navigation)
                    implementation(libs.kotlinx.serialization.json)
                }
            }
        }
    }
}
