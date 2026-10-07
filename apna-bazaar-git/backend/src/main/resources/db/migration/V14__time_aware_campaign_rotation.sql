ALTER TABLE chat_promotion ADD COLUMN daypart text NOT NULL DEFAULT 'anytime'
 CHECK (daypart IN ('anytime','morning','lunch','afternoon','evening'));

INSERT INTO offering(id,provider_id,category_id,name,description,offering_type,base_price,unit,is_available)
SELECT md5('heyhood-demo-bajji-'||c.slug)::uuid,p.id,k.id,'Mirchi bajji',
 'FICTIONAL DEMO LISTING: Mirchi bajji with chutney. Evening pickup 16:00–20:00; confirm with the provider.',
 'food_item',60,'plate',true
FROM community c JOIN provider p ON p.community_id=c.id AND p.shop_name='Neighbour Kitchen · Demo'
JOIN category k ON k.name='Sweets & Snacks' WHERE c.slug IN ('tridasa','sayuk') ON CONFLICT(id) DO NOTHING;
INSERT INTO offering_schedule(id,offering_id,day_scope,serves_from,serves_to,accepts_preorder,accepts_realtime)
SELECT md5('heyhood-demo-bajji-schedule-'||c.slug)::uuid,md5('heyhood-demo-bajji-'||c.slug)::uuid,
 'daily','16:00','20:00',false,true FROM community c WHERE c.slug IN ('tridasa','sayuk') ON CONFLICT(id) DO NOTHING;

-- Replace only the previous seeded category campaigns; preserve operator-created campaigns.
UPDATE chat_promotion m SET active=false FROM community c,category k
WHERE m.community_id=c.id AND m.id=md5('apna-demo-category-campaign-'||c.slug||'-'||k.name)::uuid;
CREATE TEMP TABLE hood_campaigns(key text,shop text,item text,title text,body text,cta text,daypart text) ON COMMIT DROP;
INSERT INTO hood_campaigns VALUES
('breakfast','Neighbour Kitchen','Steamed idli with chutney','Breakfast plans?','Want to try idli in your hood? Neighbour Kitchen has an idli plate on its menu. Check the pickup details.','Show me breakfast ↗','morning'),
('batter','Morning Batter','Idli and dosa batter','Breakfast at home?','Your next dosa could start close by. Morning Batter lists fresh batter for your breakfast plans.','Check the batter ↗','morning'),
('biryani','Lakshmi’s Biryani','Chicken biryani','Lunch, sorted?','Want to try biryani in your hood? Lakshmi’s Biryani has a chicken biryani option. Check its menu and schedule.','Take me to biryani ↗','lunch'),
('shake','Cool Sips','Fresh mango shake','A little afternoon break','Fancy a mango shake in your hood? Cool Sips has one on the menu. Check the pickup options.','Show me the shakes ↗','afternoon'),
('bajji','Neighbour Kitchen','Mirchi bajji','Evening snack plans?','Want to try bajji in your hood? Neighbour Kitchen lists mirchi bajji for evening pickup. Check the details.','Take me to bajji ↗','evening'),
('icecream','Scoop Squad','Chocolate ice cream','Make room for a scoop','Ice cream in your hood sounds like a good evening plan. Explore Scoop Squad’s flavours and pickup schedule.','Show me the scoops ↗','evening'),
('kids','Little Champions','Kids karate','Something for the kids','Looking for a new activity close by? Little Champions has kids karate in your hood. Check the batches.','Explore kids classes ↗','anytime'),
('flowers','Petal Stories','Jasmine garland','A little something lovely','Flowers for someone in your hood? Petal Stories has garlands and bouquets to explore.','Pick some petals ↗','anytime'),
('gifts','Wrapped With Love','Personalised mug','A gift, close by','Need a little gift in your hood? Wrapped With Love has personalised options. Browse the catalog.','Find a gift ↗','anytime'),
('repair','Neighbour Fix','Plumbing home visit','That fix you’ve been putting off','A leaking tap doesn’t need to stay on your list. Find a neighbour’s plumbing service and check appointment options.','Check the service ↗','anytime');
INSERT INTO chat_promotion(id,community_id,offering_id,title,body,cta_text,kind,daypart)
SELECT md5('heyhood-rotating-campaign-'||c.slug||'-'||d.key)::uuid,c.id,o.id,d.title,d.body,d.cta,'promoted',d.daypart
FROM community c CROSS JOIN hood_campaigns d
JOIN LATERAL(SELECT o.id FROM offering o JOIN provider p ON p.id=o.provider_id
 WHERE p.community_id=c.id AND p.shop_name=d.shop||' · Demo' AND o.is_available
 ORDER BY CASE WHEN lower(o.name)=lower(d.item) THEN 0 ELSE 1 END,o.name LIMIT 1) o ON true
WHERE c.slug IN ('tridasa','sayuk') ON CONFLICT(id) DO NOTHING;
