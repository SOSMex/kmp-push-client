import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import com.vanniktech.maven.publish.tasks.JavadocJar as PublicationDocumentationJar
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.publish.PublishingExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
}

val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.1.0")
val centralRelease = providers.gradleProperty("centralRelease")
    .map(String::toBooleanStrict)
    .orElse(false)
val centralAutomaticRelease = providers.gradleProperty("centralAutomaticRelease")
    .map(String::toBooleanStrict)
    .orElse(false)

val publishedModuleDescriptions = mapOf(
    "push-core" to "Provider-neutral Kotlin Multiplatform push state, events, and deduplication contracts.",
    "push-fcm" to "Firebase Cloud Messaging adapter and Android/iOS host gateways for KMP Push Client.",
    "push-onesignal" to "OneSignal adapter, identity support, Android gateway, and iOS callback bridge for KMP Push Client.",
    "push-test" to "Deterministic fakes and an in-memory event ledger for KMP Push Client tests.",
)

allprojects {
    group = "io.github.sosmex.push"
    version = releaseVersion.get()

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "githubPackages"
                    url = uri("https://maven.pkg.github.com/SOSMex/kmp-push-client")
                    credentials(PasswordCredentials::class) {
                        username = providers.environmentVariable("GITHUB_ACTOR").orNull
                        password = providers.environmentVariable("GITHUB_TOKEN").orNull
                    }
                }
            }
        }
    }

    plugins.withId("com.vanniktech.maven.publish") {
        require(name in publishedModuleDescriptions) {
            "Maven publication is restricted to the four public SDK modules"
        }

        extensions.configure<MavenPublishBaseExtension> {
            configure(
                KotlinMultiplatform(
                    javadocJar = JavadocJar.Empty(),
                    sourcesJar = SourcesJar.Sources(),
                ),
            )

            pom {
                name.set("KMP Push Client ${project.name}")
                description.set(publishedModuleDescriptions.getValue(project.name))
                inceptionYear.set("2026")
                url.set("https://github.com/SOSMex/kmp-push-client")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("sosmex")
                        name.set("SOSMex")
                        url.set("https://github.com/SOSMex")
                    }
                }
                organization {
                    name.set("SOSMex")
                    url.set("https://github.com/SOSMex")
                }
                scm {
                    url.set("https://github.com/SOSMex/kmp-push-client")
                    connection.set("scm:git:https://github.com/SOSMex/kmp-push-client.git")
                    developerConnection.set("scm:git:ssh://git@github.com/SOSMex/kmp-push-client.git")
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/SOSMex/kmp-push-client/issues")
                }
            }

            if (centralRelease.get()) {
                publishToMavenCentral(automaticRelease = centralAutomaticRelease.get())
                signAllPublications()
            }
        }

        // There is no Dokka task yet. Publish useful project documentation rather than
        // an empty Central-required javadoc classifier.
        tasks.withType<PublicationDocumentationJar>().configureEach {
            from(rootProject.file("README.md"))
            from(rootProject.file("LICENSE"))
            from(rootProject.file("docs")) {
                include("getting-started.md", "spec.md", "rfc-0001-provider-honest-boundary.md")
                into("docs")
            }
        }
    }
}
