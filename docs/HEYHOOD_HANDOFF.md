# HeyHood — living product and engineering handoff

Updated: 7 October 2026 (Asia/Kolkata).
Repository: https://github.com/vedaprakashn/apna-bazaar

This is the functional context, implementation guide, and conversation decision record for a new collaborator. It describes the current implementation, including the changes accompanying this document. It is not a verbatim export of every chat turn: some earlier turns are only available as contextual summaries. Do not treat reconstructed history as exact quotations. The original chat remains the source for exact wording.

## Product and business case

HeyHood began as Apna Bazaar. It makes the informal economy inside large Indian gated communities discoverable: home kitchens, batter sellers, bakers, groceries, classes, workshops, fitness, and home services currently scattered across WhatsApp groups and word of mouth.

A resident asks a natural-language question; Aapta finds relevant providers within the selected community and links to their storefront and WhatsApp. Sellers should register a persistent catalog once, then update daily/weekly availability. HeyHood connects neighbours; it does not currently handle payments, delivery, order fulfilment, or verified bookings.

Pilot communities: MyHome Tridasa (~2,700 flats) and MyHome Sayuk (~3,800 flats), Hyderabad. Founder access to both communities helps onboarding. Search demand and zero-result queries identify missing supply. Lean canvas milestones: 50 daily searches for the next stage; 30 WhatsApp taps per seller per week to validate seller value. These are targets, not achieved production metrics.

Proposed revenue: free listings; seller subscriptions (Starter ₹99/month, Growth ₹299/month, annual ₹2,499); paid broadcast promotions; community/RWA licensing. These are business plans, not implemented billing products. The founder wants eventual Android/iOS native experiences after functional completion.

## Brand and experience decisions

- Brand: **HeyHood — Good things. Close by.**
- Homepage supporting line: **Your hood. Your people. Your next find. ✨**
- AI persona: **Aapta**, selected as a warm, trusted community guide.
- Persona tagline: **Your hood, decoded 👀**.
- Voice: friendly, contemporary, community-specific; avoid stiff language such as “AI discovery”, “Find dairy in my community”, and “Back to discovery”.
- Return link: **← Back to your hood**.
- Search loading line: **Checking your hood… 👀**.
- Colors: electric violet, warm yellow, peach/pastel accents, warm light backgrounds; previous green theme retired.
- Font: self-hosted **Plus Jakarta Sans** variable Latin font; system fallback for Indic scripts. OFL license included alongside the font.
- Logo: generated HeyHood violet/yellow logo in `frontend/assets/heyhood-logo.png`; Aapta also has a small CSS face avatar.

## URLs and deployment

Base: https://heyhood-production-b1b8.up.railway.app

| Page | Path |
| --- | --- |
| Resident chat | `/chatbot/index.html` |
| Analytics | `/admin/index.html` |
| Campaign studio | `/admin/campaigns.html` |
| Provider storefront | `/provider/index.html?community=tridasa&id=PROVIDER_UUID` |
| Android ZIP | `/downloads/HeyHood-Android.zip` |
| Health | `/actuator/health` |

The old `apna-bazaar-production-b1b8.up.railway.app` hostname was removed. Do not distribute it.

Railway project settings bookmark:
https://railway.com/project/c916bca3-683b-40c2-948c-fccb95b7c92e/service/27ba8812-a8df-416f-845d-4fdbf1b208db/settings?environmentId=ccd4ca5d-cc25-479c-9146-03bf020d53f5

Repository root contains `apna-bazaar-git`. Set Railway Root Directory to `/apna-bazaar-git`, using its Dockerfile and railway.toml. Spring Boot serves the static frontend and API from the same origin; PostgreSQL is a separate service. `main` triggers auto-deployment. Check the public HTML/API after merging; a merge alone is not proof deployment finished.

