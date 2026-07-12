package com.acmecorp.acmeapp.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room + KSP wiring for a module that owns a `@Database`. Apply on top of `acme.kmp.library`
 * / `acme.feature` (targets must already exist so the per-target KSP configs are present).
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.ksp)
            apply(libs.plugins.room)
        }

        val schemaDir = "$projectDir/schemas"
        extensions.configure<RoomExtension> {
            schemaDirectory(schemaDir)
        }

        // Room KSP runs per platform target (no commonMain metadata processing).
        dependencies {
            add("kspAndroid", libs.room.compiler)
            add("kspIosArm64", libs.room.compiler)
            add("kspIosSimulatorArm64", libs.room.compiler)
        }
    }
}
