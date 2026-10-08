CREATE TABLE provider_contact_feedback (
 resident_id uuid NOT NULL REFERENCES resident_profile(id), provider_id uuid NOT NULL REFERENCES provider(id),
 outcome varchar(20) NOT NULL DEFAULT 'waiting' CHECK(outcome IN ('waiting','connected','not_connected')),
 contacted_at timestamptz NOT NULL DEFAULT now(), answered_at timestamptz,
 PRIMARY KEY(resident_id,provider_id)
);
CREATE TABLE provider_review (
 resident_id uuid NOT NULL REFERENCES resident_profile(id), provider_id uuid NOT NULL REFERENCES provider(id),
 stars integer NOT NULL CHECK(stars BETWEEN 1 AND 5), body varchar(1000) NOT NULL DEFAULT '',
 hidden boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(resident_id,provider_id)
);
ALTER TABLE provider ADD COLUMN verification_checked_at timestamptz;
CREATE TABLE provider_verification_audit (
 id uuid PRIMARY KEY, provider_id uuid NOT NULL REFERENCES provider(id), verified boolean NOT NULL,
 identity_checked boolean NOT NULL, flat_checked boolean NOT NULL, contact_checked boolean NOT NULL,
 evidence varchar(1000) NOT NULL, checked_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE provider_review_moderation_audit (
 id uuid PRIMARY KEY, provider_id uuid NOT NULL, resident_id uuid NOT NULL, hidden boolean NOT NULL,
 reason varchar(1000) NOT NULL, checked_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX provider_review_public_idx ON provider_review(provider_id,updated_at DESC) WHERE NOT hidden;
