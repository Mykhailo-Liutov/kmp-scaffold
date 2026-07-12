plugins {
    id("acme.kmp.library")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.domain.sample" }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
        }
    }
}
