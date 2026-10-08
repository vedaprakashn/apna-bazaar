package com.apnabazaar.controller;
import com.apnabazaar.service.ResidentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api/{slug}/admin/residents")
public class ResidentOperatorController {
 private final JdbcTemplate jdbc;private final ResidentService residents;private final String key;
 public ResidentOperatorController(JdbcTemplate jdbc,ResidentService residents,@Value("${CAMPAIGN_ADMIN_TOKEN:}")String key){this.jdbc=jdbc;this.residents=residents;this.key=key;}
 private void auth(String header){if(key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure CAMPAIGN_ADMIN_TOKEN in Railway for operator approval and availability updates");String supplied=header!=null&&header.startsWith("Bearer ")?header.substring(7):"";if(!MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Operator key required");}
 @GetMapping public List<Map<String,Object>> list(@PathVariable String slug,@RequestHeader(value="Authorization",required=false)String header){auth(header);return jdbc.queryForList("SELECT id,name,flat_number,verification,created_at FROM resident_profile WHERE community_id=? ORDER BY created_at DESC",residents.community(slug));}
 public record Review(String status){}
 @PostMapping("/{id}/review")public void review(@PathVariable String slug,@RequestHeader(value="Authorization",required=false)String header,@PathVariable UUID id,@RequestBody Review review){auth(header);if(!Set.of("verified","rejected","pending").contains(review.status()==null?"":review.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid review status");if(jdbc.update("UPDATE resident_profile SET verification=?,reviewed_at=NOW() WHERE id=? AND community_id=?",review.status(),id,residents.community(slug))==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Resident not found");}
 @GetMapping("/availability")public List<Map<String,Object>> offerings(@PathVariable String slug,@RequestHeader(value="Authorization",required=false)String header){auth(header);return jdbc.queryForList("SELECT o.id,o.name,o.live_status,o.availability_updated_at,p.shop_name FROM offering o JOIN provider p ON p.id=o.provider_id WHERE p.community_id=? AND p.status='active' ORDER BY p.shop_name,o.name",residents.community(slug));}
 @PostMapping("/availability/{id}")public void availability(@PathVariable String slug,@RequestHeader(value="Authorization",required=false)String header,@PathVariable UUID id,@RequestBody Review review){auth(header);if(!Set.of("available","sold_out","preorder","unconfirmed").contains(review.status()==null?"":review.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid availability");if(jdbc.update("UPDATE offering o SET live_status=?,availability_updated_at=NOW() FROM provider p WHERE o.provider_id=p.id AND o.id=? AND p.community_id=?",review.status(),id,residents.community(slug))==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Offering not found");}
}
