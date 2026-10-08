# HeyHood: product thesis, community matching model and India strategy

**Founder working document · 8 October 2026 · India-first, Hyderabad pilot**

Brand: **HeyHood — Good things. Close by.** Conversation guide: **Aapta**.

This document explains the business HeyHood is trying to build, how its modules fit together, and what would make the proposition credible to an Indian startup investor. It incorporates the founder's latest thesis: **the accumulated context of a community, combined with the ability to turn unmet demand into reliable supply, is the candidate moat**.

Read alongside [the investor brief](HEYHOOD_INDIA_INVESTOR_BRIEF.md) and [the engineering handoff](HEYHOOD_HANDOFF.md). Product facts come from the founder's requirements and current repository/release record. Competitor observations come from the sources listed at the end. Recommendations, targets and financial examples are labelled; they are not achieved traction or independently verified market estimates.

## Contents

1. The thesis in one minute
2. The problem and whose problem it is
3. What business we are building—and the boundaries
4. How to bucket the product
5. One matching model, different roles and lifecycles
6. What “community context” contains
7. Turning missed demand into outside supply
8. Trust, identity and reputation
9. What is live versus planned
10. Competition in the Indian market
11. How the moat can become real
12. Go-to-market and community liquidity
13. Revenue and unit economics
14. Market sizing without an inflated TAM
15. Metrics and an evidence-led roadmap
16. Investor assessment and founder decisions
17. Sources and research limitations

## 1. The thesis in one minute

A large apartment community already contains a small economy. Residents cook, teach, repair, organise activities, share rides and help each other. Much of that supply is invisible because discovery depends on knowing the right person or being in the right WhatsApp group.

HeyHood makes that economy discoverable. A resident describes a need in their own language; the product uses the community's current context to find a suitable person, offering, ride or activity. The resident connects, and feedback helps the community understand which matches were useful.

When there is no suitable supply, the same demand becomes an operator opportunity. Repeated, qualified requests for car washing, bicycle repairs or another service can justify onboarding a provider who serves that community. Over time, HeyHood can connect existing community supply and help create missing supply.

**Recommended category description:** a community discovery and service-matching platform, beginning with India's large gated communities.

**Recommended business thesis:** help residents find and connect with relevant supply; build a trusted community context; use unmet demand to activate additional services and generate measurable provider value.

**Recommended opening wedge:** frequently needed food and everyday services, with classes as a useful recurring category. Rides and plans share the model and can improve retention; they should not make the opening pitch sound like an unfocused “everything app.”

The current product is a working pilot implementation, substantially populated with fictional providers and a shared test WhatsApp destination. Product capability is not evidence of marketplace liquidity, provider earnings or customer willingness to pay.

## 2. The problem and whose problem it is

### The resident's problem

The problem is not a lack of WhatsApp messages. It is the inability to reliably answer a question at the moment of need:

- “Who has idli or fresh batter this morning?”
- “Is there Hindi tuition for my child on weekends?”
- “Who can do makeup for a function tomorrow?”
- “Anyone heading to the airport tonight at 11?”
- “Can we organise bicycle servicing if enough neighbours want it?”
- “Is there a doctor living in our community, and which flat?”

The answer may exist, but be scattered across a seller's messages, a residents' group, a notice, a personal contact and an expired menu. New residents face the sharpest version of this problem: they do not yet know the providers, groups or local shorthand.

The current workaround imposes several costs: asking the same question repeatedly, waiting for someone to notice it, checking whether an old message still applies, and guessing whether a provider can meet a specific time or language requirement. The cost is inconvenience and missed local opportunities; the pilot must measure its frequency rather than assume every household experiences it daily.

### The provider's problem

A home seller, tutor or small service team can already deliver value but may lack distribution inside the community. Reposting in multiple groups is effort, visibility is temporary, and a new resident may never encounter their offering.

The useful promise is persistent discoverability plus accurate availability—not “create a listing and never do anything again.” A fresh menu, schedule, sold-out status or visiting-service window still needs maintenance. HeyHood must make that maintenance easy or provide operator support while self-service is deferred.

Providers want relevant enquiries that become business, not impressions detached from outcomes. A car-wash team may care more about five households on the same Sunday than fifty unqualified clicks over a month.

### The community operator's problem

An operator cannot easily see what is missing. Repeated requests in different languages and groups do not become a clear sourcing decision. The operator needs to know:

- Which needs are genuinely unmet?
- How many different households want the service, and when?
- Is supply absent, temporarily unavailable, poorly indexed or simply not responding?
- Would enough demand support a visiting provider or a community service day?
- Did bringing in that provider lead to useful connections and repeat demand?

This operator may initially be the founder. Later it could be a community coordinator, an RWA-appointed person or a HeyHood operations team. These are operating choices, not established roles in every community today.

### The outside service provider's problem

A local mechanic, AC technician, car-wash team or cleaning business needs demand that is geographically concentrated and actionable. Travelling to a neighbourhood without knowing whether anyone will book is inefficient. Access rules and scheduling also matter.

HeyHood's potential value is a qualified route into a specific community: known service demand, a feasible window, consented enquiries and evidence of outcomes. It should not promise exclusive access, guaranteed jobs or RWA endorsement unless those conditions actually exist.

### Who uses, benefits, permits and pays?

| Party | Main job | Potential value | Economic role |
| --- | --- | --- | --- |
| Resident/seeker | Find someone or something suitable now | Less discovery effort; relevant local choices | Free discovery is the initial proposition; not necessarily the payer |
| Resident provider | Be found for an offering | Persistent reach and relevant enquiries | Candidate subscriber or promotion buyer |
| Visiting service provider/team | Serve concentrated community demand | Lower uncertainty and potentially more jobs per visit | Candidate lead, campaign, service-day or subscription payer |
| Community/RWA | Permit and govern participation/access | Useful resident utility and better visibility of needs | Distribution/trust partner; a possible licensing customer, not an assumed payer |
| HeyHood/operator | Maintain context and activate supply | More useful matches and sustainable revenue | Bears acquisition, curation, infrastructure and support costs |

One resident can be a seeker in the morning, a provider in the evening and a ride participant at night. Roles belong to an interaction; they are not permanent labels attached to a person.

## 3. What business we are building—and the boundaries

HeyHood starts with **discovery and connection**, then adds the minimum coordination needed for each type of match. It can evolve toward bookings and transactions where the economics and product evidence support them.

It is not currently a delivery company, a citywide professional-services operator, a payments platform or a confirmed-booking agent. Its Android app is a web shell loading the responsive experience; a rich native Android/iOS implementation remains future work.

**Explicit scope boundaries:**

