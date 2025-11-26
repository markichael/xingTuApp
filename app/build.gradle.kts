plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.xingtuclone" // 注意：这里最好改成跟你代码里的一致，或者保持你原来的 "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.xingtuclone"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    // 🔥🔥🔥 核心修改开始 🔥🔥🔥
    buildFeatures {
        compose = true // 开启 Compose
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // 对应 Kotlin 1.9.0
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // 🔥🔥🔥 核心修改结束 🔥🔥🔥
}

dependencies {
    implementation("androidx.camera.viewfinder:viewfinder-core:1.5.1")
    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 图标库
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.foundation:foundation")

    // 🔥 Google ML Kit 人脸检测
    implementation("com.google.mlkit:face-detection:16.1.6")

    // ❌ 删除下面这行 guava (它和下面的 play-services 冲突/重复)
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.0")

    // 🔥 必须保留这行 (用于 await)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // 🔥 GPUImage 滤镜
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")
    // Coil 图片加载
    implementation("io.coil-kt:coil-compose:2.5.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}