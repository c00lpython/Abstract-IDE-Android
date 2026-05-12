plugins {
    id("com.android.application")
}

android {
    namespace = "com.xcore.abstractide"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xcore.abstractide"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release_keystore.jks")
            storePassword = "release123"
            keyAlias = "release"
            keyPassword = "release123"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}