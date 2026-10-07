CREATE TABLE hood_plan (
 id uuid PRIMARY KEY,
 community_id uuid NOT NULL REFERENCES community(id),
 title varchar(180) NOT NULL,
 description text NOT NULL,
 category varchar(60) NOT NULL,
 starts_at timestamptz NOT NULL,
 location varchar(160) NOT NULL,
 minimum_interested integer NOT NULL CHECK (minimum_interested BETWEEN 2 AND 1000),
 is_demo boolean NOT NULL DEFAULT true,
 status varchar(20) NOT NULL DEFAULT 'gathering' CHECK (status IN ('gathering','confirmed','cancelled'))
);
CREATE TABLE hood_plan_vote (
 plan_id uuid NOT NULL REFERENCES hood_plan(id) ON DELETE CASCADE,
 visitor_id uuid NOT NULL,
 choice varchar(10) NOT NULL CHECK (choice IN ('in','pass')),
 updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(plan_id,visitor_id)
);
INSERT INTO hood_plan(id,community_id,title,description,category,starts_at,location,minimum_interested)
SELECT md5(c.id::text||':plan:'||x.title)::uuid,c.id,x.title,x.description,x.category,x.starts_at::timestamptz,x.location,x.minimum
FROM community c CROSS JOIN (VALUES
 ('Bicycle service workshop','Bring your cycle for a community servicing workshop. We need 20 interested neighbours before an organiser can confirm it.','Repair & care','2026-10-11 09:00:00+05:30','Clubhouse courtyard · demo',20),
 ('Pottery, chai & a little mess','A hands-on pottery session for neighbours. Materials and final pricing will be shared if enough people are interested.','Creative weekends','2026-10-18 16:00:00+05:30','Activity room · demo',12),
 ('E-waste collection drive','Got old chargers, cables or gadgets? Let’s gather interest for a community collection drive.','Community action','2026-10-17 10:00:00+05:30','Main gate collection point · demo',15),
 ('Sunday kids’ football','A beginner-friendly football morning. Register interest so an organiser can arrange the right group size.','Kids & sport','2026-10-11 07:00:00+05:30','Community play area · demo',16)
) AS x(title,description,category,starts_at,location,minimum)
WHERE c.slug IN ('tridasa','sayuk');
