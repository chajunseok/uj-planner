// 안드로이드 쪽은 com.android.kotlin.multiplatform.library 를 쓴다.
// AGP 9 부터 com.android.library 와 kotlin.multiplatform 은 같이 적용할 수 없다.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
}

// domain 과 같은 규칙. macOS 호스트에서만 iOS 타깃을 선언한다.
val buildIos = providers.gradleProperty("uj.ios").orNull?.toBooleanStrictOrNull()
    ?: System.getProperty("os.name").startsWith("Mac")

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.uj.planner.shared"
        compileSdk = 37
        minSdk = 26
        // 계측 테스트를 만들지 않으므로 withDeviceTest 없음.
        // DB 와 무관한 계산은 domain 에서 검증하므로 withHostTest 도 없음.
    }

    if (buildIos) {
        listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "Shared"
                // 정적 프레임워크로 둔다. dSYM 과 서명 문제를 아예 만들지 않는다.
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))

            // compose.xxx 접근자는 deprecate 됐지만 그대로 쓴다. CMP 의 material3 는 본 버전과 별개 라인이라
            // 1.12.0 이 없고 alpha 만 있다 — 좌표를 손으로 박으면 alpha 를 고정하게 되고, 이 접근자가
            // 아티팩트마다 맞는 버전을 골라 주는 일을 우리가 떠안게 된다.
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

// Room 컴파일러는 타깃별 configuration 에 건다. common 메타데이터 한 번으로는 안 된다 —
// 생성물이 플랫폼별 actual(RoomDatabaseConstructor 의 actual 과 DAO 의 _Impl)이기 때문이다.
//
// 설정 이름은 kspAndroid 다 — kspAndroidMain 은 **태스크** 이름이라 헷갈리기 쉽다.
// `./gradlew :shared:dependencies | grep ^ksp` 가 "KSP dependencies for the ... source set" 이라고 알려 준다.
dependencies {
    add("kspAndroid", libs.room.compiler)
    if (buildIos) {
        add("kspIosX64", libs.room.compiler)
        add("kspIosArm64", libs.room.compiler)
        add("kspIosSimulatorArm64", libs.room.compiler)
    }
}
