package com.apnabazaar.controller;
import com.apnabazaar.dto.ClickRequest;
import com.apnabazaar.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/{communitySlug}/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<?> search(@PathVariable String communitySlug,
                                     @RequestParam String q,
                                     @RequestParam(required = false) UUID sessionId) {
        return ResponseEntity.ok(searchService.search(communitySlug, q, sessionId));
    }

    @PostMapping("/click")
    public ResponseEntity<?> recordClick(@RequestBody ClickRequest req) {
        searchService.recordClick(req.searchEventId(), req.providerId(), req.clickType());
        return ResponseEntity.ok().build();
    }
}
