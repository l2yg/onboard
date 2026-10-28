import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version "8.9.2"
    id("org.jetbrains.kotlin.android") version "2.2.10"
}

android {
    namespace = "dev.onboard.probe"
    compileSdk = 35
    useLibrary("android.car")

    defaultConfig {
        applicationId = "dev.onboard.probe"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
}
