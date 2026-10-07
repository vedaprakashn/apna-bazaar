ALTER TABLE help_contact ADD COLUMN flat_number varchar(30);
ALTER TABLE help_contact ADD COLUMN specialty varchar(100);
-- Fictional resident profiles; no verified credentials or real telephone numbers.
UPDATE help_contact h SET scope='community',service_area=c.name,
 flat_number=CASE h.name
  WHEN 'Dr. Ananya Rao · Demo' THEN 'T2-304'
  WHEN 'Dr. Vikram Shah · Demo' THEN 'T5-210'
  WHEN 'Meera Nair · Demo' THEN 'T1-206'
  WHEN 'Hood Care Nursing · Demo' THEN 'T3-209'
  WHEN 'Rohan Iyer · Demo' THEN 'T3-118'
  WHEN 'Kavya Menon · Demo' THEN 'T4-402'
  WHEN 'MoveWell Physio · Demo' THEN 'T4-305' END,
 specialty=CASE h.name
  WHEN 'Dr. Ananya Rao · Demo' THEN 'General medicine'
  WHEN 'Dr. Vikram Shah · Demo' THEN 'Paediatrics'
  WHEN 'Meera Nair · Demo' THEN 'Home nursing'
  WHEN 'Hood Care Nursing · Demo' THEN 'Home nursing'
  WHEN 'Rohan Iyer · Demo' THEN 'Civil and property law'
  ELSE 'Physiotherapy' END
FROM community c WHERE h.community_id=c.id AND h.is_demo AND h.section='professional';
UPDATE help_contact SET location='Flat '||flat_number||' · demo' WHERE is_demo AND section='professional' AND flat_number IS NOT NULL;
INSERT INTO help_contact (id,community_id,section,category,name,phone,service_area,availability,location,scope,notes,flat_number,specialty)
SELECT md5(c.id::text||':help:'||x.name)::uuid,c.id,'professional','Doctors',x.name,
 '+91 00000 00000',c.name,'Example: evenings by appointment','Flat '||x.flat||' · demo','community',
 'Fictional resident doctor. Specialty and credentials are unverified; no live booking.',x.flat,x.specialty
FROM community c CROSS JOIN (VALUES
 ('Dr. Sneha Reddy · Demo','T1-507','Cardiology'),
 ('Dr. Arjun Menon · Demo','T3-602','Dermatology'),
 ('Dr. Priya Desai · Demo','T4-308','Orthopaedics')
) AS x(name,flat,specialty) WHERE c.slug IN ('tridasa','sayuk');
INSERT INTO help_contact (id,community_id,section,category,name,phone,service_area,availability,location,scope,notes)
SELECT md5(c.id::text||':help:ambulance')::uuid,c.id,'urgent','Ambulance','Nearby Ambulance Desk · Demo',
 '+91 00000 00000','Hyderabad · nearby area (demo)','Hours not verified','Nearby response service · demo','nearby',
 'Fictional ambulance listing. In a real emergency in India, call 112.'
FROM community c WHERE c.slug IN ('tridasa','sayuk');
