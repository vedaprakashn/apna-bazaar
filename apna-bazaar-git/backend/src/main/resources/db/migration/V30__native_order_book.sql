ALTER TABLE provider ADD COLUMN public_bio TEXT NOT NULL DEFAULT '';
ALTER TABLE provider ADD COLUMN delivery_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE provider ADD COLUMN serves_from TIME;
ALTER TABLE provider ADD COLUMN serves_to TIME;
CREATE TABLE commerce_order (
 id UUID PRIMARY KEY,
 reference VARCHAR(32) NOT NULL UNIQUE,
 community_id UUID NOT NULL REFERENCES community(id),
 provider_id UUID NOT NULL REFERENCES provider(id),
 buyer_id UUID REFERENCES resident_profile(id),
 idempotency_key UUID NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 source VARCHAR(12) NOT NULL CHECK(source IN ('heyhood','whatsapp','phone','paper','excel','walkin')),
 customer_name VARCHAR(100) NOT NULL,
 customer_flat VARCHAR(50) NOT NULL,
 pickup_flat VARCHAR(50) NOT NULL,
 fulfilment VARCHAR(8) NOT NULL CHECK(fulfilment IN ('Pickup','Delivery')),
 fulfilment_at TIMESTAMPTZ NOT NULL,
 items JSONB NOT NULL,
 total NUMERIC(12,2) NOT NULL CHECK(total>=0),
 note VARCHAR(500) NOT NULL DEFAULT '',
 status VARCHAR(12) NOT NULL CHECK(status IN ('requested','accepted','preparing','ready','completed','rejected','cancelled','expired')),
 revision BIGINT NOT NULL DEFAULT 0,
 completion_method VARCHAR(24),
 reason VARCHAR(300) NOT NULL DEFAULT '',
 otp_cipher TEXT,
 otp_expires_at TIMESTAMPTZ,
 otp_issued_at TIMESTAMPTZ,
 otp_attempts INT NOT NULL DEFAULT 0,
 otp_locked_until TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(provider_id,idempotency_key)
);
CREATE INDEX commerce_order_provider_queue ON commerce_order(provider_id,status,fulfilment_at);
CREATE INDEX commerce_order_buyer ON commerce_order(buyer_id,created_at DESC);
CREATE TABLE commerce_order_event (
 id UUID PRIMARY KEY,
 order_id UUID NOT NULL REFERENCES commerce_order(id),
 revision BIGINT NOT NULL,
 actor VARCHAR(20) NOT NULL,
 status VARCHAR(12) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(order_id,revision)
);
CREATE TABLE provider_order_notification (
 id UUID PRIMARY KEY,
 provider_id UUID NOT NULL REFERENCES provider(id),
 order_id UUID NOT NULL REFERENCES commerce_order(id),
 revision BIGINT NOT NULL,
 title VARCHAR(150) NOT NULL,
 body VARCHAR(500) NOT NULL,
 read_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(order_id,revision)
);
