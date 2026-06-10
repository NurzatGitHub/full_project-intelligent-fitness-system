plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.fitnesscoachai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fitnesscoachai"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Конвертер для парсинга JSON в объекты Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp для логирования запросов (полезно при отладке)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")


    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    // MediaPipe Tasks Vision (Pose Landmarker)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    //    Google Sign-In зависимость
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    // CameraX
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // ExoPlayer (Media3)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Encrypted SharedPreferences (token storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Pull-to-refresh on Home (so users can manually retry weekly plan)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Markwon — proper Markdown rendering for chat bubbles
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}