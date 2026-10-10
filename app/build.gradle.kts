plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.annesophie.semaine"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.annesophie.semaine"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("ma-semaine.keystore")
            storePassword = "masemaine"
            keyAlias = "masemaine"
            keyPassword = "masemaine"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("junit:junit:4.13.2")
}
