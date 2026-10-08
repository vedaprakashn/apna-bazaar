package com.apnabazaar.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
@Service
public class HoodRideService {
 private final JdbcTemplate jdbc;
 public HoodRideService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<Map<String,Object>> find(UUID community,String kind,String direction,String destination,Instant at,int seats,UUID visitor){return find(community,kind,direction,destination,at,seats,visitor,"",null);}
 public List<Map<String,Object>> find(UUID community,String kind,String direction,String destination,Instant at,int seats,UUID visitor,String arrangement,Instant until){
  Instant from=at==null?Instant.now():at.minusSeconds(3600),to=until!=null?until:(at==null?Instant.now().plusSeconds(7*86400):at.plusSeconds(3600));
  String term=destination==null?"":destination.trim().toLowerCase(Locale.ROOT);
  if(Set.of("rgia","shamshabad","rajiv gandhi international airport").contains(term))term="airport";
  String pattern="%"+term.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
  return jdbc.queryForList("""
   SELECT r.id,r.kind,r.direction,r.destination,occ.departure_at,r.seats,r.pickup,
    CASE WHEN p.id IS NULL THEN r.name WHEN p.share_name THEN p.name ELSE 'A neighbour' END AS name,
    CASE WHEN p.id IS NULL THEN r.flat_number WHEN p.share_flat THEN p.flat_number ELSE NULL END AS flat_number,
    COALESCE(p.verification,'demo') AS verification,r.whatsapp_number,r.notes,r.status,r.is_demo,
    r.arrangement,r.recurrence_until,array_to_string(r.weekdays,',') AS weekdays,r.exchange_terms,(r.owner_id=?) AS mine
   FROM hood_ride r LEFT JOIN resident_profile p ON p.id=r.owner_id
   JOIN LATERAL (
    SELECT min(x.at) AS departure_at FROM (
     SELECT r.departure_at AS at WHERE r.recurrence_until IS NULL
     UNION ALL
     SELECT (day::date + (r.departure_at AT TIME ZONE 'Asia/Kolkata')::time) AT TIME ZONE 'Asia/Kolkata'
     FROM generate_series(GREATEST((?::timestamptz AT TIME ZONE 'Asia/Kolkata')::date,(r.departure_at AT TIME ZONE 'Asia/Kolkata')::date),
       LEAST((?::timestamptz AT TIME ZONE 'Asia/Kolkata')::date,r.recurrence_until),interval '1 day') day
     WHERE r.recurrence_until IS NOT NULL AND extract(isodow FROM day)::int=ANY(r.weekdays)
    ) x WHERE x.at>NOW() AND x.at BETWEEN ? AND ?
   ) occ ON occ.departure_at IS NOT NULL
   WHERE r.community_id=? AND r.status='open' AND r.kind=? AND r.direction=?
    AND lower(r.destination) LIKE ? AND r.seats>=? AND (?='' OR r.arrangement=?)
   ORDER BY occ.departure_at LIMIT 30
   """,visitor,java.sql.Timestamp.from(from),java.sql.Timestamp.from(to),java.sql.Timestamp.from(from),java.sql.Timestamp.from(to),community,kind,direction,pattern,seats,arrangement,arrangement);
 }
}
