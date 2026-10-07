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

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable String communitySlug,
                                           @RequestParam(defaultValue = "14") int days) {
        LocalDate to = LocalDate.now();
        return ResponseEntity.ok(analyticsService.getCommunityDashboard(communitySlug, to.minusDays(days), to));
    }
}
