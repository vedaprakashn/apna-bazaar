package com.apnabazaar.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
@RequestMapping("/api/{slug}/promotions")
@RequiredArgsConstructor
public class ChatPromotionController {
    private final JdbcTemplate jdbc;

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable String slug) {
        return jdbc.queryForList("""
            SELECT m.id,m.title,m.kind,p.id AS provider_id,p.shop_name AS shop,
                   o.name AS offering,o.base_price AS price,o.unit,c.icon_emoji AS emoji
            FROM chat_promotion m JOIN community co ON co.id=m.community_id
            JOIN offering o ON o.id=m.offering_id JOIN provider p ON p.id=o.provider_id
            LEFT JOIN category c ON c.id=o.category_id
            WHERE co.slug=? AND m.active AND o.is_available AND p.status='active'
            ORDER BY m.id
            """, slug);
    }

    public record Event(UUID promotionId, UUID deliveryId, UUID sessionId, String type) {}

    @PostMapping("/events")
    public void event(@PathVariable String slug, @RequestBody Event event) {
        if (event.promotionId()==null || event.deliveryId()==null || event.sessionId()==null
            || !("impression".equals(event.type()) || "click".equals(event.type())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid promotion event");
        if ("impression".equals(event.type())) {
            int count = jdbc.update("""
                INSERT INTO chat_promotion_delivery(id,promotion_id,session_id)
                SELECT ?,m.id,? FROM chat_promotion m JOIN community c ON c.id=m.community_id
                JOIN offering o ON o.id=m.offering_id JOIN provider p ON p.id=o.provider_id
                WHERE m.id=? AND c.slug=? AND m.active AND o.is_available AND p.status='active'
                ON CONFLICT(id) DO NOTHING
                """, event.deliveryId(),event.sessionId(),event.promotionId(),slug);
            if (count==0 && !Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM chat_promotion_delivery d JOIN chat_promotion m ON m.id=d.promotion_id
                JOIN community c ON c.id=m.community_id WHERE d.id=? AND d.session_id=? AND m.id=? AND c.slug=?)
                """, Boolean.class,event.deliveryId(),event.sessionId(),event.promotionId(),slug)))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion unavailable");
        } else {
            int count = jdbc.update("""
                UPDATE chat_promotion_delivery d SET clicked_at=COALESCE(d.clicked_at,NOW())
                FROM chat_promotion m JOIN community c ON c.id=m.community_id
                WHERE d.promotion_id=m.id AND d.id=? AND d.session_id=? AND m.id=? AND c.slug=?
                """,event.deliveryId(),event.sessionId(),event.promotionId(),slug);
            if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Impression not recorded");
        }
    }
}
