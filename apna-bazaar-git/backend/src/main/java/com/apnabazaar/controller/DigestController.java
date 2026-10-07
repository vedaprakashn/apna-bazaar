package com.apnabazaar.controller;
import com.apnabazaar.repository.CommunityRepository;
import com.apnabazaar.repository.DailyPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.Map;

@RestController
@RequestMapping("/api/{communitySlug}/digest")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DigestController {
    private final DailyPostRepository dailyPostRepo;
    private final CommunityRepository communityRepo;

    @GetMapping
    public ResponseEntity<?> getDigest(@PathVariable String communitySlug) {
        var community = communityRepo.findBySlug(communitySlug).orElseThrow();
        var posts = dailyPostRepo.findActiveTodayForCommunity(
            community.getId(), LocalDate.now(ZoneId.of("Asia/Kolkata")));
        var sellers = posts.stream().map(dp -> Map.of(
            "sellerName", dp.getProvider().getName(),
            "shopName",   dp.getProvider().getShopName() != null ? dp.getProvider().getShopName() : dp.getProvider().getName(),
            "flat",       dp.getProvider().getFlatNumber() != null ? dp.getProvider().getFlatNumber() : "",
            "items",      dp.getLineItems().stream().map(li -> Map.of(
                "name", li.getItemName(),
                "price", li.getPrice() != null ? li.getPrice().toString() : "",
                "pickupTime", li.getPickupTime() != null ? li.getPickupTime() : ""
            )).toList()
        )).toList();
        return ResponseEntity.ok(Map.of("date", LocalDate.now().toString(), "sellers", sellers));
    }
}
