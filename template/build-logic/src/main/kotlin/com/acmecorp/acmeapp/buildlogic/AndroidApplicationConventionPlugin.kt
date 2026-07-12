package com.acmecorp.acmeapp.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.android.application)
            apply(libs.plugins.composeCompiler)
        }

        extensions.configure<ApplicationExtension> {
            compileSdk = Versions.COMPILE_SDK
            defaultConfig {
                minSdk = Versions.MIN_SDK
                targetSdk = Versions.TARGET_SDK
                // CI injects a monotonic versionCode (GitHub run number); defaults to 1 locally.
                versionCode = (
                        providers.gradleProperty("versionCode").orNull
                            ?: providers.environmentVariable("VERSION_CODE").orNull
                        )?.toInt() ?: 1
                versionName = providers.gradleProperty("versionName").orNull ?: "0.1.0"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildFeatures {
                compose = true
            }
        }

        configureDetekt()
        configureKtlint()
    }
}
