#!/usr/bin/env bash
# 에뮬레이터 조작 헬퍼
#   ./tools/emu.sh start | open | flex | fold | state
#   ./tools/emu.sh size <프리셋|reset>    — 화면 크기를 바꿔 다른 기기를 흉내 낸다
#
# ponytail: 커버 화면은 wm size 오버라이드로 흉내 낸다.
#   에뮬레이터 37.1.11 + API 36 에서 접힘 시 커버 디스플레이 전환이
#   동작하지 않아서다. 에뮬레이터가 고쳐지면 `size cover` 는 지워도 된다.
#
# ponytail: `flex` 는 기기 상태 2(HALF_OPENED)가 아니라 1 을 건다.
#   이 에뮬레이터 이미지는 기기 상태 번호와 창 라이브러리(FoldingFeature)의 매핑이 한 칸 어긋나 있어서
#   1 → HALF_OPENED, 2 → FLAT, 3 → 힌지 없음 으로 앱에 전달된다. 실기기는 어긋나지 않는다.
#   `flex` 를 걸어도 앱이 플렉스 화면으로 안 넘어가면 이미지가 고쳐진 것이니 2 로 되돌린다.
set -euo pipefail

# SDK 위치는 OS 마다 다르다. 이미 잡혀 있으면 그걸 쓰고, 아니면 이 OS 의 기본 위치를 찾는다.
if [ -z "${ANDROID_HOME:-}" ]; then
  case "$(uname -s)" in
    Darwin)               ANDROID_HOME="$HOME/Library/Android/sdk" ;;
    MINGW*|MSYS*|CYGWIN*) ANDROID_HOME="$LOCALAPPDATA/Android/Sdk" ;;
    *)                    ANDROID_HOME="$HOME/Android/Sdk" ;;
  esac
fi
export ANDROID_HOME
[ -d "$ANDROID_HOME" ] || { echo "Android SDK 를 못 찾음: $ANDROID_HOME — ANDROID_HOME 을 직접 지정한다" >&2; exit 1; }

# 윈도우에서는 실행 파일에 .exe 가 붙는다.
EXE=""
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) EXE=".exe" ;; esac
ADB="$ANDROID_HOME/platform-tools/adb$EXE"
S=(-s emulator-5554)

# 화면 크기 프리셋. 같은 AVD 에 wm size/density 만 덮어써서 다른 기기의 창을 흉내 낸다.
# 레이아웃 분기(커버 판정·읽기 폭 제한)를 보는 용도이고, 실기기 검증을 대신하지는 못한다.
preset() {
  case "$1" in
    flip7)     echo "1080x2520 420" ;;  # 이 AVD 의 본화면 — 펼친 플립
    cover)     echo "1048x948 420"  ;;  # 플립·레이저의 정사각형 커버
    phone)     echo "1080x2340 440" ;;  # 흔한 바형 폰
    tablet)    echo "1600x2560 240" ;;  # 태블릿, 펼친 북형 폴더블 — 1067x1707dp
    landscape) echo "2340x1080 440" ;;  # 가로로 돌린 폰 — 창 높이가 낮다
    foldcover) echo "904x2316 420"  ;;  # 북형 폴더블의 길쭉한 커버
    *)         return 1 ;;
  esac
}

case "${1:-}" in
  start)
    "$ANDROID_HOME/emulator/emulator$EXE" -avd flip7 -no-boot-anim &
    echo "부팅 대기..."
    for _ in $(seq 1 70); do
      [ "$("$ADB" "${S[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && { echo "준비 완료"; exit 0; }
      sleep 5
    done
    echo "부팅 시간 초과" >&2; exit 1 ;;
  open)  "$ADB" "${S[@]}" shell cmd device_state state reset ;;  # 센서 값(180도)대로 — 앱에는 FLAT
  flex)  "$ADB" "${S[@]}" shell cmd device_state state 1 ;;      # 앱에는 HALF_OPENED (위 주석)
  fold)  "$ADB" "${S[@]}" shell cmd device_state state 1 ;;      # CLOSED — 이 이미지에서는 flex 와 같다
  size)
    if [ "${2:-}" = "reset" ]; then
      "$ADB" "${S[@]}" shell wm size reset; "$ADB" "${S[@]}" shell wm density reset; exit 0
    fi
    read -r size density <<<"$(preset "${2:-}")" || { echo "프리셋: flip7 cover phone tablet landscape foldcover reset" >&2; exit 1; }
    "$ADB" "${S[@]}" shell wm size "$size"
    "$ADB" "${S[@]}" shell wm density "$density" ;;
  state)
    "$ADB" "${S[@]}" shell cmd device_state state | tr -d '\r'
    "$ADB" "${S[@]}" shell dumpsys window displays | grep -m1 -oE "cur=[0-9]+x[0-9]+" ;;
  *) sed -n '2,4p' "$0"; exit 1 ;;
esac
