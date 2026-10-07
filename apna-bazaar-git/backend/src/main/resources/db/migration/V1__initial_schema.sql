-- ============================================================
-- Apna Bazaar — Full Schema V1
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── ENUMS ────────────────────────────────────────────────────

CREATE TYPE provider_type_enum AS ENUM ('food_seller','grocery','service','tuition','other');
CREATE TYPE provider_status_enum AS ENUM ('active','pending','suspended','inactive');
CREATE TYPE offering_type_enum AS ENUM ('food_item','grocery_item','service_package','class','other');
CREATE TYPE delivery_type_enum AS ENUM ('pickup','home_delivery','both');
CREATE TYPE domain_enum AS ENUM ('food','grocery','services','education','other');
CREATE TYPE post_source_enum AS ENUM ('whatsapp_manual','whatsapp_auto','app_post','excel_upload');
CREATE TYPE billing_cycle_enum AS ENUM ('monthly','quarterly','annual');
CREATE TYPE subscription_status_enum AS ENUM ('active','expired','cancelled','trial');
CREATE TYPE click_type_enum AS ENUM ('whatsapp_tap','profile_view','menu_expand');
CREATE TYPE time_bucket_enum AS ENUM (
    'early_morning','morning_630','morning_7','morning_730','morning_8','morning_9',
    'mid_morning','lunch_11','lunch_12','afternoon_1','afternoon_2',
    'evening_4','evening_5','evening_6','evening_7','night_8','night_9','late_night'
);
CREATE TYPE zero_result_status_enum AS ENUM ('new','reviewing','provider_sought','fulfilled','declined');
CREATE TYPE insight_type_enum AS ENUM (
    'peak_time_mismatch','unmet_demand','competitor_gap',
    'pricing_signal','day_pattern','low_stock_miss','high_performer'
);

-- ── CORE TABLES ──────────────────────────────────────────────

