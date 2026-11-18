plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "ai.familystop"
    compileSdk = 36
    defaultConfig {
        applicationId = "ai.familystop.app"
        minSdk = 28 // Pleos Playground requirement
        targetSdk = 34 // Pleos Connect emulator image is API 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures { compose = true }
    // Per-app language (ADR-0007): res/values is Korean (see res/resources.properties), values-en is English.
    androidResources { generateLocaleConfig = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:drive"))
    implementation(project(":feature:input"))
    implementation(project(":feature:fallback"))
    implementation(project(":feature:metrics"))
    implementation(project(":feature:activity"))
    implementation(project(":feature:handoff"))
    implementation(project(":feature:recommend"))
    implementation(project(":feature:voice"))
    implementation(project(":impl:pleos"))
    implementation(project(":impl:platform"))
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
