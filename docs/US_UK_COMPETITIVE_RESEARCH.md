# HeyHood: US/UK competitive research and product gaps

Researched **8 October 2026 (Asia/Kolkata)**, after the Firebase notification source deployment. Scope: community discovery, informal commerce, apartment living, requests, activities, rides, trust and monetization. Provider self-service remains intentionally deferred.

Method: inspected current official product pages, help articles and dated announcements. Compared them with HeyHood's current source/API behavior. This is desk research, not logged-in mobile usability testing or a comprehensive vendor audit. Features described as vendor claims are not independently proven; unpublished features and regional availability remain uncertain. Features explicitly labelled “coming soon” are not counted as shipped. No competitor revenue/user-count claims are needed to reach the recommendations.

## Main finding

HeyHood's largest practical gaps are **trusted membership, real provider contact routing, enquiry outcomes and coordination after a match**. The existing discovery, directory, plans and rides modules give it useful foundations.

Conversational AI discovery is already part of the competitive landscape. Nextdoor describes Ask on its product page and announced improved AI-powered Search on 2 September 2026. That announcement specifies the improved AI search for the **US**; its Local Faves and business-page improvements cover the US, Canada and UK. The product positioning should therefore emphasise:

> Find what’s available inside your apartment community, in your language, and connect with the neighbour offering it.

The strongest candidate differentiators are apartment-level daily availability, provider catalogs, explicit flat/community context, mixed Indian-language search and structured matching across services, requests, rides and activities. This is a proposed positioning, not a claim that no other product has these capabilities. Pilot usage must prove residents prefer it.

Nextdoor also announced neighborhood-level advertising intelligence on 7 October 2026. A demand log is valuable, but collecting local search signals alone is not an exclusive moat. Reliable live supply and successful local matches make those signals more useful.

## Competitor comparison

