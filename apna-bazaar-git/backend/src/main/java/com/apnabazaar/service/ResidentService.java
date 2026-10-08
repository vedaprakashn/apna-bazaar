package com.apnabazaar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

@Service @RequiredArgsConstructor
public class ResidentService {
 private final JdbcTemplate jdbc;
 public UUID community(String slug) {
  var ids=jdbc.queryForList("SELECT id FROM community WHERE slug=?",UUID.class,slug);
  if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Community not found");return ids.getFirst();
 }
 public String hash(String token) {
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 public UUID authorize(String slug,String auth) {
  if(auth==null||!auth.startsWith("Bearer ")||auth.length()>200)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Set up your resident profile to continue");
  var ids=jdbc.queryForList("SELECT id FROM resident_profile WHERE community_id=? AND token_hash=?",UUID.class,community(slug),hash(auth.substring(7)));
  if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"This profile is not available on this device");return ids.getFirst();
 }
 public Map<String,Object> profile(UUID id){return jdbc.queryForMap("SELECT id,name,flat_number,share_name,share_flat,verification,reviewed_at,created_at FROM resident_profile WHERE id=?",id);}
 public boolean validEntity(UUID community,String kind,UUID id){
  String table=switch(kind){case "provider"->"provider";case "ride"->"hood_ride";case "plan"->"hood_plan";default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a shop, ride or plan");};
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM "+table+" WHERE id=? AND community_id=?)",Boolean.class,id,community));
 }
 public List<Map<String,Object>> requests(UUID community){return jdbc.queryForList("""
  SELECT r.id,r.title,r.body,r.expires_at,r.status,r.created_at,
   CASE WHEN p.share_name THEN p.name ELSE 'A neighbour' END AS name,
   CASE WHEN p.share_flat THEN p.flat_number ELSE NULL END AS flat_number,p.verification,
   (SELECT count(*) FROM hood_request_response s WHERE s.request_id=r.id) AS responses
  FROM hood_request r JOIN resident_profile p ON p.id=r.resident_id
  WHERE r.community_id=? AND r.status='open' AND r.expires_at>NOW() ORDER BY r.created_at DESC LIMIT 50
  """,community);}
}
