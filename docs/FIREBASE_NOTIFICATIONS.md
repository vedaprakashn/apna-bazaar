# HeyHood Firebase notifications

Project: **HeyHood / japamala-8284d**. Android package: **com.heyhood.app**. The uploaded `google-services (3).json` matches both. It is Android client configuration, not a sending credential; kept outside Git.

## Railway setup

Open the HeyHood **application service**, not Postgres, then **Variables → New Variable**:

| Variable | Value/source |
| --- | --- |
| `FIREBASE_PROJECT_ID` | `japamala-8284d` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Entire private JSON downloaded at Firebase Console → Project settings → Service accounts → Generate new private key |
| `CAMPAIGN_ADMIN_TOKEN` | Long random operator password; reuse the configured campaign/operator key if one already exists |
| `FIREBASE_PUSH_ENABLED` | Optional, defaults `true`; set `false` to pause all server pushes |
| `FIREBASE_PUSH_INTERVAL_MS` | Optional, defaults `900000` (15 minutes) |

Keep private JSON and the operator password out of chat, Git, frontend code and screenshots. Save/apply the Railway variables. The service account must belong to the configured project. Ensure **Firebase Cloud Messaging API (HTTP v1)** is enabled in that project's Google Cloud APIs. Sender uses scoped service-account OAuth, never a legacy FCM server key. Current deployment accepts the standard `https://oauth2.googleapis.com/token` credential endpoint only. Invalid/missing credentials fail closed without preventing the rest of the app from starting.

## Activate and test

1. Install the new Firebase-enabled APK; the previous shell cannot receive push.
2. Open **My stuff → A little buzz from your hood → Turn notifications on**. Allow Android's permission if prompted. Native preferences stay hidden in ordinary browsers. This subscribes the installation to its current community; no account/login needed.
3. Open https://heyhood-production-b1b8.up.railway.app/admin/campaigns.html on a laptop. Enter `CAMPAIGN_ADMIN_TOKEN` into Admin key. It is not persisted in browser storage.
4. Click **Check Firebase & notification analytics**: credentials should be loaded and opted-in phones >0. This checks configuration, not proof of OAuth/network authorization or phone delivery.
5. On an active, currently eligible campaign, choose **Enable phone push**, then **Send phone notification** and confirm. Notifications are off per campaign by default. Subsequent automatic dispatch runs every 15 minutes. Existing in-chat promotion timing is unchanged.
6. Test with the app foregrounded, backgrounded and phone locked. Tap the notification: the correct shop page should open. Verify the opened count increases once. Test permission denial and turning notifications off. On the device, ensure Google Play services/network access are available.

## Timing, scope and analytics

Notifications use campaign community, start/end window, current offering/provider eligibility and daypart in IST: morning 8–11, lunch 11–15, afternoon 15–17, evening 17–22; anytime still obeys quiet hours. Sold-out offerings do not send. A phone receives at most three accepted/uncertain pushes a day, at least four hours apart, never 10pm–8am. One campaign per device per day; uncertain timeout outcomes are not retried automatically. Phone display can be delayed by Android/FCM. Test send respects these same limits; zero sends can be expected.

Pilot dispatch processes up to 100 installations per cycle, ordering least-recently attempted first; it is not yet a high-volume campaign queue. Tokens older than 60 days without an app sync are excluded; reopen the app to refresh. Unregistered Firebase tokens are disabled. Fictional catalog campaigns carry a Demo title prefix. Pending delivery claims consume quota; crashed/uncertain sends are not blindly repeated.

Analytics distinguishes **accepted by Firebase**, **received while app foregrounded**, **opened**, **failed** and **uncertain**. Background display/read receipts are not inferred. Event updates are idempotent and require that installation's private capability. No phone numbers, resident names or flats are required for subscribing; FCM token is private and not exposed through operator API responses. Capabilities stay in private app preferences; only SHA-256 hashes are stored server-side. Clearing app data creates a new installation.

No browser push, iOS APNs setup, service worker, Firebase login or automatic WhatsApp delivery was added. Existing in-chat system prompts continue locally; push uses operator-enabled campaigns rather than broadcasting every chat prompt or resident request.

## APIs

- `GET /api/push/status`: configuration/project readiness, no credentials.
- `PUT /api/{slug}/push/installations/{id}`: native opt-in and token/community sync, private installation Bearer capability.
- `DELETE /api/push/installations/{id}`: authenticated opt-out/token removal.
- `POST /api/push/installations/{id}/events`: authenticated received/opened event.
- `GET /api/{slug}/admin/push`: protected aggregate stats.
- `PATCH /api/{slug}/admin/push/campaigns/{id}`: protected push toggle.
- `POST /api/{slug}/admin/push/campaigns/{id}/send`: protected confirmed send, eligible opt-in devices only.

Migration V25 stores installation registrations, campaign push switches and deduplicated delivery/open records. Private credential/token contents are never logged.
