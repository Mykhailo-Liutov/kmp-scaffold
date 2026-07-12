plugins {
    `kotlin-dsl`
}

group = "com.acmecorp.acmeapp.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Exposes the version-catalog accessors (`libs`) to plugin source.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "acme.kmp.library"
            implementationClass = "com.acmecorp.acmeapp.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "acme.kmp.compose"
            implementationClass = "com.acmecorp.acmeapp.buildlogic.KmpComposeConventionPlugin"
        }
        register("feature") {
            id = "acme.feature"
            implementationClass = "com.acmecorp.acmeapp.buildlogic.FeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "acme.android.application"
            implementationClass = "com.acmecorp.acmeapp.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("room") {
            id = "acme.room"
            implementationClass = "com.acmecorp.acmeapp.buildlogic.RoomConventionPlugin"
        }
    }
}
