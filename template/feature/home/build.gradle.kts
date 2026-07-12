plugins {
    id("acme.feature")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.feature.home" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation)
        }
    }
}
