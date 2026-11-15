plugins {
    id("com.android.library")
    id("org.jlleitschuh.gradle.ktlint")
}

android { namespace = "ai.familystop.core.model" }

dependencies {
    testImplementation("junit:junit:4.13.2")
}
