# U.J planner

주간 자동 배치 플래너. 고정 일정만 사람이 넣고, 가변 일정은 앱이 남는 시간에 배치한다.
안드로이드(8.0 이상, 폰·태블릿·폴더블)와 iOS 에서 같은 앱이 돈다.

- **PRD**: `.claude/prd/2609/260918.uj-planner.prd.md`
- **계획**: `.claude/plan/2609/260918.uj-planner.plan.md`
- **디자인 의뢰문**: `.claude/design/2609/260918.uj-planner.design.md`
- **디자인 구현 스펙**: `.claude/design/2609/260918.uj-planner.design-spec.md` — 색·타이포·치수·블록 상태의 출처. 색 값은 `ui/theme/` 에만 둔다
- Application ID: `com.uj.planner` / minSdk 26 / targetSdk 36 / **compileSdk 37** / JDK 17
- Gradle 9.7.1 / AGP 9.3.3 / Kotlin 2.4.20 / KSP 2.3.12 — 버전은 `gradle/libs.versions.toml` 한 곳에만 적는다

`compileSdk` 가 `targetSdk` 보다 높은 것은 의도한 것이다. 최신 androidx 가 compileSdk 37 이상을 요구한다.
앱의 동작 기준은 `targetSdk` 36 이고, `compileSdk` 는 컴파일에 쓰는 API 목록일 뿐이다.

AGP 9 는 Kotlin 이 내장이다. **`org.jetbrains.kotlin.android` 플러그인을 적용하지 않는다** — 적용하면 빌드가 깨진다.

## Git 작업 규칙

### 브랜치

| 브랜치 | 역할 |
|---|---|
| `main` | 안정 버전. 직접 커밋하지 않는다 |
| `develop` | 통합 브랜치. 모든 작업이 여기로 모인다 |
| `<종류>/<주제>` | 작업 브랜치. **`develop`에서 따서 `develop`으로 머지한다** |

작업 브랜치 이름은 `<종류>/<주제>` 형식이고 주제는 kebab-case 영문이다.

```
feature/week-grid
fix/preferred-window
refactor/scheduler-extract
chore/room-setup
test/deadline-ordering
docs/prd-update
```

종류는 커밋 타입과 같은 단어를 쓴다 — 외울 게 하나로 줄어든다.

**`main`과 `develop`에 직접 커밋하지 않는다.** 아무리 작은 수정이라도 작업 브랜치를 따고,
끝나면 `develop`으로 머지한다.

### 작업 브랜치 하나의 흐름

```
develop 에서 브랜치 생성
  → 구현
  → 검증            (아래 "검증 명령" 전부)
  → 커밋
  → 코드 리뷰        /ecc:kotlin-review — 대상은 `git diff develop...HEAD`
  → 수정 커밋        지적 반영
  → 재검증
  → develop 으로 머지 (--no-ff)
```

**리뷰 없이 머지하지 않는다.** 문서·설정만 바꾼 브랜치(`docs/*`, 코드 없는 `chore/*`)는 예외다.

머지 기준은 이렇다.

- **CRITICAL·HIGH 지적은 전부 해결**해야 머지할 수 있다.
- MEDIUM 이하는 고치거나, 고치지 않으면 **이유를 커밋 본문에 남긴다.**
- 지적을 고친 뒤에는 검증 명령을 처음부터 다시 돌린다.

리뷰에서 나온 수정은 `fix:` 또는 `refactor:` 커밋으로 따로 남긴다. 원래 커밋에 뭉개 넣지 않는다 —
무엇이 리뷰로 바뀌었는지 기록에 남아야 한다.

### 커밋 메시지

`<타입>: <한국어 개조식 명사형>` 형식이다. 타입만 영어로 빌려 오고 나머지는 한국어로 쓴다.

```
feat: 주간 그리드 화면 추가
fix: 선호 시간대 밖에 배치되던 문제 수정
refactor: 스케줄러 순수 Kotlin으로 분리
chore: Room 의존성 추가
test: 마감 정렬 케이스 보강
docs: 배치 규칙 설명 보강
```

타입은 `feat` / `fix` / `refactor` / `chore` / `test` / `docs` 여섯 개다.

**`~다` 체를 쓰지 않는다.** `~했다`, `~한다`, `~이다`, `~됐다` 모두 해당하며 제목·본문·불릿 전부 적용된다.
제목은 명사로 끝낸다 (`추가`, `수정`, `분리`, `보강`).

## 아키텍처

