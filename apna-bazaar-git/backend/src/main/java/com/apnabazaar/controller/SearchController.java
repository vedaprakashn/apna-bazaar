package com.apnabazaar.controller;
import com.apnabazaar.dto.ClickRequest;
import com.apnabazaar.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import com.apnabazaar.service.ChatRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/{communitySlug}/search")
@CrossOrigin(origins = "*")
public class SearchController {
    private final SearchService searchService;
    private final ChatRateLimiter limiter;
    private final boolean railwayProxy;

    public SearchController(SearchService searchService, ChatRateLimiter limiter,
                            @Value("${RAILWAY_PROJECT_ID:}") String railwayProjectId) {
        this.searchService=searchService;
        this.limiter=limiter;
        this.railwayProxy=!railwayProjectId.isBlank();
    }

    private String clientAddress(HttpServletRequest request) {
        // Railway's edge appends the connecting IP. Never trust a caller-prepended first entry.
        if(railwayProxy) {
            String forwarded=request.getHeader("X-Forwarded-For");
            if(forwarded != null) {
                String[] hops=forwarded.split(",");
                String address=hops[hops.length-1].trim();
                if(!address.isEmpty() && address.matches("[0-9a-fA-F:.]+")) return address;
            }
        }
        return request.getRemoteAddr();
    }

    @GetMapping
    public ResponseEntity<?> search(@PathVariable String communitySlug,
                                     @RequestParam String q,
                                     @RequestParam(required = false) UUID sessionId, HttpServletRequest request) {
        var decision=limiter.acquire(clientAddress(request));
        if(!decision.allowed()) return ResponseEntity.status(429)
            .header("Retry-After",Long.toString(decision.retryAfterSeconds()))
            .body(Map.of("error","You're sending messages too quickly. Please wait before trying again.",
                "retryAfterSeconds",decision.retryAfterSeconds()));
        return ResponseEntity.ok(searchService.search(communitySlug, q, sessionId));
    }

    @PostMapping("/click")
    public ResponseEntity<?> recordClick(@RequestBody ClickRequest req) {
        searchService.recordClick(req.searchEventId(), req.providerId(), req.clickType());
        return ResponseEntity.ok().build();
    }
}