- Maintenance tickets, security operations, society accounting and managed complaint resolution are out of scope. MyGate and similar systems already address community operations.
- A general-purpose social feed or compulsory broadcasting is not the product. Resident needs should be searchable and targeted.
- Provider self-service remains deferred; operator-assisted onboarding is acceptable for the pilot.
- WhatsApp remains the pilot communication channel. In-app conversations are a later step, not a current requirement to migrate every conversation.
- Routine people/professional-directory results remain community-resident focused. External urgent-service numbers are a separate help path.
- Outside-community services are a deliberate expansion of supply serving the community, not permission to silently replace every resident result with a citywide listing.

A useful rule is: **add a module when it reuses the community context and matching model, and has a specific coordination need that HeyHood can serve well.** Do not add it merely because residents might do that activity somewhere.

### The long-term platform path

The founder's ambition is a community-aware platform for discovering and accessing services across many communities: initially closer to **Justdial-style discovery and provider subscriptions**, and later, where proven, closer to **Urban Company-style service booking and delivery coordination**. UrbanClap is the earlier brand name for Urban Company; use the current name in investor comparisons.

These are different operating models. A directory monetises reach/leads. A booking marketplace coordinates commitments and transactions. A managed-service business takes on a deeper role in service standards, fulfilment and support. Moving between them requires operating capability and agreements, not just adding a booking button.

| Horizon | Consumer proposition | Provider/partner proposition | Economic model to test | Evidence needed before expansion |
| --- | --- | --- | --- | --- |
| **Community discovery** | “Tell us what you need in your hood” | Be found for relevant, current offerings | Subscriptions and labelled campaigns | Repeat discovery and useful connections |
| **Supply activation across communities** | Find a suitable service even when resident supply is missing | Reach qualified demand and viable service windows in specified communities | Coverage subscriptions, qualified activations/leads | Repeatable sourcing, actual provider value and paying renewal |
| **Booked services** | Select and confirm a real slot/job | Predictable accepted work and clearer coordination | Transparent booking/completion-linked economics | Real capacity, confirmation/cancellation workflow and outcome attribution |
| **Partner commerce at home** | Arrange diagnostics collection, wellness or prescription fulfilment through one front door | Reach relevant, consented household demand | Contracted referral, distribution or marketplace economics | Authorised integrations, partner serviceability and reliable fulfilment/status |

Subscription “people” should be defined in the pitch. The initial candidate payer is the provider/business, while consumer discovery stays open. Resident premium subscriptions would be a separate hypothesis requiring demonstrated household value.

The health/wellness/medicine path is a long-term opportunity, not current functionality or existing partner commitments. A direct consumer experience can be delivered through third-party partners; investors would generally distinguish that distribution/marketplace model from a D2C brand selling its own products. HeyHood can offer a direct household channel without claiming to manufacture medicines or own a clinical service.

## 4. How to bucket the product

The category tree should help browsing, but product strategy should be organised around the type of interaction. “Food,” “beauty” and “airport” are topics; they do not describe how a match becomes useful.

| Interaction bucket | Current/proposed examples | What is matched | Distinct coordination requirement | Business role |
| --- | --- | --- | --- | --- |
| **Get something** | Food, batter, groceries, flowers, gifts | A need to an item and provider | Quantity, price/unit, stock, order/pickup window | Frequent discovery; potential seller leads/promotions |
| **Get something done** | Tuition, makeup, tailoring, car wash, AC repairs | A need to capability and available service time | Location, duration, service scope, availability and relevant credentials | Core provider value; strongest proposed outside-supply revenue path |
| **Go together** | Airport cab sharing, recurring school runs | Complementary travel needs/offers | Route, direction, time, seats, recurrence and agreement | Resident utility/retention; monetisation is not assumed |
| **Do something together** | Workshops, hobby sessions, community service days | Interest to an activity or organiser | Threshold, date, venue, capacity and organiser confirmation | Demand aggregation; possible paid supplier participation |
| **Help or share** | Resident professionals, urgent contacts; future give/borrow | A need to a relevant person, contact or item | Urgency, consent, scope; future collection/return rules | Trust and useful community participation; urgent help is not an upsell funnel |

Use topic categories underneath these buckets. For example, “Beauty & personal care” groups makeup, bridal makeup, mehendi, manicure and pedicure; “Clothing & tailoring” groups tailoring and pleating. The exact consumer navigation can remain Shops, Rides, Plans and Help while the internal product framework is consistent.

### Three axes that prevent confusion

Every interaction also has:

1. **Supply origin:** resident provider, visiting provider approved to serve the community, or explicitly requested outside partner.
2. **Economic arrangement:** paid service/product, cost sharing, reciprocal help or free participation.
3. **Time pattern:** available now, a one-off appointment, recurring schedule or threshold-based activity.

A school pickup exchange is recurring, reciprocal, resident-to-resident travel. A Sunday car-wash camp is paid, visiting-provider supply, potentially aggregated into one service window. A blood test is a paid external appointment requiring actual partner availability and confirmation. They share matching infrastructure but need different commitments.

## 5. One matching model, different roles and lifecycles

### Shared conceptual objects

This is a target product model, not a claim that every current database table has already been unified.

| Object | Meaning | Example |
| --- | --- | --- |
| Community | Geographic/membership boundary and local rules | MyHome Tridasa |
| Resident/person | A participant with membership and privacy choices | A resident linked to flat T2-102 |
| Provider | A person, shop, team or organisation offering capability | A home kitchen or visiting car-wash team |
| Offering | What can be supplied on an ongoing basis | Dosa, Hindi tuition or exterior car wash |
| Availability instance | When and how an offering is actually available | Sunday 9–12, six service slots |
| Need/request | A seeker's explicit intent and constraints | “Car wash this Sunday morning” |
| Match | A candidate pairing with a reason and trust context | Relevant team, window, scope and price basis |
| Commitment | What the parties explicitly agree | Confirmed ride; booked slot; organiser-confirmed event |
| Outcome | What was reported or confirmed afterwards | Connected, completed, cancelled or still waiting |
| Gap | A qualified need without suitable current supply | Four households seeking bike servicing next weekend |

The current system has structured provider offerings, rides, plans, requests and contacts with separate lifecycles. Reusing concepts does not mean forcing all of them into one generic listing that loses important fields.

### Shared discovery sequence

**Understand the need → establish community/scope → extract constraints → check current supply and trust → present suitable choices → connect or request agreement → collect an outcome.**

Aapta is the conversational entry point, not the business itself. Residents must also be able to browse or use direct urgent contacts. Supported languages and Romanised mixtures reduce input friction; matching still depends on accurate intent interpretation and grounded provider data.

### The central consumer interface: a WhatsApp-like chat

The founder's chosen front door is **one simple chat window**. A resident should express a need naturally rather than first learn which module or supplier system handles it. “Hindi tuition,” “airport tomorrow,” “a car wash Sunday” and a future “book my blood-test collection” start in the same place.

