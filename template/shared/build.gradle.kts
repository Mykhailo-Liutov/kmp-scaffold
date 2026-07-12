import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("acme.kmp.library")
    id("acme.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.shared" }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.home)
            api(projects.feature.catalog)
            api(projects.feature.profile)
            api(projects.core.designsystem)
            implementation(projects.core.common)
            implementation(projects.core.ui)
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.core.navigation)

            implementation(libs.compose.navigation)
            implementation(libs.bundles.koin.compose)
        }
    }
}
