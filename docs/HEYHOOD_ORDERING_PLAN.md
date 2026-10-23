# HeyHood native ordering: consumer and provider modes

Decision date: 23 October 2026 (India time).
Status: core persistence implementation is built and under release validation as of 23 October 2026. The real storefront cart, order APIs, private provider console, consumer order tracking and handover codes now exist; see HEYHOOD_HANDOFF.md for exact release evidence. The design below also includes future work: account-bound provider ownership, reliable background order push, pagination and coordinated key rotation. Pilot provider access currently uses the existing operator-issued private capability, not full two-persona phone login. Consumer checkout requires a verified resident profile and ORDER_OTP_SECRET.

## Interactive provider UX preview

`/seller/orders-demo.html` is a standalone, responsive frontend demonstration, not a live order portal. It includes Incoming/In motion/Past orders, order details, accept/reject, preparation/readiness and a simulated handover-code dialog. A separate Consumer preview shows sample alerts and consumer-only codes for trying both roles. Data is fictional and kept only in browser memory; refresh/reset restores it. No order, account, payment, notification or code-verification API is called. Production security and persistence still require the implementation below.

## Product boundary

HeyHood owns discovery, order collection, provider decisions and fulfilment tracking. Native HeyHood checkout does not depend on WhatsApp: it is neither the native order record, provider alert nor verified delivery-confirmation channel. During transition, providers can record orders agreed on WhatsApp, phone, paper, Excel or in person in the same order book; they continue coordinating externally until the consumer is linked to HeyHood. This does not require automatic WhatsApp group or Business account integration. Payments remain directly between the consumer and provider permanently; HeyHood does not collect, settle or refund money. Displayed totals describe the order, not proof of payment. Do not mark orders paid or claim a payment happened.

One app and backend have two role-based views:

- Consumer mode: discover providers, open their public storefront, build a single-provider cart, request fulfilment, track orders and reveal the handover code.
- Provider mode: manage the published menu, see an incoming order inbox, accept/reject requests, manage preparation and readiness, and verify completion.

A person can be both a consumer and an approved provider and switch mode. Selecting Provider mode never grants access to somebody else's shop. Provider-to-account ownership must be approved by the operator and enforced on every API request. Separate shareable URLs are entry points, not separate identity systems or databases.

## Consumer journey

1. Open a provider from discovery/chat or a shared public storefront URL. Browsing remains open without login; the storefront remains standalone without links back to the consumer home.
2. Add published, eligible offerings to a cart on that storefront. Choose quantities and a requested fulfilment date/time. Item cards show price/unit, serving windows and availability. A provider must explicitly offer delivery for that option to appear; otherwise default to pickup. Show any supported delivery charge before submission. Never assume a delivery service exists.
3. The cart stays with that one provider. Switching to another provider offers Keep existing cart or Start a new cart; never silently combine or discard items. Store the draft on the device by community/provider and restore it after login.
4. Before submitting, require an authenticated resident account. The existing phone-login and private-flat verification flow needs production configuration and end-to-end verification. Do not allow an anonymous browser ID to impersonate a verified consumer. Capture a private fulfilment name/flat or address with clear consent to share it with this provider for this order.
5. Review item/quantity/price snapshots, total, requested time, fulfilment type and a short note. State: “This is a request until the provider accepts. Payment is arranged directly with the provider.” Revalidate all prices, provider/community status, dated availability, serving days and requested times on the server. Changed terms return an updated review for explicit consumer confirmation.
6. Tap “Place order request”. Create one reference and show “Sent · waiting for the shop to accept”. A request is not a confirmed order or inventory reservation.
7. Track the order in My orders, accessible from the cart success screen and My stuff. Acceptance, rejection, readiness and cancellation create private in-app alerts. For an accepted order, show the handover code only to its consumer, with “Share this only once you’ve received your order.”
8. When the provider marks Ready, alert the consumer. A pickup says “Ready for pickup”; a delivery says “Ready for delivery”, not “Delivered”. The consumer reveals the code at actual handover. The provider verifies it; both views then show Completed.

## Provider journey

Provider mode has two primary destinations: Menu and Orders. Existing draft/publish item editing remains under Menu. Orders opens with Incoming, Active and History tabs, clear counts and lightweight refresh alerts. No default access to analytics about another shop or private consumer profiles.

Each order card shows reference, time requested, item quantities, total, fulfilment type, the consumer's order-specific fulfilment details and a status timeline. Only the relevant provider can view these details.

