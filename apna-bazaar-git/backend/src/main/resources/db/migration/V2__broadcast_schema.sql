-- V2: Broadcast / Nudge system

CREATE TYPE broadcast_type_enum AS ENUM ('organic','sponsored','ai_generated');
CREATE TYPE broadcast_status_enum AS ENUM ('draft','scheduled','sent','cancelled','recurring_active','recurring_paused');
CREATE TYPE broadcast_channel_enum AS ENUM ('chatbot_digest','whatsapp_push','both');

CREATE TABLE broadcast (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id        UUID NOT NULL REFERENCES community(id),
    provider_id         UUID REFERENCES provider(id),
    message             TEXT NOT NULL,
    broadcast_type      broadcast_type_enum NOT NULL DEFAULT 'organic',
    status              broadcast_status_enum NOT NULL DEFAULT 'draft',
    channel             broadcast_channel_enum NOT NULL DEFAULT 'chatbot_digest',
    scheduled_at        TIMESTAMPTZ,
    sent_at             TIMESTAMPTZ,
    recurrence_pattern  VARCHAR(100),
    is_recurring        BOOLEAN DEFAULT FALSE,
    promo_amount        INT,         -- paise (100 = ₹1)
    payment_status      VARCHAR(50),
    impressions         INT DEFAULT 0,
    searches_triggered  INT DEFAULT 0,
    whatsapp_taps       INT DEFAULT 0,
    metadata            JSONB DEFAULT '{}',
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_broadcast_community_status ON broadcast(community_id, status);
CREATE INDEX idx_broadcast_scheduled ON broadcast(scheduled_at) WHERE status = 'scheduled';
CREATE INDEX idx_broadcast_community_sent ON broadcast(community_id, sent_at);