Environment variable names (never copy secret values into this document):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` configure the application datasource.
- Railway Postgres exposes `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`. Reference those into the app's DB variables; they are different services' variable names.
- `APNA_OPENAI_API_KEY`; `OPENAI_MODEL` defaults to `gpt-4.1-mini`.
- `CAMPAIGN_ADMIN_TOKEN` is a separate token for campaign writes. The last explicit production write-auth check found it unset; confirm its current Railway value securely before editing campaigns.
- `CHAT_MESSAGES_PER_MINUTE` defaults to 10.
- Railway injects `PORT`.

Database connection failures were resolved by correcting app DB variables and service references. The app successfully started with PostgreSQL 18.6 in the provided logs. `Database driver: undefined/unknown` alone was not a startup failure.

`heyhood.in` was reported available by NIXI RDAP on 7 October 2026; availability can change. No purchase or custom DNS setup has been performed. Use the Railway domain for now. Future custom domain DNS must use Railway's actual verification/target records; see `apna-bazaar-git/DEPLOY.md`.

## Code layout and local setup

- `apna-bazaar-git/backend`: Spring Boot 3.4.5, Java 21, Maven, Spring AI/OpenAI, PostgreSQL, Flyway, JPA/JDBC, Quartz.
- `apna-bazaar-git/frontend`: static HTML/CSS/JavaScript; chat, provider and admin pages.
- Maven packages `../frontend` into the backend's static resources.
- `apna-bazaar-git/excel/apna_bazaar_daily_menu_template.xlsx`: existing daily-menu template.
- `android-shell`: Android WebView shell source and reproducible build script.
- `docs/HEYHOOD_HANDOFF.md`: this living handoff; root `AGENTS.md` instructs future coding agents to maintain it.

This cloud workspace toolchain:

```sh
cd /workspace/apna-bazaar/apna-bazaar-git
JAVA_HOME=/workspace/tools/jdk-21 \
MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
/workspace/tools/apache-maven-3.9.9/bin/mvn \
  -f backend/pom.xml -s /workspace/setup/maven-settings.xml \
  -B -ntp -Dmaven.repo.local=/workspace/cache/m2 verify
```

These paths are environment-specific, not required on a friend's laptop. Normally use Java 21 and Maven, or follow the Docker instructions. Local cloud PostgreSQL container: `apna-postgres`. Local app instances have used different ports; do not assume a test server's port is the production port. Static frontend testing can use a Python HTTP server, routing API calls to the backend; production is same-origin.

## Resident discovery

- Compact mobile header: Aapta, selected community, Explore and an operator menu.
- Mobile Explore opens a native HTML dialog styled as a searchable bottom sheet. Category selection closes it and submits a question. Close button, backdrop and Escape dismissal; desktop retains visible navigation/category grid.
- Mobile chat fills the visual viewport; only the conversation scrolls. Composer stays at the bottom and respects safe area. VisualViewport resizing supports reduced keyboard space.
- Mobile send control: SVG arrow; laptop: **Find it**. While pending: a square **Stop search** control. AbortController cancels the browser wait; late responses are discarded. This does not guarantee cancellation of server-side OpenAI work/billing.
- Submission blurs the mobile composer. Responses do not call input.focus(); deliberate typing during a pending response is preserved.
- This release: three first-use prompts (**Breakfast?**, **Classes for kids**, **Need a repair**), removed after the first user message, including when restoring history.
- Provider cards show a linked shop name, plain resident name and flat, compact match explanation, and WhatsApp. Only the shop name opens the storefront.
- Pricing limitation: search output identifies a provider and match explanation, not a structured matched-offering ID. This release labels the card price **Catalog from ₹…**, using the provider's lowest listed catalog price. It is not represented as a quoted price for the requested item. Structured offering-level results remain a next iteration.
- Provider storefront shows catalog, prices, schedules and today's menu, with contact action.

## Conversation retention

Chat state is structured JSON in `sessionStorage`, separately per community. It preserves browser session ID, message/card/promotion entries, input draft and scroll offset. Returning from a shop or WhatsApp reconstructs interactive cards rather than injecting saved HTML. Stored promotions retain delivery IDs, impression state and dismissal state; restoration should not inflate analytics.

This is same-tab session retention, not account-backed history, cross-device sync or guaranteed persistence after killing the app. Searches still interpret each request independently; true server-side conversational follow-up memory is not implemented.

This release refreshes restored card contact numbers from the current catalog so cards saved before WhatsApp activation become actionable without deleting the conversation.

## Languages and AI behavior

Supported: English, Hindi, Telugu, Tamil, Kannada, Malayalam, Marathi, Punjabi, Bengali and Assamese, including Romanized and mixed-language forms such as Hinglish, Tenglish and Tanglish/Tamglish. This is a tested target, not a promise every phrasing is correct.

Search first interprets intent, reply language and teaching language, then matches the selected community catalog. Uses low-temperature JSON responses; validates/deduplicates provider IDs. Query normalization preserves Unicode letters and combining marks. Native script should remain native; Latin-only input should receive Romanized replies.

Important regression phrases:

- `hindi tuition`
- `ennaku hindi tuiton venum da` (Tamil/Romanized request for Hindi tuition)
- `idly ivvu bey`, `idly unda ra` (Telugu/Romanized idli requests)
- Hindi-language equivalent and other supported scripts.

Earlier bugs stripped Indic characters and confused the user's language with the language being taught. Guards also separate cooked idli from batter and teaching requests from unrelated providers. Do not invent prices, stock, availability, hours or provider IDs.

## Catalog and ingestion

30 demo providers, 120 offerings, 21 categories per pilot community. Fictional names/flats/prices remain clearly marked Demo. Categories include food/tiffin, batter, juices/shakes, snacks, groceries, dairy, classes, fitness, repairs/cleaning, and added Meat & Seafood, Biryani, Flowers, Ice Creams, Gifts & Crafts.

Flyway migration progression:

- V1–V3: original schema, broadcasts, serving/order windows.
- V4–V7: demo catalog, resident identities/flats, Hindi tuition, four offerings per demo provider.
- V8: initial in-chat promotions.
- V9: five additional categories/providers and offerings.
- V10: category campaign bodies, CTA text, date windows.
- V11: broadcast/promoted campaign mix.
- V12 (this release): assign shared pilot WhatsApp contact to all existing providers.

Do not edit a migration already applied in production. Add a new migration.

Full permanent catalog import from Excel is not implemented. Existing `/ingest/daily-menu` handles daily-menu data; provider registration is separate. A usable seller onboarding/catalog import flow needs validation, preview, update semantics and a UI. Never promise that the existing Excel file alone creates a complete live supplier catalog.

## WhatsApp pilot contact

User explicitly authorized **+91 9740893534** for all current providers. Store normalized digits `919740893534`; open `https://wa.me/919740893534?text=...`. V12 updates existing providers, not every future registration automatically.

