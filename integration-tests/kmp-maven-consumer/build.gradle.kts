import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.4.10"
    id("com.android.kotlin.multiplatform.library") version "9.0.1"
}

val sdkVersion = providers.gradleProperty("sdkVersion").orElse("0.1.0")
val pushProvider = providers.gradleProperty("pushProvider").orElse("fcm").get()
require(pushProvider == "fcm" || pushProvider == "onesignal") {
    "pushProvider must be 'fcm' or 'onesignal'"
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "KmpPushConsumer"
            isStatic = true
        }
    }

    android {
        namespace = "io.github.sosmex.push.consumer"
        compileSdk = 36
        minSdk = 24
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("src/${pushProvider}CommonMain/kotlin")
            dependencies {
                implementation("io.github.sosmex.push:push-core:${sdkVersion.get()}")
                implementation("io.github.sosmex.push:push-${pushProvider}:${sdkVersion.get()}")
            }
        }
        androidMain {
            kotlin.srcDir("src/${pushProvider}AndroidMain/kotlin")
        }
        iosMain {
            kotlin.srcDir("src/${pushProvider}IosMain/kotlin")
        }
    }
}
