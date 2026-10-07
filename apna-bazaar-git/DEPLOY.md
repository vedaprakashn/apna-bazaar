# Deploy Apna Bazaar on Railway

The backend serves the chatbot and admin HTML from the same domain. PostgreSQL runs as a separate Railway service. Netlify is not required.

1. Sign in at https://railway.com and create a project from the GitHub repository `vedaprakashn/apna-bazaar`, branch `main`.
2. In the application service settings, set Root Directory to `/apna-bazaar-git`. The Dockerfile and railway.toml are in this directory, not the repository root. If setting the config file path explicitly, use `/apna-bazaar-git/railway.toml`.
3. Add PostgreSQL to the same project. The following variable references assume the database service is named `Postgres`; select references in Railway's variable editor if the name differs.
4. Add these variables to the application service:

| Variable | Value |
| --- | --- |
| `DB_HOST` | `${{Postgres.PGHOST}}` |
| `DB_PORT` | `${{Postgres.PGPORT}}` |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `APNA_OPENAI_API_KEY` | Enter your OpenAI key securely in Railway |
| `OPENAI_MODEL` | `gpt-4o-mini` |

The key configured in Codex is not automatically transferred to Railway. Never commit it. Railway supplies PORT automatically; the application reads it. Flyway initializes the database on first startup.

5. Deploy the application and wait for a successful health check. Inspect build/deploy logs if it fails.
6. Under application Settings → Networking, generate a public domain.
7. Open `https://YOUR-DOMAIN/chatbot/index.html`. Admin pages are `/admin/index.html` and `/admin/broadcast.html`. Health is `/actuator/health`.

The chatbot uses the same domain for API calls, so no backend URL edit is needed. Local standalone frontend serving on port 8000 still uses backend port 8080.

## Verify

Check health returns `{"status":"UP"}`. Try a chatbot search. A fresh database has communities but no sellers, so zero results are expected until the catalog is populated. General catalog import is not implemented yet; current Excel ingestion handles daily menus only.

This is a pilot deployment: API endpoints currently lack authentication, and searches use paid OpenAI calls. Keep access limited while authentication and request limits are implemented. Static admin content is partly illustrative.

## Local Docker

From `apna-bazaar-git`, copy `.env.template` to `.env`, fill the key securely, and run `docker compose up --build`. Open the backend's `/chatbot/index.html` path on port 8080.
