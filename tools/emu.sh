#!/usr/bin/env bash
# 플립7 에뮬레이터 조작 헬퍼
#   ./tools/emu.sh start | open | flex | fold | cover | main | state
#
# ponytail: 커버 화면은 wm size 오버라이드로 흉내 낸다.
#   에뮬레이터 37.1.11 + API 36 arm64 에서 접힘 시 커버 디스플레이 전환이
#   동작하지 않아서다. 에뮬레이터가 고쳐지면 `cover`/`main` 은 지워도 된다.
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
  open)  "$ADB" "${S[@]}" shell cmd device_state state 3 ;;   # OPENED
  flex)  "$ADB" "${S[@]}" shell cmd device_state state 2 ;;   # HALF_OPENED
  fold)  "$ADB" "${S[@]}" shell cmd device_state state 1 ;;   # CLOSED
  cover) "$ADB" "${S[@]}" shell wm size 1048x948; "$ADB" "${S[@]}" shell wm density 420 ;;
  main)  "$ADB" "${S[@]}" shell wm size reset;    "$ADB" "${S[@]}" shell wm density reset ;;
  state)
    "$ADB" "${S[@]}" shell cmd device_state state | tr -d '\r'
    "$ADB" "${S[@]}" shell dumpsys window displays | grep -m1 -oE "cur=[0-9]+x[0-9]+" ;;
  *) sed -n '2,4p' "$0"; exit 1 ;;
esac
