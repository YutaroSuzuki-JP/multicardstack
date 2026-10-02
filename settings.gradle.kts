rootProject.name = "multicardstack"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":multicardstack")
include(":sample:shared")
include(":sample:androidApp")
