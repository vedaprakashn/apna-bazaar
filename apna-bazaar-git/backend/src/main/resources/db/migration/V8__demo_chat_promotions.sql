CREATE TABLE chat_promotion (
 id UUID PRIMARY KEY, community_id UUID NOT NULL REFERENCES community(id),
 offering_id UUID NOT NULL REFERENCES offering(id), title TEXT NOT NULL,
 kind TEXT NOT NULL CHECK (kind IN ('broadcast','promoted')), active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE chat_promotion_delivery (
 id UUID PRIMARY KEY, promotion_id UUID NOT NULL REFERENCES chat_promotion(id),
 session_id UUID NOT NULL, viewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), clicked_at TIMESTAMPTZ
);
CREATE INDEX idx_promotion_delivery_viewed ON chat_promotion_delivery(promotion_id,viewed_at);
INSERT INTO chat_promotion(id,community_id,offering_id,title,kind)
SELECT md5('apna-demo-promotion-'||c.slug||'-'||d.shop)::uuid,c.id,
 md5('apna-demo-offering-'||c.slug||'-'||d.shop)::uuid,d.title,d.kind
FROM community c CROSS JOIN (VALUES
 ('Neighbour Kitchen','A little home-cooked inspiration','broadcast'),
 ('Cool Sips','Shake up your afternoon','promoted'),
 ('Weekend Bakehouse','Make room for a weekend treat','promoted'),
 ('Creative Club','Plan a creative weekend','broadcast')
) d(shop,title,kind)
WHERE c.slug IN ('tridasa','sayuk');
