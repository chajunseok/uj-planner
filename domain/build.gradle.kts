// 안드로이드 플러그인을 적용하지 않는다 — 그래서 android.* 가 컴파일되지 않는다.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

/**
 * iOS 타깃을 선언할지. **macOS 호스트에서만 참이다.**
 *
 * Kotlin/Native 의 애플 플랫폼 컴파일은 macOS 에서만 된다. 그런데 타깃을 선언해 두기만 해도
 * 네이티브 commonizer 가 구성 단계에서 애플 플랫폼 라이브러리를 요구해 다른 OS 의 sync 와
 * 빌드를 망가뜨릴 수 있다. 그래서 "돌지 않는 태스크" 로 두지 않고 아예 선언하지 않는다.
 *
 * `-Puj.ios=true` 로 강제할 수 있다. 맥 러너에서 쓴다.
 * providers.gradleProperty 로 읽어야 configuration cache 와 싸우지 않는다.
 */
val buildIos = providers.gradleProperty("uj.ios").orNull?.toBooleanStrictOrNull()
    ?: System.getProperty("os.name").startsWith("Mac")

kotlin {
    jvmToolchain(17)

    jvm()

    if (buildIos) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            // api 로 노출한다. app 이 LocalDate 를 시그니처로 주고받는다.
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
