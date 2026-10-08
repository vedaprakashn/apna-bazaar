ALTER TABLE chat_promotion ADD COLUMN push_enabled BOOLEAN NOT NULL DEFAULT false;
CREATE TABLE push_installation (
 id UUID PRIMARY KEY,
 community_id UUID NOT NULL REFERENCES community(id),
 secret_hash CHAR(64) NOT NULL,
 fcm_token TEXT UNIQUE,
 enabled BOOLEAN NOT NULL DEFAULT false,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE push_campaign_delivery (
 id UUID PRIMARY KEY,
 installation_id UUID NOT NULL REFERENCES push_installation(id),
 promotion_id UUID NOT NULL REFERENCES chat_promotion(id),
 local_day DATE NOT NULL,
 status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','accepted','failed','unknown')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 received_at TIMESTAMPTZ,
 opened_at TIMESTAMPTZ,
 UNIQUE(installation_id,promotion_id,local_day)
);
CREATE INDEX idx_push_delivery_installation ON push_campaign_delivery(installation_id,created_at);
