ALTER TABLE provider ADD COLUMN whatsapp_group_url varchar(400);
-- Keep the individual number; the configured group takes precedence in the UI.
UPDATE provider SET whatsapp_group_url = 'https://chat.whatsapp.com/B88QvxEoABCLa2wPEpy9SY';