CREATE TABLE community (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    total_flats   INT,
    status        VARCHAR(50) DEFAULT 'active',
    slug          VARCHAR(100) UNIQUE NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES category(id),
    name        VARCHAR(100) NOT NULL,
    icon_emoji  VARCHAR(10),
    domain      domain_enum NOT NULL,
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE provider (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id     UUID NOT NULL REFERENCES community(id),
    name             VARCHAR(200) NOT NULL,
    flat_number      VARCHAR(50),
    whatsapp_number  VARCHAR(20),
    shop_name        VARCHAR(200),
    provider_type    provider_type_enum NOT NULL DEFAULT 'food_seller',
    status           provider_status_enum NOT NULL DEFAULT 'pending',
    is_verified      BOOLEAN DEFAULT FALSE,
    rating           NUMERIC(3,2) DEFAULT 0,
    review_count     INT DEFAULT 0,
    joined_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE offering (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id   UUID NOT NULL REFERENCES provider(id),
    category_id   UUID REFERENCES category(id),
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    offering_type offering_type_enum NOT NULL DEFAULT 'food_item',
    base_price    NUMERIC(10,2),
    unit          VARCHAR(50),
    is_available  BOOLEAN DEFAULT TRUE,
    sort_order    INT DEFAULT 0,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE daily_post (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id   UUID NOT NULL REFERENCES provider(id),
    post_date     DATE NOT NULL,
    raw_message   TEXT,
    source        post_source_enum NOT NULL DEFAULT 'excel_upload',
    is_active     BOOLEAN DEFAULT TRUE,
    ingested_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE daily_line_item (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_post_id    UUID NOT NULL REFERENCES daily_post(id) ON DELETE CASCADE,
    offering_id      UUID REFERENCES offering(id),
    item_name        VARCHAR(200) NOT NULL,
    price            NUMERIC(10,2),
    pickup_time      VARCHAR(100),
    pickup_location  VARCHAR(200),
    delivery_type    delivery_type_enum DEFAULT 'pickup',
    quantity_available INT,
    notes            TEXT,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- ── SUBSCRIPTION TABLES ──────────────────────────────────────

CREATE TABLE subscription_plan (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    billing_cycle billing_cycle_enum NOT NULL,
    price         NUMERIC(10,2) NOT NULL,
    feature_flags JSONB DEFAULT '{}',
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE provider_subscription (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id  UUID NOT NULL REFERENCES provider(id),
    plan_id      UUID NOT NULL REFERENCES subscription_plan(id),
    starts_at    TIMESTAMPTZ NOT NULL,
    ends_at      TIMESTAMPTZ NOT NULL,
    status       subscription_status_enum NOT NULL DEFAULT 'trial',
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ── ANALYTICS TABLES ─────────────────────────────────────────

CREATE TABLE search_event (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id         UUID NOT NULL REFERENCES community(id),
    session_id           UUID,
    raw_query            TEXT NOT NULL,
    normalised_query     TEXT,
    matched_category_id  UUID REFERENCES category(id),
    result_count         INT DEFAULT 0,
    had_results          BOOLEAN DEFAULT FALSE,
    query_time           TIME NOT NULL,
    query_date           DATE NOT NULL,
    day_of_week          SMALLINT,
    time_bucket          time_bucket_enum,
    device_type          VARCHAR(50),
    created_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE search_result_impression (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    search_event_id UUID NOT NULL REFERENCES search_event(id) ON DELETE CASCADE,
    provider_id     UUID NOT NULL REFERENCES provider(id),
    offering_id     UUID REFERENCES offering(id),
    rank_shown      INT NOT NULL,
    match_reason    TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE provider_click_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    search_event_id UUID NOT NULL REFERENCES search_event(id) ON DELETE CASCADE,
    provider_id     UUID NOT NULL REFERENCES provider(id),
    offering_id     UUID REFERENCES offering(id),
    click_type      click_type_enum NOT NULL,
    clicked_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE zero_result_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id        UUID NOT NULL REFERENCES community(id),
    raw_query           TEXT NOT NULL,
    normalised_query    TEXT,
    closest_category_id UUID REFERENCES category(id),
    occurrence_count    INT DEFAULT 1,
    first_seen          DATE NOT NULL,
    last_seen           DATE NOT NULL,
    status              zero_result_status_enum DEFAULT 'new',
    UNIQUE (community_id, normalised_query)
);

-- ── AGGREGATED / ROLLED-UP TABLES ────────────────────────────

CREATE TABLE search_trend_daily (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id      UUID NOT NULL REFERENCES community(id),
    category_id       UUID REFERENCES category(id),
    trend_date        DATE NOT NULL,
    search_count      INT DEFAULT 0,
    unique_searchers  INT DEFAULT 0,
    result_clicks     INT DEFAULT 0,
    click_through_rate NUMERIC(5,4) DEFAULT 0,
    peak_time_bucket  time_bucket_enum,
    UNIQUE (community_id, category_id, trend_date)
);

CREATE TABLE provider_analytics_daily (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id              UUID NOT NULL REFERENCES provider(id),
    analytics_date           DATE NOT NULL,
    impressions              INT DEFAULT 0,
    profile_clicks           INT DEFAULT 0,
    whatsapp_clicks          INT DEFAULT 0,
    searches_matched         INT DEFAULT 0,
    impression_to_click_rate NUMERIC(5,4) DEFAULT 0,
    peak_demand_time         time_bucket_enum,
    zero_stock_misses        INT DEFAULT 0,
    UNIQUE (provider_id, analytics_date)
);

CREATE TABLE demand_insight (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id     UUID NOT NULL REFERENCES community(id),
    category_id      UUID REFERENCES category(id),
    provider_id      UUID REFERENCES provider(id),
    insight_type     insight_type_enum NOT NULL,
    insight_message  TEXT NOT NULL,
    supporting_data  JSONB DEFAULT '{}',
    week_of          DATE NOT NULL,
    is_actioned      BOOLEAN DEFAULT FALSE,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- ── INDEXES ──────────────────────────────────────────────────

CREATE INDEX idx_provider_community ON provider(community_id);
CREATE INDEX idx_provider_status ON provider(status);
CREATE INDEX idx_offering_provider ON offering(provider_id);
CREATE INDEX idx_offering_category ON offering(category_id);
CREATE INDEX idx_daily_post_provider_date ON daily_post(provider_id, post_date);
CREATE INDEX idx_daily_post_date ON daily_post(post_date);
CREATE INDEX idx_daily_line_post ON daily_line_item(daily_post_id);
CREATE INDEX idx_search_event_community_date ON search_event(community_id, query_date);
CREATE INDEX idx_search_event_date ON search_event(query_date);
CREATE INDEX idx_search_event_bucket ON search_event(time_bucket);
CREATE INDEX idx_zero_result_community ON zero_result_log(community_id, status);
CREATE INDEX idx_trend_community_date ON search_trend_daily(community_id, trend_date);
CREATE INDEX idx_provider_analytics_date ON provider_analytics_daily(provider_id, analytics_date);
CREATE INDEX idx_demand_insight_community ON demand_insight(community_id, week_of);

-- ── SEED DATA ────────────────────────────────────────────────

INSERT INTO community (name, city, total_flats, slug)
VALUES ('MyHome Tridasa', 'Hyderabad', 2700, 'tridasa');

INSERT INTO community (name, city, total_flats, slug)
VALUES ('MyHome Sayuk', 'Hyderabad', 3800, 'sayuk');

INSERT INTO category (name, icon_emoji, domain, sort_order) VALUES
('Food & Tiffin', '🍱', 'food', 1),
('Batter & Dough', '🫓', 'food', 2),
('Juices & Shakes', '🥤', 'food', 3),
('Sweets & Snacks', '🍬', 'food', 4),
('Frozen Food', '❄️', 'food', 5),
('Vegetables & Greens', '🥦', 'grocery', 6),
('Dry Fruits & Nuts', '🫙', 'grocery', 7),
('Oils & Spices', '🧴', 'grocery', 8),
('Dairy', '🥛', 'grocery', 9),
('Home Painting', '🖌️', 'services', 10),
('Deep Cleaning', '🧹', 'services', 11),
('Chimney Cleaning', '🔧', 'services', 12),
('Plumbing', '🔧', 'services', 13),
('Electrician', '⚡', 'services', 14),
('Tuition & Classes', '📚', 'education', 15),
('Yoga & Fitness', '🧘', 'services', 16);

INSERT INTO subscription_plan (name, billing_cycle, price, feature_flags) VALUES
('Free', 'monthly', 0, '{"listing": true, "analytics": false, "featured": false, "insights": false}'),
('Starter', 'monthly', 99, '{"listing": true, "analytics": true, "featured": false, "insights": false}'),
('Growth', 'monthly', 299, '{"listing": true, "analytics": true, "featured": true, "insights": true}'),
('Premium Annual', 'annual', 2499, '{"listing": true, "analytics": true, "featured": true, "insights": true, "priority_support": true}');
