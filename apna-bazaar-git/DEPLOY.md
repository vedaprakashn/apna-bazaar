# Deploy HeyHood on Railway

## Project links

- [Railway application settings](https://railway.com/project/c916bca3-683b-40c2-948c-fccb95b7c92e/service/27ba8812-a8df-416f-845d-4fdbf1b208db/settings?environmentId=ccd4ca5d-cc25-479c-9146-03bf020d53f5)
- [Live chatbot](https://heyhood-production-b1b8.up.railway.app/chatbot/index.html)
- [Live analytics](https://heyhood-production-b1b8.up.railway.app/admin/index.html)

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
| `OPENAI_MODEL` | `gpt-4.1-mini` |

The key configured in Codex is not automatically transferred to Railway. Never commit it. Railway supplies PORT automatically; the application reads it. Flyway initializes the database on first startup.

5. Deploy the application and wait for a successful health check. Inspect build/deploy logs if it fails.
6. Under application Settings → Networking, generate a public domain.
7. Open `https://YOUR-DOMAIN/chatbot/index.html`. Admin pages are `/admin/index.html` and `/admin/broadcast.html`. Health is `/actuator/health`.

The chatbot uses the same domain for API calls, so no backend URL edit is needed. Local standalone frontend serving on port 8000 still uses backend port 8080.

## Verify

Check health returns `{"status":"UP"}`. Try a chatbot search. Flyway seeds the pilot communities with fictional providers and offerings. General catalog import is not implemented yet; current Excel ingestion handles daily menus only.

This is a pilot deployment: Campaign writes require CAMPAIGN_ADMIN_TOKEN; other endpoint access still needs production authentication. Searches use paid OpenAI calls and are limited to 10 messages/minute per IP per instance. Analytics uses recorded events.

## Local Docker

From `apna-bazaar-git`, copy `.env.template` to `.env`, fill the key securely, and run `docker compose up --build`. Open the backend's `/chatbot/index.html` path on port 8080.

## HeyHood branding and custom domain

The public brand is now **HeyHood — Good things. Close by.** The repository and Railway service can keep their existing technical names.

1. Register `heyhood.in` with an accredited registrar. NIXI RDAP reported it available on 7 October 2026; purchase availability can change.
2. In the Railway app service, open **Settings → Networking → Custom Domain** and enter `heyhood.in`. The app listens on port **8080** unless Railway injects another `PORT`.
3. In your domain provider's DNS settings, add the target and verification records **exactly as Railway displays them**. For the root `@` record, use the provider's supported CNAME flattening / ALIAS / ANAME method if required. Railway provides the target; do not guess it from the existing public URL.
4. Wait for Railway to verify DNS and issue HTTPS. `https://heyhood.in/` opens the chatbot; `/admin/index.html` is analytics and `/admin/campaigns.html` is campaign management. Add `www.heyhood.in` separately in Railway if wanted.

Frontend API calls use the current origin, so they work on the custom domain without hardcoded API URL changes.

For campaign editing, set **CAMPAIGN_ADMIN_TOKEN** to a long random value in the app service's Railway Variables and redeploy. Enter that same key in the campaign studio. Use a separate value from the OpenAI API key. Viewing campaigns and the seeded in-chat promotions works without this editing key.

Current demo URLs after the Railway domain rename:
- Chat: https://heyhood-production-b1b8.up.railway.app/chatbot/index.html
- Analytics: https://heyhood-production-b1b8.up.railway.app/admin/index.html
- Campaign studio: https://heyhood-production-b1b8.up.railway.app/admin/campaigns.html

The previous apna-bazaar public domain was removed by the Railway rename. Use the HeyHood hostname for bookmarks and shared links.

## Living handoff

See [HEYHOOD_HANDOFF.md](../docs/HEYHOOD_HANDOFF.md) for current functionality, decisions, known limitations and collaborator setup.
