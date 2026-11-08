plugins {
    id("com.android.library")
    id("org.jlleitschuh.gradle.ktlint")
}

android { namespace = "ai.familystop.core.domain" }

dependencies {
    api(project(":core:model")) // Ports expose model types
}
