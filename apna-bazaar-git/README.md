# 🏡 Apna Bazaar

Hyperlocal community marketplace for gated communities.
OpenAI-powered conversational discovery for informal sellers in communities like MyHome Tridasa and MyHome Sayuk.

## Quick Start (Docker — no installs needed)

```bash
cp .env.template .env
# Add your APNA_OPENAI_API_KEY to .env
docker-compose up --build
```

Open `frontend/chatbot/index.html` in your browser.

## Quick Start (Local Java)

```bash
cp .env.template .env
# Fill in your values
set -a; source .env; set +a
cd backend && mvn spring-boot:run
```

## Deploy to Railway

See [DEPLOY.md](DEPLOY.md) for full instructions.

## Project Structure

```
apna-bazaar/
├── backend/                Spring Boot 3.3 + PostgreSQL
│   ├── Dockerfile
│   ├── railway.toml
│   ├── pom.xml
│   └── src/main/java/com/apnabazaar/
│       ├── controller/     REST endpoints (6 controllers)
│       ├── dto/            Request/response records (14 files)
│       ├── entity/         JPA entities (16 tables)
│       ├── repository/     Spring Data repos (14 files)
│       └── service/        Business logic (5 services)
├── frontend/
│   ├── chatbot/index.html  Buyer chatbot (mobile-first)
│   └── admin/
│       ├── index.html      Analytics dashboard
│       └── broadcast.html  Broadcast manager
├── excel/                  Daily menu upload template
├── docker-compose.yml
└── DEPLOY.md
```

## API Quick Reference

```
GET  /api/{community}/search?q=...       AI-powered catalog search
POST /api/{community}/ingest/daily-menu  Upload Excel menu file
GET  /api/{community}/digest             Today's active sellers
GET  /api/{community}/admin/dashboard    Analytics dashboard
POST /api/{community}/broadcasts         Create broadcast/nudge
```

Community slugs: `tridasa`, `sayuk`

## AI configuration

Set `APNA_OPENAI_API_KEY` securely in your backend environment. It is mapped to Spring AI’s OpenAI client; `OPENAI_MODEL` defaults to `gpt-4.1-mini`. The custom key name works with cloud environment settings, where `OPENAI_API_KEY` is reserved. Never put a key in HTML.

The backend serves the chatbot at `/chatbot/index.html`; deployed search uses the same domain automatically. A standalone frontend on port 8000 uses the local backend on port 8080. The backend searches database providers, so register providers and offerings before expecting results.

## Pilot catalog and live analytics

Flyway V4 inserts 24 fictional providers in each pilot community, covering all 16 categories. Listings are marked Demo, with no real contacts or fabricated ratings. They have recurring schedules; AI search receives these schedules. The migration preserves existing providers and does not create fake search events.

The operator dashboard at `/admin/index.html` loads `/api/{community}/admin/activity` and shows actual search activity, missed queries, category coverage and provider interest. Demo taps record `profile_view`; real WhatsApp buttons record `whatsapp_tap`. Date ranges and daily charts use India Standard Time.

The resident chatbot and dashboard share responsive styling. Backend JSON APIs remain independent of the web presentation, allowing Android/iOS clients after functional completion. Native screens, authentication, notification delivery and app-store packaging remain future work.

AI search accepts Indian-language scripts, transliterated queries and mixed-language requests. Unicode query normalization preserves native scripts in analytics. Flyway V6 adds a fictional Hindi tutor in each pilot community (25 demo providers per community). Shop names on search cards open the provider storefront; resident names are plain text.

Flyway V7 expands each of the 25 demo providers to four offerings, with sample prices and recurring schedules. These catalogs are visible on provider storefronts and available to AI discovery.

Demo chat broadcasts/promotions rotate every three minutes while the page is visible and search is idle. They link to genuine demo catalog offerings, with no invented discounts or live availability. Analytics counts a view when at least half the card is visible, unique browser chat sessions per message, and storefront clicks. `GET /api/{community}/promotions` and `POST /api/{community}/promotions/events` are reusable for a future mobile client. Flyway V8 creates the demo messages; it seeds no view or click events.

Search interprets the original short request into English intent before catalog matching, so informal transliteration and typos (for example, Tanglish “ennaku hindi tuiton venum da”) do not depend on English keywords. Replies use the original request. This uses two AI calls per search.

The community AI guide is Aapta (आप्त, trusted friend), with a friendly character badge and consistent voice in chat and neighbourhood notes.

Supported chat languages: English, Hindi, Telugu, Tamil, Kannada, Malayalam, Marathi, Punjabi, Bengali and Assamese; native scripts, romanized requests and mixed-language queries are supported. Search uses structured JSON with low randomness and preserves the native script in replies.

Mixed-language chat includes Hinglish, Tenglish, Tanglish/Tamglish and English mixed with each supported language, including informal spelling. Bengali and Assamese share an alphabet but retain distinct language preferences.

Chat search is limited to 10 requests per visitor IP in a rolling 60-second window, across communities and session IDs. The server returns HTTP 429 with Retry-After before OpenAI runs; the chat displays the wait time. `CHAT_MESSAGES_PER_MINUTE` changes the limit. Limiter state is bounded and local to one app instance; multiple replicas would need a shared store. Forwarded IPs are used only when running on Railway.
