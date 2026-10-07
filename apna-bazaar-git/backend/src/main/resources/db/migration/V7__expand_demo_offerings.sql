-- Expand only fictional demo catalogs, retaining each provider's existing category and schedule.
CREATE TEMP TABLE demo_extra_items (shop text, item text, price numeric, unit text) ON COMMIT DROP;
INSERT INTO demo_extra_items VALUES
('Neighbour Kitchen','Steamed idli with chutney',50,'plate'),
('Neighbour Kitchen','Masala dosa',75,'plate'),
('Neighbour Kitchen','Onion uttapam',70,'plate'),
('Morning Batter','Ragi dosa batter',100,'kg'),
('Morning Batter','Adai mixed lentil batter',120,'kg'),
('Morning Batter','Pesarattu green gram batter',110,'kg'),
('Cool Sips','Banana almond shake',100,'glass'),
('Cool Sips','Chocolate milkshake',120,'glass'),
('Cool Sips','Fresh mosambi juice',80,'glass'),
('Weekend Bakehouse','Blueberry muffins',75,'piece'),
('Weekend Bakehouse','Eggless vanilla cupcakes',65,'piece'),
('Weekend Bakehouse','Chocolate brownies',90,'piece'),
('Freezer Favourites','Frozen vegetable momos',160,'pack'),
('Freezer Favourites','Frozen paneer cutlets',200,'pack'),
('Freezer Favourites','Frozen aloo tikki',140,'pack'),
('Green Basket','Leafy greens bundle',60,'bundle'),
('Green Basket','Tomato and onion combo',100,'kg'),
('Green Basket','Fresh coriander and mint',30,'bundle'),
('Nut Corner','Premium cashews',500,'500 g'),
('Nut Corner','Seedless raisins',220,'500 g'),
('Nut Corner','Festive dry fruit gift box',900,'box'),
('Pure Pantry','Cold-pressed sesame oil',420,'litre'),
('Pure Pantry','Homemade sambar powder',120,'200 g'),
('Pure Pantry','Homemade idli podi',100,'200 g'),
('Daily Dairy','Homemade curd',60,'500 g'),
('Daily Dairy','Fresh cow milk',70,'litre'),
('Daily Dairy','Homemade ghee',450,'500 g'),
('Colour Crew','Single room wall painting',3500,'room'),
('Colour Crew','Wall touch-up service',1200,'visit'),
('Colour Crew','Balcony waterproof coating',2800,'package'),
('Sunday Shine','Car interior vacuum cleaning',250,'visit'),
('Sunday Shine','Car wash and interior combo',500,'visit'),
('Sunday Shine','Car dashboard polishing',200,'visit'),
('Kitchen Care','Chimney filter deep cleaning',500,'visit'),
('Kitchen Care','Kitchen exhaust fan cleaning',450,'visit'),
('Kitchen Care','Hob and burner cleaning',600,'visit'),
('Neighbour Fix','Tap replacement',250,'job'),
('Neighbour Fix','Blocked sink cleaning',450,'job'),
('Neighbour Fix','Flush tank repair',500,'job'),
('Bright Home','Ceiling fan installation',400,'job'),
('Bright Home','Switch and socket replacement',200,'job'),
('Bright Home','Light fitting installation',250,'job'),
('Little Champions','Beginner kids karate ages 6–8',1200,'month'),
('Little Champions','Intermediate karate ages 9–12',1500,'month'),
('Little Champions','Kids karate trial session',200,'session'),
('Calm Circle','Guided meditation circle',400,'session'),
('Calm Circle','Beginner pranayama classes',1000,'month'),
('Calm Circle','Gentle stretching and mobility',1200,'month'),
('Melody Room','Carnatic beginner vocal batch',1800,'month'),
('Melody Room','Carnatic rhythm and swara practice',1000,'month'),
('Melody Room','One-to-one Carnatic vocal lesson',600,'session'),
('Rhythm Studio','Beginner Kathak ages 6–10',1600,'month'),
('Rhythm Studio','Adult beginner Kathak batch',2000,'month'),
('Rhythm Studio','Kathak footwork workshop',500,'session'),
('Creative Club','Watercolour painting workshop',450,'session'),
('Creative Club','Clay pottery workshop',650,'session'),
('Creative Club','Kids acrylic painting workshop',500,'session'),
('Bright Letters','English cursive handwriting',900,'month'),
('Bright Letters','Print handwriting for beginners',800,'month'),
('Bright Letters','Handwriting speed and neatness',1000,'month'),
('Home Sparkle','Bathroom deep cleaning',700,'bathroom'),
('Home Sparkle','Kitchen deep cleaning',1200,'kitchen'),
('Home Sparkle','Sofa shampoo cleaning',1500,'sofa'),
('Cool Air Care','Split AC filter cleaning',450,'unit'),
('Cool Air Care','AC cooling diagnostic visit',500,'visit'),
('Cool Air Care','AC annual maintenance package',1800,'year'),
('Two Wheel Care','Bike oil change service',350,'job'),
('Two Wheel Care','Bike chain cleaning and lubrication',250,'job'),
('Two Wheel Care','Bike brake adjustment',200,'job'),
('Friday Chaat Club','Pani puri',50,'plate'),
('Friday Chaat Club','Bhel puri',60,'plate'),
('Friday Chaat Club','Samosa chaat',80,'plate'),
('Language Corner','Hindi reading and pronunciation',900,'month'),
('Language Corner','Hindi writing and grammar',1000,'month'),
('Language Corner','Hindi exam revision classes 6–10',1200,'month');
INSERT INTO offering (id,provider_id,category_id,name,description,offering_type,base_price,unit,is_available)
SELECT md5('apna-demo-extra-'||p.id::text||'-'||d.item)::uuid,p.id,base.category_id,d.item,
'FICTIONAL DEMO LISTING: '||d.item||'. Same collection or appointment hours as the provider schedule.',
base.offering_type,d.price,d.unit,true
FROM demo_extra_items d JOIN provider p ON p.shop_name=d.shop||' · Demo'
JOIN community c ON c.id=p.community_id AND c.slug IN ('tridasa','sayuk')
JOIN LATERAL (SELECT o.category_id,o.offering_type FROM offering o WHERE o.provider_id=p.id ORDER BY o.created_at,o.id LIMIT 1) base ON true
WHERE p.id=md5('apna-demo-provider-'||c.slug||'-'||d.shop)::uuid
   OR (d.shop='Language Corner' AND p.id=md5('apna-demo-hindi-provider-'||c.slug)::uuid)
ON CONFLICT (id) DO NOTHING;
INSERT INTO offering_schedule (id,offering_id,day_scope,days_of_week,serves_from,serves_to,accepts_preorder,accepts_realtime)
SELECT md5('apna-demo-extra-schedule-'||o.id::text)::uuid,o.id,s.day_scope,s.days_of_week,s.serves_from,s.serves_to,s.accepts_preorder,s.accepts_realtime
FROM demo_extra_items d JOIN provider p ON p.shop_name=d.shop||' · Demo'
JOIN offering o ON o.id=md5('apna-demo-extra-'||p.id::text||'-'||d.item)::uuid
JOIN LATERAL (SELECT os.* FROM offering_schedule os JOIN offering original ON original.id=os.offering_id WHERE original.provider_id=p.id AND original.id<>o.id ORDER BY original.created_at,original.id LIMIT 1) s ON true
ON CONFLICT (id) DO NOTHING;
