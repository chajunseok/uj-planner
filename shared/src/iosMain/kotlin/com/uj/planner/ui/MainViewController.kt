package com.uj.planner.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.uj.planner.data.PlannerRepository
import com.uj.planner.data.createPlannerDatabase
import com.uj.planner.ui.theme.PlannerTheme
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/**
 * iOS 의 진입점. Swift 쪽에서 이 함수 하나만 부르면 된다.
 *
 * ```swift
 * struct ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController { MainViewControllerKt.MainViewController() }
 *     func updateUIViewController(_ controller: UIViewController, context: Context) {}
 * }
 * ```
 *
 * 힌지는 항상 null 이다 — 접히는 iPhone 은 없다. 화면을 가르는 규칙 자체는 안드로이드와 같은
 * [PlannerRoot] 를 쓰므로, 아이패드처럼 큰 화면에서도 안드로이드 태블릿과 같게 동작한다.
 *
 * ponytail: 설정의 내보내기·가져오기는 비워 둔다. iOS 의 문서 선택기(UIDocumentPicker)와
 * security-scoped bookmark 배선이 필요한데, 아직 요청되지 않았다. 필요해지면 안드로이드의
 * AndroidDataSection 과 같은 모양으로 iosMain 에 만들어 넣으면 된다 — 받는 쪽은 이미 슬롯이다.
 */
fun MainViewController(): UIViewController {
    val repository = PlannerRepository(createPlannerDatabase())
    return ComposeUIViewController {
        PlannerTheme {
            PlannerRoot(
                repository = repository,
                fold = null,
                version = appVersion(),
                dataSection = {},
            )
        }
    }
}

/** Info.plist 의 버전. 못 읽으면 비워 둔다 — 버전 줄 하나 때문에 화면이 죽지 않게 한다. */
private fun appVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
