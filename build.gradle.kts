plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "io.github.sosmex.push"
    version = "0.1.0"
}

