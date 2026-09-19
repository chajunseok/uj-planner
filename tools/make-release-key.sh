#!/usr/bin/env bash
# 릴리스 서명 키를 만들고 keystore.properties 를 쓴다. 한 번만 돌린다.
#
# 키는 레포 밖(~/.uj-planner/release.jks)에 둔다. 레포가 public 이라 키와 비밀번호가 커밋되면 안 된다.
# 이 키를 잃으면 같은 앱으로 업데이트할 수 없다 — 새 키로 서명한 APK 는 덮어 설치가 안 돼서, 앱을 지우고 다시 깔아야 한다.
set -euo pipefail

KEY_DIR="$HOME/.uj-planner"
KEY="$KEY_DIR/release.jks"
ALIAS="uj-planner"
PROPS="$(cd "$(dirname "$0")/.." && pwd)/keystore.properties"

if [ -e "$KEY" ]; then
    echo "이미 키가 있습니다: $KEY" >&2
    echo "덮어쓰지 않습니다. 새로 만들려면 기존 키로 서명한 앱을 더 업데이트할 수 없다는 것을 확인하고 직접 옮기세요." >&2
    exit 1
fi

read -r -s -p "키 비밀번호 (6자 이상): " PASS; echo
read -r -s -p "한 번 더: " PASS2; echo
[ "$PASS" = "$PASS2" ] || { echo "두 입력이 다릅니다." >&2; exit 1; }
[ "${#PASS}" -ge 6 ] || { echo "6자 이상이어야 합니다." >&2; exit 1; }

mkdir -p "$KEY_DIR"; chmod 700 "$KEY_DIR"
# 비밀번호를 인자로 넘기면 ps 에 보인다. 환경 변수로 넘긴다.
export UJ_KEY_PASS="$PASS"
keytool -genkeypair -keystore "$KEY" -storetype PKCS12 -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=U.J planner" \
    -storepass:env UJ_KEY_PASS -keypass:env UJ_KEY_PASS
chmod 600 "$KEY"

umask 177
cat > "$PROPS" <<PROPS_EOF
storeFile=$KEY
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
PROPS_EOF

echo
echo "키:   $KEY"
echo "설정: $PROPS (gitignore 됨)"
echo "이 두 파일과 비밀번호를 레포 밖의 안전한 곳에 백업해 두세요."
echo "릴리스 APK: ./gradlew :app:assembleRelease → app/build/outputs/apk/release/app-release.apk"
