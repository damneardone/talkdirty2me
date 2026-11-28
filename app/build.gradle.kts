plugins {
    id("com.android.application")
    // Kotlin support is built into AGP 9.0+ (kotl.in/gradle/agp-built-in-kotlin)
    // — do NOT apply org.jetbrains.kotlin.android here.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.damneardone.talkdirty2me"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.damneardone.talkdirty2me"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Runtime-only JSON parsing (Json.parseToJsonElement); the serialization
    // *compiler plugin* is not needed and is not applied under AGP 9 built-in Kotlin.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}