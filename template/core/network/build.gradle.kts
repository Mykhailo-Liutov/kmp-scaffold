plugins {
    id("acme.kmp.library")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.core.network" }

    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.ktor)
            api(libs.kotlinx.serialization.json)
            api(libs.koin.core)
            implementation(projects.core.common)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
