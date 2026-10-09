pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val sdkRepository = providers.gradleProperty("sdkRepository").orElse("local").get()
require(sdkRepository == "local" || sdkRepository == "central") {
    "sdkRepository must be 'local' or 'central'"
}

dependencyResolutionManagement {
    repositories {
        if (sdkRepository == "local") {
            exclusiveContent {
                forRepository {
                    maven {
                        name = "localPushPublications"
                        url = uri("../../build/test-maven")
                    }
                }
                filter { includeGroup("io.github.sosmex.push") }
            }
        } else {
            // A dedicated Central repository is the only allowed source for SDK coordinates.
            // The general Central declaration below excludes them, so this mode cannot fall
            // back to a stale local repository or another configured Maven source.
            exclusiveContent {
                forRepository {
                    mavenCentral { name = "centralPushPublications" }
                }
                filter { includeGroup("io.github.sosmex.push") }
            }
        }

        google()
        mavenCentral {
            name = "centralDependencies"
            content { excludeGroup("io.github.sosmex.push") }
        }
    }
}

rootProject.name = "kmp-push-maven-consumer"