Chat and storefront messages include shop, community and flat so the shared test contact can identify the intended listing. The UI states this is a shared pilot contact. It does not claim these fictional residents own that number. Opening WhatsApp does not automatically send the message.

Click analytics are best effort and must not block the contact action. Existing session cards refresh phone data from the catalog. Future production contacts should be per-provider.

Group support is planned, not implemented. A group invite URL is a different destination from a personal `wa.me` number; model an explicit contact type and validated destination. Group links cannot use the same prefilled personal-message flow, and a link click does not prove a group join.

## Promotions and campaigns

21 active category campaigns per pilot community: 6 broadcast, 15 promoted; four older generic campaigns remain paused for history. Each campaign has title, body, CTA text, offering/provider destination, active flag and optional schedule.

Campaign studio creates/edits/enables/pauses campaigns and selects an offering. Writes require `CAMPAIGN_ADMIN_TOKEN`; the token is entered into the studio, not persisted in browser storage. Operator links live in the mobile menu; this visual separation is not access-control enforcement.

In-chat campaign polling is approximately every three minutes when the page is visible and not busy. This release additionally pauses insertion while the user has a draft, focuses the composer, recently typed, reads older chat, or opens Explore/operator menu. Compact cards have one storefront CTA and a dismissal control. Dismissed campaigns stay dismissed for that community/browser session; no new impression is created on restoration.

Impressions require at least 50% visibility. Delivery IDs deduplicate views/clicks. Distinct sessions are not verified unique residents. Campaign dismissal currently has no separate server analytics event.

Native push notifications and automated WhatsApp delivery are future work. Existing digest/broadcast service endpoints should be reviewed before claiming operational delivery; their presence is not proof a messaging channel is configured.

## Analytics and API map

Analytics includes searches, matched vs missed queries, trends, provider profile views, WhatsApp taps, promotion views/clicks and sessions. Times are interpreted in India Standard Time. Demo catalog does not mean analytics events are fabricated.