Modules remain useful views, not separate products the resident must mentally assemble. Chat can surface a storefront, an eligible ride, a plan threshold or a future appointment card. Browse shortcuts and direct urgent contacts support chat; they should not displace it with a dense dashboard.

For transactions, conversation gathers intent while structured cards handle consequential choices: service/date/price, a real slot, the responsible provider, cancellation terms and an explicit confirmation action. Availability and booking status must come from authoritative systems. An AI sentence is not confirmation that a partner accepted a job.

The experience should remember the resident's community and conversation, remain usable with the mobile keyboard, and label whether supply lives in or merely serves the hood. More backend integrations should make the front door easier, rather than create a growing list of separate consumer interfaces.

### Different definitions of completion

| Interaction | A useful discovery | A meaningful commitment | Evidence of completion |
| --- | --- | --- | --- |
| Food/product | Correct item/provider at an appropriate time | Provider accepts the request/order | Collection/delivery confirmed by parties; not tracked as a verified transaction today |
| Tuition/service | Appropriate capability, timing and scope | Trial, appointment or service agreed | Resident/provider reports the session/job happened |
| Ride | Compatible route, time and seats | Both parties accept pickup and sharing terms | Ride outcome reported; future workflow |
| Plan/workshop | Relevant activity with visible conditions | Organiser confirms; participant agrees to attend | Attendance/provider completion; future workflow |
| Professional/help contact | Correct, consented contact in the right scope | Person agrees to assist where appropriate | Private self-report; urgent help does not wait for an AI workflow |

Current “Yes, connected” feedback proves only a resident's reported connection. It does not prove a purchase, finished repair, safe ride or booked appointment.

## 6. What “community context” contains

The founder's moat thesis is stronger than “we have a database of sellers.” Community context is a maintained understanding of how supply and needs behave in one place.

| Context layer | Useful information | Why it improves a match |
| --- | --- | --- |
| Membership/location | Community, flat association, privacy preferences | Establishes who belongs and what “near me” means |
| Supply capability | Offerings, specialties, languages, pricing basis | Knows what a person can provide, beyond the shop name |
| Current availability | Menus, stock, hours, service visits, schedules | Avoids sending residents toward yesterday's or unsuitable supply |
| Constraints and norms | Pickup points, entry approval, allowed service windows | Makes proposed arrangements feasible locally |
| Demand | Normalised needs, dates, frequency and unmet intent | Reveals missing services and useful sourcing opportunities |
| Relationships/outcomes | Reported connections, repeat use, consented reviews | Helps distinguish visible providers from useful providers |
| Participation | Plans, thresholds, reciprocal arrangements | Can concentrate demand and coordinate a viable service visit |

Not all these layers are fully implemented. Membership invitations, OTP recovery, mutual ride agreements and richer completion signals are planned. Do not present them as an existing proprietary context dataset.

The valuable asset is **accurate, permissioned, regularly refreshed context with observed outcomes**. A stale catalog and an undifferentiated query log are weak assets. The resident's trust is lost if the product invents a flat, misidentifies a language, overstates availability or treats a promoted provider as the organic answer.

Community context also includes sensitive information. Keep individual searches and enquiries private, publish only appropriate aggregated demand, and do not use health or child-related details as an indiscriminate advertising audience. Phone OTP later authenticates a person; it is not consent to distribute their phone number to providers.

## 7. Turning missed demand into outside supply

This is the most important extension of the product thesis:

> Repeated needs tell HeyHood which provider to bring into a community, when to bring them, and whether the provider created value.

### The operating loop

1. **Observe a miss.** Capture the requested category, community, meaningful constraints and whether suitable results were available.
2. **Classify why it missed.** Separate absent supply from search failure, stale data, unsuitable timing, incorrect scope or non-response.
3. **Aggregate equivalent needs.** Combine “car wash,” “gaadi dhona” and equivalent multilingual requests without losing date/location constraints.
4. **Qualify intent.** Ask residents whether they want a specific service window or an introduction. A query is an interest signal, not a booking.
5. **Source a provider.** Use local teams or businesses that can genuinely serve the community. Check identity/contact, service capability and required entry arrangements.
6. **Run a small activation.** Offer a clear service window or a plan with the provider's minimum viable participation threshold.
7. **Connect and measure.** Track explicit connections, accepted requests and later completed jobs where a reliable workflow exists.
8. **Retain useful supply.** Invite a repeat window or ongoing listing based on outcomes, not merely the original miss count.

These are recommended operating/product steps. Current missed-search analytics do not already execute this entire sourcing workflow.

### What three or four misses should trigger

The founder's proposed threshold is a good **early sourcing trigger**, not proof of commercial demand and not an automatic provider activation rule.

**Recommended initial rule:** review three or four relevant misses in one community for the same normalised service within a rolling seven-day window. Check how many distinct resident/household sources they represent and whether their requested dates overlap. The seven-day window and qualification checks are proposed defaults to test, not live automation.

Until flat membership/phone identity is established, be honest about the unit: distinct sessions or device profiles are not necessarily distinct people or households. After private membership verification, household-level counts are more meaningful. Multiple people from one flat may still create only one car-wash booking.

| Observation | Interpretation | Recommended action |
| --- | --- | --- |
| One resident asks four versions of the same question | Possibly one need or poor retrieval | Resolve the intent; do not claim four customers |
| Four households want car wash on Sunday | Promising concentrated demand | Confirm service scope/window; test a visiting team |
| Four misses for a service already in the catalog | Likely indexing/language or availability problem | Fix matching/data before recruiting duplicate supply |
| Four needs spread over unrelated dates | Real category interest, weak batching signal | Seek flexible supply rather than announce a service day |
| Three connects but no provider response | Reliability gap rather than absence | Investigate contact/availability; avoid promoting ineffective supply |

### Worked car-wash example

Suppose four different households search for a car wash in Tridasa during the week. This is a hypothetical example, not measured pilot demand.

- HeyHood groups the requests into exterior car wash, the same community and Sunday morning.
- Each household can opt into “Interested in a Sunday wash?” The platform does not automatically disclose their details or broadcast to everyone.
- A local team says it needs six confirmed cars to make the visit worthwhile. That requirement comes from the team's economics; it is not a universal threshold.
- The operator can create a plan: Sunday car-wash window, clear price/scope, six confirmations needed, with the team marked as a visiting provider.
- If the threshold is met, the provider/operator confirms the slot and entry arrangements. Interest votes alone must not appear as booked work.
- After the visit, the platform gathers outcomes and relevant reviews; repeat demand can support the next Sunday slot.

The same pattern applies to bicycle servicing, AC maintenance, chimney cleaning, tailoring collection or a makeup session. Jobs with very different durations, skill needs or visit costs should not be bundled just because their categories are adjacent.

### Partner integrations: the later household-commerce path

