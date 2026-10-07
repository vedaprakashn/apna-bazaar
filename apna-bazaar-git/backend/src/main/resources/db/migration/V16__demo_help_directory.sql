CREATE TABLE help_contact (
 id uuid PRIMARY KEY,
 community_id uuid NOT NULL REFERENCES community(id),
 section varchar(20) NOT NULL CHECK (section IN ('urgent','professional')),
 category varchar(60) NOT NULL,
 name varchar(160) NOT NULL,
 phone varchar(30),
 service_area varchar(200) NOT NULL,
 availability varchar(160) NOT NULL,
 location varchar(200),
 scope varchar(20) NOT NULL CHECK (scope IN ('community','nearby')),
 notes text,
 is_demo boolean NOT NULL DEFAULT true,
 consent_to_listing boolean NOT NULL DEFAULT false,
 verified_at timestamptz,
 verification_source text,
 is_active boolean NOT NULL DEFAULT true
);
CREATE INDEX help_contact_community_idx ON help_contact(community_id, section) WHERE is_active;
-- Fictional examples deliberately have non-dialable numbers and no verification claim.
INSERT INTO help_contact (id,community_id,section,category,name,phone,service_area,availability,location,scope,notes)
SELECT md5(c.id::text || ':help:' || x.name)::uuid,c.id,x.section,x.category,x.name,
 '+91 00000 00000', CASE WHEN x.scope='community' THEN c.name ELSE 'Hyderabad · nearby area (demo)' END,
 x.availability,x.location,x.scope,x.notes
FROM community c CROSS JOIN (VALUES
 ('urgent','First aid','Community First Aid Desk','Example: 8am–8pm','Clubhouse · demo','community','Fictional first-aid desk. Not an emergency response service.'),
 ('urgent','Police','Neighbourhood Police Station','Hours not verified','Nearby station · demo','nearby','Example listing; replace with an official, verified local police number.'),
 ('urgent','Fire services','Nearby Fire Response Desk','Hours not verified','Nearby fire station · demo','nearby','Example listing; for a real emergency in India call 112.'),
 ('urgent','Snake rescue','Hood Wildlife Rescue','Example: 7am–7pm','Nearby rescue team · demo','nearby','Fictional rescue listing. Keep your distance and contact trained help.'),
 ('professional','Doctors','Dr. Ananya Rao · Demo','Example: Mon–Sat, 9am–1pm','T2-304 · demo','community','General physician. Fictional identity; qualifications are not verified.'),
 ('professional','Doctors','Dr. Vikram Shah · Demo','Example: Mon–Fri, 5pm–8pm','Nearby clinic · demo','nearby','Family medicine example. No live appointments available.'),
 ('professional','Nurses','Meera Nair · Demo','Example: Mon–Sat, 8am–6pm','T1-206 · demo','community','Home nursing example. Services and credentials are not verified.'),
 ('professional','Nurses','Hood Care Nursing · Demo','Example: by appointment','Nearby care centre · demo','nearby','Fictional home-care team; no confirmed availability.'),
 ('professional','Lawyers','Rohan Iyer · Demo','Example: weekdays, 6pm–8pm','T3-118 · demo','community','Property and civil matters example. No verified professional registration.'),
 ('professional','Physiotherapists','Kavya Menon · Demo','Example: Mon–Sat, 10am–5pm','T4-402 · demo','community','Physiotherapy and mobility support example; no booking integration.'),
 ('professional','Physiotherapists','MoveWell Physio · Demo','Example: weekdays, 9am–7pm','Nearby studio · demo','nearby','Fictional nearby physiotherapy studio. Contact details are placeholders.')
) AS x(section,category,name,availability,location,scope,notes)
WHERE c.slug IN ('tridasa','sayuk');
