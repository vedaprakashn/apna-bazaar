package com.apnabazaar.dto;
import com.apnabazaar.entity.Provider;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Builder
public record MatchedProvider(
    UUID id, String name, String shopName, String flatNumber, String whatsappNumber, String whatsappGroupUrl,
    String matchReason, BigDecimal rating, int reviewCount,
    List<TodayItemDto> todayItems, String orderingStatus
) {
    public static MatchedProvider from(Provider p, String matchReason) {
        return MatchedProvider.builder()
            .id(p.getId()).name(p.getName())
            .shopName(p.getShopName() != null ? p.getShopName() : p.getName())
            .flatNumber(p.getFlatNumber()).whatsappNumber(p.getWhatsappNumber()).whatsappGroupUrl(p.getWhatsappGroupUrl())
            .matchReason(matchReason).rating(p.getRating()).reviewCount(p.getReviewCount())
            .build();
    }
}
