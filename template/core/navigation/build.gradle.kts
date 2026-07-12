plugins {
    id("acme.kmp.library")
    id("acme.kmp.compose")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.core.navigation" }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}
