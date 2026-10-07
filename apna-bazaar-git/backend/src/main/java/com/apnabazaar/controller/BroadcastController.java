package com.apnabazaar.controller;
import com.apnabazaar.dto.BroadcastRequest;
import com.apnabazaar.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/{communitySlug}/broadcasts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BroadcastController {
    private final BroadcastService broadcastService;

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String communitySlug,
                                     @RequestBody BroadcastRequest req) {
        return ResponseEntity.ok(broadcastService.create(communitySlug, req));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<?> sendNow(@PathVariable UUID id) {
        return ResponseEntity.ok(broadcastService.sendNow(id));
    }

    @GetMapping("/ai-suggestions")
    public ResponseEntity<?> aiSuggestions(@PathVariable String communitySlug) {
        return ResponseEntity.ok(broadcastService.generateAISuggestions(communitySlug));
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@PathVariable String communitySlug,
                                      @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(broadcastService.getHistory(communitySlug, limit));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<?> scheduled(@PathVariable String communitySlug) {
        return ResponseEntity.ok(broadcastService.getScheduled(communitySlug));
    }

    @GetMapping("/daily-digest")
    public ResponseEntity<?> dailyDigest(@PathVariable String communitySlug) {
        return ResponseEntity.ok(Map.of(
            "message", broadcastService.getActiveDailyDigest(communitySlug).orElse("")));
    }

    @PostMapping("/{id}/search-triggered")
    public ResponseEntity<?> recordSearch(@PathVariable UUID id) {
        broadcastService.recordBroadcastSearch(id);
        return ResponseEntity.ok().build();
    }
}
