CREATE TABLE hood_ride (
 id uuid PRIMARY KEY,
 community_id uuid NOT NULL REFERENCES community(id),
 owner_id uuid,
 kind varchar(10) NOT NULL CHECK(kind IN ('offer','request')),
 direction varchar(10) NOT NULL CHECK(direction IN ('outbound','inbound')),
 destination varchar(120) NOT NULL,
 departure_at timestamptz NOT NULL,
 seats integer NOT NULL CHECK(seats BETWEEN 1 AND 6),
 pickup varchar(120) NOT NULL,
 name varchar(80) NOT NULL,
 flat_number varchar(30),
 whatsapp_number varchar(20) NOT NULL,
 notes varchar(240),
 status varchar(10) NOT NULL DEFAULT 'open' CHECK(status IN ('open','closed')),
 is_demo boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX hood_ride_matching_idx ON hood_ride(community_id,kind,direction,departure_at) WHERE status='open';
INSERT INTO hood_ride(id,community_id,kind,direction,destination,departure_at,seats,pickup,name,flat_number,whatsapp_number,notes)
SELECT md5(c.id::text||':ride:'||x.name||x.kind)::uuid,c.id,x.kind,x.direction,x.destination,
 (((now() AT TIME ZONE 'Asia/Kolkata')::date+1)+x.departure::time) AT TIME ZONE 'Asia/Kolkata',
 x.seats,'Main gate',x.name,x.flat,'919740893534','Fictional pilot trip. Discuss timing and arrangements; no ride is confirmed.'
FROM community c CROSS JOIN (VALUES
 ('offer','outbound','Airport (RGIA)','23:00',2,'Madhav · Demo','T2-314'),
 ('offer','outbound','Airport (RGIA)','22:30',1,'Ananya · Demo','T3-205'),
 ('offer','outbound','HITEC City','09:00',3,'Rahul · Demo','T1-408'),
 ('request','outbound','Airport (RGIA)','23:00',1,'Priya · Demo','T4-211'),
 ('offer','inbound','Airport (RGIA)','22:00',2,'Neha · Demo','T2-617'),
 ('request','inbound','Airport (RGIA)','21:00',1,'Dev · Demo','T1-302')
) AS x(kind,direction,destination,departure,seats,name,flat) WHERE c.slug IN ('tridasa','sayuk');
