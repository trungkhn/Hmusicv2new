plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.hmusicv2"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.hmusicv2"
        minSdk = 26
        targetSdk = 36
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Xe tải chở dữ liệu mạng (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Thông dịch viên dịch JSON sang Kotlin (Gson)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Phù thủy load ảnh từ link mạng lên giao diện (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Bơm nguyên bộ công cụ Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    // Bơm chuyên gia quản lý Đăng ký/Đăng nhập
    implementation("com.google.firebase:firebase-auth")
    // Bơm chuyên gia quản lý Kho dữ liệu (Realtime Database)
    implementation("com.google.firebase:firebase-database")
}