A diagnostics request illustrates the long-term model. A resident asks, “Can I get a blood sample collected at home tomorrow between 4 and 5?” HeyHood must establish the requested service, community/pincode, date/window and necessary user-approved details, then query an authorised partner for real serviceability and slots. It presents the provider, price basis and a feasible choice; the resident confirms; only a successful partner response can create a confirmed appointment.

Medicine delivery and wellness packages have different input and fulfilment requirements. A pharmacy partner may need a prescription and its own validation; a wellness package needs a clear description rather than an AI-invented health claim. Keep that information out of general provider-advertising and public demand views. Partner checkout/consent can handle sensitive inputs where appropriate rather than putting them into general chat history.

Useful integration contracts include serviceability, catalog/eligibility, live availability, price, explicit confirmation, payment responsibility, booking/order status, cancellations/refunds and support ownership. API retries must not create duplicate bookings. If availability cannot be checked, the system must offer an enquiry or say it cannot confirm—not manufacture slots.

Tata 1mg, Apollo 24|7/Apollo Diagnostics and Netmeds already describe relevant consumer services [S18–S21]. These are competitor/partner candidates, not HeyHood integrations. Their consumer pages do not establish access to partner APIs, margins, commercial terms or serviceability for a particular community. The founder's named Orange Labs possibility also needs direct validation; no operating/API claim is made here.

This horizon should start with one demanded, authorised workflow. It is not a reason to build several regulated integrations before discovery and supplier economics work.

### The revenue opening

For service providers, the proposition is **qualified, concentrated, repeatable demand**, not access to raw resident data. Candidate revenue mechanisms include:

- A provider subscription for recurring discoverability and reporting.
- A clearly labelled promotional placement or community service-day campaign.
- A fee for an explicitly consented, qualified enquiry, once qualification and attribution are reliable.
- A fee linked to an accepted/completed job, only after the workflow, provider agreement and outcome measurement support it.

Begin with one paid experiment, not every mechanism at once. With direct WhatsApp handoff, commissions have an attribution and collection problem: the transaction happens outside HeyHood. A subscription or measured service-day campaign may be easier to test than promising a percentage of unobserved transactions.

Keep the visiting provider's origin clear. Do not fabricate a resident name/flat for them or dilute the resident-only professional directory. A future “serves your hood” listing is a different scope from “lives in your hood.”

## 8. Trust, identity and reputation

Trust has several distinct claims. They should never collapse into one unexplained green tick.

| Claim | What it establishes | What it does not establish |
| --- | --- | --- |
| Future phone OTP | Control of a phone number; recovery/sign-in | Flat membership, professional skill or safe conduct |
| Future private flat invitation | Access to an invitation delivered privately for a flat | The identity of every household member or their professional qualifications |
| Current operator resident review | Operator checked name/flat against community records | OTP identity or background clearance |
| Current provider check | Identity, flat association and contact checked with private evidence | Purchase verification, qualification/licence or service quality guarantee |
| Current rating/review | A resident reports an actual interaction | Independently proven purchase or completed job |
| Future professional credential check | A defined licence/credential validated for a specific activity | Every other trust claim |

The agreed membership direction is **private flat invitation delivery**, followed later by phone OTP for login/recovery. A shared PDF exposing every flat's code would only prove access to the PDF, not membership of a specific flat.

Use separate resident accounts linked to a flat: a household can have multiple people. Keep a stable resident ID; phone numbers and addresses can change. Decide household invitation reuse, expiry and revocation before implementing redemption. These mechanisms are not yet built.

For future visiting providers, the check must establish business/person identity, service coverage, contact and permission to serve the hood. It must not falsely imply residence in a flat. Adapting that supplier-type trust workflow is part of the proposed outside-supply expansion, not a shipped change to the current resident-oriented check.

Provider ratings should be interpreted with review counts and recent context. A 5-star provider with one review is not equivalent to a long record of useful service. Current demo providers remain fictional and cannot be marked verified. Legitimacy of negative feedback matters; reputation cannot become a tool to hide ordinary poor experiences.

For school rides or health/professional services, a matching result is particularly far from a completed, safe arrangement. Explicit participant agreement and the relevant professional/guardian checks remain necessary in the future workflow; HeyHood must not imply they already exist.

## 9. What is live versus planned

Status is based on the current source/release record as of 8 October 2026. “Live” means the implementation is deployed, not that it has demonstrated commercial adoption.

| Capability | Current status | Important limit |
| --- | --- | --- |
| Aapta/community search | Live | Ten Indian languages plus mixed/Romanised input are supported design targets; language and semantic accuracy need ongoing real-query testing |
| Provider storefronts, catalogs and menus | Live | Much supply is fictional; search-card starting prices are not necessarily quotes for the requested item |
| WhatsApp enquiries with English search context | Live | Shared pilot number intentionally receives demo enquiries; a tap is not a sent message or booking |
| Shops/categories, Help, Rides, Plans and My stuff | Live | Distinct modules with shared conversational discovery; not a fully unified transaction engine |
| Recurring/shared rides and school-run examples | Live discovery/posting | Mutual acceptance, capacity holds and completed-ride tracking remain future work |
| Plans/threshold voting | Live | Threshold means sufficient interest/awaiting organiser confirmation, not automatic event execution |
| Optional device-bound profiles; saves/follows; community requests | Live | No phone OTP or reliable cross-device recovery yet |
| “Did you connect?”; editable ratings/reviews | Live | Private, self-reported outcomes; one review per device profile/provider—not guaranteed one per human |
| Provider checks and review moderation | Code deployed | Operator actions require `CAMPAIGN_ADMIN_TOKEN`; last public check was locked pending configuration |
| Missed-search and promotion analytics | Live foundations | Qualified household demand, automatic external sourcing and measured completed jobs are not implemented |
| Firebase/Android promotion push | Code deployed | Last recorded server/device readiness was incomplete; do not claim operational delivery until configured and phone-tested |
| Private flat invitations; phone OTP | Agreed future direction | Not implemented |
| Outside-provider activation workflow | Founder thesis / proposed next business experiment | Requires real providers, consent, qualification and service-window operations |
| External diagnostics/other bookings | Planned | Requires actual authorised provider APIs/agreements and user confirmation |
| Provider self-service; rich native Android/iOS | Deferred | Android is currently a web shell; no shipped native iOS app claimed |
| Billing, payments and proven recurring revenue | Not established | Proposed prices and revenue streams are hypotheses |

## 10. Competition in the Indian market

The relevant market is not just “other AI neighbourhood apps.” A resident can satisfy the need through an existing group, a community app, a specialist marketplace or a familiar local business.

### Competitive layers

1. **Default behaviour:** WhatsApp groups, calls, neighbour referrals and notice boards.
2. **Community incumbents:** MyGate, NoBrokerHood, ADDA and ApnaComplex/ANACITY.
3. **Local/service discovery:** Justdial and home-services providers/platforms such as Urban Company and NoBroker Home Services.
4. **Vertical fulfillment:** food/grocery platforms, ride-sharing platforms and event marketplaces.
5. **Potential partners:** many of the same local providers and community operators who could help activate supply.

