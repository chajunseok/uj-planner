# U.J planner

갤럭시 Z 플립7용 주간 자동 배치 플래너. 고정 일정만 사람이 넣고, 가변 일정은 앱이 남는 시간에 배치한다.

- **PRD**: `.claude/prd/2609/260918.uj-planner.prd.md`
- **계획**: `.claude/plan/2609/260918.uj-planner.plan.md`
- Application ID: `com.uj.planner` / minSdk 30 / compileSdk 36 / JDK 17

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

## 검증 명령

`Makefile`이 없다. 아래 Gradle 명령이 이 레포의 검증 명령이다.

```bash
./gradlew :app:testDebugUnitTest   # 스케줄러 테스트 — 가장 먼저 돌린다
./gradlew :app:assembleDebug       # 빌드
./gradlew :app:installDebug        # 에뮬레이터에 설치
```

커밋 전에 최소한 `testDebugUnitTest`와 `assembleDebug`가 통과해야 한다.

## 에뮬레이터

```bash
./tools/emu.sh start    # AVD flip7 부팅 (1080x2520 @420dpi, 가로 힌지)
./tools/emu.sh open     # 펼침
./tools/emu.sh flex     # 반 접기 — 플렉스 모드
./tools/emu.sh fold     # 접기
./tools/emu.sh cover    # 커버 화면 크기(1048x948) 강제
./tools/emu.sh main     # 화면 크기 복구
./tools/emu.sh state    # 현재 상태 확인
```

**알려진 한계**: 에뮬레이터 37.1.11 + API 36 arm64 조합에서 접었을 때 커버 디스플레이로 전환되는
기능이 동작하지 않는다. 스톡 `7.6in Foldable` 프로필에서도 같아서 설정 문제가 아니다.
그래서 `cover` / `main` 이 `wm size` 오버라이드로 커버 크기를 흉내 낸다.
자세 전환(OPENED / HALF_OPENED / CLOSED)과 힌지 센서는 정상이므로 플렉스 모드 대응은 제대로 검증된다.

## 코드 기준

| 범주 | 기준 |
|---|---|
| 시각 | 자정 기준 분(`Int`). `LocalTime` 컨버터를 만들지 않는다 |
| 날짜 | `LocalDate` ↔ `epochDay: Long` 컨버터 하나 |
| 요일 | `java.time.DayOfWeek.value` (월=1 … 일=7) |
| 배치 실패 | `ScheduleResult.unplaced`로 **반환**한다. 예외를 던지거나 조용히 버리지 않는다 |
| 테스트 | `app/src/test/`에 순수 JUnit. 계측 테스트를 만들지 않는다 |
| DI | 프레임워크 없이 `Application`에서 직접 조립한다. 생성자 주입만 지킨다 |

스케줄러(`domain/`)는 안드로이드 의존성이 없어야 한다. `import android.*`가 들어가면 잘못된 것이다.
