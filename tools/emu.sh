#!/usr/bin/env bash
# 플립7 에뮬레이터 조작 헬퍼
#   ./tools/emu.sh start | open | flex | fold | cover | main | state
#
# ponytail: 커버 화면은 wm size 오버라이드로 흉내 낸다.
#   에뮬레이터 37.1.11 + API 36 arm64 에서 접힘 시 커버 디스플레이 전환이
#   동작하지 않아서다. 에뮬레이터가 고쳐지면 `cover`/`main` 은 지워도 된다.
#
# ponytail: `flex` 는 기기 상태 2(HALF_OPENED)가 아니라 1 을 건다.
#   이 에뮬레이터 이미지는 기기 상태 번호와 창 라이브러리(FoldingFeature)의 매핑이 한 칸 어긋나 있어서
#   1 → HALF_OPENED, 2 → FLAT, 3 → 힌지 없음 으로 앱에 전달된다. 실기기는 어긋나지 않는다.
#   `flex` 를 걸어도 앱이 플렉스 화면으로 안 넘어가면 이미지가 고쳐진 것이니 2 로 되돌린다.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export JAVA_HOME="${JAVA_HOME:-$HOME/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
ADB="$ANDROID_HOME/platform-tools/adb"
S=(-s emulator-5554)

case "${1:-}" in
  start)
    "$ANDROID_HOME/emulator/emulator" -avd flip7 -no-boot-anim &
    echo "부팅 대기..."
    for _ in $(seq 1 70); do
      [ "$("$ADB" "${S[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && { echo "준비 완료"; exit 0; }
      sleep 5
    done
    echo "부팅 시간 초과" >&2; exit 1 ;;
  open)  "$ADB" "${S[@]}" shell cmd device_state state reset ;;  # 센서 값(180도)대로 — 앱에는 FLAT
  flex)  "$ADB" "${S[@]}" shell cmd device_state state 1 ;;      # 앱에는 HALF_OPENED (위 주석)
  fold)  "$ADB" "${S[@]}" shell cmd device_state state 1 ;;      # CLOSED — 이 이미지에서는 flex 와 같다
  cover) "$ADB" "${S[@]}" shell wm size 1048x948; "$ADB" "${S[@]}" shell wm density 420 ;;
  main)  "$ADB" "${S[@]}" shell wm size reset;    "$ADB" "${S[@]}" shell wm density reset ;;
  state)
    "$ADB" "${S[@]}" shell cmd device_state state | tr -d '\r'
    "$ADB" "${S[@]}" shell dumpsys window displays | grep -m1 -oE "cur=[0-9]+x[0-9]+" ;;
  *) sed -n '2,4p' "$0"; exit 1 ;;
esac