The competitor/partner label can change by use case. MyGate may be the installed incumbent and a private invitation distribution channel; that does not establish a commercial integration or guarantee it will remain a partner.

### Source-based comparison

Features below are observations from official pages or publisher app listings. They are vendor descriptions, not independently tested guarantees. The final column is an opportunity to prove, not an assertion that a competitor lacks every related feature.

| Alternative | Observed proposition | Competitive strength / investor concern | HeyHood's testable opening |
| --- | --- | --- | --- |
| **WhatsApp / WhatsApp Business** [S1] | Direct communication, business profiles, product/service showcasing and business messaging; current site also promotes Business AI | Existing habit and relationship distribution; an extra app can add friction. “Conversational AI” alone is weak differentiation | Discover relevant supply across the hood before knowing whom to message; keep WhatsApp handoff initially and demonstrate better discovery/outcomes |
| **MyGate** [S2–S4] | Resident communication, community tools, Buy & Sell and an advertising platform | Installed community relationships, trusted access context and a commercial distribution surface. It is a direct strategic threat, not merely a maintenance app | Deep offering/time/need context and a repeatable process that converts gaps into reliable supply; demonstrate usage that listing/broadcast behaviour does not resolve |
| **NoBrokerHood + NoBroker Home Services** [S5–S7] | Society management, resident communication/forums; adjacent household services marketplace | Community reach combined with an existing services proposition. Outside-supplier activation is not automatically an uncontested category | Focus on community-specific unmet needs and concentrated service windows; prove incremental qualified jobs and lower provider acquisition/visit cost |
| **ApnaComplex / ANACITY** [S8–S9] | Community collaboration, notices/polls/document repository; Bazaar page presents provider offers and service-provider listing/recommendation actions | Community software already exposes social/commercial functions. The founder must explain why a new layer is worth adopting | Current offerings, multilingual intent, cross-module constraints and measurable follow-through rather than treating classifieds as a sufficient directory |
| **ADDA** [S10] | Publisher listing describes resident communication and society management, including bills, visitors and amenities; markets an ad-free experience | Another familiar community-platform comparison; investor likely asks about distribution and duplication | Compare actual in-community workflows during a field audit; do not base differentiation on an unverified claim that ADDA cannot do a feature |
| **Justdial** [S11] | Broad local business discovery, categories, free listings and advertising | “Local discovery + leads” already exists at national scale; breadth and provider acquisition matter | Boundary, membership, offering freshness and precise service windows inside one hood; lower noise and prove useful introductions |
| **Urban Company** [S12] | At-home beauty, cleaning, repair and other professional service categories | Residents may prefer a specialist service experience with a familiar brand; a discovered phone number is a shallower outcome | Resident supply and demand-led community batching where genuinely advantageous. External jobs must meet relevant reliability expectations |
| **Quick Ride / sRide** [S13–S14] | Route-based carpooling, recurring matching and vendor-described verification/payment functions | Matching algorithms and ride coordination are established. “Airport carpool” is not a novel moat | Existing community membership and reciprocal school/household arrangements; demonstrate useful matches beyond route proximity and build explicit agreements |
| **Swiggy** [S15] | Food and grocery ordering/discovery | Habit, selection and ordering convenience compete for the same household need | Specific home/resident offerings and community pickup/relationship value; do not claim to beat a logistics marketplace at delivery speed |
| **BookMyShow** [S16] | Discovery and ticket booking for events/activities | Paid-event discovery is established; residents can use familiar external options | Small, local, threshold-led plans/service days and informal organisers; membership and feasible participation are the distinction to test |
| **Tata 1mg / Apollo 24|7 / Apollo Diagnostics / Netmeds** [S18–S21] | Home sample collection, health packages and/or pharmacy commerce described on their respective consumer pages | Strong specialist destinations with existing service/order flows; a chat wrapper needs a genuine distribution or experience advantage | A consented community front door, appropriate service windows and real partner confirmation; contracts/API access remain unestablished |
| **Existing local service teams and resident groups** | Founder-observed offline alternatives; no single audited vendor | Strong word of mouth and low/no discovery fee; supply may see no need to pay | Add incremental business, concentrated routes and measurable repeat demand without forcing the supplier to abandon existing relationships |

### Competitive conclusion

There is real overlap. MyGate's resident page explicitly describes Buy & Sell; its advertising platform already sells access to community audiences. ApnaComplex Bazaar presents service offers and provider-listing/recommendation actions, so external services through a community platform are also an existing competitive surface. NoBroker's ecosystem combines community and services products. Quick Ride describes AI matching. WhatsApp Business currently markets AI-related tools. It would be inaccurate to pitch HeyHood as the first platform to combine community, commerce, local leads or AI.

The stronger pitch is an operationally specific hypothesis:

> HeyHood understands what a particular community can provide today and what it repeatedly cannot provide, then uses that context to create useful local matches and activate missing services.

Whether this is superior to an incumbent feature depends on adoption, context quality, provider outcomes and repeatability. It remains to be demonstrated.

### Indian field research still required

Desk research cannot establish an exclusive feature gap. Before using “competitors cannot do this” in a fundraising deck:

- Test the relevant MyGate/NoBrokerHood/ADDA/ANACITY workflows with consent in actual communities where they are installed.
- Try the same ten resident needs across the existing channels and HeyHood; measure answer quality, elapsed time and successful connections.
- Interview home sellers and service teams about their existing lead sources, margins, missed opportunities and willingness to pay.
- Establish whether RWA permission is sufficient for discovery and visiting-service pilots; do not assume formal integrations or exclusive access.
- Check city/category availability and terms on specialist platforms. A national marketing page does not prove a particular service is available in Tridasa.

## 11. How the moat can become real

### Four assets, with different strength

**1. Distribution access.** Founder residence in both proposed pilot communities helps with initial onboarding and trusted introductions. This is an advantage at launch, not a durable national moat. It must be converted into an onboarding playbook that others can execute.

**2. Maintained community context.** Accurate offerings, current availability, approved service coverage, local constraints and consented trust signals improve answers. Collecting context is useful; keeping it correct and getting providers to update it is the harder part.

**3. Evidence about unmet demand and outcomes.** Knowing that a service is requested, why it is missing, whether a team filled the gap, and whether people returned is more valuable than raw search volume. It can improve sourcing and timing decisions. This evidence should be aggregated and permissioned, not a justification for selling resident identities.

**4. Supply activation capability.** A repeatable ability to bring reliable teams into a community at the right time, with sufficient demand and measurable provider economics, is an operational advantage. Relationships, quality control and batching can be harder to copy than the interface.

### The reinforcing loop

