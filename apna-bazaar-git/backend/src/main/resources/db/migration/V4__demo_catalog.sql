-- Explicitly fictional pilot catalog. No real contacts and no fabricated analytics.
CREATE TEMP TABLE demo_catalog (category text, shop text, item text, description text, price numeric, unit text, provider_type text, offering_type text, day_scope text, starts text, ends text) ON COMMIT DROP;
INSERT INTO demo_catalog VALUES
('Food & Tiffin','Neighbour Kitchen','Vegetarian lunch box','Rice, dal and two vegetables; lunch daily 12:00–14:00; preorder by 10:00','150','meal','food_seller','food_item','daily','12:00','14:00'),
('Batter & Dough','Morning Batter','Idli and dosa batter','Fresh fermented batter; available daily 06:30–09:00','80','kg','food_seller','food_item','daily','06:30','09:00'),
('Juices & Shakes','Cool Sips','Fresh mango shake','Fresh fruit shakes and mosambi juice; daily 10:00–19:00','90','glass','food_seller','food_item','daily','10:00','19:00'),
('Sweets & Snacks','Weekend Bakehouse','Chocolate muffins','Eggless chocolate muffins and birthday cupcakes; weekends 16:00–20:00','60','piece','food_seller','food_item','weekends','16:00','20:00'),
('Frozen Food','Freezer Favourites','Frozen hara bhara kabab','Ready-to-cook vegetarian snacks; daily pickup 17:00–20:00','180','pack','food_seller','food_item','daily','17:00','20:00'),
('Vegetables & Greens','Green Basket','Seasonal vegetable basket','Fresh vegetables, leafy greens and coriander; morning pickup','220','basket','grocery','grocery_item','daily','07:00','10:00'),
('Dry Fruits & Nuts','Nut Corner','Premium almonds and cashews','Almonds, cashews and Diwali dry fruit gift boxes','450','500 g','grocery','grocery_item','daily','09:00','20:00'),
('Oils & Spices','Pure Pantry','Cold-pressed groundnut oil','Cold-pressed groundnut and sesame oil; spice powders','350','litre','grocery','grocery_item','daily','09:00','20:00'),
('Dairy','Daily Dairy','Fresh paneer','Fresh paneer, curd and milk; morning collection','120','250 g','grocery','grocery_item','daily','07:00','10:00'),
('Home Painting','Colour Crew','Home painting consultation','Interior painting, wall touch-ups and rental move-in painting','500','consultation','service','service_package','daily','09:00','18:00'),
('Deep Cleaning','Sunday Shine','Sunday car wash','Exterior car wash in your parking slot; Sunday mornings only','300','visit','service','service_package','sunday_only','08:00','12:00'),
('Chimney Cleaning','Kitchen Care','Chimney cleaning','Kitchen chimney degreasing and filter cleaning; book a home visit','800','visit','service','service_package','daily','09:00','18:00'),
('Plumbing','Neighbour Fix','Plumbing home visit','Leaking taps, blocked sinks and pipe repairs','350','visit','service','service_package','daily','08:00','19:00'),
('Electrician','Bright Home','Electrical repair visit','Switches, sockets, ceiling fans and light fittings','350','visit','service','service_package','daily','08:00','19:00'),
('Tuition & Classes','Little Champions','Kids karate','Beginner karate ages 6–12; Saturday and Sunday 09:00–10:00 in the clubhouse','1200','month','tuition','class','weekends','09:00','10:00'),
('Yoga & Fitness','Calm Circle','Morning yoga','Adults beginner yoga and guided meditation; weekdays 06:00–07:00','1500','month','tuition','class','weekdays','06:00','07:00'),
('Tuition & Classes','Melody Room','Carnatic singing','Carnatic vocal lessons and music basics for kids; weekends 11:00–12:00','1800','month','tuition','class','weekends','11:00','12:00'),
('Tuition & Classes','Rhythm Studio','Kathak dance','Kids Kathak and dance lessons; weekends 16:00–17:00','1600','month','tuition','class','weekends','16:00','17:00'),
('Tuition & Classes','Creative Club','Painting and pottery workshop','Weekend painting and pottery workshop for children and adults','500','session','tuition','class','weekends','15:00','17:00'),
('Tuition & Classes','Bright Letters','Handwriting improvement','Handwriting practice for children ages 7–14; weekdays 17:00–18:00','900','month','tuition','class','weekdays','17:00','18:00'),
('Deep Cleaning','Home Sparkle','Apartment deep cleaning','Kitchen, bathroom and complete apartment cleaning by appointment','2500','visit','service','service_package','daily','09:00','18:00'),
('Electrician','Cool Air Care','AC servicing','Air conditioner cleaning, cooling diagnostics and repair home visits','650','visit','service','service_package','daily','09:00','18:00'),
('Deep Cleaning','Two Wheel Care','Bike servicing','Bike maintenance and minor repairs at your parking slot on Sunday','550','visit','service','service_package','sunday_only','09:00','13:00'),
('Sweets & Snacks','Friday Chaat Club','Friday chaat pop-up','Pani puri, bhel and samosa chaat; Friday evenings 17:00–20:00','80','plate','food_seller','food_item','specific_days','17:00','20:00');
INSERT INTO provider (id,community_id,name,shop_name,flat_number,whatsapp_number,provider_type,status,is_verified,rating,review_count)
SELECT md5('apna-demo-provider-'||c.slug||'-'||d.shop)::uuid,c.id,'Demo • '||d.shop,d.shop||' · Demo','Demo clubhouse',NULL,d.provider_type::provider_type_enum,'active',false,0,0
FROM demo_catalog d CROSS JOIN community c WHERE c.slug IN ('tridasa','sayuk') ON CONFLICT (id) DO NOTHING;
INSERT INTO offering (id,provider_id,category_id,name,description,offering_type,base_price,unit,is_available)
SELECT md5('apna-demo-offering-'||c.slug||'-'||d.shop)::uuid,md5('apna-demo-provider-'||c.slug||'-'||d.shop)::uuid,k.id,d.item,'FICTIONAL DEMO LISTING: '||d.description,d.offering_type::offering_type_enum,d.price,d.unit,true
FROM demo_catalog d JOIN category k ON k.name=d.category CROSS JOIN community c WHERE c.slug IN ('tridasa','sayuk') ON CONFLICT (id) DO NOTHING;
INSERT INTO offering_schedule (id,offering_id,day_scope,days_of_week,serves_from,serves_to,accepts_preorder,accepts_realtime)
SELECT md5('apna-demo-schedule-'||c.slug||'-'||d.shop)::uuid,md5('apna-demo-offering-'||c.slug||'-'||d.shop)::uuid,d.day_scope::day_scope_enum,CASE WHEN d.day_scope='specific_days' THEN 'FRI' ELSE NULL END,d.starts::time,d.ends::time,false,true
FROM demo_catalog d CROSS JOIN community c WHERE c.slug IN ('tridasa','sayuk') ON CONFLICT (id) DO NOTHING;
