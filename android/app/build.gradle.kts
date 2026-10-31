import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "vn.cfop.rubik"
    compileSdk = 34

    defaultConfig {
        applicationId = "vn.cfop.rubik"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Git SHA của bản build này — dùng để so sánh với bản mới nhất trên GitHub
        // khi kiểm tra cập nhật. Lấy từ git lúc build, không cần sửa workflow.
        val gitSha = try {
            val proc = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD"))
            proc.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
        } catch (e: Exception) {
            "unknown"
        }
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
    }

    // Fixed debug keystore so every CI build signs with the same key:
    // updates install over the previous version instead of failing.
    // The keystore is stored base64-encoded (debug.keystore.b64) and
    // decoded at configuration time. Debug keys are not sensitive.
    signingConfigs {
        // Configure the built-in debug signing config (AGP already creates
        // one named "debug", so use getByName instead of create).
        getByName("debug") {
            val b64File = file("debug.keystore.b64")
            val ksFile = file("debug.keystore")
            if (!ksFile.exists() && b64File.exists()) {
                ksFile.writeBytes(Base64.getDecoder().decode(b64File.readText().trim()))
            }
            storeFile = ksFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
