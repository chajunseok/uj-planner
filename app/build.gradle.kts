import java.util.Properties

// AGP 9 는 Kotlin 이 내장이라 kotlin-android 플러그인을 적용하지 않는다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 릴리스 서명. 키와 비밀번호는 레포에 두지 않는다 — keystore.properties 는 gitignore 되어 있고 tools/make-release-key.sh 가 만든다.
// 파일이 없으면 릴리스 빌드는 서명 없이 나온다(설치 불가). 디버그 빌드는 영향받지 않는다.
val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

android {
    namespace = "com.uj.planner"
    // 최신 androidx 가 37 이상을 요구한다. 동작 기준은 targetSdk(36 = 플립7의 Android 16)다.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.uj.planner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 아이콘 라이브러리가 통째로 들어가 축소 없이는 APK 가 47MB 다. 쓰는 것만 남긴다.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.window)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
