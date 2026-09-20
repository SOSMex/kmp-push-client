plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":push-core"))
    implementation(project(":push-fcm"))
    implementation(project(":push-onesignal"))
    implementation(project(":push-test"))
    implementation(libs.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines.test)
}

application {
    mainClass.set("io.github.sosmex.push.sample.MainKt")
}

kotlin {
    jvmToolchain(17)
}
