package com.apnabazaar.controller;
import com.apnabazaar.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/{communitySlug}/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {
    private final AnalyticsService analyticsService;
    private final com.apnabazaar.service.LiveDashboardService liveDashboard;

    @GetMapping("/activity")
    public ResponseEntity<?> activity(@PathVariable String communitySlug,
                                      @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(liveDashboard.activity(communitySlug, days));
    }

    @GetMapping("/catalog")
    public ResponseEntity<?> catalog(@PathVariable String communitySlug) {
        return ResponseEntity.ok(liveDashboard.catalog(communitySlug));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable String communitySlug,
                                           @RequestParam(defaultValue = "14") int days) {
        LocalDate to = LocalDate.now();
        return ResponseEntity.ok(analyticsService.getCommunityDashboard(communitySlug, to.minusDays(days), to));
    }
}
