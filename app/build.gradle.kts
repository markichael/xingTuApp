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
    // 🔥🔥🔥 核心依赖开始 🔥🔥🔥
    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    // UI 组件库
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.5.4") // 版本号可能随时间变化
    // 图标库 (你之前的代码需要这个)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    // 🔥🔥🔥 核心依赖结束 🔥🔥🔥
    // Coil: 用于在 Compose 中加载图片
    // 🔥 滤镜库祖师爷：GPUImage
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}