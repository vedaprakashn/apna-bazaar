-- Fictional resident identities for demo cards; no real contact information.
WITH identities(shop,name,flat) AS (VALUES
('Neighbour Kitchen','Anitha Rao · Demo','T1-101'),
('Morning Batter','Lakshmi Devi · Demo','T2-102'),
('Cool Sips','Meena Sharma · Demo','T3-103'),
('Weekend Bakehouse','Neha Kapoor · Demo','T4-104'),
('Freezer Favourites','Priya Reddy · Demo','T1-105'),
('Green Basket','Kavitha Reddy · Demo','T2-106'),
('Nut Corner','Rahul Jain · Demo','T3-107'),
('Pure Pantry','Suresh Kumar · Demo','T4-108'),
('Daily Dairy','Deepa Nair · Demo','T1-109'),
('Colour Crew','Ravi Verma · Demo','T2-110'),
('Sunday Shine','Arjun Singh · Demo','T3-111'),
('Kitchen Care','Vinod Kumar · Demo','T4-112'),
('Neighbour Fix','Prakash Rao · Demo','T1-113'),
('Bright Home','Manoj Yadav · Demo','T2-114'),
('Little Champions','Kiran Reddy · Demo','T3-115'),
('Calm Circle','Asha Menon · Demo','T4-116'),
('Melody Room','Sowmya Iyer · Demo','T1-117'),
('Rhythm Studio','Nandini Gupta · Demo','T2-118'),
('Creative Club','Divya Patel · Demo','T3-119'),
('Bright Letters','Rekha Sharma · Demo','T4-120'),
('Home Sparkle','Sunil Kumar · Demo','T1-121'),
('Cool Air Care','Vikram Rao · Demo','T2-122'),
('Two Wheel Care','Rohit Singh · Demo','T3-123'),
('Friday Chaat Club','Pooja Shah · Demo','T4-124'))
UPDATE provider p SET name=d.name,flat_number=d.flat
FROM identities d,community c
WHERE p.community_id=c.id AND c.slug IN ('tridasa','sayuk')
  AND p.id=md5('apna-demo-provider-'||c.slug||'-'||d.shop)::uuid
  AND p.name LIKE 'Demo • %';
