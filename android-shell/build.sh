#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to a Java 17 or newer JDK}"
bt="$ANDROID_SDK_ROOT/build-tools/35.0.0"
if [ ! -d "$bt" ]; then bt="$ANDROID_SDK_ROOT/build-tools/android-15"; fi
platform="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
build_dir="${HEYHOOD_BUILD_DIR:-/tmp/heyhood-android-build}"
mkdir -p "$build_dir/classes" "$build_dir/dex" "$build_dir/compiled"
"$bt/aapt2" compile --dir res -o "$build_dir/resources.zip"
"$bt/aapt2" link -o "$build_dir/unsigned.apk" --manifest AndroidManifest.xml -I "$platform" "$build_dir/resources.zip"
"$JAVA_HOME/bin/javac" --release 8 -classpath "$platform" -d "$build_dir/classes" src/com/heyhood/app/MainActivity.java
"$JAVA_HOME/bin/jar" cf "$build_dir/classes.jar" -C "$build_dir/classes" .
JAVA_HOME="$JAVA_HOME" "$bt/d8" --min-api 26 --lib "$platform" --output "$build_dir/dex" "$build_dir/classes.jar"
(cd "$build_dir/dex" && zip -q "$build_dir/unsigned.apk" classes.dex)
"$bt/zipalign" -f -p 4 "$build_dir/unsigned.apk" "$build_dir/aligned.apk"
if [ ! -f "$build_dir/debug.keystore" ]; then
 "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$build_dir/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=HeyHood Debug,O=HeyHood,C=IN' -keyalg RSA -keysize 2048 -validity 10000
fi
JAVA_HOME="$JAVA_HOME" "$bt/apksigner" sign --ks "$build_dir/debug.keystore" --ks-pass pass:android --out "$build_dir/HeyHood.apk" "$build_dir/aligned.apk"
JAVA_HOME="$JAVA_HOME" "$bt/apksigner" verify "$build_dir/HeyHood.apk"
echo "APK: $build_dir/HeyHood.apk"
