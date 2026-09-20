# iOS 앱 껍데기

화면과 배치 규칙은 전부 `shared` 모듈에 있다. 여기 있는 Swift 두 파일은 그것을 띄우기만 한다.

> **이 폴더의 코드는 아직 한 번도 컴파일되지 않았다.** Kotlin/Native 의 iOS 컴파일은 macOS 에서만
> 되고, 지금까지의 작업은 윈도우에서 했다. 맥에서 처음 빌드할 때 손볼 것이 나올 수 있다.

## Xcode 프로젝트 만들기

`.xcodeproj` 를 손으로 쓰지 않았다. 800줄짜리 XML 이고 검증 없이 쓰면 거의 틀리기 때문이다.
맥에서 아래 순서로 만들면 몇 분이면 된다.

1. Xcode → **File ▸ New ▸ Project ▸ iOS ▸ App**
   - Product Name `UjPlanner`, Interface **SwiftUI**, Language **Swift**
   - 저장 위치는 이 `iosApp/` 폴더
2. 생성된 `ContentView.swift` 와 `UjPlannerApp.swift` 를 지우고 이 폴더의 두 파일을 넣는다
3. **Build Phases ▸ + ▸ New Run Script Phase** 를 만들고 Compile Sources 보다 **위로** 옮긴다

   ```sh
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode -Puj.ios=true
   ```

4. **Build Settings**
   - `Framework Search Paths` 에 `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
   - `Other Linker Flags` 에 `$(inherited) -framework Shared`
   - `User Script Sandboxing` 을 `No` (Gradle 이 프로젝트 밖을 읽어야 한다)
5. 실행

## 아직 없는 것

**설정의 내보내기·가져오기.** `UIDocumentPicker` 와 security-scoped bookmark 배선이 필요한데
아직 요청되지 않았다. 안드로이드는 `app/src/main/kotlin/com/uj/planner/ui/AndroidDataSection.kt`
가 그 역할을 하고, `PlannerRoot` 가 그 자리를 슬롯으로 받으므로 iOS 용을 만들어 끼우면 된다.

iOS 에서는 앱이 스스로 프로세스를 끝내는 것이 스토어 정책 위반이라, 가져오기 뒤 "앱 재시작" 은
안드로이드와 같은 방법을 쓸 수 없다. `PlannerRoot` 를 `key(generation) { }` 로 감싸고 DB 를
다시 여는 쪽이 맞다 — 그러면 안드로이드에서도 `Runtime.exit` 를 지울 수 있다.
