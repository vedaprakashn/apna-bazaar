package com.apnabazaar.service;
import com.apnabazaar.dto.*;
import com.apnabazaar.entity.*;
import com.apnabazaar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final OpenAiChatModel chatModel;
    private final CommunityRepository communityRepo;
    private final ProviderRepository providerRepo;
    private final DailyPostRepository dailyPostRepo;
    private final AnalyticsService analyticsService;
    private final OrderingWindowService orderingWindowService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SearchResponse search(String communitySlug, String query, UUID sessionId) {
        Community community = communityRepo.findBySlug(communitySlug)
            .orElseThrow(() -> new RuntimeException("Community not found: " + communitySlug));
        List<Provider> providers = providerRepo.findActiveWithOfferings(community.getId());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<DailyPost> posts = dailyPostRepo.findActiveTodayForCommunity(community.getId(), today);
        Map<UUID, List<DailyPost>> postsByProvider = posts.stream()
            .collect(Collectors.groupingBy(dp -> dp.getProvider().getId()));

        String aiResponse = callOpenAi(query, buildCatalog(providers, postsByProvider));
        List<MatchedProvider> matched = parseResponse(aiResponse, providers, postsByProvider);

        SearchEvent event = analyticsService.recordSearchEvent(
            community, query, normalise(query), matched.size(), sessionId);
        if (matched.isEmpty()) analyticsService.recordZeroResult(community, query, normalise(query));
        else analyticsService.recordImpressions(event, matched);

        return SearchResponse.builder()
            .intro(Optional.ofNullable(extract(aiResponse, "intro")).orElse(matched.isEmpty() ? aiResponse : "Here are your community matches."))
            .providers(matched)
            .sessionId(event.getId())
            .totalResults(matched.size())
            .build();
    }

    @Transactional
    public void recordClick(UUID searchEventId, UUID providerId, String clickType) {
        analyticsService.recordClick(searchEventId, providerId, clickType);
    }

    private String buildCatalog(List<Provider> providers, Map<UUID, List<DailyPost>> posts) {
        var sb = new StringBuilder();
        for (Provider p : providers) {
            sb.append("SELLER ID:").append(p.getId())
              .append(" | ").append(p.getName())
              .append(" | Shop: ").append(p.getShopName() != null ? p.getShopName() : p.getName())
              .append(" | Flat: ").append(p.getFlatNumber())
              .append("\nPERMANENT CATALOG:\n");
            if (p.getOfferings() != null) {
                p.getOfferings().stream().filter(o -> Boolean.TRUE.equals(o.getIsAvailable())).forEach(o -> {
                    sb.append("  - ").append(o.getName()).append(": ").append(o.getDescription())
                      .append(" | Price: Rs.").append(o.getBasePrice()).append("/").append(o.getUnit()).append("\n");
                    o.getSchedules().stream().filter(sc -> Boolean.TRUE.equals(sc.getIsActive())).forEach(sc ->
                        sb.append("    Schedule: ").append(sc.getDayScope()).append(" ")
                          .append(sc.getDaysOfWeek() == null ? "" : sc.getDaysOfWeek()).append(" ")
                          .append(sc.getServesFrom()).append("–").append(sc.getServesTo()).append(" Asia/Kolkata\n"));
                });
            }
            List<DailyPost> dp = posts.get(p.getId());
            if (dp != null && !dp.isEmpty()) {
                sb.append("TODAY'S MENU:\n");
                dp.forEach(post -> post.getLineItems().forEach(li ->
                    sb.append("  - ").append(li.getItemName())
                      .append(li.getPrice() != null ? " Rs." + li.getPrice() : "")
                      .append(li.getPickupTime() != null ? " | Pickup: " + li.getPickupTime() : "")
                      .append("\n")));
            }
            sb.append("---\n\n");
        }
        return sb.toString();
    }

    private String callOpenAi(String query, String catalog) {
        String sys = """
            You are the Apna Bazaar assistant for a gated community. Help residents find sellers.
            SELLER DATABASE:
            %s
            Rules:
            1. Match semantically — understand intent, not keywords. Respect stated days and schedules.
            Only use sellers from this database. Explain when availability is on a different day.
            Listings marked DEMO are fictional; do not invent contacts, ratings, or stock.
            2. Be warm, peppy, use food emojis naturally
            3. Keep intro to ONE punchy sentence max
            4. ALWAYS respond in this exact format when sellers found:
            <intro>One punchy sentence!</intro>
            <sellers>[{"id":"<uuid>","matchReason":"specific item and why, 1 line"}]</sellers>
            If nothing matches, reply conversationally with no JSON.
            """.formatted(catalog);
        var prompt = new Prompt(List.of(new SystemMessage(sys), new UserMessage(query)));
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    private List<MatchedProvider> parseResponse(String raw, List<Provider> providers,
                                                 Map<UUID, List<DailyPost>> posts) {
        var m = Pattern.compile("<sellers>([\\s\\S]*?)</sellers>").matcher(raw);
        if (!m.find()) return List.of();
        Map<UUID, Provider> map = providers.stream().collect(Collectors.toMap(Provider::getId, p -> p));
        try {
            List<Map<String, String>> parsed = objectMapper.readValue(m.group(1).trim(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            return parsed.stream().map(r -> {
                try {
                    UUID id = UUID.fromString(r.get("id"));
                    Provider p = map.get(id);
                    if (p == null) return null;
                    List<TodayItemDto> items = buildItems(posts.get(id));
                    return MatchedProvider.builder()
                        .id(p.getId()).name(p.getName())
                        .shopName(p.getShopName() != null ? p.getShopName() : p.getName())
                        .flatNumber(p.getFlatNumber()).whatsappNumber(p.getWhatsappNumber())
                        .matchReason(r.get("matchReason"))
                        .rating(p.getRating()).reviewCount(p.getReviewCount())
                        .todayItems(items)
                        .orderingStatus(items.stream().map(TodayItemDto::orderingStatus)
                            .filter(Objects::nonNull).findFirst().orElse(null))
                        .build();
                } catch (Exception e) { return null; }
            }).filter(Objects::nonNull).toList();
        } catch (Exception e) { log.error("Parse error", e); return List.of(); }
    }

    private List<TodayItemDto> buildItems(List<DailyPost> dp) {
        if (dp == null) return List.of();
        return dp.stream().flatMap(p -> p.getLineItems().stream()).map(li -> {
            String status = null;
            try { status = orderingWindowService.resolve(li).toChatLine(); } catch (Exception ignored) {}
            return new TodayItemDto(li.getItemName(), li.getPrice(), li.getPickupTime(),
                li.getPickupLocation(), li.getDeliveryType() != null ? li.getDeliveryType().name() : null, status);
        }).toList();
    }

    private String extract(String raw, String tag) {
        var m = Pattern.compile("<" + tag + ">([\\s\\S]*?)</" + tag + ">").matcher(raw);
        return m.find() ? m.group(1).trim() : null;
    }

    private String normalise(String q) {
        return q.toLowerCase().trim().replaceAll("[^a-z0-9 ]","").replaceAll("\\s+"," ");
    }
}
