plugins {
    id("acme.kmp.library")
}

kotlin {
    androidLibrary { namespace = "com.acmecorp.acmeapp.data.sample" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.domain.sample)
            implementation(projects.core.common)
        }
    }
}
