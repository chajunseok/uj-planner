// 안드로이드 플러그인을 적용하지 않는다 — 그래서 android.* 가 컴파일되지 않는다.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    // 지금은 jvm 하나뿐이다. iOS 타깃은 다음 단계에서 macOS 호스트에서만 붙인다.
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
