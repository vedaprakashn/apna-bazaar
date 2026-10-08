CREATE TABLE resident_flat_invitation (
 id uuid PRIMARY KEY, community_id uuid NOT NULL REFERENCES community(id),
 flat_number varchar(30) NOT NULL, code_hash char(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL, consumed_by uuid REFERENCES resident_profile(id),
 consumed_at timestamptz, revoked_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX resident_invitation_flat ON resident_flat_invitation(community_id,lower(flat_number));
ALTER TABLE resident_profile ADD COLUMN flat_verified_at timestamptz;
ALTER TABLE resident_profile ADD COLUMN invitation_id uuid REFERENCES resident_flat_invitation(id);
ALTER TABLE resident_profile ADD COLUMN firebase_uid varchar(128);
ALTER TABLE resident_profile ADD COLUMN phone_last4 char(4);
ALTER TABLE resident_profile ADD COLUMN phone_verified_at timestamptz;
CREATE UNIQUE INDEX resident_phone_identity ON resident_profile(community_id,firebase_uid) WHERE firebase_uid IS NOT NULL;

CREATE TABLE hood_ride_agreement (
 id uuid PRIMARY KEY, ride_id uuid NOT NULL REFERENCES hood_ride(id),
 resident_id uuid NOT NULL REFERENCES resident_profile(id), occurrence_at timestamptz NOT NULL,
 seats integer NOT NULL CHECK(seats BETWEEN 1 AND 6),
 state varchar(12) NOT NULL DEFAULT 'requested'
 CHECK(state IN ('requested','proposed','confirmed','declined','cancelled','expired')),
 terms varchar(500) NOT NULL DEFAULT '', revision integer NOT NULL DEFAULT 1,
 hold_until timestamptz, updated_at timestamptz NOT NULL DEFAULT now(), created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(ride_id,resident_id,occurrence_at)
);
CREATE INDEX hood_ride_seats ON hood_ride_agreement(ride_id,occurrence_at,state);
ALTER TABLE hood_plan ADD COLUMN organiser_id uuid REFERENCES resident_profile(id);
ALTER TABLE hood_plan ADD COLUMN capacity integer CHECK(capacity BETWEEN 2 AND 1000);
ALTER TABLE hood_plan ADD COLUMN confirmed_at timestamptz;
ALTER TABLE hood_plan ADD COLUMN confirmation_note varchar(500);
ALTER TABLE hood_plan ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE hood_plan ADD CONSTRAINT plan_capacity_minimum CHECK(capacity IS NULL OR capacity>=minimum_interested);
ALTER TABLE hood_plan_vote ADD COLUMN queue_state varchar(12) NOT NULL DEFAULT 'interested'
 CHECK(queue_state IN ('interested','attending','waitlisted'));
ALTER TABLE hood_plan_vote ADD COLUMN joined_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE hood_plan_vote ADD COLUMN reminder_enabled boolean NOT NULL DEFAULT false;
ALTER TABLE hood_plan_vote ADD COLUMN reminder_sent_at timestamptz;

CREATE TABLE resident_notification (
 id uuid PRIMARY KEY, community_id uuid NOT NULL REFERENCES community(id), resident_id uuid NOT NULL REFERENCES resident_profile(id),
 event_key varchar(220) NOT NULL, title varchar(180) NOT NULL, body varchar(600) NOT NULL,
 path varchar(300) NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), read_at timestamptz,
 UNIQUE(resident_id,event_key)
);
CREATE INDEX resident_notification_inbox ON resident_notification(resident_id,created_at DESC);