- Incoming: Accept or Reject. Rejection needs a brief consumer-safe reason. Never expose the delivery code or allow a provider to retrieve it.
- Accepted: optionally Start preparing. This action is useful for food but not required for every service.
- Accepted/Preparing: Mark ready, creating the consumer alert exactly once for the transition.
- Ready: enter the consumer's handover code, then Verify & complete. No unrestricted “Complete” button bypasses verification.
- History: completed/rejected/cancelled/expired orders, searchable by reference and date. Keep outcomes distinct: accepting, making ready and actual completion are different metrics.

For v1, providers accept or reject the submitted terms; they cannot silently change prices, quantities, fees or times. A later “Suggest a change” workflow requires the consumer to approve the revised terms before confirmation and a new handover code. An accepted order remains valid if its offering is subsequently edited or archived: immutable order snapshots preserve the original agreement.

## Order lifecycle

| Current state | Actor and action | Result |
| --- | --- | --- |
| Requested | Provider accepts | Accepted; generate consumer handover code; notify consumer |
| Requested | Provider rejects, with reason | Rejected; notify consumer |
| Requested | Consumer cancels | Cancelled; alert provider |
| Requested | No answer within the disclosed response window | Expired; notify both; never auto-accept |
| Accepted | Provider starts work | Preparing (optional) |
| Accepted or Preparing | Provider marks ready | Ready; notify consumer |
| Ready | Assigned provider submits valid consumer code | Completed; notify both |
| Accepted/Preparing/Ready | Provider cannot fulfil, with reason | Cancelled; notify consumer; invalidate code |

Consumers can cancel a Requested order immediately. Once accepted, they can request cancellation; the provider explicitly resolves it, rather than the consumer silently cancelling work already underway. Cancellation is orthogonal as a request until resolved; it does not itself mark delivery or payment. Completed, Rejected, Cancelled and Expired are terminal. A consumer/provider dispute or accidental completion needs an operator-audited correction process, not a casual reverse-state button.

The request expiry/fulfilment policies must be visible at checkout. A sensible initial response window is 30 minutes, capped by the requested fulfilment start. Prevent submission when the provider cannot reasonably respond before that time. A long-delayed pickup must not expire an accepted code arbitrarily: expired code renewal belongs to the same order, with only one valid version at a time.

## Handover code design

This code confirms handover; it is separate from the phone-login OTP. It is not a payment credential or proof that payment was made.

- Generate a cryptographically random six-digit code on acceptance. Scope it to the order and a code version; codes for other orders must fail.
- Do not expose the code through provider APIs, lists, notifications, logs, analytics events or public links. A provider sees only a code-entry field.
- Store a keyed verifier for checking and encrypted code material only if needed for the consumer to retrieve it on another device. Keep encryption/HMAC keys in server secrets, outside source and database; support key versioning. Do not store plaintext or a bare unsalted six-digit hash, and do not use the code itself as access to an order.
- Check role, provider ownership, community, current state and code validity under a row lock. Only Ready can complete. Wrong attempts increment and commit a bounded failure counter even when returning an error. Limit attempts per order and caller; after five failed attempts lock verification temporarily and offer the consumer an authenticated code renewal, invalidating the previous version. Never bypass the check as a recovery shortcut.
- Completion is atomic and single-use. Repeated requests cannot create multiple completions or alerts. Cancellation and completion races must resolve to one terminal state.
- Consumer order access remains authenticated; a shareable shop URL, reference or guessed order UUID cannot reveal codes or fulfilment details.
- For remotely delivered services/classes, define an equivalent “service received” moment before enabling this flow. Do not encourage sharing a code before actual fulfilment.

## Alerts and reliability

Create an order event and a durable private notification/outbox record in the same transaction as each accepted transition. Provider order pages show Incoming counts and new-order alerts; consumers see a private timeline and My orders updates. Refresh on focus and poll initially; adding server-sent events is an optimisation, not the source of truth. Failure to play a sound must never hide an incoming request.

Provider alerts must go to the approved provider owner account(s), not a public shop identifier. Consumer alerts go only to the buyer. Notification text should avoid full addresses, phones and handover codes on lock screens.

Background alerts use opt-in Firebase push when identity/device binding, permissions and sending credentials are configured and tested. Existing promotional push delivery is not automatically an order notification subscription. Keep transactional order alerts separate from marketing campaigns and their quotas. Use an outbox with retries and deduplication; the order inbox remains authoritative if push fails. No SMS or WhatsApp delivery is assumed.

## Unified provider console and alert routing

The UX preview now includes desktop left navigation (scrolling compact tabs on mobile): Orders, Menu, History, Notifications and Shop profile. Orders includes new and all unfinished orders, including older carry-over work. History shows completed/declined records with fulfilment-date filters (IST): today, yesterday, last seven days, all dates or a chosen date, plus outcome/name/reference filters. Existing price snapshots are not rewritten by menu edits. The preview's menu and profile changes remain in browser memory and do not update the real provider editor or database.

