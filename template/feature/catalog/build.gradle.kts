plugins {
    id("acme.feature")
    id("acme.room")
}

kotlin {
    androidLibrary {
        namespace = "com.acmecorp.acmeapp.feature.catalog"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.core.navigation)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
