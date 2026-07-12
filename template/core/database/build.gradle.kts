plugins {
    id("acme.kmp.library")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.core.database" }

    sourceSets {
        commonMain.dependencies {
            api(libs.room.runtime)
            api(libs.sqlite.bundled)
            api(libs.coroutines.core)
        }
    }
}
