plugins {
    kotlin("jvm") version "2.4.10"
    application
}

val sdkVersion = providers.gradleProperty("sdkVersion").orElse("0.1.0")

dependencies {
    implementation("io.github.sosmex.push:push-core:${sdkVersion.get()}")
    implementation("io.github.sosmex.push:push-fcm:${sdkVersion.get()}")
    implementation("io.github.sosmex.push:push-onesignal:${sdkVersion.get()}")
    implementation("io.github.sosmex.push:push-test:${sdkVersion.get()}")
}

kotlin { jvmToolchain(17) }
application { mainClass.set("ConsumerKt") }
