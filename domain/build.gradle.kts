// 안드로이드 플러그인을 적용하지 않는다 — 그래서 android.* 가 컴파일되지 않는다.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
