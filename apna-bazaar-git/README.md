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

Set `APNA_OPENAI_API_KEY` securely in your backend environment. It is mapped to Spring AI’s OpenAI client; `OPENAI_MODEL` defaults to `gpt-4o-mini`. The custom key name works with cloud environment settings, where `OPENAI_API_KEY` is reserved. Never put a key in HTML.

The chatbot calls the backend search API. For deployments, set `API_BASE` in `frontend/chatbot/index.html` to the backend URL; local development defaults to port 8080. The backend searches database providers, so register providers and offerings before expecting results.
