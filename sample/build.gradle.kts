plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":push-core"))
    implementation(project(":push-test"))
    implementation(libs.coroutines.core)
}

application {
    mainClass.set("io.github.sosmex.push.sample.MainKt")
}

kotlin {
    jvmToolchain(17)
}

