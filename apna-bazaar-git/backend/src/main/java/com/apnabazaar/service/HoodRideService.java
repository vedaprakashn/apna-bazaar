package com.apnabazaar.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
@Service
public class HoodRideService {
 private final JdbcTemplate jdbc;
 public HoodRideService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<Map<String,Object>> find(UUID community,String kind,String direction,String destination,Instant at,int seats,UUID visitor){
  Instant from=at==null?Instant.now():at.minusSeconds(3600),to=at==null?Instant.now().plusSeconds(7*86400):at.plusSeconds(3600);
  String term=destination==null?"":destination.trim().toLowerCase(Locale.ROOT);
  if(Set.of("rgia","shamshabad","rajiv gandhi international airport").contains(term))term="airport";
  String pattern="%"+term.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
  return jdbc.queryForList("""
   SELECT id,kind,direction,destination,departure_at,seats,pickup,name,flat_number,
    whatsapp_number,notes,status,is_demo,(owner_id=?) AS mine
   FROM hood_ride WHERE community_id=? AND status='open' AND departure_at>now()
    AND kind=? AND direction=? AND lower(destination) LIKE ?
    AND departure_at BETWEEN ? AND ? AND seats>=?
   ORDER BY departure_at LIMIT 30
   """,visitor,community,kind,direction,pattern,java.sql.Timestamp.from(from),java.sql.Timestamp.from(to),seats);
 }
}
