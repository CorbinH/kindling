enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.hq.hydraulic.software")
    }
}

rootProject.name = "kindling"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
}