| Event | Provider inbox | Consumer inbox |
| --- | --- | --- |
| Order request submitted | New-order alert, unread count, deep link to request | Request acknowledgement / timeline |
| Provider accepts or declines | State visible in order queue | Decision alert with order link; accepted code is retrieved privately inside the order |
| Provider marks ready | Ready state | Pickup-ready or delivery-ready alert, never a delivered claim |
| Handover code verified | Completion confirmation | Completion confirmation |
| Expiry or confirmed cancellation | Terminal-state alert | Terminal-state alert |

For production, persist each state change, event and recipient-specific notification/outbox row atomically. Send only after commit, retry failures, deduplicate by order/event/recipient, and link into the authenticated order. Store unread/read state per account and enforce provider ownership and buyer identity. Refresh/reconnect always recovers the current server state; a missed push never loses an order. The prototype simulates this with local unread badges, a provider inbox, a new-order trigger and the separate selected-consumer preview; it does not implement durable delivery.

Use opt-in Firebase transactional push to registered, account-bound devices for background alerts when configured and tested. Keep marketing consent/quotas separate from order alerts, and never put OTPs or full fulfilment details in push payloads. The handover code stays in the authenticated consumer's order, not provider alerts, push or SMS. Browser polling/focus refresh is a foreground fallback; no claim of reliable background delivery without permissions and the platform integration.

## Server/data outline

- Provider membership: approved provider-to-resident-account association, role and revocation timestamp. Migrate pilot edit links to account-based ownership without trusting a client-selected provider ID. Capability links can remain a temporary operator provisioning mechanism; they do not establish phone identity.
- Order: UUID, human reference, community, provider, buyer, status, revision, idempotency key, fulfilment type/time, private fulfilment details, totals, requested/accepted/ready/completed timestamps, rejection/cancellation reason and code metadata. Bound note/quantity/order limits server-side. No payment instrument or settlement fields.
- Order items: immutable offering/category/name/unit/quantity/unit-price/line-total snapshots. Use integer minor units or exact decimal arithmetic; never browser floats for authoritative totals. Archived offerings retain history.
- Order events: immutable actor/action/from/to/revision/time audit record, with no code values. Write events on actual transitions only.
- Checkout uses a consumer-scoped idempotency key. A repeated key with a different cart is a conflict; retries after network loss recover the existing order. Lock and revalidate offerings against concurrent menu publishing. No stock counts/reservations are invented; a future inventory feature must reserve capacity explicitly.
- Authorisation: public APIs read published menus only; consumer APIs read/write the buyer's own orders; provider APIs require approved ownership of that shop. Every lookup enforces community scope. Responses with private data use no-store. Rate-limit requests, status mutations and code verification.

Candidate endpoints:

- `POST /api/{community}/orders` — authenticated order request, idempotent.
- `GET /api/{community}/orders/mine` and `GET /api/{community}/orders/{id}` — buyer views; consumer code retrieval separately authenticated.
- `POST /api/{community}/orders/{id}/cancel` — buyer cancellation/request.
- `GET /api/{community}/provider-workspace/{provider}/orders` — provider inbox with safe, paginated fields.
- `POST .../orders/{id}/accept|reject|prepare|ready|cancel|verify-handover` — explicit, revision-checked state transitions; no arbitrary status setter.
- `GET /api/{community}/provider-memberships/mine` — server-approved mode choices.

## Transitional provider-recorded orders

Adoption is gradual: consumers ordering through HeyHood create requests automatically; providers can also record an order agreed outside the app. Both use the same provider queue and history rather than maintaining a second paper/Excel ledger. The UX preview adds Add order, external source selection, customer name/flat, menu or custom lines, agreed unit prices/quantities, fulfilment date/time, pickup/delivery, notes, a live total and explicit confirmation that terms were agreed outside HeyHood. These records enter Accepted/confirmed, not a fresh consumer request requiring the provider to accept it again. Custom agreed prices are snapshots and do not change the catalog. Excel/paper are manual source tags, not an implemented spreadsheet upload/import or WhatsApp message ingestion integration.

Every production order should preserve source (HeyHood, WhatsApp, phone, paper, Excel, in person), entry method (consumer checkout vs provider record), creator account, timestamps and optional external reference. Only the consumer checkout service can assert automatic HeyHood origin; providers choose external origins. Provider creation must enforce shop ownership, validation, idempotency and auditability. Date/outcome filters, source labels and attribution distinguish native demand from manually captured business; do not count provider entry as a consumer conversion or invent completed revenue. Detect possible duplicates for review, not automatic merging.

