import java.util.Properties

plugins {
    id("acme.android.application")
    // kmp-scaffold:firebase:begin
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.appdistribution)
    // kmp-scaffold:firebase:end
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

val isBuildingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

android {
    namespace = "com.acmecorp.acmeapp.android"
    defaultConfig {
        applicationId = "com.acmecorp.acmeapp"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        } else if (isBuildingRelease) {
            throw GradleException(
                "keystore.properties is required for release builds."
            )
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")

            // kmp-scaffold:firebase:begin
            firebaseAppDistribution {
                // CI writes the service-account key and points GOOGLE_APPLICATION_CREDENTIALS at it.
                serviceCredentialsFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS").orEmpty()
                groups = providers.gradleProperty("fadGroups").orElse("").get()
                releaseNotesFile = providers.gradleProperty("fadReleaseNotes").orElse("").get()
                artifactType = "APK"
            }
            // kmp-scaffold:firebase:end
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("prod") {
            dimension = "environment"
        }
        create("stage") {
            dimension = "environment"
            applicationIdSuffix = ".stage"
            versionNameSuffix = "-stage"
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.common)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    // kmp-scaffold:firebase:begin
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    // kmp-scaffold:firebase:end

    debugImplementation(libs.compose.ui.tooling)
}
