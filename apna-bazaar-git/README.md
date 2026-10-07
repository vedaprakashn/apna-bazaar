# 🏡 Apna Bazaar

Hyperlocal community marketplace for gated communities.
AI-powered conversational discovery for informal sellers in communities like MyHome Tridasa and MyHome Sayuk.

## Quick Start (Docker — no installs needed)

```bash
cp .env.template .env
# Add your ANTHROPIC_API_KEY to .env
docker-compose up --build
```

Open `frontend/chatbot/index.html` in your browser.

## Quick Start (Local Java)

```bash
cp .env.template .env
# Fill in your values
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