An external customer name, flat or phone is not a verified resident account. Keep `consumer_id` unlinked unless the consumer explicitly claims/accepts the order through a verified, authorised account-linking flow. No alerts or handover OTP are said to have reached an unlinked customer. Providers coordinate externally and track fulfilment internally during this period.

For unlinked orders, an explicit “customer received the order” provider acknowledgement may complete the record with `completion_method=provider_recorded`, visibly not OTP-verified. Preserve the actor/time. This cannot bypass OTP for a linked consumer's order: those orders still require Ready + valid consumer-only code. The prototype demonstrates both paths, with no consumer code generated or shown for the unlinked manual case. Production recovery, claims and any later verification must preserve rather than rewrite the original audit trail.

Progression: external order → provider records and manages it → consumer voluntarily joins/claims an order → future orders happen directly through HeyHood. Never create a consumer account, send messages or infer consent merely because a provider entered someone's details. Order collection/tracking remains payment-free for HeyHood throughout.

## Delivery plan and acceptance checks

1. Configure and verify existing phone/flat login; add approved provider account ownership and the Consumer/Provider switch. Browsing stays open. Never allow the role picker itself to register or claim a shop.
2. Implement order schema, snapshot validation, idempotent checkout and state-machine tests before exposing checkout controls.
3. Add provider-page cart, authenticated review/submit, consumer My orders and provider Orders tabs. Keep cart contents across login and navigation. Make totals/time/status readable at 320–1440px, keyboard accessible and usable without sound.
4. Add accept/reject/preparing/ready actions, private event inboxes, expiry and cancellation handling. Test simultaneous decisions and offline retry recovery.
5. Add encrypted, consumer-only handover code retrieval, bounded verification and completion; test wrong/cross-order/replayed/expired codes, failure-count persistence, lockout and cancellation races.
6. Wire transactional push only after real device/role delivery tests. Do not advertise background delivery before those pass.
7. Replace WhatsApp commerce CTAs with View shop / Add to cart / Track order, and remove obsolete enquiry/contact copy. Roll out as a coherent flow once ordering works, rather than leaving the current storefront with neither contact nor checkout. Ride agreements, emergency calling and professional help are separate modules; do not accidentally remove emergency phone access in a commerce change.
8. Pilot with dummy orders in an isolated test community. Demonstrate the complete consumer → provider → consumer loop on two accounts/devices before enabling real requests. Demo providers must not accept genuine fulfilment requests or imply a staffed shop. Publish a clear pilot/demo experience if using fictional sellers.

Release gates include: no private order access by guessing IDs; provider cannot read the code; unpublished/disabled items cannot be ordered for the requested date; price changes require fresh review; duplicate submission creates one request; unsupported fulfilment types are rejected; pending requests never auto-confirm; alerts deduplicate; wrong-code lockouts persist; completion requires Ready and a valid code; no payment/WhatsApp dependency.

Measure storefront → cart → submitted request → accepted → ready → completed, plus rejection/expiry/cancellation reasons and response times, by provider and originating search/campaign. These are in-app events, so traffic attribution no longer depends on someone sending a WhatsApp enquiry. Separate test traffic from real usage and never count acceptance as completed revenue.


## First persistent implementation boundaries — 23 October 2026

Implemented in V30: order snapshots, optimistic revisions with row locks, request deduplication, own-buyer and private-capability shop APIs, explicit transitions, manual unlinked records, private durable inbox/read state, consumer cancellation before acceptance, provider cancellation with reason, 30-minute request expiry and encrypted consumer-only handover codes. Codes require stable `ORDER_OTP_SECRET` (32+ characters), five-attempt lockout, expiry and consumer renewal; provider data/alerts never contain the code. Payments remain external. Public shop CTAs and chat commerce route through the cart. Real console embeds the existing menu editor and persists profile changes. The demo remains a separate browser-memory sample.

Not delivered by this increment: phone-linked provider membership/mode switching, transactional background push/outbox sending, stock reservation, general multi-day scheduling/recurrences, order export/pagination, consumer claiming of external orders, payment handling, SMS and code-key rotation tooling. Server access uses private shop capabilities during the pilot and verified resident tokens for consumers. Consumer request/code writes use the existing 10/minute credential limit. Provider order actions have a separate 60/minute credential limit, configurable through ORDER_PROVIDER_WRITES_PER_MINUTE; the chat limit does not impose a ten-order business capacity limit. Foreground polling is 15 seconds; it is not a background notification guarantee. Lists cap at 200 matching records and dated history is available.
