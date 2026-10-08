package com.apnabazaar.controller;
import com.apnabazaar.dto.ClickRequest;
import com.apnabazaar.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import com.apnabazaar.service.ChatRateLimiter;
import com.apnabazaar.service.MessageModerationService;
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
    private final MessageModerationService moderation;

    public SearchController(SearchService searchService, ChatRateLimiter limiter, MessageModerationService moderation,
                            @Value("${RAILWAY_PROJECT_ID:}") String railwayProjectId) {
        this.searchService=searchService;
        this.limiter=limiter;
        this.moderation=moderation;
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
        if (q.isBlank() || q.length()>500) return ResponseEntity.badRequest().body(Map.of("error", "Please send a question between 1 and 500 characters."));
        var screened=moderation.check(q);
        if(screened==MessageModerationService.Decision.BLOCK) return ResponseEntity.unprocessableEntity()
            .body(Map.of("error",MessageModerationService.BLOCK_REPLY,"blocked",true));
        if(screened!=MessageModerationService.Decision.ALLOW) return ResponseEntity.status(503)
            .body(Map.of("error","I can’t check that message right now. Please try again in a moment."));
        return ResponseEntity.ok(searchService.search(communitySlug, q, sessionId));
    }

    public record Conversation(String q, UUID sessionId, java.util.List<String> history) {}
    @PostMapping
    public ResponseEntity<?> conversation(@PathVariable String communitySlug,@RequestBody Conversation chat,HttpServletRequest request) {
        var decision=limiter.acquire(clientAddress(request));
        if(!decision.allowed())return ResponseEntity.status(429).header("Retry-After",Long.toString(decision.retryAfterSeconds())).body(Map.of("error","Give it a moment before sending another message."));
        var history=chat.history()==null?java.util.List.<String>of():chat.history();
        if(chat.q()==null||chat.q().isBlank()||chat.q().length()>500||history.size()>6||history.stream().anyMatch(q->q==null||q.length()>500))return ResponseEntity.badRequest().body(Map.of("error","Please shorten that message."));
        var screened=moderation.check(chat.q()+"\n"+String.join("\n",history));
        if(screened==MessageModerationService.Decision.BLOCK)return ResponseEntity.unprocessableEntity().body(Map.of("error",MessageModerationService.BLOCK_REPLY,"blocked",true));
        if(screened!=MessageModerationService.Decision.ALLOW)return ResponseEntity.status(503).body(Map.of("error","Could not check that message. Please retry."));
        return ResponseEntity.ok(searchService.search(communitySlug,chat.q(),chat.sessionId(),history));
    }

    @PostMapping("/click")
    public ResponseEntity<?> recordClick(@RequestBody ClickRequest req) {
        searchService.recordClick(req.searchEventId(), req.providerId(), req.clickType());
        return ResponseEntity.ok().build();
    }
}
