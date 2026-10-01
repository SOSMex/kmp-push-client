pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "localPushPublications"
                    url = uri("../../build/test-maven")
                }
            }
            filter { includeGroup("io.github.sosmex.push") }
        }
        mavenCentral()
    }
}

rootProject.name = "push-maven-consumer"
