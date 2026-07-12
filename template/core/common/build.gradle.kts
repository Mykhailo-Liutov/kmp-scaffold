plugins {
    id("acme.kmp.library")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.core.common" }

    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            api(libs.kermit)
            api(libs.koin.core)
            api(libs.arrow.core)
        }
    }
}
