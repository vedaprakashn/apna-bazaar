package com.apnabazaar.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class HelpDirectoryService {
 public static final Set<String> CATEGORIES = Set.of("Ambulance","Doctors","Nurses","Lawyers","Physiotherapists","First aid","Police","Fire services","Snake rescue");
 private final JdbcTemplate jdbc;
 public HelpDirectoryService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public List<Map<String,Object>> contacts(UUID communityId,List<String> categories,String specialty) {
  var rows=jdbc.queryForList("""
   SELECT id,section,category,name,phone,service_area,availability,location,scope,
    notes,is_demo,consent_to_listing,verified_at,verification_source,flat_number,specialty,whatsapp_number
   FROM help_contact WHERE community_id=? AND is_active=true AND (scope='community' OR section='urgent') ORDER BY section,category,name
   """,communityId);
  return rows.stream().filter(c->categories.isEmpty()||categories.contains(c.get("category"))).filter(c->specialty==null||specialty.isBlank()||specialty.equalsIgnoreCase(String.valueOf(c.get("specialty")))).toList();
 }
}
