plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI sets GITHUB_RUN_NUMBER, so every build installs over the last one.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "dev.velocore.console"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.velocore.console"
        minSdk = 28 // the console runs Android 9
        targetSdk = 28
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    signingConfigs {
        // One fixed key so a new build always installs over the old one on the bike.
        // It only signs this app; it is not the console's platform key.
        create("console") {
            storeFile = file("signing/console.jks")
            storePassword = "velocore-console"
            keyAlias = "console"
            keyPassword = "velocore-console"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("console")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("console")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // targetSdk 28 matches the console on purpose; don't fail on Play Store rules.
        checkReleaseBuilds = false
        abortOnError = false
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
