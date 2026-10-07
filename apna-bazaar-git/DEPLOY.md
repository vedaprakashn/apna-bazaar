# Deploying Apna Bazaar

## Option A — Railway CLI (Fastest, ~10 minutes)

```bash
npm install -g @railway/cli
railway login

cd backend
railway init
railway up

# Add environment variables
railway variables set APNA_OPENAI_API_KEY=your-openai-api-key
railway variables set DB_HOST=...   # from Railway PostgreSQL plugin
railway variables set DB_PORT=5432
railway variables set DB_NAME=railway
railway variables set DB_USERNAME=postgres
railway variables set DB_PASSWORD=...
```

Add PostgreSQL in Railway dashboard: New → Database → PostgreSQL

## Option B — Railway via GitHub

1. Push this repo to GitHub
2. railway.app → New Project → Deploy from GitHub repo
3. Select repo → Railway detects Dockerfile automatically
4. Add PostgreSQL plugin: New → Database → PostgreSQL
5. Add variables in Railway dashboard:
   ```
   APNA_OPENAI_API_KEY = your-openai-api-key
   DB_HOST     = ${{Postgres.PGHOST}}
   DB_PORT     = ${{Postgres.PGPORT}}
   DB_NAME     = ${{Postgres.PGDATABASE}}
   DB_USERNAME = ${{Postgres.PGUSER}}
   DB_PASSWORD = ${{Postgres.PGPASSWORD}}
   ```
6. Get your public URL: Settings → Networking → Generate Domain

## Option C — Docker Compose (Local, no installs)

```bash
cp .env.template .env
# Add APNA_OPENAI_API_KEY to .env
docker-compose up --build
# API at http://localhost:8080
# Open frontend/chatbot/index.html in browser
```

## Hosting the Frontend

The HTML files are static — no server needed.

**Share directly:** WhatsApp the chatbot/index.html file to residents.

**GitHub Pages:** Enable in repo Settings → Pages → serve from /frontend.

**Netlify Drop:** Drag the frontend/ folder to app.netlify.com/drop — instant URL.

After deploying backend, update the API_BASE in frontend/chatbot/index.html:
```javascript
const API_BASE = 'https://your-app.up.railway.app';
```

## Verify Deployment

```bash
# Health check
curl https://your-app.up.railway.app/actuator/health

# Test search
curl "https://your-app.up.railway.app/api/tridasa/search?q=idli+batter"

# Upload daily menu
curl -X POST https://your-app.up.railway.app/api/tridasa/ingest/daily-menu \
  -F "file=@excel/apna_bazaar_daily_menu_template.xlsx"
```
