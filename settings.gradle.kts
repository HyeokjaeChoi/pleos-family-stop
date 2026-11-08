pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
rootProject.name = "pleos-family-stop"
include(
    ":app",
    ":core:model", ":core:domain", ":core:testing", ":core:designsystem",
    ":feature:activity", ":feature:drive", ":feature:input", ":feature:recommend",
    ":feature:fallback",
    ":feature:metrics",
    ":feature:voice",
    ":impl:pleos", ":impl:platform",
)
