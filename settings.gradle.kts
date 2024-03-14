pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://repo.nokee.dev/release") }
    }
}

rootProject.name = "th2-gradle-plugin"
include("plugin")
project(":plugin").name = "th2-gradle-plugin"
