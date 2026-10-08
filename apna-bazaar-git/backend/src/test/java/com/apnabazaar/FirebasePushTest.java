package com.apnabazaar;

import com.apnabazaar.controller.*;
import com.apnabazaar.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import java.sql.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FirebasePushTest {
    @Test void quietHoursAndCampaignTimeAreIndiaLocal() {
        assertFalse(PushPolicy.eligible("anytime",Instant.parse("2026-10-08T01:30:00Z"))); // 7am
        assertTrue(PushPolicy.eligible("morning",Instant.parse("2026-10-08T02:30:00Z"))); // 8am
        assertTrue(PushPolicy.eligible("lunch",Instant.parse("2026-10-08T06:30:00Z")));
        assertFalse(PushPolicy.eligible("morning",Instant.parse("2026-10-08T06:30:00Z")));
        assertTrue(PushPolicy.eligible("evening",Instant.parse("2026-10-08T16:29:59Z")));
        assertFalse(PushPolicy.eligible("anytime",Instant.parse("2026-10-08T16:30:00Z"))); // 10pm
        assertFalse(PushPolicy.eligible("invalid",Instant.parse("2026-10-08T08:30:00Z")));
    }
    @Test void noCredentialsOrClientConfigCannotSend() {
        for(String credential:new String[]{"", "{}", "{\"project_info\":{\"project_id\":\"japamala-8284d\"}}"}) {
            var sender=new FirebasePushSender(new ObjectMapper(),"japamala-8284d",credential);
            assertFalse(sender.configured());assertEquals("failed",sender.send("token","Title","Body",java.util.Map.of(),false).status());
        }
    }
    @Test void registerRequiresDeviceSecretAndExplicitOptIn() throws Exception {
        var jdbc=mock(JdbcTemplate.class);var residents=mock(ResidentService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new PushDeviceController(jdbc,residents,new ChatRateLimiter(10),mock(FirebasePushSender.class),"")).build();
        String path="/api/tridasa/push/installations/"+UUID.randomUUID();
        String body="{\"token\":\"test-notification-token-00001\",\"consent\":false}";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(put(path).header("Authorization","Bearer "+"a".repeat(64)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(jdbc);
    }
    @Test void anotherCapabilityCannotTakeOverInstallation() throws Exception {
        var jdbc=mock(JdbcTemplate.class);var residents=mock(ResidentService.class);
        when(residents.community("tridasa")).thenReturn(UUID.randomUUID());when(residents.hash(anyString())).thenReturn("hash");
        var mvc=MockMvcBuilders.standaloneSetup(new PushDeviceController(jdbc,residents,new ChatRateLimiter(10),mock(FirebasePushSender.class),"")).build();
        // A conflicting installation update affects zero rows unless its hash matches.
        mvc.perform(put("/api/tridasa/push/installations/"+UUID.randomUUID()).header("Authorization","Bearer "+"a".repeat(64))
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"test-notification-token-00001\",\"consent\":true}")).andExpect(status().isUnauthorized());
    }
    @Test void operatorEndpointsNeverSendWithoutOperatorKey() throws Exception {
        var jdbc=mock(JdbcTemplate.class);var sender=mock(FirebasePushSender.class);var campaigns=mock(PushCampaignService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new AdminPushController(jdbc,mock(ResidentService.class),sender,campaigns,"operator-test-key")).build();
        mvc.perform(post("/api/tridasa/admin/push/campaigns/"+UUID.randomUUID()+"/send")).andExpect(status().isUnauthorized());
        verifyNoInteractions(jdbc,sender,campaigns);
    }
    @Test void missingFirebaseDoesNotDispatchOrReserveMessages() {
        var jdbc=mock(JdbcTemplate.class);var sender=mock(FirebasePushSender.class);
        var service=new PushCampaignService(jdbc,sender,true);
        assertEquals(0,service.send(UUID.randomUUID(),UUID.randomUUID()).get("accepted"));service.scheduled();
        verifyNoInteractions(jdbc);
    }
    @Test void quotaCooldownAndDuplicateClaimsPreventSending() throws Exception {
        Instant now=Instant.parse("2026-10-08T06:30:00Z");
        for(String scenario:List.of("quota","cooldown","duplicate","new")) {
            var jdbc=mock(JdbcTemplate.class);var sender=mock(FirebasePushSender.class);
            var connection=mock(Connection.class);var statement=mock(Statement.class);var rs=mock(ResultSet.class);
            when(sender.configured()).thenReturn(true);when(connection.createStatement()).thenReturn(statement);when(statement.executeQuery(anyString())).thenReturn(rs);when(rs.getBoolean(1)).thenReturn(true);
            when(jdbc.execute(org.mockito.ArgumentMatchers.<ConnectionCallback<Map<String,Integer>>>any())).thenAnswer(call->((ConnectionCallback<?>)call.getArgument(0)).doInConnection(connection));
            UUID id=UUID.randomUUID(),community=UUID.randomUUID(),campaign=UUID.randomUUID();
            when(jdbc.queryForList(startsWith("SELECT i.id"),any(Object[].class))).thenReturn(List.of(Map.of("id",id,"community_id",community,"fcm_token","test-token")));
            Map<String,Object> quota=new HashMap<>();quota.put("today",scenario.equals("quota")?3L:0L);quota.put("last",scenario.equals("cooldown")?Timestamp.from(now.minusSeconds(3600)):null);
            when(jdbc.queryForMap(startsWith("SELECT count"),any(Object[].class))).thenReturn(quota);
            when(jdbc.queryForList(startsWith("SELECT m.id"),any(Object[].class))).thenReturn(List.of(Map.of("id",campaign,"title","Test","body","Neighbour shop","daypart","lunch","provider_id",UUID.randomUUID(),"slug","tridasa","demo",true)));
            when(jdbc.update(startsWith("INSERT INTO push_campaign_delivery"),any(Object[].class))).thenReturn(scenario.equals("duplicate")?0:1);
            when(sender.send(anyString(),anyString(),anyString(),anyMap(),eq(false))).thenReturn(new FirebasePushSender.Result("accepted",false));
            var service=new PushCampaignService(jdbc,sender,true,Clock.fixed(now,ZoneOffset.UTC));
            var result=service.send(community,null);
            if(scenario.equals("new")){assertEquals(1,result.get("accepted"));verify(sender).send(eq("test-token"),eq("Demo · Test"),eq("Neighbour shop"),anyMap(),eq(false));}
            else {assertEquals(0,result.get("accepted"));verify(sender,never()).send(anyString(),anyString(),anyString(),anyMap(),anyBoolean());}
        }
    }
}
