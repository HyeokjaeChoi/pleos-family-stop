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
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:testing")) // fakes = AppGraph defaults until #14
    implementation(project(":core:designsystem"))
    implementation(project(":feature:drive"))
    implementation(project(":feature:input"))
    implementation(project(":feature:metrics"))
    implementation(project(":impl:pleos"))
    implementation(project(":impl:platform"))
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
}
