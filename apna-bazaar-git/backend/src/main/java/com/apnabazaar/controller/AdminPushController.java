package com.apnabazaar.controller;

import com.apnabazaar.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController @RequestMapping("/api/{slug}/admin/push")
public class AdminPushController {
    private final JdbcTemplate jdbc;private final ResidentService residents;
    private final FirebasePushSender sender;private final PushCampaignService campaigns;private final String key;
    public AdminPushController(JdbcTemplate jdbc,ResidentService residents,FirebasePushSender sender,PushCampaignService campaigns,@Value("${CAMPAIGN_ADMIN_TOKEN:}") String key) {
        this.jdbc=jdbc;this.residents=residents;this.sender=sender;this.campaigns=campaigns;this.key=key;
    }
    private void auth(String header) {
        if(key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure CAMPAIGN_ADMIN_TOKEN in Railway");
        String token=header!=null && header.startsWith("Bearer ")?header.substring(7):"";
        if(!MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Operator key required");
    }
    @GetMapping public Map<String,Object> stats(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String header) {
        auth(header);UUID c=residents.community(slug);
        return Map.of("configured",sender.configured(),"projectId",sender.project(),"optedInDevices",jdbc.queryForObject("SELECT count(*) FROM push_installation WHERE community_id=? AND enabled",Long.class,c),
                "campaigns",jdbc.queryForList("""
                SELECT m.id,m.title,m.push_enabled,count(d.id) FILTER(WHERE d.status='accepted') AS accepted,
                  count(d.id) FILTER(WHERE d.received_at IS NOT NULL) AS foreground_received,
                  count(d.id) FILTER(WHERE d.opened_at IS NOT NULL) AS opened,
                  count(d.id) FILTER(WHERE d.status='failed') AS failed,count(d.id) FILTER(WHERE d.status='unknown') AS unknown
                FROM chat_promotion m LEFT JOIN push_campaign_delivery d ON d.promotion_id=m.id
                WHERE m.community_id=? GROUP BY m.id ORDER BY m.title
                """,c));
    }
    public record Choice(boolean enabled) {}
    @PatchMapping("/campaigns/{id}") public void enable(@PathVariable String slug,@PathVariable UUID id,
            @RequestHeader(value="Authorization",required=false) String header,@RequestBody Choice choice) {
        auth(header);if(jdbc.update("UPDATE chat_promotion SET push_enabled=? WHERE community_id=? AND id=?",choice.enabled(),residents.community(slug),id)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Campaign not found");
    }
    @PostMapping("/campaigns/{id}/send") public Map<String,Integer> send(@PathVariable String slug,@PathVariable UUID id,
            @RequestHeader(value="Authorization",required=false) String header) {
        auth(header);UUID c=residents.community(slug);
        if(!sender.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure Firebase service account in Railway");
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM chat_promotion WHERE community_id=? AND id=? AND push_enabled AND active)",Boolean.class,c,id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Enable this active campaign for push first");
        return campaigns.send(c,id);
    }
}
