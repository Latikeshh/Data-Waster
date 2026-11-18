plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.databurner"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.databurner"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val desktopKeystore = file("C:/Users/Latikesh/Desktop/laki.class")
            val assetKeystore = file("src/main/assets/laki.class")
            storeFile = if (desktopKeystore.exists()) desktopKeystore else assetKeystore
            storePassword = "123456"
            keyAlias = "key123456"
            keyPassword = "123456"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
