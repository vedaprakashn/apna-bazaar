package com.apnabazaar.controller;

import com.apnabazaar.service.ChatRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/{slug}/plans")
public class HoodPlanController {
 private final JdbcTemplate jdbc;
 private final ChatRateLimiter limiter;
 private final boolean railwayProxy;
 public HoodPlanController(JdbcTemplate jdbc,ChatRateLimiter limiter,@Value("${RAILWAY_PROJECT_ID:}") String project) {this.jdbc=jdbc;this.limiter=limiter;railwayProxy=!project.isBlank();}
 public record Vote(UUID visitorId,String choice) {}
 @GetMapping
 public ResponseEntity<?> list(@PathVariable String slug,@RequestParam(required=false) UUID visitorId) {
  var communities=jdbc.queryForList("SELECT id FROM community WHERE slug=?",slug);
  if(communities.isEmpty())return ResponseEntity.notFound().build();
  return ResponseEntity.ok(jdbc.queryForList("""
   SELECT p.*, (p.starts_at<=now()) AS ended,
    (SELECT count(*) FROM hood_plan_vote v WHERE v.plan_id=p.id AND v.choice='in') AS interested,
    (SELECT choice FROM hood_plan_vote v WHERE v.plan_id=p.id AND v.visitor_id=?) AS my_vote
   FROM hood_plan p WHERE p.community_id=? ORDER BY p.starts_at,p.title
   """,visitorId,communities.getFirst().get("id")));
 }
 @PostMapping("/{id}/vote")
 @Transactional
 public ResponseEntity<?> vote(@PathVariable String slug,@PathVariable UUID id,@RequestBody Vote vote,HttpServletRequest request) {
  if(vote.visitorId()==null||!Set.of("in","pass","clear").contains(vote.choice()==null?"":vote.choice()))return ResponseEntity.badRequest().body(Map.of("error","Choose I’m in, Meh, or undo."));
  String ip=request.getRemoteAddr();
  if(railwayProxy&&request.getHeader("X-Forwarded-For")!=null){var hops=request.getHeader("X-Forwarded-For").split(",");var last=hops[hops.length-1].trim();if(last.matches("[0-9a-fA-F:.]+"))ip=last;}
  var allowed=limiter.acquire("plans:"+ip);
  if(!allowed.allowed())return ResponseEntity.status(429).header("Retry-After",Long.toString(allowed.retryAfterSeconds())).body(Map.of("error","Give it a moment before changing your vote again."));
  var plans=jdbc.queryForList("""
   SELECT p.status,p.starts_at>now() AS upcoming FROM hood_plan p
   JOIN community c ON c.id=p.community_id WHERE p.id=? AND c.slug=? FOR UPDATE OF p
   """,id,slug);
  if(plans.isEmpty())return ResponseEntity.notFound().build();
  var plan=plans.getFirst();
  if(!Boolean.TRUE.equals(plan.get("upcoming"))||!"gathering".equals(plan.get("status")))return ResponseEntity.status(409).body(Map.of("error","This plan is no longer collecting interest."));
  if("clear".equals(vote.choice()))jdbc.update("DELETE FROM hood_plan_vote WHERE plan_id=? AND visitor_id=?",id,vote.visitorId());
  else jdbc.update("""
   INSERT INTO hood_plan_vote(plan_id,visitor_id,choice) VALUES(?,?,?)
   ON CONFLICT(plan_id,visitor_id) DO UPDATE SET choice=excluded.choice,updated_at=now()
   """,id,vote.visitorId(),vote.choice());
  return list(slug,vote.visitorId());
 }
}
