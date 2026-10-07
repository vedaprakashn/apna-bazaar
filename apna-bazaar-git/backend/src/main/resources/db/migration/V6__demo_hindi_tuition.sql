-- Add a genuine catalog fact for the Hindi-tuition demo. AI must not invent this skill.
INSERT INTO provider (id,community_id,name,shop_name,flat_number,provider_type,status,is_verified,rating,review_count)
SELECT md5('apna-demo-hindi-provider-'||slug)::uuid,id,'Suman Sharma · Demo','Language Corner · Demo','T2-214','tuition','active',false,0,0
FROM community WHERE slug IN ('tridasa','sayuk') ON CONFLICT (id) DO NOTHING;
INSERT INTO offering (id,provider_id,category_id,name,description,offering_type,base_price,unit,is_available)
SELECT md5('apna-demo-hindi-offering-'||c.slug)::uuid,md5('apna-demo-hindi-provider-'||c.slug)::uuid,k.id,
'Hindi tuition for school children',
'FICTIONAL DEMO LISTING: Hindi language tuition, reading, writing, grammar and school exam support for classes 1–10. हिंदी की ट्यूशन: पढ़ना, लिखना और व्याकरण. Weekdays 17:00–18:00.',
'class',900,'month',true
FROM community c CROSS JOIN category k WHERE c.slug IN ('tridasa','sayuk') AND k.name='Tuition & Classes' ON CONFLICT (id) DO NOTHING;
INSERT INTO offering_schedule (id,offering_id,day_scope,serves_from,serves_to,accepts_preorder,accepts_realtime)
SELECT md5('apna-demo-hindi-schedule-'||slug)::uuid,md5('apna-demo-hindi-offering-'||slug)::uuid,'weekdays','17:00','18:00',false,true
FROM community WHERE slug IN ('tridasa','sayuk') ON CONFLICT (id) DO NOTHING;
