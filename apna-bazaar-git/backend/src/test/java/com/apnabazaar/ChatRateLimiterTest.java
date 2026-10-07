package com.apnabazaar;

import com.apnabazaar.service.ChatRateLimiter;
import com.apnabazaar.controller.SearchController;
import com.apnabazaar.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatRateLimiterTest {
    @Test
    void rollingWindowExpiresIndividualMessagesAndSeparatesVisitors() {
        var now=new AtomicLong();
        var limiter=new ChatRateLimiter(10,now::get);
        assertTrue(limiter.acquire("a").allowed());
        now.set(10_000);
        for(int i=0;i<9;i++) assertTrue(limiter.acquire("a").allowed());
        assertEquals(50,limiter.acquire("a").retryAfterSeconds());
        assertTrue(limiter.acquire("b").allowed());
        now.set(59_999);
        assertFalse(limiter.acquire("a").allowed());
        now.set(60_000);
        assertTrue(limiter.acquire("a").allowed());
        assertFalse(limiter.acquire("a").allowed());
        now.set(70_000);
        assertTrue(limiter.acquire("a").allowed());
    }
    @Test
    void concurrentRequestsCannotExceedTheLimit() throws Exception {
        var limiter=new ChatRateLimiter(10,()->0);
        try(var pool=Executors.newFixedThreadPool(12)) {
            var tasks=new ArrayList<Callable<Boolean>>();
            for(int i=0;i<100;i++)tasks.add(()->limiter.acquire("one-client").allowed());
            long allowed=0;
            for(var result:pool.invokeAll(tasks))if(result.get())allowed++;
            assertEquals(10,allowed);
        }
    }
    @Test
    void rejectsBeforeAiAcrossCommunitiesAndSessionIds() throws Exception {
        var service=mock(SearchService.class);
        var limiter=new ChatRateLimiter(10,()->0);
        var mvc=MockMvcBuilders.standaloneSetup(new SearchController(service,limiter,"railway-test")).build();
        for(int i=0;i<10;i++)mvc.perform(get("/api/tridasa/search").param("q","idli")
            .param("sessionId",UUID.randomUUID().toString())
            .header("X-Forwarded-For","198.51.100."+i+", 203.0.113.8")).andExpect(status().isOk());
        mvc.perform(get("/api/sayuk/search").param("q","idli")
            .param("sessionId",UUID.randomUUID().toString())
            .header("X-Forwarded-For","192.0.2.99, 203.0.113.8"))
            .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","60"));
        verify(service,times(10)).search(anyString(),anyString(),any());
        mvc.perform(get("/api/sayuk/search").param("q","idli")
            .header("X-Forwarded-For","203.0.113.9")).andExpect(status().isOk());
        verify(service,times(11)).search(anyString(),anyString(),any());
    }
}