| API | Purpose |
| --- | --- |
| `GET /api/{slug}/search?q=...&sessionId=...` | AI discovery; response sessionId is the search-event ID |
| `POST /api/{slug}/search/click` | profile_view / whatsapp_tap |
| `GET /api/{slug}/admin/activity?days=14` | analytics used by the UI |
| `GET /api/{slug}/admin/catalog` | available catalog rows; this release includes WhatsApp contact |
| `GET /api/{slug}/providers/{id}/storefront` | community-scoped provider, catalog, schedules, today menu |
| `POST /api/{slug}/providers` | provider registration |
| `POST /api/{slug}/ingest/daily-menu` | multipart daily-menu Excel upload |
| `GET /api/{slug}/promotions` | active campaigns within schedule |
| `POST /api/{slug}/promotions/events` | impression / click |
| `GET /api/{slug}/admin/campaigns` | campaigns including paused |
| `GET /api/{slug}/admin/campaigns/offerings` | campaign offering picker |
| `POST /api/{slug}/admin/campaigns` | authenticated create |
| `PUT /api/{slug}/admin/campaigns/{id}` | authenticated edit |

Community slugs: `tridasa`, `sayuk`. Provider/event/delivery IDs are UUIDs in real calls. Search browser-session ID and search-event ID are different concepts.

Rate limit: 10 search messages per minute per IP per app instance, rolling window. HTTP 429 includes Retry-After. Railway forwarded-IP handling is explicitly gated by Railway environment detection; local requests use the remote address. In-memory limiter is not shared across replicas. Authentication and broader write-endpoint protection are still pilot limitations.

## Android shell

Source in `android-shell`: Java Activity + WebView, no login; loads the live chatbot. Same-origin pages remain inside; WhatsApp/external links open another app/browser. Android Back uses WebView history. Includes connection retry, JavaScript/DOM storage, system-bar insets, HTTPS-only app traffic. Android 8.0+.

Build with JDK 17+ and Android SDK platform 35/build-tools 35.0.0:

```sh
ANDROID_SDK_ROOT=/path/to/sdk JAVA_HOME=/path/to/jdk android-shell/build.sh
```

Output defaults to `/tmp/heyhood-android-build/HeyHood.apk`; override `HEYHOOD_BUILD_DIR`. Debug keystore stays outside Git. Current hosted ZIP includes the APK and INSTALL.txt. It is a debug-signed pilot, not a Play Store release; signature/archive checks passed, physical-device runtime validation is limited to user feedback. This shell is not the planned rich native Android/iOS implementation.

The user initially requested no APK in GitHub. After chat-local download links failed, the user explicitly authorized hosting the ZIP on the HeyHood website; that artifact is now versioned to deploy it. Do not interpret this as permission to publish future unrelated binaries or private conversation exports publicly.

## Validation and release practice

Maven verify and the meaningful existing unit checks passed in this release. Local browser checks cover:

- 320–430px mobile, desktop, reduced viewport, no horizontal/page overflow.
- Explore search, no-match state, dismissal, category selection and resizing to desktop.
- History/draft/community isolation without repeated searches.
- Arrow/Find it/square states; cancellation; no stale response; success/error/429 recovery.
- Mobile keyboard focus behavior and deliberate typing during a response.
- Three starter prompts; removal and restoration.
- Promotion pause while typing/reading, dismissal persistence, true impression count.
- V12 locally applied: 30 providers per community use the shared number; restored cards enable contact; chat/storefront handoffs include context; analytics errors do not prevent navigation.

Browser scripts/screenshots in `/workspace/setup` are diagnostic workspace artifacts, not part of the repository's permanent test suite. Retest important flows on a physical Android phone. Do not describe frontend request cancellation as server AI cancellation, or WhatsApp clicks as confirmed orders.

Direct pushes to `main` returned remote Internal Server Error despite valid permission, no protection/rules, and a fresh source-only history. Pushing a feature branch and merging a PR worked. The APK was not the cause. Use that workflow while the direct-push issue persists. A user's `temp` README commit also succeeded; do not overwrite their unrelated branch.

## Next iteration — proposed, not implemented

1. Real seller onboarding and Excel catalog preview/import, then activate one genuine supplier end to end.
2. Offering-level search results with exact matched item, price and freshness/availability cues.
3. Saved providers/offerings and an intentional clear/new-chat action, with an agreed local persistence policy.
4. Contextual follow-up questions such as “anything cheaper?” or “weekends only?” backed by scoped server conversation state.
5. Contact types for individual WhatsApp and group invite links; per-provider configuration.
6. Storefront search/category filters and a sticky contact action where needed.
7. Error/offline UX and physical-device keyboard/Back/deep-link tests.
8. Operator authentication/roles, endpoint protection, shared rate limiter before scaling replicas.
9. Native app implementation and notifications after functional completion; retain clean API contracts.

