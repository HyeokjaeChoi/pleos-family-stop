plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "ai.familystop.core.designsystem"
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    api("androidx.compose.material3:material3") // features use Text/Icon with App tokens
    api("androidx.compose.material:material-icons-core")
    testImplementation("junit:junit:4.13.2")
}
