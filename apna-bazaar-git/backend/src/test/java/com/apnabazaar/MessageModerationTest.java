package com.apnabazaar;
import com.apnabazaar.service.*;
import com.apnabazaar.controller.SearchController;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.MediaType;

class MessageModerationTest {
    @Test void flagsBlockAndBenignClassesPass() {
        var builder=RestClient.builder().baseUrl("https://api.openai.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        var service=new MessageModerationService(builder.build());
        server.expect(requestTo("https://api.openai.com/v1/moderations")).andRespond(withSuccess("{\"results\":[{\"categories\":{\"sexual\":true}}]}",MediaType.APPLICATION_JSON));
        assertEquals(MessageModerationService.Decision.BLOCK,service.check("explicit request"));
        server.verify();server.reset();
        server.expect(requestTo("https://api.openai.com/v1/moderations")).andRespond(withSuccess("{\"results\":[{\"categories\":{\"sexual\":false,\"sexual/minors\":false,\"harassment\":false,\"harassment/threatening\":false,\"hate\":false,\"hate/threatening\":false,\"violence/graphic\":false}}]}",MediaType.APPLICATION_JSON));
        assertEquals(MessageModerationService.Decision.ALLOW,service.check("Adult beginner Kathak classes"));
        server.verify();
    }
    @Test void missingOrUnavailableScreeningFailsClosed() {
        var builder=RestClient.builder().baseUrl("https://api.openai.com");
        var server=MockRestServiceServer.bindTo(builder).build();var service=new MessageModerationService(builder.build());
        server.expect(anything()).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        assertEquals(MessageModerationService.Decision.UNAVAILABLE,service.check("Breakfast"));server.verify();server.reset();
        server.expect(anything()).andRespond(withServerError());
        assertEquals(MessageModerationService.Decision.UNAVAILABLE,service.check("Breakfast"));server.verify();
        assertEquals(MessageModerationService.Decision.BLOCK,service.check("you fucking asshole"));
    }
    @Test void rejectedMessagesNeverReachDiscoveryOrSearchAnalytics() throws Exception {
        var search=mock(SearchService.class);var moderation=mock(MessageModerationService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new SearchController(search,new ChatRateLimiter(10,()->0),moderation,"")).build();
        when(moderation.check("abuse")).thenReturn(MessageModerationService.Decision.BLOCK);
        mvc.perform(get("/api/tridasa/search").param("q","abuse")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.blocked").value(true));
        when(moderation.check("breakfast")).thenReturn(MessageModerationService.Decision.UNAVAILABLE);
        mvc.perform(get("/api/tridasa/search").param("q","breakfast")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/tridasa/search").param("q","a".repeat(501))).andExpect(status().isBadRequest());
        verifyNoInteractions(search);
    }
}
