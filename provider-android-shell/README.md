# HeyHood Provider Android shell

Separate Android 8.0+ pilot app, package `com.heyhood.provider`, named **HeyHood Provider**. It can be installed alongside the consumer HeyHood app without replacing it.

On first launch, paste the complete private provider console link issued by the operator at `https://heyhood-production-b1b8.up.railway.app/admin/providers.html`. The link must include the community, provider ID and `#key=` fragment. Public shop links cannot manage orders. The operator key is never entered into this app. Replaced/expired provider credentials remain rejected by the server; obtain a new private link and use **⋯ → Switch shop**.

The private entry link is encrypted using an Android Keystore AES-GCM key in app-private preferences. Backups are disabled. The console uses the existing server authentication, WebView session storage and persistent PostgreSQL records. This shell does not add phone login or bypass provider access checks. Same-host HTTPS navigation stays in the WebView; supported external links open in another app. File/content access and cleartext/mixed content are disabled. Switch shop removes the saved link and local WebView data; it does not revoke access on other devices or delete server orders.

Orders, menu, dated history, shop profile and the foreground notification inbox come from the live web console. **No FCM service, background order notifications, offline ordering or in-app payments are added.** Permissions: Internet only. First launch needs connectivity and a valid provider link.

## Build

Requires JDK 17+ and official Android SDK platform 35 / build tools 35.0.0. The native shell has no third-party Android dependencies and uses SDK javac/D8/AAPT2/zipalign/apksigner directly.

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk \
JAVA_HOME=/path/to/jdk \
HEYHOOD_PROVIDER_KEYSTORE=/private/path/provider-pilot.keystore \
./build.sh
```

Output defaults to `/tmp/heyhood-provider-build/HeyHood-Provider.apk`; override with `HEYHOOD_PROVIDER_BUILD_DIR`. If the selected keystore does not exist, the script creates a pilot debug key (alias androiddebugkey, standard debug passwords). Preserve it privately for future in-place pilot updates. Use protected release signing for production/Play Store; this build is for sideloaded pilot testing. Never commit an APK or signing key. A separate provider Firebase registration would be needed for future native push.

Validate the link-entry boundary without a device:

```sh
mkdir -p /tmp/provider-link-checks
javac --release 8 -d /tmp/provider-link-checks src/com/heyhood/provider/ProviderLink.java test/com/heyhood/provider/ProviderLinkTest.java
java -cp /tmp/provider-link-checks com.heyhood.provider.ProviderLinkTest
```

Cloud validation: APK compiled, manifest/package inspected and v2/v3 signatures verified. Physical-device installation, keyboard/insets, encrypted persistence across relaunch, and provider console operation still require an Android device test. The existing web console's browser validation does not establish native WebView/device behaviour.
