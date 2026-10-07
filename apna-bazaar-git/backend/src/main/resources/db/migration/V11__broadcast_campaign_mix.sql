UPDATE chat_promotion m SET kind='broadcast'
FROM community c CROSS JOIN (VALUES('Yoga & Fitness'),('Tuition & Classes'),('Plumbing'),('Electrician'),('Dairy'),('Flowers')) k(name)
WHERE c.slug IN ('tridasa','sayuk') AND m.id=md5('apna-demo-category-campaign-'||c.slug||'-'||k.name)::uuid;
