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

rootProject.name = "kmp-push-client"

include(":push-core")
include(":push-fcm")
include(":push-onesignal")
include(":push-test")
include(":sample")

