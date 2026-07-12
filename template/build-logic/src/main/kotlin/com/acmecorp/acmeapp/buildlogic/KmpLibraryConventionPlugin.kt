package com.acmecorp.acmeapp.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

/**
 * Base KMP library: Android (AGP KMP library plugin) + iOS arm64/simulatorArm64 targets,
 * JVM toolchain 17, shared commonTest deps, and JVM-host unit tests for every module.
 * Each module sets its own `androidLibrary { namespace = ... }`.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.kmp.library)
            apply(libs.plugins.kotlin.multiplatform)
        }

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()
            jvmToolchain(17)

            // commonTest runs on the JVM (android host) for every module — no per-module opt-in.
            val androidLibrary = (this as ExtensionAware).extensions
                .getByName("androidLibrary") as KotlinMultiplatformAndroidLibraryExtension
            androidLibrary.withHostTest {}

            sourceSets.apply {
                commonTest.dependencies {
                    implementation(libs.kotlin.test)
                    implementation(libs.coroutines.test)
                    implementation(libs.turbine)
                }
            }
        }

        extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
            finalizeDsl { android ->
                android.compileSdk = Versions.COMPILE_SDK
                android.minSdk = Versions.MIN_SDK
            }
        }

        // iOS/native unit-test execution is disabled by choice; tests run on the JVM host.
        tasks.withType(KotlinNativeTest::class.java).configureEach { enabled = false }

        configureDetekt()
        configureKtlint()
    }
}
