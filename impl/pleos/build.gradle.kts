plugins {
    id("com.android.library")
    id("org.jlleitschuh.gradle.ktlint")
}

android { namespace = "ai.familystop.impl.pleos" }

dependencies {
    implementation(project(":core:domain"))
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    testImplementation("junit:junit:4.13.2")
}
