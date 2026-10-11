-- Private capability access is provisioned by the operator, never by public provider ID.
CREATE TABLE provider_workspace (
 provider_id UUID PRIMARY KEY REFERENCES provider(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 draft JSONB,
 revision BIGINT NOT NULL DEFAULT 0,
 published_revision BIGINT NOT NULL DEFAULT 0,
 published_at TIMESTAMPTZ,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE offering ADD COLUMN archived BOOLEAN NOT NULL DEFAULT false;
CREATE TABLE offering_day_availability (
 offering_id UUID NOT NULL REFERENCES offering(id) ON DELETE CASCADE,
 service_date DATE NOT NULL,
 available BOOLEAN NOT NULL,
 PRIMARY KEY(offering_id,service_date)
);
CREATE FUNCTION provider_item_available(item_id UUID, on_date DATE) RETURNS BOOLEAN
LANGUAGE SQL STABLE AS $$
 SELECT NOT o.archived AND coalesce(d.available,o.is_available,false)
 FROM offering o LEFT JOIN offering_day_availability d ON d.offering_id=o.id AND d.service_date=on_date
 WHERE o.id=item_id
$$;
