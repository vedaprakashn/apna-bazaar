# HeyHood Android pilot

Android 8.0+ WebView app opening https://heyhood-production-b1b8.up.railway.app/chatbot/index.html. Same-origin pages stay in the app; WhatsApp and external links open externally. Conversation storage, back navigation, connection retry and system insets are retained.

Version 1.1 adds Firebase Cloud Messaging for project `japamala-8284d`, package `com.heyhood.app`. Notifications are off by default. Open **My stuff → A little buzz from your hood → Turn notifications on**. Android 13+ asks for notification permission. Turning notifications off removes the server token and deletes the FCM token. Phone settings can disable them too; the app synchronizes this when reopened. No resident login/profile is required. Switching the selected community updates device subscription when a page loads.

Foreground messages use the same Android notification channel as background messages. Tapping opens the linked shop; external paths are rejected. Opens and foreground receipts are retried on reconnect. Neither Firebase acceptance nor foreground receipt proves reading. Firebase Analytics is not bundled or enabled.

## Build

Use JDK 17+, Android SDK platform/build tools 35, and the checked-in Gradle wrapper (8.11.1, checksum pinned), AGP 8.9.2, Google Services plugin 4.4.4 and Firebase BoM 34.4.0.

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk \
JAVA_HOME=/path/to/jdk \
HEYHOOD_GOOGLE_SERVICES_JSON=/private/path/google-services.json \
HEYHOOD_KEYSTORE=/private/path/debug.keystore \
./build.sh
```

The build copies the client JSON into ignored `app/google-services.json`. Never commit client keys, service-account JSON, APKs or signing keys. Output: `/tmp/heyhood-android-build/HeyHood.apk`, or `HEYHOOD_BUILD_DIR`. `HEYHOOD_KEYSTORE` selects an existing Android debug keystore (alias androiddebugkey, standard debug passwords) so pilot updates keep their signature; otherwise Gradle uses its standard local debug key. Use a separate protected release signing configuration for Play Store distribution.

The APK was built and its signature verified in the cloud environment; Android device notification delivery/permission behavior needs a physical-device test. See [Firebase setup](../docs/FIREBASE_NOTIFICATIONS.md). iOS/APNs and browser Web Push are not implemented in this pilot.
