plugins {
    id("acme.feature")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.feature.profile" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation)
        }
    }
}