모듈은 셋이고 `app → shared → domain` 단방향이다. 반대 방향 의존은 없다.

```
uj-planner/
├── domain/     Kotlin Multiplatform. 배치 규칙과 그 입출력 타입만
├── shared/     Kotlin Multiplatform + com.android.kotlin.multiplatform.library. data(Room)
└── app/        안드로이드 껍데기. Application 조립, MainActivity, 런처 아이콘, R8·서명
```

**iOS 타깃은 macOS 호스트에서만 선언한다**(`domain`·`shared` 의 `buildIos`). 선언만 해도 네이티브
commonizer 가 애플 플랫폼 라이브러리를 요구해 다른 OS 의 빌드를 깨뜨릴 수 있어서다.
`-Puj.ios=true` 로 강제할 수 있다.

**`app` 에 `org.jetbrains.kotlin.multiplatform` 을 적용하지 않는다.** AGP 9 부터
`com.android.application` 과 공존할 수 없다. 공용 코드가 필요하면 `shared` 로 옮긴다.

**`domain` 에 안드로이드 플러그인을 적용하지 않는다.** 그래서 `import android.*` 가 컴파일 자체가
안 된다. 이 경계는 규율이 아니라 빌드가 강제한다. 경계를 넓히고 싶어지면 코드를 옮기지 말고
왜 필요한지부터 따진다.

```
domain/src/commonMain/kotlin/com/uj/planner/domain/
├── model/        FixedBlock, FlexTaskSpec, Window, Slot, ScheduleResult
├── Scheduler.kt  schedule(ScheduleInput) 과 freeSlots(ScheduleInput) — 순수 함수
└── WeekMath.kt   주 시작일·절단점·남은 횟수. 시스템 시계를 읽는 nowLocalDateTime() 도 여기 하나뿐 — 화면과 저장소가 같은 시간대로 "오늘" 을 계산하게

shared/src/commonMain/kotlin/com/uj/planner/data/
├── entity/ dao/     Room 엔티티와 DAO
├── Converters.kt    LocalDate ↔ epochDay
├── PlannerDatabase.kt  @Database + @ConstructedBy. actual 은 KSP 가 타깃별로 생성
├── DatabaseBuilder.kt  빌더 마감(드라이버·콜백·디스패처). 빌더 생성만 플랫폼별
└── PlannerRepository.kt

shared/src/androidMain/kotlin/com/uj/planner/data/   Backup(SAF·파일), 안드로이드 빌더
shared/src/iosMain/kotlin/com/uj/planner/data/       iOS 빌더

app/src/main/kotlin/com/uj/planner/
├── PlannerApp.kt   Application — 의존성 수동 조립
└── ui/             PlannerNavHost, AdaptiveHost + week/ edit/ settings/ missed/ today/ cover/ flex/ components/ theme/ icons/
```

데이터는 한 방향으로만 흐른다.

```
Room DAO (Flow) → Repository → ViewModel (StateFlow) → Composable
        ▲                                                    │
        └──────────────────── 사용자 액션 ────────────────────┘
```

| 규칙 | |
|---|---|
| `Scheduler` 호출 | **`PlannerRepository` 에서만.** ViewModel도 Composable도 직접 부르지 않는다 |
| 시간표를 바꾸는 경로 | `recomputeWeek` · `resolveMissed` · `movePlacement` 셋뿐. 일정·가용 시간 저장은 안에서 `recomputeWeek` 를 부른다 |
| 손으로 옮긴 배치 | `pinned` 로 표시하고 재계산에서 지우지 않는다. 그 일정을 다시 저장하거나 `다시 짜기` 를 누르면 풀린다 |
| 배치 결과 | `Placement` 테이블에 **저장한다.** 조회할 때마다 계산하지 않는다 |
| 화면 이동 | Navigation Compose. 목적지는 `week` / `edit/{kind}?id={id}` / `settings` 셋 |
| 테마 | 라이트 단일. 시스템 다크 모드를 따라가지 않는다 |
| 내보내기·가져오기 | `Backup` 이 DB 파일을 통째로 다룬다. 가져오기는 DB 를 닫고 파일을 바꾼 뒤 **앱을 다시 시작한다** — 열려 있는 Room 인스턴스를 살려 두지 않는다 |
| 커버 화면 | 네비게이션 스택을 갖지 않는다. `AdaptiveHost` 가 그 위에서 갈라낸다 |
| 화면 분기 | **기기 이름이 아니라 창의 성질로만 판단한다.** 창 크기와 힌지 방향 둘뿐이고, 판단은 `AdaptiveHost` 한 곳에 있다 |
| 넓은 화면 | 폼은 `Modifier.readableWidth()` 로 폭을 묶는다. 주간 그리드는 묶지 않는다 — 7열은 넓을수록 좋다 |
| 주간 그리드 높이 | 행 높이는 고정이 아니라 **남는 높이에서 계산한다.** 모자라면 최소 높이를 지키고 스크롤한다 |
| 커버·플렉스 | "지금 할 차례" 판정은 `TodayViewModel` 한 곳. 두 화면은 같은 `Focus` 를 글자 크기만 달리해 그린다 |

