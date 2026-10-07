# HeyHood Android pilot shell

Opens https://heyhood-production-b1b8.up.railway.app/chatbot/index.html directly. No login. Android 8.0+; internet connection required. Same-site pages stay inside the app; WhatsApp and external links open the installed app/browser. Android Back navigates web history. JavaScript and DOM storage support the website's conversation retention. The app has a retry banner for connection failures and respects system bar insets.

This is a debug-signed pilot APK, not a Play Store release. The signing keystore is outside the repository. Use a stable private release keystore for future distributable updates. No device or emulator runtime test was available in the build environment.

Build using Android SDK platform 35, build tools 35.0.0 and Java 17+:

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk JAVA_HOME=/path/to/jdk ./build.sh
```

APK output defaults to `/tmp/heyhood-android-build/HeyHood.apk`; override with `HEYHOOD_BUILD_DIR`. Install by opening the APK on your phone and allowing installation from your browser when Android prompts.
