@file:Suppress("UnstableApiUsage")

rootProject.name = "AcmeApp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-provisions the JDK 17 toolchain (only JDK 25 is installed locally).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

includeBuild("build-logic")

include(":shared")
include(":androidApp")

include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:navigation")

include(":feature:home")
include(":feature:catalog")
include(":feature:profile")