More useful supply → better matches → more repeat resident intent → clearer gaps → more effective sourcing → better provider economics → more reliable/current supply.

Rides and plans can strengthen community participation, but the economic loop must be validated rather than inferred from module count. A new community does not automatically inherit the previous community's liquidity. Cross-community benefits may come from a visiting team's route/capacity, a reusable catalog or a proven sourcing playbook.

### What is not a moat on its own

- An OpenAI integration or a friendly persona.
- A Gen-Z interface, category tiles or a WebView app.
- A list of providers that can be copied or reconstructed.
- A zero-result log without qualification or action.
- “People live in gated communities” as a market insight.
- A low acquisition cost measured only while the founder personally does the work.

Community incumbents already possess some resident access and context. HeyHood must show deeper commercial context and better supply activation, not imply that the existence of community data is unique.

## 12. Go-to-market and community liquidity

### Start dense, not wide

The proposed pilots are MyHome Tridasa (~2,700 flats) and MyHome Sayuk (~3,800 flats), founder-supplied figures that have not been independently verified. They are not claimed active user counts, signed RWA customers or proof of access to all residents.

Start with one community and enough genuine supply in a few frequent categories to make the first searches useful. Use the second community to test whether the playbook transfers beyond the founder's initial operating effort. Do not interpret adding a community row in the database as launching an economically active marketplace.

### Suggested launch sequence

1. Confirm community permission, identity/privacy practices and what can be distributed through existing channels.
2. Curate genuine providers, current offerings, contacts and service windows. Keep demo supply separate from live proof.
3. Test high-frequency resident queries across English and local/Romanised language forms before promoting the product.
4. Invite a small resident cohort. Use newcomer introductions, relevant provider referrals and approved QR/invitation placements rather than paid mass acquisition first.
5. Collect connection feedback and inspect why matches fail. Track operator effort per community.
6. Run one qualified gap-to-provider experiment, such as a car-wash or bicycle-service window.
7. Measure resident repeat use, completed-service evidence where available, provider incremental economics and actual payment for the experiment.
8. Repeat in the second community with a non-founder operator where possible.

No messages to residents or suppliers are authorised or sent by this document. It is a proposed operating plan.

### The liquidity measure

A community is liquid for a need when the resident can find suitable, current supply with a reasonable chance of connecting in the required time. Provider count alone is insufficient.

Measure liquidity by category and time: morning food may be healthy while evening repairs are empty; a large tuition directory may still have no Hindi weekend slot. Track the eligible results, supply freshness and reported outcomes that correspond to actual resident intents.

### Entry and expansion discipline

Launch food/everyday services first; use rides/plans as relevant utilities rather than simultaneous separate growth businesses. Expand to adjacent communities in a geographic cluster so visiting providers can serve more than one community without unrealistic travel assumptions.

A builder/RWA partnership may distribute the product, but sales cycles, committee changes, data permissions and installed incumbents can slow it. Budget human effort. A community champion is a distribution strategy to test, not free labour that automatically scales.

## 13. Revenue and unit economics

### Candidate revenue streams

| Mechanism | Who pays and for what | Prerequisite | Principal risk |
| --- | --- | --- | --- |
| Seller subscription | Resident seller/provider pays for useful recurring reach/tools | Relevant leads, repeat value, willingness to renew | Low ARPU; seller may prefer free groups |
| Labelled campaigns | Provider pays for incremental attention to a specific offering/window | Real reach, consent/preferences, click/outcome measurement | Ad overload damages relevance and trust |
| Service-day activation | Visiting team pays for a measured community activation | Qualified demand, workable minimum volume, permission and attribution | Sourcing/coordinating costs consume the fee |
| Qualified lead fee | Provider pays for an agreed definition of a useful consented enquiry | Clear qualification, deduplication and fair billing | Low conversion, disputed leads or unwanted calls |
| Accepted/completed-job fee | Provider/partner pays on a verifiable event | Real booking/outcome workflow and commercial agreement | WhatsApp leakage; payment/reconciliation/support burden |
| Community licensing | RWA/builder pays for measurable resident utility | Budget owner, demonstrated value and renewal | Already paying for/using an incumbent; utility may be expected free |
| External partner referral/booking | Approved partner pays for appropriate appointments | Authorised integration, transparent attribution and confirmation | Regulated categories; insufficient local density or partner margin |

The founder's earlier seller price ideas were free listings, ₹99/month Starter, ₹299/month Growth and ₹2,499 annual. Earlier ideas also included paid promotions and community licensing. These are proposed prices, not collected revenue or implemented billing.

Free basic discoverability may be necessary to maintain supply. Paid placement must be labelled and must not pretend to be the best organic match. A service provider's willingness to pay depends on incremental contribution, not just gross ticket size.

### Provider value equation

**Provider value ≈ incremental completed jobs × contribution per job + value of repeat customers − platform fees − extra servicing/admin cost.**

Measure the provider's counterfactual: how many of those customers would have found them through an existing group or referral anyway? Do not call all platform-attributed enquiries incremental revenue.

For blue-collar teams, clustered visits can improve economics by reducing uncertain travel and increasing work per service window. This needs real observations of jobs/visit, travel time, cancellation and contribution. A theoretical saving is not proven profit.

### Community contribution equation

**Monthly community contribution = collected recurring fees + collected campaign/lead fees − AI/moderation/infrastructure allocation − onboarding and curation − community/provider support − payment/communication costs − failed-activation costs.**

Founder time has an economic cost. “No logistics” does not imply zero operating cost: recruiting teams, keeping data current and resolving incorrect introductions take work even without delivery or maintenance tickets.

Track model input/output cost, number of AI calls per search, moderation calls and query volume. Shared catalog/context handling may reduce cost, but freshness and correct scope cannot be sacrificed merely to cache more aggressively. Proposed estimates from the earlier lean canvas should be replaced with observed bills before fundraising.

### Revenue example—illustrative only

Assume one community has **25 paying providers at an average ₹199/month**, **10 separately paid campaigns at ₹99/month each**, and **one ₹3,000/month community licence**. These are modelling inputs, not pilot facts or a recommended final price.

| Component | Calculation | Monthly revenue |
| --- | --- | --- |
| Provider subscriptions | 25 × ₹199 | ₹4,975 |
| Campaigns | 10 × ₹99 | ₹990 |
| Community licence | 1 × ₹3,000 | ₹3,000 |
| **Combined** | Sum, assuming no bundling/double counting | **₹8,965** |
| Without a paying RWA | Subscriptions + campaigns | ₹5,965 |

This is revenue before costs, tax effects, collection loss and churn. Do not call it contribution profit. If campaign credits are included in subscriptions, or community licensing replaces seller fees, subtract the overlapping amounts rather than counting them twice.

The important experiment is which revenue stream actually converts and renews. Do not assume all three payers will accept the model at once.

