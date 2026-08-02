plugins {
    id("com.android.application")
}

val backendUrl = providers.gradleProperty("MIMO_BACKEND_URL")
    .orElse("")
    .get()
    .trimEnd('/')
    .replace("\"", "\\\"")

android {
    namespace = "com.mimo.assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mimo.assistant"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "MIMO_BACKEND_URL", "\"$backendUrl\"")
        }
        getByName("release") {
            buildConfigField("String", "MIMO_BACKEND_URL", "\"$backendUrl\"")
        }
    }
}
