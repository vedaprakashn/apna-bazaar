package com.apnabazaar.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/{slug}/help-contacts")
public class HelpDirectoryController {
    private final JdbcTemplate jdbc;
    public HelpDirectoryController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        var communities = jdbc.queryForList("SELECT id,name FROM community WHERE slug=?", slug);
        if (communities.isEmpty()) return ResponseEntity.notFound().build();
        var community = communities.getFirst();
        var contacts = jdbc.queryForList("""
            SELECT id,section,category,name,phone,service_area,availability,location,scope,
                   notes,is_demo,consent_to_listing,verified_at,verification_source
            FROM help_contact WHERE community_id=? AND is_active=true
            ORDER BY section,category,name
            """, community.get("id"));
        return ResponseEntity.ok(Map.of("community", community.get("name"), "contacts", contacts));
    }
}
