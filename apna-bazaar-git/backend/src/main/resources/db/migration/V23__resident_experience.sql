CREATE TABLE resident_profile (
 id uuid PRIMARY KEY, community_id uuid NOT NULL REFERENCES community(id), token_hash varchar(64) NOT NULL,
 name varchar(80) NOT NULL, flat_number varchar(30) NOT NULL, share_name boolean NOT NULL DEFAULT false,
 share_flat boolean NOT NULL DEFAULT false, verification varchar(20) NOT NULL DEFAULT 'pending'
 CHECK(verification IN ('pending','verified','rejected')), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE resident_saved (
 resident_id uuid NOT NULL REFERENCES resident_profile(id), kind varchar(15) NOT NULL CHECK(kind IN ('provider','ride','plan')),
 entity_id uuid NOT NULL, following boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(resident_id,kind,entity_id)
);
CREATE TABLE hood_request (
 id uuid PRIMARY KEY, community_id uuid NOT NULL REFERENCES community(id), resident_id uuid NOT NULL REFERENCES resident_profile(id),
 title varchar(120) NOT NULL, body varchar(1000) NOT NULL, expires_at timestamptz NOT NULL,
 status varchar(12) NOT NULL DEFAULT 'open' CHECK(status IN ('open','matched','closed')), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE hood_request_response (
 id uuid PRIMARY KEY, request_id uuid NOT NULL REFERENCES hood_request(id), resident_id uuid NOT NULL REFERENCES resident_profile(id),
 body varchar(1000) NOT NULL, status varchar(12) NOT NULL DEFAULT 'offered' CHECK(status IN ('offered','accepted','declined')),
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(request_id,resident_id)
);
CREATE INDEX hood_request_discovery_idx ON hood_request(community_id,expires_at) WHERE status='open';
ALTER TABLE offering ADD COLUMN live_status varchar(20) NOT NULL DEFAULT 'unconfirmed'
 CHECK(live_status IN ('unconfirmed','available','sold_out','preorder'));
ALTER TABLE offering ADD COLUMN availability_updated_at timestamptz;
ALTER TABLE hood_ride ADD COLUMN arrangement varchar(20) NOT NULL DEFAULT 'lift' CHECK(arrangement IN ('lift','shared_cab','school_run'));
ALTER TABLE hood_ride ADD COLUMN recurrence_until date;
ALTER TABLE hood_ride ADD COLUMN weekdays integer[];
ALTER TABLE hood_ride ADD COLUMN exchange_terms varchar(240) NOT NULL DEFAULT '';
ALTER TABLE hood_ride ADD CONSTRAINT ride_recurrence_fields CHECK((recurrence_until IS NULL AND weekdays IS NULL) OR (recurrence_until IS NOT NULL AND weekdays IS NOT NULL AND cardinality(weekdays)>0 AND weekdays <@ ARRAY[1,2,3,4,5,6,7]));
INSERT INTO hood_ride(id,community_id,kind,direction,destination,departure_at,seats,pickup,name,flat_number,whatsapp_number,notes,arrangement,recurrence_until,weekdays,exchange_terms)
SELECT md5(c.id::text||':routine:'||x.name)::uuid,c.id,x.kind,x.direction,x.destination,
 ((now() AT TIME ZONE 'Asia/Kolkata')::date+1+x.time)::timestamp AT TIME ZONE 'Asia/Kolkata',2,'Main gate',x.name,x.flat,'919740893534',
 'Fictional example. Parents must agree schedules, driver and handover.','school_run',(now() AT TIME ZONE 'Asia/Kolkata')::date+14,ARRAY[1,2,3,4,5],x.terms
FROM community c CROSS JOIN (VALUES
 ('Asha · Demo','offer','inbound','Oakridge school','15:30'::time,'T2-204','Can help with afternoon pickups; looking for morning drops in exchange.'),
 ('Vikram · Demo','request','inbound','Oakridge school','15:30'::time,'T3-305','Need afternoon pickups. I can handle morning drops in return.'),
 ('Meera · Demo','offer','outbound','DPS school','08:00'::time,'T1-206','Morning school run; happy to discuss alternating days.')
) AS x(name,kind,direction,destination,time,flat,terms) WHERE c.slug IN ('tridasa','sayuk');
INSERT INTO hood_ride(id,community_id,kind,direction,destination,departure_at,seats,pickup,name,flat_number,whatsapp_number,notes,arrangement)
SELECT md5(c.id::text||':shared-cab')::uuid,c.id,'offer','outbound','Airport (RGIA)',
 ((now() AT TIME ZONE 'Asia/Kolkata')::date+1+'23:00'::time)::timestamp AT TIME ZONE 'Asia/Kolkata',2,'Main gate','Kiran · Demo','T4-402','919740893534','Split the actual cab fare by agreement. No cab has been booked.','shared_cab'
FROM community c WHERE c.slug IN ('tridasa','sayuk');