## Conversation and decision record

The following reconstructs the available context chronologically. It is a decision log, not a complete word-for-word chat transcript. Update it as new requests are implemented.

### Setup and initial deployment

- User selected `vedaprakashn/apna-bazaar`, main, and requested cloud-environment onboarding; retried setup.
- User asked whether Anthropic integration was live and requested replacing it with OpenAI; configured an OpenAI key.
- User explained the invisible community economy and provided the lean canvas/business milestones.
- User requested running the app, chatbot/analytics URLs, Excel template, GitHub push and deployment.
- Railway build initially failed because root directory was unset; corrected to `/apna-bazaar-git`.
- PostgreSQL connection refused; reviewed app DB references versus PostgreSQL PG variables. Startup succeeded after correcting values.
- Used Railway-generated public domain before having a custom domain.

### Discovery, catalog and analytics

- User requested dummy data covering all categories, richer offerings, responsive and more polished UI, analytics for searches/misses, and future native mobile extraction.
- Requested resident name/flat in cards, WhatsApp Message contact action, linked shop storefront with complete menu, and only the shop name linked.
- Requested smaller homepage hero and bookmarked the Railway project.
- Reported Hindi tuition mismatch and Romanized Tamil/Telugu failures; specified ten languages plus Hinglish/Tenglish/Tanglish and other mixed forms.
- Requested richer demo providers/offers, promotional nudges around every three minutes and exposure analytics.
- Selected persona Aapta after rejecting earlier name options.
- Requested 10 messages/minute limit; friendlier community-specific copy and tile questions.
- Added meat, biryani, flowers, ice cream, gifts; requested shop-specific campaigns and direct storefront CTAs, managed through a back office eventually.
- Deferred native promotional notifications until the mobile app stage.

### Rebrand and mobile shell

- Selected HeyHood — Good things. Close by.; requested .in availability, logo and a more Gen-Z visual theme.
- User renamed the Railway service/domain to heyhood-production-b1b8.up.railway.app.
- Requested a newer font: selected self-hosted Plus Jakarta Sans.
- Requested conversation retention across outgoing links; implemented per-community sessionStorage state.
- Requested persona tagline refresh: Your hood, decoded 👀.
- Requested visible Campaigns tab, then accepted moving it into a compact mobile operator menu in the later iteration.
- Requested Android APK with no login, opening the live URL; built a minimal WebView shell.
- Requested better mobile categories and WhatsApp-like one-screen chat with bottom composer.
- Requested a generic homepage line: Your hood. Your people. Your next find. ✨.
- GitHub direct pushes repeatedly failed; source-only cleanup/fresh clone did not help. User tested browser README edit on temp successfully. Feature-branch push and PR merge succeeded.
- Chat-local APK/ZIP links were not downloadable. User subsequently authorized website hosting; public ZIP was verified byte-for-byte.

### Mobile usability iterations

- User accepted compact header, searchable Explore sheet, small seller cards; deployed and verified live.
- User selected arrow-only mobile send and Find it on laptops.
- Requested ChatGPT-like square icon during processing; implemented a real browser-request stop action, tested abort/late responses.
- Reported keyboard opening when a response arrived; removed unconditional input.focus(), blur mobile submission, preserve deliberate typing.
- Requested replacing Back to Discovery; deployed Back to your hood.
- Asked whether all eight proposed usability items were implemented and requested the next iteration. Audit found starter prompts and gentler promotions incomplete; this release finishes those and clarifies catalog starting prices.

### Current handoff release

- User authorized +91-9740893534 for all providers to test live WhatsApp buttons; wants individual/group contacts in future. V12 assigns normalized shared test contact; cards/storefronts identify pilot routing and include shop/community/flat in the message.
- User requested all functional context and conversation in a living .md for a friend/collaborator. Created this contextual handoff and AGENTS.md maintenance instructions. No guaranteed verbatim transcript export tool is available in this workspace.
- Collaborator access: repository owner can invite their friend's GitHub account via repository Settings → Collaborators / Manage access. Chat collaboration depends on the chat client's sharing features; the coding assistant cannot add someone to this chat or assume they inherit a workspace/session automatically.

- User requested the loading line also connect to “hood”; changed Finding your local gems to Checking your hood… 👀 in this release.
