plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "ai.familystop.feature.voice"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
}