## 검증 명령

`Makefile`이 없다. 아래 Gradle 명령이 이 레포의 검증 명령이다.

```bash
./gradlew :domain:jvmTest          # 스케줄러 테스트 — 가장 먼저 돌린다
./gradlew :app:assembleDebug       # 빌드
./gradlew :app:installDebug        # 에뮬레이터에 설치

# 500줄 초과 파일 — 출력이 비어 있어야 한다
find . -name "*.kt" -not -path "*/build/*" | xargs wc -l | awk '$1 > 500 && $2 != "total"'
```

커밋 전에 최소한 `:domain:jvmTest` 와 `:app:assembleDebug` 가 통과하고, 500줄 검사 출력이 비어 있어야 한다.

### iOS

```bash
./gradlew :shared:compileKotlinIosArm64 -Puj.ios=true   # macOS 에서만
```

**맥이 아니면 iOS 는 컴파일되지 않는다.** Kotlin/Native 의 애플 플랫폼 컴파일은 macOS 전용이고,
그래서 `domain` 과 `shared` 는 맥 호스트에서만 iOS 타깃을 선언한다(`buildIos`).

그 결과 **`shared/src/iosMain/` 과 `iosApp/` 의 코드는 아직 한 번도 컴파일된 적이 없다.**
맥에서 처음 빌드할 때 손볼 것이 나올 수 있다. 윈도우·리눅스에서 그 코드를 고칠 때는
"컴파일로 확인할 수 없다" 는 것을 알고 고친다. 연결 절차는 `iosApp/README.md` 에 있다.

## 릴리스 빌드와 서명

```bash
./tools/make-release-key.sh        # 한 번만. 키는 ~/.uj-planner/release.jks, 설정은 keystore.properties (둘 다 레포 밖/ gitignore)
./gradlew :app:assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

| 규칙 | |
|---|---|
| 키와 비밀번호 | **레포에 두지 않는다.** public 레포다. `*.jks` · `keystore.properties` 는 gitignore 되어 있다 |
| 키를 잃으면 | 같은 앱으로 업데이트할 수 없다. 새 키의 APK 는 덮어 설치가 안 돼 앱을 지워야 하고, 데이터는 설정의 내보내기·가져오기로만 옮길 수 있다 |
| 업데이트 배포 | `versionCode` 를 올린다. 같거나 낮으면 덮어 설치가 거부된다 |
| 디버그 ↔ 릴리스 | 서명이 달라 서로 덮어 설치되지 않는다. 실사용 기기에는 처음부터 릴리스 APK 를 깐다 |
| 코드 축소 | 릴리스만 R8 축소를 켠다(6.2MB). 릴리스에서만 나는 문제는 축소 규칙부터 의심한다 |
| APK 크기 | 그중 **4.7MB 가 번들 SQLite**(`libsqliteJni.so` × 4 ABI)다. 줄이고 싶으면 안드로이드만 `AndroidSQLiteDriver` 로 바꾸면 되지만 **그러면 안 된다** — 아래 참고 |

**번들 SQLite 를 쓰는 이유.** 안드로이드의 시스템 SQLite 버전은 OS 버전을 따라간다. 내보내기가
쓰는 `VACUUM INTO` 는 SQLite 3.27 부터인데 그건 안드로이드 11 이후다. minSdk 가 26 이라
안드로이드 8~10 에서는 시스템 드라이버로는 내보내기가 깨진다. 번들 드라이버는 모든 안드로이드
버전과 iOS 에서 같은 SQLite 를 쓰게 해 이 차이를 없앤다. 4.7MB 는 그 대가다.

`keystore.properties` 가 없으면 릴리스 빌드는 서명 없이 나온다. 디버그 빌드와 검증 명령은 영향받지 않는다.

## 에뮬레이터

```bash
./tools/emu.sh start    # AVD flip7 부팅 (1080x2520 @420dpi, 가로 힌지)
./tools/emu.sh open     # 펼침
./tools/emu.sh flex     # 반 접기 — 플렉스 모드
./tools/emu.sh fold     # 접기
./tools/emu.sh state    # 현재 상태 확인

