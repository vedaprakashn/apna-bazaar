package com.apnabazaar.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/{slug}/admin/campaigns")
public class AdminCampaignController {
    private final JdbcTemplate jdbc;
    private final String token;
    public AdminCampaignController(JdbcTemplate jdbc,@Value("${CAMPAIGN_ADMIN_TOKEN:}") String token) {
        this.jdbc=jdbc; this.token=token;
    }
    private void authorize(String authorization) {
        if(token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Set CAMPAIGN_ADMIN_TOKEN in Railway to enable campaign editing");
        String supplied=authorization!=null && authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
        if(!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid campaign admin key");
    }
    @GetMapping
    public List<Map<String,Object>> list(@PathVariable String slug) {
        return jdbc.queryForList("""
            SELECT m.*,p.id AS provider_id,p.shop_name AS shop,k.name AS category,
                   to_char(m.starts_at AT TIME ZONE 'Asia/Kolkata','YYYY-MM-DD"T"HH24:MI') AS starts_local,
                   to_char(m.ends_at AT TIME ZONE 'Asia/Kolkata','YYYY-MM-DD"T"HH24:MI') AS ends_local
            FROM chat_promotion m JOIN community c ON c.id=m.community_id
            JOIN offering o ON o.id=m.offering_id JOIN provider p ON p.id=o.provider_id
            LEFT JOIN category k ON k.id=o.category_id WHERE c.slug=? ORDER BY m.active DESC,k.sort_order,m.title
            """,slug);
    }
    @GetMapping("/offerings")
    public List<Map<String,Object>> offerings(@PathVariable String slug) {
        return jdbc.queryForList("""
            SELECT o.id,o.name,p.shop_name AS shop,k.name AS category FROM offering o
            JOIN provider p ON p.id=o.provider_id JOIN community c ON c.id=p.community_id
            LEFT JOIN category k ON k.id=o.category_id
            WHERE c.slug=? AND p.status='active' AND o.is_available ORDER BY k.sort_order,p.shop_name,o.name
            """,slug);
    }
    public record Campaign(@NotNull UUID offeringId,@NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=2000) String body,@NotBlank @Size(max=80) String ctaText,
        @Pattern(regexp="broadcast|promoted") @NotNull String kind,boolean active,String startsAt,String endsAt) {}
    @PostMapping
    public Map<String,UUID> create(@PathVariable String slug,@RequestHeader(value="Authorization",required=false) String auth,
                                  @Valid @RequestBody Campaign c) {
        authorize(auth); UUID id=UUID.randomUUID(); save(slug,id,c,true); return Map.of("id",id);
    }
    @PutMapping("/{id}")
    public Map<String,UUID> update(@PathVariable String slug,@PathVariable UUID id,
                                  @RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody Campaign c) {
        authorize(auth); save(slug,id,c,false); return Map.of("id",id);
    }
    private Timestamp timestamp(String value) {
        if(value==null || value.isBlank())return null;
        try {return Timestamp.from(OffsetDateTime.parse(value).toInstant());}
        catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Campaign times need an ISO date/time with offset");}
    }
    private void save(String slug,UUID id,Campaign c,boolean create) {
        Timestamp start=timestamp(c.startsAt()),end=timestamp(c.endsAt());
        if(start!=null && end!=null && !end.after(start))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"End must be after start");
        int count;
        if(create)count=jdbc.update("""
            INSERT INTO chat_promotion(id,community_id,offering_id,title,body,cta_text,kind,active,starts_at,ends_at)
            SELECT ?,co.id,o.id,?,?,?,?,?,?,? FROM offering o JOIN provider p ON p.id=o.provider_id
            JOIN community co ON co.id=p.community_id WHERE co.slug=? AND o.id=? AND p.status='active' AND o.is_available
            """,id,c.title().trim(),c.body().trim(),c.ctaText().trim(),c.kind(),c.active(),start,end,slug,c.offeringId());
        else count=jdbc.update("""
            UPDATE chat_promotion m SET offering_id=o.id,title=?,body=?,cta_text=?,kind=?,active=?,starts_at=?,ends_at=?
            FROM offering o JOIN provider p ON p.id=o.provider_id JOIN community co ON co.id=p.community_id
            WHERE m.id=? AND m.community_id=co.id AND co.slug=? AND o.id=? AND p.status='active' AND o.is_available
            """,c.title().trim(),c.body().trim(),c.ctaText().trim(),c.kind(),c.active(),start,end,id,slug,c.offeringId());
        if(count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Campaign or offering not found in this community");
    }
}
