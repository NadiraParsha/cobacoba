plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Plugin ini harus ada jika Anda menggunakan Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.smartfan"
    // PERBAIKAN: Gunakan SDK versi stabil yang paling umum saat ini (34)
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartfan"
        minSdk = 24
        // PERBAIKAN: targetSdk disamakan dengan compileSdk agar konsisten
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Tambahkan ini jika Anda menggunakan View Binding (rekomendasi)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --- Dependensi Inti Android ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Tambahkan ini untuk MockWebServer
    implementation("com.squareup.okhttp3:mockwebserver:4.11.0")


    // --- Dependensi untuk Grafik (MPAndroidChart) ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Dependensi untuk Jaringan (Retrofit & OkHttp) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // PERBAIKAN: Hanya deklarasikan logging-interceptor satu kali
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // --- Dependensi untuk Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- Dependensi Firebase ---
    // PERBAIKAN: Gunakan deklarasi yang konsisten dan hapus referensi 'libs' yang membingungkan
    implementation("com.google.firebase:firebase-firestore:24.11.1") // Menggunakan versi terbaru yang stabil

    // --- Dependensi untuk Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
