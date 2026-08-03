plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.woodenfish.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.woodenfish.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "1.6"
    }

    signingConfigs {
        create("doki") {
            storeFile = file(System.getenv("DOKI_KEYSTORE") ?: "doki.keystore")
            storePassword = System.getenv("DOKI_STORE_PASS") ?: "doki-password"
            keyAlias = System.getenv("DOKI_KEY_ALIAS") ?: "doki"
            keyPassword = System.getenv("DOKI_KEY_PASS") ?: "doki-password"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("doki")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("doki")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // WorkManager for scheduled notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
