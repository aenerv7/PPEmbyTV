plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

// 签名密钥从 gitignored 的 keystore.properties 读取，避免密码入库
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKey = keystoreProps.getProperty("storeFile") != null &&
    keystoreProps.getProperty("storePassword") != null &&
    keystoreProps.getProperty("keyAlias") != null

android {
    namespace = "magi.aenerv7.ppembytv"
    compileSdk = 35

    defaultConfig {
        applicationId = "magi.aenerv7.ppembytv"
        minSdk = 23
        targetSdk = 35
        versionCode = 7
        versionName = (project.findProperty("versionName") as String?) ?: "0.0.7"
    }

    signingConfigs {
        getByName("debug") {
            // 项目内自带的 debug keystore（仅本地环境存在）；不存在时保持 AGP 默认
            // （~/.android/debug.keystore，AGP 会自动生成），保证 CI 可直接构建。
            val f = rootProject.file(".android/debug.keystore")
            if (f.exists()) {
                storeFile = f
            }
        }
        create("release") {
            if (hasReleaseKey) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            } else {
                val f = rootProject.file(".android/debug.keystore")
                if (f.exists()) {
                    storeFile = f
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                } else {
                    // CI 且未配置签名 secrets：用 AGP 默认 debug keystore（release workflow 会预先生成）
                    storeFile = File(System.getProperty("user.home"), ".android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.zxing.core)
    implementation(libs.nanohttpd)
    implementation(libs.lottie.compose)
}
