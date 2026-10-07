-- ============================================================
-- V3: Serving windows, pre-order windows, order slots
-- ============================================================

-- ── ENUMS ────────────────────────────────────────────────────

CREATE TYPE day_scope_enum AS ENUM (
    'daily',         -- every day
    'weekdays',      -- Monday–Friday
    'weekends',      -- Saturday–Sunday
    'specific_days', -- comma-list in days_of_week column e.g. 'MON,WED,FRI'
    'sunday_only',
    'saturday_only'
);

CREATE TYPE preorder_day_offset_enum AS ENUM (
    'same_day',      -- pre-order today for today's serving window
    'day_before',    -- pre-order today for tomorrow's serving window
    'two_days_prior' -- advance booking e.g. cakes, catering
);

CREATE TYPE order_type_enum AS ENUM (
    'preorder',   -- placed before preorder_closes_at
    'realtime'    -- placed during the serving window
);

CREATE TYPE order_status_enum AS ENUM (
    'placed',
    'confirmed',
    'ready',
    'completed',
    'cancelled'
);

-- ── OFFERING_SCHEDULE ─────────────────────────────────────────
-- Reusable weekly template per offering.
-- A seller sets this once; daily_line_item can override for a specific day.

CREATE TABLE offering_schedule (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id              UUID NOT NULL REFERENCES offering(id) ON DELETE CASCADE,

    -- When this schedule is active
    day_scope                day_scope_enum NOT NULL DEFAULT 'daily',
    days_of_week             VARCHAR(30),  -- 'MON,WED,FRI' when day_scope = specific_days

    -- Serving window: when food is ready and available
    serves_from              TIME NOT NULL,
    serves_to                TIME NOT NULL,

    -- Pre-order window
    accepts_preorder         BOOLEAN DEFAULT TRUE,
    preorder_day_offset      preorder_day_offset_enum DEFAULT 'same_day',
    preorder_opens_at        TIME,         -- null = opens immediately / always open
    preorder_closes_at       TIME,         -- e.g. 09:00 means cut-off at 9 AM

    -- Realtime (walk-in) orders during serving window
    accepts_realtime         BOOLEAN DEFAULT TRUE,
    -- How many minutes BEFORE serves_to the realtime window closes
    -- e.g. 30 means stop taking realtime orders 30 min before kitchen closes
    realtime_cutoff_minutes  INT DEFAULT 0,

    -- Capacity
    max_orders_per_slot      INT,          -- null = unlimited
    is_active                BOOLEAN DEFAULT TRUE,
    created_at               TIMESTAMPTZ DEFAULT NOW(),

    -- A seller can have multiple schedules per offering (e.g. different weekend hours)
    CONSTRAINT chk_serves CHECK (serves_to > serves_from),
    CONSTRAINT chk_preorder CHECK (
        NOT accepts_preorder OR preorder_closes_at IS NOT NULL
    )
);

-- ── DAILY_LINE_ITEM additions ─────────────────────────────────
-- Add time window columns to daily_line_item so each day's post
-- can override the master schedule, or set windows for ad-hoc items.

ALTER TABLE daily_line_item
    ADD COLUMN serves_from              TIME,
    ADD COLUMN serves_to                TIME,
    ADD COLUMN accepts_preorder         BOOLEAN DEFAULT TRUE,
    ADD COLUMN preorder_closes_at       TIME,       -- e.g. 09:00
    ADD COLUMN preorder_day_offset      preorder_day_offset_enum DEFAULT 'same_day',
    ADD COLUMN accepts_realtime         BOOLEAN DEFAULT TRUE,
    ADD COLUMN realtime_cutoff_minutes  INT DEFAULT 0,
    ADD COLUMN quantity_ordered         INT DEFAULT 0,
    ADD COLUMN order_cutoff_at          TIMESTAMPTZ; -- absolute computed cutoff for today

-- Computed helper: order_cutoff_at is set by the backend when the daily post
-- is ingested. It is the absolute timestamp before which orders are accepted.
-- For same-day pre-order closing at 09:00 on 2026-04-08 it is 2026-04-08 09:00 IST.
-- For day-before pre-order closing at 20:00 for tomorrow it is 2026-04-07 20:00 IST.

-- ── ORDER_SLOT ────────────────────────────────────────────────
-- Optional lightweight reservation before WhatsApp redirect.
-- Even if unused initially, the table is ready when you want it.

CREATE TABLE order_slot (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_line_item_id  UUID NOT NULL REFERENCES daily_line_item(id),
    community_id        UUID NOT NULL REFERENCES community(id),

    buyer_name          VARCHAR(200),
    buyer_flat          VARCHAR(50),
    buyer_whatsapp      VARCHAR(20),

    quantity            INT NOT NULL DEFAULT 1,
    order_type          order_type_enum NOT NULL,
    ordered_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status              order_status_enum NOT NULL DEFAULT 'placed',
    notes               TEXT,

    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_order_slot_line_item ON order_slot(daily_line_item_id);
CREATE INDEX idx_order_slot_community ON order_slot(community_id, ordered_at);
CREATE INDEX idx_offering_schedule_offering ON offering_schedule(offering_id);

-- ── SEED: example schedules for the sample providers ─────────
-- These show the full range of patterns

-- Lakshmi (batter) — day-before pre-order, pickup 5–9 PM daily
-- Inserted by the backend after providers are seeded via Excel.
-- Shown here as reference only:

COMMENT ON TABLE offering_schedule IS
'Reusable weekly serving + ordering window template per offering.
 day_scope controls which days this applies.
 serves_from/serves_to = when food is ready.
 preorder_closes_at = cut-off time for advance orders.
 preorder_day_offset = same_day means order today for today;
                       day_before means order today for tomorrow.
 realtime_cutoff_minutes = how many minutes before serves_to
                           the kitchen stops taking walk-in orders.
 Example patterns:
   Batter seller:    serves 17:00–21:00, preorder day_before closes 20:00,
                     accepts_realtime=false
   Lunch kitchen:    serves 13:00–15:00, preorder same_day closes 09:00,
                     accepts_realtime=true, realtime_cutoff_minutes=30
   Juice bar:        serves 10:00–19:00, accepts_preorder=false,
                     accepts_realtime=true, realtime_cutoff_minutes=0
   Frozen snacks:    serves 00:00–23:59, accepts_preorder=true,
                     accepts_realtime=true (anytime)
   Sunday special:   day_scope=sunday_only, serves 09:00–13:00,
                     preorder day_before closes 21:00';