| Product / market reviewed | Verified on official pages | Lesson for HeyHood | Relevant gap today |
| --- | --- | --- | --- |
| **[Nextdoor](https://about.nextdoor.com/)** — US and UK; specific features vary by region | Neighborhood recommendations, AI Ask; local businesses, Faves, For Sale & Free, events/groups, local news and alerts. Its September announcement describes verified neighbors/businesses and paid Opportunity Alerts routing requests to local providers. | Reputation and provider relevance matter alongside natural-language search. Community demand can feed relevant leads. | Optional operator-reviewed profiles exist, but robust membership/recovery and resident endorsements are incomplete. No provider lead subscription workflow. |
| **[Cobu](https://www.cobuai.com/residents)** — US apartment communities | Private building code, moderated community, events/RSVPs, groups, feed, marketplace and local perks. Current corporate site also markets CobuAI as a leasing/AI-visibility product. | Closest building-level comparison. Member boundaries and organised participation support repeat use and property-operator value. | No invite-code membership, interest circles, integrated plan calendar/reminders or community perks. Compare its resident layer separately from its leasing pitch. |
| **[Front Porch Forum](https://frontporchforum.com/isfpfforme)** — US, primarily Vermont and nearby areas | Address-assigned local forums, private house/apt number, professional moderation, email/web/mobile participation, events and a business directory. Paid plans add directory prominence and keyword alerts. | Local identity, calm communication and accountable moderation can create a useful habit without an endless social feed. | Automated screening exists, but reporting, blocking, moderator queues and appeal handling do not. Digests exist conceptually; operational personalized delivery is not established. |
| **[BuildingLink](https://www.buildinglink.io/solutions/resident-experience/)** — US residential-property software | Announcements, maintenance status, event RSVP/calendar sync, amenity reservations, staff directory, opt-in Neighbornet, services/offers and multiple notification channels. | Operator licensing value often comes from measurable resident coordination and fewer repeated administrative questions. | No complete organiser/plan-confirmation workflow, calendar export or operational RWA notices layer. Full property management would be a large scope expansion. |
| **[Olio](https://olioapp.com/en/)** — UK-origin local sharing | Nearby surplus/free food, giveaway items, selling and volunteer collection from businesses. | Giving away surplus creates useful everyday reasons to return beyond buying services. | No structured free-food/extra-items listings, pickup windows, reservation and collected/expired states. Generic Hood requests only partly cover this. |
| **[Buy Nothing](https://buynothingproject.org/)** — US-origin, global participation | Give/ask/gratitude posts; nearby sharing; private communities for apartments/workplaces/schools, with host approval and invitations. Private-community creation is a subscriber benefit. | A sharing circle can make a large building feel smaller and encourage reciprocal help. | Need/response matching exists; dedicated item availability, lends/returns, gratitude and private community membership are incomplete. |
| **[Liftshare](https://liftshare.com/uk/how-it-works)** — UK; specialist benchmark | Driver/passenger matching, secure messaging, request-to-share and driver confirmation, regular/one-off journeys, profiles/ratings and suggested cost contributions. Payment arranged between sharers. | Discovery should lead to an agreed ride, with both sides acknowledging the details. | Rides can be posted/searched/closed, including recurring school runs, but have no mutual match confirmation, capacity holds or weekly exception/reciprocity workflow. |
| **[Taskrabbit](https://www.taskrabbit.co.uk/)** — UK services; specialist benchmark with US operations | Tasker comparison by price/skills/reviews, identity checks, scheduling, chat/payment/review and support. | A service marketplace exposes what happens after the customer finds a provider. | WhatsApp handoff works, but enquiries are not tracked through response/acceptance/completion; no actual appointment/ordering integration yet. |

Market labels identify the market reviewed, not an assertion that every feature is available globally. BuildingLink/Liftshare/Taskrabbit are adjacent benchmarks; they are not identical to HeyHood's apartment micro-commerce proposition.

## What is already present — avoid building it twice

- Contextual multilingual search and follow-ups, community-scoped catalog/provider pages, daily offering data, flat context and WhatsApp enquiries.
- Availability status with fresh/stale confirmation, sold-out filtering and protected operator updates.
- Hood requests, neighbour responses and explicit requester acceptance; expiry and closure.
- Recurring/reciprocal school-run and shared-cab discovery, with example data.
- Hood Plans interest voting/thresholds, save/follow, optional profiles/privacy switches, operator residency review and My stuff.
- Query/missed-result analytics, promotion analytics, rate limiting and automated abuse screening.
- Android Firebase notification code is now deployed, with opt-in, per-campaign activation, timing/cap rules and separate acceptance/open counts. **Sending is not yet operationally verified**: the public check currently reports no valid server credential loaded, operator endpoints report missing configuration, and a real updated-phone delivery test is still required.

Much of the supply is fictional demo data, and the shared WhatsApp number is an intentional pilot destination. Feature completeness should not be confused with an operational real-supplier marketplace.

## Prioritized gaps

| Priority | Improvement | Smallest useful version | Why / dependency | Success measure |
| --- | --- | --- | --- | --- |
| **P0** | **Real supply and contact reliability** | Operator imports/curates a consented live catalog; each real provider gets its own confirmed contact, hours, flat visibility and freshness. Keep demos visibly separate. Complete Firebase credential/device activation. | The shared test number cannot demonstrate seller-level lead conversion. No provider self-service needed. | Correct-contact rate; active categories with real supply; stale-result rate. |
| **P1** | **Trusted community access + recovery** | Optional phone-based sign-in for participation, community invite/join request, operator approval against community records, cross-device recovery. Keep basic browsing open. | Current browser/app capabilities are device-bound; clearing storage loses access. A checked flat and a professional credential are different facts. | Approved-member activation; successful account recovery; duplicate/spam requests. |
| **P1** | **Enquiry follow-through** | Record an enquiry when contacting a real provider. My stuff asks “Connected?”, “Still waiting?” and “Sorted?”. Allow archive, outcome and useful private feedback. | WhatsApp cannot tell HeyHood whether a message was sent or a purchase completed. Collect resident/provider confirmation instead of treating taps as bookings. | Enquiry-to-response and enquiry-to-confirmed-outcome rates; time to response. |
| **P1** | **Recommendations + accountability** | Resident endorsement after a reported interaction, clear labels for what is verified, report/block action and an operator moderation queue. | Nextdoor/Faves, FPF moderation and Taskrabbit reputation all make trust visible. Avoid fabricated stars or claiming operator approval is a background check. | Useful endorsed providers; reports handled; feedback coverage; repeat enquiries. |
| **P1** | **Ride agreements** | Request to join → driver/passenger confirmation → agreed pickup/time/seats; capacity holds, cancellation and only-participant updates. For school runs, weekly rota and one-off exceptions. | Liftshare illustrates the missing step after discovery. The user’s school-drop/pickup exchange needs an agreement, not just a matching card. | Mutually agreed matches; cancelled/no-show matches; repeat ride pairs. |
| **P1** | **Hood Plans completion** | Organiser confirms threshold/date/venue, capacity/waitlist, one-tap calendar export, reminders, cancellation and attendance confirmation. | Current threshold means interest/awaiting confirmation. Cobu and BuildingLink emphasise RSVP and event coordination. | Interest-to-confirmed-event and RSVP-to-attendance rates. |
| **P2** | **Personal notification preferences and updates** | Separate categories for shop offers, followed-shop stock changes, request responses, ride updates and plan changes. Per-category mute/digest and campaign dismissal. | Current pushes cover operator-enabled promotions, not all followed/transactional events. Build retention around expressed intent. | Useful notification opens; opt-out/dismissal rates; response/event participation—not send count alone. |
| **P2** | **Give / borrow / lost & found** | Reuse the same matching framework for free/sell/borrow/lost/found posts, photos, availability, collection/return windows and resolved status. | Olio/Buy Nothing/Cobu offer frequent reciprocal utility. Generic requests exist, but item lifecycle does not. | Resolved listings; completed pickups/returns; weekly returning residents. |
| **P2** | **Interest circles and newcomer welcome** | Optional school/year-group, hobby or building circles; one personal welcome view, saved interests and tailored suggestions. Use searchable structured activities, not a general broadcast feed. | Cobu/FPF demonstrate local belonging. Helps a resident discover the right people before knowing names. | First useful match in seven days; participation/retention by cohort. |
| **P3** | **External appointment integrations** | Explicit “outside my hood” scope for a specific need; one real diagnostics partner with slots, user-confirmed booking, cancellation and booking status. | Taskrabbit's completed workflow is a model. Orange Labs/Apollo require actual authorized APIs/operating agreements; demo slots are not bookings. Preserve inside-community default for professionals. | Partner-confirmed appointments; booking errors/cancellations; completion. |

Provider self-service is a competitive capability, but **intentionally excluded from the proposed next iteration** under the user's instruction. Operators can do live catalog/contact/stock curation first.

## Recommended next iterations

**Iteration 1 — Make real matches trustworthy.** Onboard real providers through the operator workflow, correct each provider's contact destination with consent, add optional recoverable community identity, enquiry outcomes and report/block. Start lightweight resident recommendations once there are real interactions. Activate and device-test the deployed notifications.

**Iteration 2 — Complete the modules residents already use.** Add mutual ride confirmation/cancellation and the reciprocal school rota; give plans an organiser confirmation/calendar/reminder workflow. Send participant/follower updates according to preferences.

**Iteration 3 — Add a high-frequency neighbour utility.** A compact Give/Borrow/Lost & found layer, with photos and resolution. Add newcomer/interest-circle suggestions if demand supports them. Avoid another permanent top-level tab unless usage warrants it; expose structured types inside the matching/request experience.

**Later — Partner booking and operator integrations.** Add the blood-test booking use case through an actual provider API, then selective RWA notices/amenity integrations if communities request them. Keep full rent, gate/security, packages, property-accounting and general news-feed functionality out of this roadmap for now; those are separate businesses with substantial operating scope.

This ordering follows the original pilot milestones (50 daily searches; 30 WhatsApp taps/week for strong seller demand) while adding outcome measures. A tap remains a lead signal, not proof of revenue. Report actual pilot data before adding additional adoption targets or conversion assumptions.

## Commercial observations

Front Porch Forum's public business-plan page lists a Free plan, Standard at **$11/month billed monthly or $10/month billed annually**, and Enhanced at **$19/month billed monthly or $18/month billed annually**, with directory prominence and keyword alerts. These are displayed US prices on the research date, not comparable Indian price recommendations. They support testing a mix of free discovery and paid, demonstrably useful visibility/lead services.

Nextdoor's September release describes paid Opportunity Alerts; its October release adds neighborhood-level advertiser intelligence. HeyHood can connect missed-demand insights to operator recruitment and, later, relevant provider leads. Do not sell an aggregate “AI moat” story without showing real lead quality and supply freshness. Cobu's private-building model and BuildingLink's operator tools support testing community licensing, but pilots should show improved discovery/coordination before expanding into property operations.

## Sources and evidence notes

1. [Nextdoor product overview](https://about.nextdoor.com/) — Ask, marketplace, events/groups, news/alerts; current overview.
2. [Nextdoor local discovery announcement, 2 September 2026](https://about.nextdoor.com/press-releases/nextdoor-deepens-its-commitment-to-local-business-discovery-and-recommendations) — regional feature availability, verified recommendations, Opportunity Alerts; onboarding/dashboard/feed items explicitly “coming soon” excluded from shipped claims.
3. [Nextdoor advertiser intelligence, 7 October 2026](https://about.nextdoor.com/press-releases/nextdoor-introduces-neighborhood-intelligence-for-advertisers) — public announced capabilities, available on request; marketing performance claims not independently verified or used as HeyHood targets.
4. [Nextdoor small-business product](https://business.nextdoor.com/en-us/small-business) — business pages/posts/recommendations/ads, with eligibility qualifications.
5. [Cobu resident product](https://www.cobuai.com/residents) — property code, events, groups, feed, marketplace, perks, moderation.
6. [Cobu current corporate product](https://www.cobuai.com/) — separates resident engagement layer from AI-visibility/leasing proposition.
7. [Front Porch Forum FAQ](https://frontporchforum.com/isfpfforme) — local forum membership, address privacy and professional moderation.
8. [Front Porch Forum business plans](https://frontporchforum.com/business-plans) — displayed pricing and keyword alerts.
9. [BuildingLink resident experience](https://www.buildinglink.io/solutions/resident-experience/) — operational notices, RSVP/calendar, opt-in resident directory and services/offers. Page retains an older parking-app date; not used to infer a new release.
10. [Olio](https://olioapp.com/en/) — local food/free-items sharing and selling.
11. [Buy Nothing](https://buynothingproject.org/) — give/ask/gratitude and app/group participation.
12. [Buy Nothing private-community creation](https://help.buynothingproject.org/article/29-how-do-i-create-a-private-community) — updated 21 January 2026; subscriber creation, host approval and invitations.
13. [Buy Nothing participation](https://help.buynothingproject.org/article/37-how-do-i-join-a-buy-nothing-group) — updated 23 January 2026; location-based and private communities.
14. [Liftshare workflow](https://liftshare.com/uk/how-it-works) — messaging, request to share, driver confirmation.
15. [Liftshare trust/safety](https://liftshare.com/uk/trust-and-safety) — privacy, profiles/ratings, vehicle/meeting considerations. UK insurance/cost-sharing details must not be transferred to India as legal advice.
16. [Taskrabbit UK](https://www.taskrabbit.co.uk/) — price/skills/review selection, identity checks, scheduling and chat/pay/review.

All accessed 8 October 2026. Some Nextdoor help endpoints were unavailable during research; assertions about verification instead use its accessible dated announcement. Logged-in experiences, exact verification methods, Android/iOS parity, unpublished capabilities and comparative AI/multilingual accuracy were not tested. Recommendations and prioritization are HeyHood product judgments, not competitor claims.
