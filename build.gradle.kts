plugins {
    id("com.android.application") version "9.4.1" apply false
    id("com.android.library") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

// Shared by every library module (compileSdk/minSdk mirror :app).
subprojects {
    plugins.withType<com.android.build.gradle.api.AndroidBasePlugin> {
        extensions.configure<com.android.build.api.dsl.CommonExtension> {
            ndkVersion = "30.0.16248370"
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 36
            defaultConfig.minSdk = 28
        }
    }
}
