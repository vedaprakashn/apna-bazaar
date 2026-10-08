package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import java.security.SecureRandom;

@RestController @RequestMapping("/api/{slug}/resident")
public class ResidentController {
 private final boolean proxy; private final ResidentService residents; private final JdbcTemplate jdbc; private final MessageModerationService moderation;private final ChatRateLimiter limiter;
 public ResidentController(ResidentService residents,JdbcTemplate jdbc,MessageModerationService moderation,ChatRateLimiter limiter,@org.springframework.beans.factory.annotation.Value("${RAILWAY_PROJECT_ID:}")String project){this.proxy=!project.isBlank();this.residents=residents;this.jdbc=jdbc;this.moderation=moderation;this.limiter=limiter;}
 private void rate(HttpServletRequest request){String ip=request.getRemoteAddr();if(proxy&&request.getHeader("X-Forwarded-For")!=null){var hops=request.getHeader("X-Forwarded-For").split(",");var last=hops[hops.length-1].trim();if(last.matches("[0-9a-fA-F:.]+"))ip=last;}var d=limiter.acquire("resident:"+ip);if(!d.allowed())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Wait a moment before trying again");}
 private void text(String value,int max){if(value==null||value.isBlank()||value.length()>max)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Please check the required fields");}
 private void screen(String value){var d=moderation.check(value);if(d==MessageModerationService.Decision.BLOCK)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,MessageModerationService.BLOCK_REPLY);if(d!=MessageModerationService.Decision.ALLOW)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Could not check that text. Please retry");}
 public record Profile(UUID id,String name,String flatNumber,boolean shareName,boolean shareFlat,boolean consent,UUID legacyPlanVisitorId,UUID legacyRideVisitorId,Map<UUID,String> legacyTokens){}
 @PostMapping("/profile") @Transactional public Map<String,Object> create(@PathVariable String slug,@RequestBody Profile p,HttpServletRequest request){
  rate(request);text(p.name(),80);text(p.flatNumber(),30);if(!p.consent())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm creating your profile");screen(p.name()+" "+p.flatNumber());
  UUID c=residents.community(slug),id=p.id()==null?UUID.randomUUID():p.id();byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);
  int added=jdbc.update("INSERT INTO resident_profile(id,community_id,token_hash,name,flat_number,share_name,share_flat) VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING",id,c,residents.hash(token),p.name().trim(),p.flatNumber().trim(),p.shareName(),p.shareFlat());
  if(added==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"A profile already exists. Use its saved device session");
  for(UUID legacy:new UUID[]{p.legacyPlanVisitorId(),p.legacyRideVisitorId()}) {
   if(legacy!=null&&!legacy.equals(id)&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM resident_profile WHERE id=?)",Boolean.class,legacy))) {
    String proof=p.legacyTokens()==null?null:p.legacyTokens().get(legacy);
    if(proof==null||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM resident_profile WHERE id=? AND token_hash=?)",Boolean.class,legacy,residents.hash(proof))))throw new ResponseStatusException(HttpStatus.CONFLICT,"Use the existing profile for these saved activities");
   }
  }
  if(p.legacyPlanVisitorId()!=null&&!p.legacyPlanVisitorId().equals(id)) {
   jdbc.update("""
    INSERT INTO hood_plan_vote(plan_id,visitor_id,choice)
    SELECT v.plan_id,?,v.choice FROM hood_plan_vote v JOIN hood_plan p ON p.id=v.plan_id WHERE p.community_id=? AND v.visitor_id=?
    ON CONFLICT(plan_id,visitor_id) DO NOTHING
    """,id,c,p.legacyPlanVisitorId());
   jdbc.update("DELETE FROM hood_plan_vote v USING hood_plan p WHERE v.plan_id=p.id AND p.community_id=? AND v.visitor_id=?",c,p.legacyPlanVisitorId());
  }
  if(p.legacyRideVisitorId()!=null&&!p.legacyRideVisitorId().equals(id))jdbc.update("UPDATE hood_ride SET owner_id=? WHERE community_id=? AND owner_id=?",id,c,p.legacyRideVisitorId());
  return Map.of("profile",residents.profile(id),"token",token);
 }
 @GetMapping("/profile") public Map<String,Object> profile(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth){return residents.profile(residents.authorize(slug,auth));}
 @PutMapping("/profile") public Map<String,Object> update(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Profile p,HttpServletRequest request){
  rate(request);UUID id=residents.authorize(slug,auth);text(p.name(),80);text(p.flatNumber(),30);screen(p.name()+" "+p.flatNumber());
  jdbc.update("UPDATE resident_profile SET reviewed_at=CASE WHEN name<>? OR flat_number<>? THEN NULL ELSE reviewed_at END,verification=CASE WHEN name<>? OR flat_number<>? THEN 'pending' ELSE verification END,name=?,flat_number=?,share_name=?,share_flat=? WHERE id=?",p.name().trim(),p.flatNumber().trim(),p.name().trim(),p.flatNumber().trim(),p.name().trim(),p.flatNumber().trim(),p.shareName(),p.shareFlat(),id);return residents.profile(id);
 }
 public record Saved(String kind,UUID entityId,boolean following){}
 @PostMapping("/saved") public void save(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Saved s,HttpServletRequest request){rate(request);UUID id=residents.authorize(slug,auth);if(s.kind()==null||s.entityId()==null||!residents.validEntity(residents.community(slug),s.kind(),s.entityId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found in your community");if(s.following()&&!"provider".equals(s.kind()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Follow a shop; rides and plans can be saved");jdbc.update("INSERT INTO resident_saved(resident_id,kind,entity_id,following) VALUES(?,?,?,?) ON CONFLICT(resident_id,kind,entity_id) DO UPDATE SET following=excluded.following",id,s.kind(),s.entityId(),s.following());}
 @DeleteMapping("/saved/{kind}/{id}") public void unsave(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@PathVariable String kind,@PathVariable UUID id,HttpServletRequest request){rate(request);jdbc.update("DELETE FROM resident_saved WHERE resident_id=? AND kind=? AND entity_id=?",residents.authorize(slug,auth),kind,id);}
 @GetMapping("/requests") public List<Map<String,Object>> requests(@PathVariable String slug){return residents.requests(residents.community(slug));}
 public record Need(UUID id,String title,String body,Instant expiresAt,boolean publish){}
 @PostMapping("/requests") public Map<String,Object> post(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Need n,HttpServletRequest request){
  rate(request);UUID owner=residents.authorize(slug,auth);text(n.title(),120);text(n.body(),1000);if(!n.publish()||n.expiresAt()==null||!n.expiresAt().isAfter(Instant.now())||n.expiresAt().isAfter(Instant.now().plusSeconds(30*86400)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm publishing with an expiry within 30 days");screen(n.title()+" "+n.body());UUID id=n.id()==null?UUID.randomUUID():n.id();
  int added=jdbc.update("INSERT INTO hood_request(id,community_id,resident_id,title,body,expires_at) VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING",id,residents.community(slug),owner,n.title().trim(),n.body().trim(),java.sql.Timestamp.from(n.expiresAt()));
  if(added==0&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM hood_request WHERE id=? AND resident_id=?)",Boolean.class,id,owner)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Create a new request");return Map.of("id",id);
 }
 public record Reply(String body){}
 @PostMapping("/requests/{id}/responses") public void respond(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@PathVariable UUID id,@RequestBody Reply reply,HttpServletRequest request){
  rate(request);UUID owner=residents.authorize(slug,auth);text(reply.body(),1000);screen(reply.body());
  int added=jdbc.update("""
   INSERT INTO hood_request_response(id,request_id,resident_id,body)
   SELECT ?,r.id,?,? FROM hood_request r WHERE r.id=? AND r.community_id=? AND r.resident_id<>? AND r.status='open' AND r.expires_at>NOW()
   ON CONFLICT(request_id,resident_id) DO UPDATE SET body=excluded.body WHERE hood_request_response.status='offered'
   """,UUID.randomUUID(),owner,reply.body().trim(),id,residents.community(slug),owner);
  if(added==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request is no longer available for a response");
 }
 public record Accept(UUID responseId){}
 @PostMapping("/requests/{id}/accept") @Transactional public void accept(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@PathVariable UUID id,@RequestBody Accept a,HttpServletRequest request){
  rate(request);UUID owner=residents.authorize(slug,auth);
  var rows=jdbc.queryForList("SELECT id FROM hood_request WHERE id=? AND resident_id=? AND community_id=? AND status='open' AND expires_at>NOW() FOR UPDATE",id,owner,residents.community(slug));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"This request cannot accept a response now");
  int changed=jdbc.update("UPDATE hood_request_response SET status='accepted' WHERE id=? AND request_id=? AND status='offered'",a.responseId(),id);
  if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Response not found");jdbc.update("UPDATE hood_request SET status='matched' WHERE id=?",id);jdbc.update("UPDATE hood_request_response SET status='declined' WHERE request_id=? AND id<>?",id,a.responseId());
 }
 @PostMapping("/requests/{id}/close") public void close(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,@PathVariable UUID id,HttpServletRequest request){rate(request);if(jdbc.update("UPDATE hood_request SET status='closed' WHERE id=? AND resident_id=? AND community_id=?",id,residents.authorize(slug,auth),residents.community(slug))==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Your request was not found");}
 @GetMapping("/activity") public Map<String,Object> activity(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth){
  UUID id=residents.authorize(slug,auth),c=residents.community(slug);Map<String,Object> result=new LinkedHashMap<>();result.put("profile",residents.profile(id));
  result.put("contacts",jdbc.queryForList("SELECT p.id,p.shop_name AS title,CASE WHEN f.answered_at IS NULL THEN NULL ELSE f.outcome END AS outcome,f.contacted_at,f.answered_at FROM provider_contact_feedback f JOIN provider p ON p.id=f.provider_id WHERE f.resident_id=? AND p.community_id=? ORDER BY f.contacted_at DESC LIMIT 50",id,c));
  result.put("saved",jdbc.queryForList("""
   SELECT s.kind,s.entity_id,s.following,COALESCE(p.shop_name,p.name,r.destination,h.title) AS title,
    CASE WHEN s.kind='ride' THEN r.status ELSE h.status END AS status
   FROM resident_saved s LEFT JOIN provider p ON s.kind='provider' AND p.id=s.entity_id
   LEFT JOIN hood_ride r ON s.kind='ride' AND r.id=s.entity_id LEFT JOIN hood_plan h ON s.kind='plan' AND h.id=s.entity_id
   WHERE s.resident_id=? ORDER BY s.created_at DESC
   """,id));
  result.put("requests",jdbc.queryForList("SELECT id,title,body,CASE WHEN status='open' AND expires_at<=NOW() THEN 'expired' ELSE status END AS status,expires_at FROM hood_request WHERE resident_id=? ORDER BY created_at DESC",id));
  result.put("responses",jdbc.queryForList("""
   SELECT s.id,s.request_id,s.body,s.status,r.title,r.resident_id=? AS mine,
    CASE WHEN p.share_name THEN p.name ELSE 'A neighbour' END AS name,
    CASE WHEN p.share_flat THEN p.flat_number ELSE NULL END AS flat_number,p.verification
   FROM hood_request_response s JOIN hood_request r ON r.id=s.request_id
   JOIN resident_profile p ON p.id=s.resident_id
   WHERE r.community_id=? AND (r.resident_id=? OR s.resident_id=?) ORDER BY s.created_at DESC
   """,id,c,id,id));
  result.put("rides",jdbc.queryForList("SELECT id,destination,departure_at,CASE WHEN status='open' AND (recurrence_until IS NULL AND departure_at<=NOW() OR recurrence_until IS NOT NULL AND recurrence_until<(NOW() AT TIME ZONE 'Asia/Kolkata')::date) THEN 'expired' ELSE status END AS status,arrangement,recurrence_until,exchange_terms FROM hood_ride WHERE community_id=? AND owner_id=? ORDER BY departure_at DESC",c,id));
  result.put("plans",jdbc.queryForList("SELECT p.id,p.title,p.starts_at,p.status,v.choice FROM hood_plan_vote v JOIN hood_plan p ON p.id=v.plan_id WHERE p.community_id=? AND v.visitor_id=? ORDER BY p.starts_at",c,id));
  result.put("updates",jdbc.queryForList("""
   SELECT o.id,p.id AS provider_id,p.shop_name,o.name,o.live_status,o.availability_updated_at
   FROM resident_saved s JOIN provider p ON p.id=s.entity_id JOIN offering o ON o.provider_id=p.id
   WHERE s.resident_id=? AND s.kind='provider' AND s.following AND o.availability_updated_at IS NOT NULL
    AND o.availability_updated_at>=s.created_at ORDER BY o.availability_updated_at DESC LIMIT 30
   """,id));return result;
 }
}
