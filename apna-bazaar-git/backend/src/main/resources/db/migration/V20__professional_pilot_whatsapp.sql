ALTER TABLE help_contact ADD COLUMN whatsapp_number varchar(30);
-- Pilot routing only: these fictional residents do not have real contact numbers.
UPDATE help_contact SET whatsapp_number='919740893534' WHERE is_demo AND section='professional';
