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
        versionCode = 4
        versionName = "1.3"
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