# 화면 크기를 덮어써 다른 기기를 흉내 낸다 — 레이아웃 분기 검증용
./tools/emu.sh size flip7 | cover | phone | tablet | landscape | foldcover | reset
```

SDK 경로는 `ANDROID_HOME` 이 없으면 OS 별 기본 위치에서 찾는다. macOS·리눅스·윈도우(Git Bash) 모두 동작한다.

`size` 프리셋은 **창 크기만** 바꾼다. 힌지 상태는 `flex` / `open` 이 따로 건다.

**알려진 한계**: 에뮬레이터 37.1.11 + API 36 arm64 조합에서 접었을 때 커버 디스플레이로 전환되는
기능이 동작하지 않는다. 스톡 `7.6in Foldable` 프로필에서도 같아서 설정 문제가 아니다.
그래서 `cover` / `main` 이 `wm size` 오버라이드로 커버 크기를 흉내 낸다.
또 하나, 이 이미지는 기기 상태 번호와 앱이 받는 `FoldingFeature` 상태의 매핑이 한 칸 어긋나 있다
(상태 1 → HALF_OPENED, 2 → FLAT, 3 → 힌지 없음). 그래서 `emu.sh flex` 는 상태 1 을 건다. 앱 쪽 문제가 아니고
실기기에서는 어긋나지 않는다. 힌지 위치(bounds)는 정상으로 전달되므로 플렉스 화면의 위아래 분할은 제대로 검증된다.

## 코드 기준

| 범주 | 기준 |
|---|---|
| 시각 | 자정 기준 분(`Int`). `LocalTime` 컨버터를 만들지 않는다 |
| 날짜 | `LocalDate` ↔ `epochDay: Long` 컨버터 하나 |
| enum | Room 이 이름 문자열로 직접 저장한다. 컨버터를 만들지 않는다. **상수 이름을 바꾸면 기존 데이터가 깨진다** |
| 요일 | `java.time.DayOfWeek.value` (월=1 … 일=7) |
| 배치 실패 | `ScheduleResult.unplaced`로 **반환**한다. 예외를 던지거나 조용히 버리지 않는다 |
| 테스트 | `domain/src/jvmTest/`에 순수 JUnit. 계측 테스트를 만들지 않는다. **DB 와 무관한 계산은 `domain` 으로 빼서 거기서 검증한다** — Repository 에는 읽기·계산 호출·쓰기만 남긴다 |
| DI | 프레임워크 없이 `Application`에서 직접 조립한다. 생성자 주입만 지킨다 |
| 아이콘 | `ui/icons/UjIcons.kt` 에만 둔다. **아이콘 라이브러리를 다시 들이지 않는다** — 쓰는 것만 경로 데이터로 담는다 |

### 파일 길이

**한 파일은 500줄을 넘지 않는다.** 넘어갈 것 같으면 미리 쪼갠다.

쪼갤 때는 **책임 단위로** 자른다. 줄 수만 맞추려고 아무 데서나 자르면 파일 수만 늘고 읽기는 더 나빠진다.

이 프로젝트에서 길어지기 쉬운 곳과 쪼갤 방향은 이렇다.

| 길어지는 곳 | 나눌 기준 |
|---|---|
| `Scheduler.kt` | 빈 슬롯 계산 / 정렬 / 배치 3단계로 파일을 나눈다 |
| `WeekGridScreen.kt` | 시간축·요일헤더·일정블록을 같은 폴더의 별도 Composable 파일로 뺀다 |
| `TaskEditScreen.kt` | 고정 탭과 가변 탭을 각각 별도 파일로 둔다 |
| `PlannerRepository.kt` | `recomputeWeek` 계열과 `resolveMissed` 계열을 나눈다 |

### 중복

**같은 로직을 두 군데에 두지 않는다.** 새로 쓰기 전에 이미 같은 일을 하는 것이 있는지 먼저 찾는다.
붙여넣기로 시작하는 코드는 대개 공용 함수로 뺄 자리다.

단, **우연히 모양만 비슷한 두 코드를 억지로 합치지 않는다.** 한쪽이 바뀔 때 다른 쪽도 반드시 같이
바뀌어야 하는 관계일 때만 합친다. 그렇지 않은 것을 합치면 나중에 풀기 어려운 결합이 된다.
