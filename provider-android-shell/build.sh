#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17 or newer}"
tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
platform="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
build_dir="${HEYHOOD_PROVIDER_BUILD_DIR:-/tmp/heyhood-provider-build}"
keystore="${HEYHOOD_PROVIDER_KEYSTORE:-$build_dir/pilot.keystore}"
mkdir -p "$build_dir/res" "$build_dir/generated" "$build_dir/classes" "$build_dir/dex"
"$tools/aapt2" compile --dir res -o "$build_dir/res"
"$tools/aapt2" link -I "$platform" --manifest AndroidManifest.xml --java "$build_dir/generated" -o "$build_dir/unsigned.apk" "$build_dir"/res/*.flat
mapfile -t sources < <(find src "$build_dir/generated" -name '*.java' -type f)
"$JAVA_HOME/bin/javac" --release 8 -classpath "$platform" -d "$build_dir/classes" "${sources[@]}"
mapfile -t classes < <(find "$build_dir/classes" -name '*.class' -type f)
JAVA_HOME="$JAVA_HOME" "$tools/d8" --release --lib "$platform" --min-api 26 --output "$build_dir/dex" "${classes[@]}"
cp "$build_dir/unsigned.apk" "$build_dir/with-dex.apk"
(cd "$build_dir/dex" && zip -q "$build_dir/with-dex.apk" classes.dex)
"$tools/zipalign" -f 4 "$build_dir/with-dex.apk" "$build_dir/aligned.apk"
if [ ! -f "$keystore" ]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$keystore" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=HeyHood Provider Pilot" >/dev/null 2>&1
fi
JAVA_HOME="$JAVA_HOME" "$tools/apksigner" sign --ks "$keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$build_dir/HeyHood-Provider.apk" "$build_dir/aligned.apk"
JAVA_HOME="$JAVA_HOME" "$tools/apksigner" verify --verbose "$build_dir/HeyHood-Provider.apk"
echo "APK: $build_dir/HeyHood-Provider.apk"
