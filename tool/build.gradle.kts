plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.light.sdk)
}

android {
    compileSdk = rootProject.ext["compileSdk"] as Int

    signingConfigs {
        create("lightsdkDev") {
            // Workspace path first; inside an SDK checkout (Light's Tool
            // Library builder stages tool/ into the baked-in SDK repo) the
            // same dev keystore lives at ../sdk/keys.
            storeFile = file(
                listOf("../../light-sdk/sdk/keys/lightsdk-dev.jks", "../sdk/keys/lightsdk-dev.jks")
                    .map(::file).first { it.exists() }
            )
            storePassword = "android"
            keyAlias = "lightsdk-dev"
            keyPassword = "android"
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    defaultConfig {
        minSdk = rootProject.ext["minSdk"] as Int
        targetSdk = rootProject.ext["targetSdk"] as Int

        manifestPlaceholders["sdkVersion"] = property("sdkVersion") as String
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("lightsdkDev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("lightsdkDev")
        }
    }

    lint {
        warningsAsErrors = false
        error += "RestrictedApi"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(rootProject.ext["jvmTarget"] as String)
        targetCompatibility = JavaVersion.toVersion(rootProject.ext["jvmTarget"] as String)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(rootProject.ext["jvmTarget"] as String))
    }
}

// zxing-cpp (the SDK's camera QR/barcode decoder) requires compileSdk 37; the
// stopwatch never scans codes — drop the group (chats/tasks pattern).
configurations.configureEach {
    exclude(group = "com.github.markusfisch")
}

// Inside an SDK checkout the SDK modules are sibling projects; in this
// workspace the included ../light-sdk build substitutes the module artifacts.
val inSdkRepo = file("../sdk").exists()

dependencies {
    implementation(
        if (inSdkRepo) project(":sdk:client")
        else "com.thelightphone:sdk-client"   // LightScreen, LightActivity
    )
    testImplementation(libs.kotlin.test)
}
