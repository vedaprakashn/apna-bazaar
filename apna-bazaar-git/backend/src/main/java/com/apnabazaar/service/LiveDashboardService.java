package com.apnabazaar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LiveDashboardService {
    private final JdbcTemplate jdbc;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private UUID community(String slug) {
        var ids = jdbc.queryForList("SELECT id FROM community WHERE slug=?", UUID.class, slug);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found");
        return ids.getFirst();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> activity(String slug, int days) {
        UUID id = community(slug);
        LocalDate today = LocalDate.now(IST), from = today.minusDays(Math.max(1, Math.min(days, 90)) - 1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from); result.put("to", today);
        result.put("metrics", jdbc.queryForMap("""
            SELECT count(*) AS searches, count(*) FILTER (WHERE query_date=?) AS today,
                   count(*) FILTER (WHERE NOT had_results) AS misses,
                   count(*) FILTER (WHERE had_results) AS matched
            FROM search_event WHERE community_id=? AND query_date BETWEEN ? AND ?
            """, today, id, from, today));
        result.put("providers", jdbc.queryForObject("SELECT count(*) FROM provider WHERE community_id=? AND status='active'", Long.class, id));
        result.put("clicks", jdbc.queryForMap("""
            SELECT count(*) AS interest, count(*) FILTER (WHERE pc.click_type='whatsapp_tap') AS whatsapp
            FROM provider_click_event pc JOIN search_event s ON s.id=pc.search_event_id
            WHERE s.community_id=? AND (pc.clicked_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN ? AND ?
            """, id, from, today));
        result.put("promotions", jdbc.queryForList("""
            SELECT m.title,m.kind,p.shop_name AS shop,count(d.id) AS views,
                   count(DISTINCT d.session_id) AS viewers,
                   count(d.clicked_at) AS clicks
            FROM chat_promotion m JOIN offering o ON o.id=m.offering_id
            JOIN provider p ON p.id=o.provider_id
            LEFT JOIN chat_promotion_delivery d ON d.promotion_id=m.id
              AND (d.viewed_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN ? AND ?
            WHERE m.community_id=? GROUP BY m.id,p.shop_name ORDER BY views DESC,m.title
            """, from,today,id));
        result.put("topQueries", jdbc.queryForList("""
            SELECT normalised_query AS query, count(*) AS searches,
                   count(*) FILTER (WHERE NOT had_results) AS misses
            FROM search_event WHERE community_id=? AND query_date BETWEEN ? AND ?
            GROUP BY normalised_query ORDER BY searches DESC, query LIMIT 15
            """, id, from, today));
        result.put("missedQueries", jdbc.queryForList("""
            SELECT normalised_query AS query, count(*) AS searches, max(query_date) AS last_seen
            FROM search_event WHERE community_id=? AND query_date BETWEEN ? AND ? AND NOT had_results
            GROUP BY normalised_query ORDER BY searches DESC, last_seen DESC LIMIT 20
            """, id, from, today));
        result.put("daily", jdbc.queryForList("""
            SELECT d::date AS date, count(s.id) AS searches,
                   count(s.id) FILTER (WHERE NOT s.had_results) AS misses
            FROM generate_series(?::date, ?::date, interval '1 day') d
            LEFT JOIN search_event s ON s.query_date=d::date AND s.community_id=?
            GROUP BY d ORDER BY d
            """, from, today, id));
        result.put("recent", jdbc.queryForList("""
            SELECT raw_query AS query, result_count AS results, query_date AS date, query_time AS time
            FROM search_event WHERE community_id=? AND query_date BETWEEN ? AND ?
            ORDER BY query_date DESC, query_time DESC LIMIT 30
            """, id, from, today));
        result.put("topProviders", jdbc.queryForList("""
            SELECT p.name, p.shop_name AS shop, count(pc.id) AS interest,
                   count(pc.id) FILTER (WHERE pc.click_type='whatsapp_tap') AS whatsapp
            FROM provider_click_event pc JOIN provider p ON p.id=pc.provider_id
            WHERE p.community_id=? AND (pc.clicked_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN ? AND ?
            GROUP BY p.id ORDER BY interest DESC LIMIT 10
            """, id, from, today));
        result.put("categories", jdbc.queryForList("""
            SELECT c.name, c.icon_emoji AS emoji, count(o.id) AS offerings
            FROM category c LEFT JOIN offering o ON o.category_id=c.id AND o.is_available=true
              AND o.provider_id IN (SELECT id FROM provider WHERE community_id=? AND status='active')
            GROUP BY c.id ORDER BY c.sort_order
            """, id));
        return result;
    }

    public List<Map<String, Object>> catalog(String slug) {
        return jdbc.queryForList("""
            SELECT p.id, p.name, p.shop_name AS shop, o.name AS offering, o.description,
                   o.base_price AS price, o.unit, c.name AS category, c.icon_emoji AS emoji
            FROM provider p JOIN offering o ON o.provider_id=p.id JOIN category c ON c.id=o.category_id
            WHERE p.community_id=? AND p.status='active' AND o.is_available=true
            ORDER BY c.sort_order, p.name
            """, community(slug));
    }
    @Transactional(readOnly = true)
    public Map<String, Object> storefront(String slug, UUID providerId) {
        UUID communityId = community(slug);
        var providers = jdbc.queryForList("""
            SELECT p.id,p.name,p.shop_name AS shop,p.flat_number AS flat,p.whatsapp_number AS whatsapp,
                   p.is_verified AS verified,c.name AS community,p.provider_type::text AS type
            FROM provider p JOIN community c ON c.id=p.community_id
            WHERE p.id=? AND p.community_id=? AND p.status='active'
            """, providerId, communityId);
        if (providers.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found in this community");
        var offerings = jdbc.queryForList("""
            SELECT o.id,o.name,o.description,o.base_price AS price,o.unit,o.is_available AS available,
                   c.name AS category,c.icon_emoji AS emoji
            FROM offering o LEFT JOIN category c ON c.id=o.category_id
            WHERE o.provider_id=? ORDER BY o.sort_order,o.name
            """, providerId);
        for (var offering : offerings) {
            offering.put("schedules", jdbc.queryForList("""
                SELECT day_scope::text AS days,days_of_week,serves_from AS starts,serves_to AS ends,
                       accepts_preorder,preorder_day_offset::text AS preorder_day,preorder_closes_at AS preorder_cutoff
                FROM offering_schedule WHERE offering_id=? AND is_active=true ORDER BY serves_from
                """, offering.get("id")));
        }
        var today = LocalDate.now(IST);
        var menu = jdbc.queryForList("""
            SELECT li.item_name AS name,li.price,li.pickup_time,li.pickup_location,li.quantity_available,
                   li.delivery_type::text AS delivery,li.notes
            FROM daily_line_item li JOIN daily_post dp ON dp.id=li.daily_post_id
            WHERE dp.provider_id=? AND dp.post_date=? AND dp.is_active=true ORDER BY li.item_name
            """, providerId, today);
        return Map.of("provider", providers.getFirst(), "offerings", offerings, "todayMenu", menu, "date", today);
    }

}
