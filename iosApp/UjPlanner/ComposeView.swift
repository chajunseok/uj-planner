import SwiftUI
import UIKit
import Shared

/// Kotlin 쪽 화면을 그대로 띄운다. 이 파일이 Swift 의 전부다 —
/// 화면과 배치 규칙은 전부 shared 모듈에 있다.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
