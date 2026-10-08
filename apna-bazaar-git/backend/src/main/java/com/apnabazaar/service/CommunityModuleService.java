package com.apnabazaar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommunityModuleService {
    private final JdbcTemplate jdbc;
    public List<Map<String,Object>> catalog(UUID community, String module) {
        if ("requests".equals(module)) return jdbc.queryForList("""
            SELECT r.id,r.title,r.body,r.expires_at,r.status,
             CASE WHEN p.share_name THEN p.name ELSE 'A neighbour' END AS name,
             CASE WHEN p.share_flat THEN p.flat_number ELSE NULL END AS flat_number,p.verification
            FROM hood_request r JOIN resident_profile p ON p.id=r.resident_id
            WHERE r.community_id=? AND r.status='open' AND r.expires_at>NOW() ORDER BY r.created_at DESC LIMIT 50
            """,community);
        if ("plans".equals(module)) return jdbc.queryForList("""
            SELECT p.id,p.title,p.description,p.category,p.starts_at,p.location,p.minimum_interested,p.status,
             (SELECT count(*) FROM hood_plan_vote v WHERE v.plan_id=p.id AND v.choice='in') AS interested
            FROM hood_plan p WHERE p.community_id=? AND p.starts_at>NOW() AND p.status<>'cancelled'
            ORDER BY p.starts_at LIMIT 50
            """,community);
        return jdbc.queryForList("""
            SELECT m.id,m.title,m.body,m.cta_text,m.daypart,p.id AS provider_id,p.shop_name AS shop
            FROM chat_promotion m JOIN offering o ON o.id=m.offering_id JOIN provider p ON p.id=o.provider_id
            WHERE m.community_id=? AND m.active AND o.is_available AND p.status='active'
             AND (m.starts_at IS NULL OR m.starts_at<=NOW()) AND (m.ends_at IS NULL OR m.ends_at>NOW())
            ORDER BY m.id LIMIT 50
            """,community);
    }
}
