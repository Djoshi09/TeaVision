plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.teainfoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.teainfoapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }

    // Keep TensorFlow Lite model files uncompressed for AssetFileDescriptor loading.
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.multidex:multidex:2.0.1")

    // Material Design 3 - explicitly declare before other dependencies
    implementation("com.google.android.material:material:1.12.0")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle & Coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // GSON for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX for camera scanning
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit for barcode/text scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Smooth UI Animations
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("io.coil-kt:coil:2.5.0")

    // MPAndroidChart for interactive charts and graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // TensorFlow Lite for on-device image recognition
    // Upgraded to support newer model ops (e.g. FULLY_CONNECTED v12)
    val tfliteVersion = "2.17.0"
    implementation("org.tensorflow:tensorflow-lite:$tfliteVersion")
    implementation("org.tensorflow:tensorflow-lite-gpu:$tfliteVersion")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")

    // ML Kit Vision API for advanced image processing
    implementation("com.google.mlkit:vision-common:17.3.0")
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Image processing library
    implementation("org.tensorflow:tensorflow-lite-metadata:0.5.0")
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")

    // OpenCV for image preprocessing (Available on MavenCentral from 4.9.0+)
    implementation("org.opencv:opencv:4.10.0")

    // Additional ML utilities
    implementation("androidx.camera:camera-extensions:1.3.1")

    // Image processing for quality assessment
    implementation("androidx.graphics:graphics-core:1.0.0-alpha03")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}