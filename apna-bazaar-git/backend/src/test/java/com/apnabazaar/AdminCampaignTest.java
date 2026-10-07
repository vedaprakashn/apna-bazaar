package com.apnabazaar;

import com.apnabazaar.controller.AdminCampaignController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCampaignTest {
    private static final String BODY="""
        {"offeringId":"00000000-0000-0000-0000-000000000001","title":"Biryani time",
         "body":"Want to try a box?","ctaText":"Take me there","kind":"promoted","active":true}
        """;
    @Test
    void campaignWritesRequireTheConfiguredAdminKey() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        var mvc=MockMvcBuilders.standaloneSetup(new AdminCampaignController(jdbc,"test-key")).build();
        mvc.perform(post("/api/tridasa/admin/campaigns").contentType("application/json").content(BODY))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tridasa/admin/campaigns").header("Authorization","Bearer wrong-key")
            .contentType("application/json").content(BODY)).andExpect(status().isUnauthorized());
        verifyNoInteractions(jdbc);
    }
    @Test
    void editingIsDisabledWithoutAnAdminKeyConfigured() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        var mvc=MockMvcBuilders.standaloneSetup(new AdminCampaignController(jdbc,"")).build();
        mvc.perform(post("/api/tridasa/admin/campaigns").contentType("application/json").content(BODY))
            .andExpect(status().isServiceUnavailable());
        verifyNoInteractions(jdbc);
    }
    @Test
    void invalidScheduleIsRejectedBeforeWriting() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        var mvc=MockMvcBuilders.standaloneSetup(new AdminCampaignController(jdbc,"test-key")).build();
        String body=BODY.replace("\"active\":true", "\"active\":true,\"startsAt\":\"2026-10-08T13:00:00+05:30\",\"endsAt\":\"2026-10-07T13:00:00+05:30\"");
        mvc.perform(post("/api/tridasa/admin/campaigns").header("Authorization","Bearer test-key")
            .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(jdbc);
    }
}