## 14. Market sizing without an inflated TAM

HeyHood's initial addressable market is suitable dense communities that can be reached, activated and maintained—not all urban Indians, all apartment residents or the entire household-services industry.

### Bottom-up model

**Annual addressable revenue = eligible communities × realistically attainable annual revenue per active community.**

Break down eligible communities by city, size/occupancy, access to the operator, existing community software, likely supplier density and permission to distribute. Establish a sourced community count before calling the result TAM.

- **TAM:** all communities/segments that could fit a proven product and business model.
- **SAM:** cities and community types the current distribution/service model can support.
- **SOM:** communities that can actually be activated and retained over a defined period with the team's capacity and capital.

Do not use the founder's earlier “5,000+ communities” assumption as a verified India market count. Do not add unrelated food, ride, ads and services market values as if HeyHood captures all of them.

### Scale arithmetic—scenario, not TAM

| Active paying communities assumed | Annual revenue at ₹5,965/month without RWA fees | Annual revenue at ₹8,965/month with RWA fees |
| --- | --- | --- |
| 100 | ₹71.58 lakh | ₹1.08 crore (rounded) |
| 1,000 | ₹7.16 crore (rounded) | ₹10.76 crore (rounded) |
| 5,000 | ₹35.79 crore | ₹53.79 crore |

These figures illustrate the revenue intensity needed for scale. They do not establish that the community counts exist within reach, that the fees are attainable or that the business is profitable.

An investor may see a viable focused business before seeing a venture-scale outcome. To support a larger outcome, prove significantly higher revenue per hood, a repeatable wider footprint, or a valuable adjacent supplier network—without inventing a transaction take rate that the product cannot observe or collect.

## 15. Metrics and an evidence-led roadmap

### The immediate measurement hierarchy

1. **Discovery quality:** correct relevant supply, appropriate scope/time, freshness and multilingual retrieval accuracy.
2. **Connection:** eligible contact attempts and explicitly reported connections, with a visible feedback response rate.
3. **Useful outcomes:** later accepted/completed arrangements where a reliable workflow is added; not inferred from taps.
4. **Retention:** weekly returning resident cohorts and recurring provider value.
5. **Economics:** actual collections, provider renewal, acquisition cost, operator hours and community contribution.
6. **Supply activation:** qualified gaps converted into useful, repeatable new supply.

Recommended immediate operating metric: **weekly resident-reported connections with eligible providers, accompanied by feedback coverage**. It is an imperfect connection proxy, not a verified successful-job north star. Longer term, use completed useful matches with explicit outcome definitions per bucket.

### Definitions that prevent inflated metrics

| Metric | Definition / important denominator |
| --- | --- |
| Miss rate | Eligible searches with no suitable result ÷ eligible searches; separate technical/search failures |
| Qualified gap households | Distinct validated/self-reported households showing the same need within a defined window; label verification confidence |
| Contact rate | Searches with at least one eligible provider contact ÷ eligible searches; distinguish repeat taps |
| Reported connection rate | “Yes, connected” outcomes ÷ answered contact follow-ups; also report follow-up response coverage |
| Activation conversion | Qualified sourcing tests that produce accepted/completed service evidence ÷ tests launched |
| Repeat provider value | Providers retaining relevant demand/revenue over comparable periods; show median and distribution |
| Resident retention | Returning activated-resident cohorts, measured by week/month—not cumulative installs |
| Community contribution | Collected community revenue minus its attributable variable/human costs |

A high reported connection rate with very low feedback coverage can be biased. Sessions are not verified people. Votes are not attendance. Campaign views are not leads. WhatsApp taps are not messages or orders.

### Reconcile the original milestone gates

The founder's lean canvas suggested **50 daily searches** to unlock the next stage and **30 WhatsApp taps per top seller per week** to validate seller value. Keep them as separate hypotheses/stage gates, not achieved traction and not automatically compatible totals.

For illustration, at 50 searches/day and 30% of searches producing one provider contact, the community produces about **105 contacts/week**. Spread evenly over 20 providers, that is **5.25/week each**, not 30. Giving 20 providers 30 contacts/week would require about **286 searches/day** at that conversion assumption. Multiple contacts per search change the arithmetic; measure them explicitly.

A top seller can outperform the average, but selecting only the top seller hides weak supply-side economics. Report median and category distribution, and evaluate connection/job quality alongside contact volume.

### Suggested pilot stages

| Stage | Proposed work | Evidence to earn the next stage |
| --- | --- | --- |
| **Establish real discovery** | Replace a focused set of demos with consented providers, contacts and fresh offerings; validate query quality | Residents consistently get appropriate real choices; stale/wrong-contact problems are understood |
| **Establish useful connections** | Use the deployed feedback/reviews; introduce reliable membership/recovery when ready | Repeat cohorts and connection outcomes; providers describe concrete incremental value |
| **Test the founder's moat loop** | Run one or two qualified outside-service activations triggered by genuine gaps | A viable service window, explicit participant/provider agreement, outcome evidence and actual willingness to pay |
| **Test repeatability** | Replicate in the second community and a nearby cluster with non-founder operation | Comparable activation, retention and economics; logged onboarding/curation effort |
| **Deepen the right workflow** | Add booking, native UX or in-app conversation only where observed friction justifies it | A measurable gain in outcomes/economics, not merely a longer feature list |

A 90-day pilot plan can use these stages, but timing is an execution assumption. Do not promise launches or commercial outcomes before the required people, access and provider commitments exist.

## 16. Investor assessment and founder decisions

### How an Indian investor may initially classify HeyHood

An investor might view it as a community marketplace, a local service lead platform, a consumer utility with RWA distribution, or a feature an existing community app could add. Position it as an applied-AI product only when AI materially improves discovery efficiency and language access; “AI startup” is not a substitute for a business model.

The likely first objections are legitimate:

- Why would residents use another app alongside WhatsApp/MyGate?
- Why would a provider pay when groups and referrals are free?
- What stops an installed community platform from reproducing the features?
- How can many isolated communities create venture-scale economics?
- Is outside service activation a scalable system or a founder-operated local agency?
- How do you know misses represent unmet demand rather than faulty AI retrieval?
- How will you attribute revenue while the transaction happens on WhatsApp?

The strongest answers are measured comparisons and repeatable operating results. The companion investor brief provides pitch wording and a fuller question/answer framework.

### Indian operating realities that shape the funding case

