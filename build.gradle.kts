plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Add this line to apply the Google services plugin
}

android {
    namespace = "com.example.moneynow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.moneynow"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase BOM for version management
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    // Firebase Authentication and Realtime Database Dependencies
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // Optional: Firebase Analytics (if you need it)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-functions:21.1.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

