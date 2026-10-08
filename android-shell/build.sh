#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK (platform/build tools 35)}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17 or newer}"
if [ ! -f app/google-services.json ]; then
 : "${HEYHOOD_GOOGLE_SERVICES_JSON:?Set this to your downloaded google-services.json path}"
 mkdir -p app
 cp "$HEYHOOD_GOOGLE_SERVICES_JSON" app/google-services.json
fi
./gradlew --no-daemon --console=plain :app:assembleDebug
build_dir="${HEYHOOD_BUILD_DIR:-/tmp/heyhood-android-build}"
mkdir -p "$build_dir"
cp app/build/outputs/apk/debug/app-debug.apk "$build_dir/HeyHood.apk"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify "$build_dir/HeyHood.apk"
echo "APK: $build_dir/HeyHood.apk"