| Reality | Consequence for HeyHood |
| --- | --- |
| Residents already use WhatsApp and a community app | A new installation must earn its place; preserve familiar contact habits initially |
| Household language and Romanised input vary | Multilingual recall must be tested by actual need, not just response language |
| RWAs/committees govern access and can change | Permission and distribution need continuity; neither is an exclusive data right |
| Home sellers and small teams have limited administrative time | Operator assistance is useful early; include its cost and plan a sustainable refresh process |
| Provider fee tolerance depends on incremental margin | Test renewals after results, not only willingness expressed in an interview |
| Service teams cross multiple communities | Route/service-window concentration can matter more than broad ad reach |
| Families share flats but are separate people | Membership, phone login and household counts require distinct concepts |
| Consumers expect clear price and reliable fulfilment | Do not carry a directory promise into a managed-service pitch without added capability |
| Food, health, pharmacy and school travel differ materially | Partner/provider responsibilities and qualified claims must be category-specific |
| Community demand is private context | Aggregated signals can guide sourcing; they are not unrestricted resale rights to household data |

For a Hyderabad founder, ecosystem introductions and local operator angels may support an early experiment, but names, programmes and investor appetite need separate current validation. Do not claim grant eligibility, signed RWA partnerships or investor interest from the existence of a pilot product.

### Funding lens for India

Bain/IVCA's **India Venture Capital Report 2026** reports approximately **US$16 billion in Indian VC/growth-equity investment during 2025** [S17]. More relevant than that headline, it describes a more measured consumer-tech environment, interest in vertical platforms, and emphasis on retention-led growth and disciplined unit economics. It also describes investor interest in applied/vertical AI. These are observations about 2025, not proof that a particular fund wants to finance HeyHood in October 2026.

For the pitch, this favours a focused use case and retained, paying demand over a broad unproven super-app narrative. Partner household commerce and an own-brand D2C business should remain distinct: the report's discussion of scaled D2C brands does not make HeyHood a D2C brand or validate its unit economics.

For HeyHood, relevant prospective backers may include operator angels, early-stage consumer/marketplace investors and strategically aligned community/service businesses. This is a targeting hypothesis, not a claim of investor interest. Institutional seed readiness requires stronger repeatability and economics than an attractive demo.

Do not choose the funding narrative by whichever label is fashionable. Choose it from where the product earns recurring value: community software, measurable provider demand or transacted services. Each has different margin, sales-cycle and operational expectations.

### Decisions to make before a fundraising deck

1. Which opening job/category creates repeat resident use?
2. Who is the first paying customer, and what exactly do they buy?
3. What qualifies an unmet need and triggers a sourcing test?
4. How are visiting providers approved, labelled and scheduled without blurring community residency?
5. Who refreshes data and operates activations, and what does that cost?
6. Which trust claim belongs to each badge, and how is it reviewed/revoked?
7. What evidence distinguishes a connected conversation from a completed useful job?
8. Which part of the context/sourcing loop improved after the last community launch?
9. What would cause us to stop expanding a module or community?
10. What capital amount/runway is justified by a concrete evidence milestone?

**The pitch to earn:** a trusted understanding of a specific community turns scattered local capability and unmet intent into reliable matches and new services, with economics that can be repeated across communities.

## 17. Sources and research limitations

Researched/accessed 8 October 2026. Official pages are primary descriptions, not audited performance evidence. No competitor app was logged into for this work; no supplier/resident was contacted. Pages may change, features may vary by city/account and public marketing does not prove implementation depth. Scale claims, “verified” labels and guarantees are the vendors' claims; this document does not independently endorse them. No funding/valuation or live user-count estimates are presented as facts.

| Ref | Source | Used for |
| --- | --- | --- |
| S1 | [WhatsApp Business app](https://whatsappbusiness.com/products/business-app/) | Business communication, product showcasing and AI-related messaging on the current landing page; regional availability not established |
| S2 | [MyGate resident app](https://mygate.com/community-management/residents/) | Community communication, resident tools and Buy & Sell |
| S3 | [MyGate Buy & Sell](https://mygate.com/buy-sell/) | Neighbour marketplace positioning |
| S4 | [MyGate advertising platform](https://mygate.com/ad-platform/) | Existing monetisation of community audiences |
| S5 | [NoBrokerHood](https://www.nobrokerhood.com/) | Society/resident platform and advertising navigation |
| S6 | [NoBrokerHood communication](https://www.nobrokerhood.com/communication-management) | Forums and community communication |
| S7 | [NoBroker Home Services](https://www.nobroker.in/home-services) | Adjacent home-services proposition |
| S8 | [ApnaComplex community collaboration](https://www.apnacomplex.com/community-collaboration-tools) | Notices, polls, repository and commercial links; current parent/product naming varies across pages |
| S9 | [ApnaComplex Bazaar](https://www.apnacomplex.com/bazaar) | Existing community commercial surface; check current operating coverage directly |
| S10 | [ADDA publisher app listing](https://play.google.com/store/apps/details?id=com.threefiveeight.adda) | Publisher listing for the India/global community app; current description includes communication and an ad-free positioning |
| S11 | [Justdial](https://www.justdial.com/) | Local discovery, categories, listings and advertising |
| S12 | [Urban Company Hyderabad](https://www.urbancompany.com/hyderabad) | At-home professional-service categories |
| S13 | [Quick Ride](https://quickride.in/) | Carpooling, recurring matching and vendor-described verification |
| S14 | [sRide](https://sride.co/) | Route matching, verification and ride/payment positioning |
| S15 | [Swiggy](https://www.swiggy.com/) | Food/grocery discovery and ordering proposition |
| S16 | [BookMyShow](https://in.bookmyshow.com/) | Events/activities discovery and tickets |
| S17 | [Bain/IVCA: India Venture Capital Report 2026](https://www.bain.com/insights/india-venture-capital-report-2026/) | Prior-year Indian funding context; report covers 2025, not individual October 2026 fund appetite |
| S18 | [Tata 1mg Labs](https://www.1mg.com/labs) | Blood/lab tests and home sample collection; displayed city may differ from the pilot |
| S19 | [Apollo 24|7 lab tests](https://www.apollo247.com/lab-tests) | Lab/health packages and home sample collection descriptions |
| S20 | [Apollo Diagnostics](https://www.apollodiagnostics.in/) | Home collection, test/package and date/time booking descriptions |
| S21 | [Netmeds](https://www.netmeds.com/) | Medicine, wellness and pharmacy-commerce proposition |

**Access limitations to resolve before external pitching:** ADDA's generic site redirected this environment to a US page with little readable content, and India subdomain requests failed. Its Google Play publisher listing was successfully read and supplies the limited feature observations above; logged-in India workflows were not tested. Cookr pages were unavailable in this research, so no current feature or operating claim about Cookr is used. Startup India scheme-page access was blocked, so no grant eligibility or current programme terms are claimed. No “first/only” competitive assertion is supported by this desk research.

This document is strategic analysis, not an assurance of revenue, investor interest or access to provider/RWA integrations. The next useful proof is a real resident need matched to reliable supply—or a qualified gap that leads to a successful, repeatable service activation. The long-term ambition is a chat-first household access platform, with each new integration justified by demand and a reliable partner workflow.
