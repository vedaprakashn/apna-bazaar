package com.apnabazaar.controller;
import com.apnabazaar.dto.ProviderRegistrationRequest;
import com.apnabazaar.entity.Provider;
import com.apnabazaar.repository.CommunityRepository;
import com.apnabazaar.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{communitySlug}/providers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProviderController {
    private final ProviderRepository providerRepo;
    private final com.apnabazaar.service.LiveDashboardService storefront;

    @GetMapping("/{providerId}/storefront")
    public ResponseEntity<?> storefront(@PathVariable String communitySlug,
                                        @PathVariable java.util.UUID providerId) {
        return ResponseEntity.ok(storefront.storefront(communitySlug, providerId));
    }
    private final CommunityRepository communityRepo;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String communitySlug) {
        var community = communityRepo.findBySlug(communitySlug).orElseThrow();
        return ResponseEntity.ok(
            providerRepo.findByCommunityIdAndStatus(community.getId(), Provider.ProviderStatus.active));
    }

    @PostMapping
    public ResponseEntity<?> register(@PathVariable String communitySlug,
                                       @RequestBody ProviderRegistrationRequest req) {
        var community = communityRepo.findBySlug(communitySlug).orElseThrow();
        var provider = providerRepo.save(Provider.builder()
            .community(community).name(req.name()).shopName(req.shopName())
            .flatNumber(req.flatNumber()).whatsappNumber(req.whatsappNumber())
            .providerType(Provider.ProviderType.valueOf(
                req.providerType() != null ? req.providerType() : "food_seller"))
            .status(Provider.ProviderStatus.pending).build());
        return ResponseEntity.ok(provider);
    }
}
