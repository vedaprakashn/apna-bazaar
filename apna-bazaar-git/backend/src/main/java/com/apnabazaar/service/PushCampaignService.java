package com.apnabazaar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.sql.*;
import java.util.*;

@Service
public class PushCampaignService {
    private final JdbcTemplate jdbc; private final FirebasePushSender sender; private final boolean enabled; private final Clock clock;
    @Autowired
    public PushCampaignService(JdbcTemplate jdbc,FirebasePushSender sender,@Value("${FIREBASE_PUSH_ENABLED:true}") boolean enabled) {
        this(jdbc,sender,enabled,Clock.systemUTC());
    }
    public PushCampaignService(JdbcTemplate jdbc,FirebasePushSender sender,boolean enabled,Clock clock) {this.jdbc=jdbc;this.sender=sender;this.enabled=enabled;this.clock=clock;}
    private static final String CAMPAIGNS = """
        SELECT m.id,m.title,m.body,m.daypart,p.id AS provider_id,co.slug,
               p.name LIKE '% · Demo' AS demo
        FROM chat_promotion m JOIN community co ON co.id=m.community_id
        JOIN offering o ON o.id=m.offering_id JOIN provider p ON p.id=o.provider_id
        WHERE m.community_id=? AND m.active AND m.push_enabled AND o.is_available
          AND o.live_status<>'sold_out' AND p.status='active'
          AND (m.starts_at IS NULL OR m.starts_at<=NOW()) AND (m.ends_at IS NULL OR m.ends_at>NOW())
        """;
    @Scheduled(fixedDelayString="${FIREBASE_PUSH_INTERVAL_MS:900000}",initialDelay=60000)
    public void scheduled() {
        if(!enabled || !sender.configured())return;
        send(null,null);
    }
    public Map<String,Integer> send(UUID community,UUID promotion) {
        if(!enabled || !sender.configured())return Map.of("accepted",0,"failed",0,"unknown",0);
        return jdbc.execute((ConnectionCallback<Map<String,Integer>>) connection -> {
            try(var statement=connection.createStatement();var rs=statement.executeQuery("SELECT pg_try_advisory_lock(8284108511)")) {
                rs.next();if(!rs.getBoolean(1))return Map.of("accepted",0,"failed",0,"unknown",0);
            }
            try { return dispatch(community,promotion); }
            finally { try(var statement=connection.createStatement()){statement.execute("SELECT pg_advisory_unlock(8284108511)");} }
        });
    }
    private Map<String,Integer> dispatch(UUID community,UUID promotion) {
        Instant now=clock.instant();var day=now.atZone(PushPolicy.INDIA).toLocalDate();
        int accepted=0,failed=0,unknown=0;
        if(!PushPolicy.eligible("anytime",now))return Map.of("accepted",0,"failed",0,"unknown",0);
        var devices=jdbc.queryForList("SELECT i.id,i.community_id,i.fcm_token FROM push_installation i WHERE enabled AND fcm_token IS NOT NULL AND updated_at>NOW()-interval '60 days' "+(community==null?"":"AND community_id=? ")+"ORDER BY (SELECT max(d.created_at) FROM push_campaign_delivery d WHERE d.installation_id=i.id) NULLS FIRST,updated_at DESC LIMIT 100",community==null?new Object[]{}:new Object[]{community});
        for(var device:devices) {
            UUID installation=(UUID)device.get("id");
            var quota=jdbc.queryForMap("SELECT count(*) FILTER(WHERE local_day=? AND status IN ('pending','accepted','unknown')) AS today,max(created_at) FILTER(WHERE status IN ('pending','accepted','unknown')) AS last FROM push_campaign_delivery WHERE installation_id=?",java.sql.Date.valueOf(day),installation);
            if(((Number)quota.get("today")).intValue()>=3 || quota.get("last") instanceof Timestamp last && last.toInstant().isAfter(now.minusSeconds(4*3600)))continue;
            var campaigns=jdbc.queryForList(CAMPAIGNS+(promotion==null?"":" AND m.id=?")+" ORDER BY (SELECT max(d.created_at) FROM push_campaign_delivery d WHERE d.installation_id=? AND d.promotion_id=m.id) NULLS FIRST,m.id",
                    promotion==null?new Object[]{device.get("community_id"),installation}:new Object[]{device.get("community_id"),promotion,installation});
            for(var campaign:campaigns) {
                if(!PushPolicy.eligible((String)campaign.get("daypart"),now))continue;
                UUID delivery=UUID.randomUUID();
                int claimed=jdbc.update("INSERT INTO push_campaign_delivery(id,installation_id,promotion_id,local_day) VALUES(?,?,?,?) ON CONFLICT(installation_id,promotion_id,local_day) DO NOTHING",delivery,installation,campaign.get("id"),java.sql.Date.valueOf(day));
                if(claimed==0)continue;
                String path="/provider/index.html?community="+campaign.get("slug")+"&id="+campaign.get("provider_id");
                String title=(Boolean.TRUE.equals(campaign.get("demo"))?"Demo · ":"")+campaign.get("title");
                String body=(String)campaign.get("body");if(body.length()>500)body=body.substring(0,497)+"…";
                var result=sender.send((String)device.get("fcm_token"),title,body,Map.of("deliveryId",delivery.toString(),"path",path),false);
                jdbc.update("UPDATE push_campaign_delivery SET status=? WHERE id=?",result.status(),delivery);
                if(result.invalidToken())jdbc.update("UPDATE push_installation SET enabled=false,fcm_token=NULL WHERE id=? AND fcm_token=?",installation,device.get("fcm_token"));
                switch(result.status()){case "accepted"->accepted++;case "failed"->failed++;default->unknown++;}
                break;
            }
        }
        return Map.of("accepted",accepted,"failed",failed,"unknown",unknown);
    }
}
