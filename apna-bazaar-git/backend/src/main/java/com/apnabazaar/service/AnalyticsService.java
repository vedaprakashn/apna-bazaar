package com.apnabazaar.service;
import com.apnabazaar.dto.*;
import com.apnabazaar.entity.*;
import com.apnabazaar.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final SearchEventRepository searchEventRepo;
    private final SearchResultImpressionRepository impressionRepo;
    private final ProviderClickEventRepository clickRepo;
    private final ZeroResultLogRepository zeroResultRepo;
    private final ProviderAnalyticsDailyRepository providerAnalyticsRepo;
    private final DemandInsightRepository insightRepo;
    private final CommunityRepository communityRepo;
    private final ProviderRepository providerRepo;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Transactional
    public SearchEvent recordSearchEvent(Community community, String rawQuery,
                                          String normalisedQuery, int resultCount, UUID sessionId) {
        LocalDateTime now = LocalDateTime.now(IST);
        return searchEventRepo.save(SearchEvent.builder()
            .community(community).sessionId(sessionId)
            .rawQuery(rawQuery).normalisedQuery(normalisedQuery)
            .resultCount(resultCount).hadResults(resultCount > 0)
            .queryTime(now.toLocalTime()).queryDate(now.toLocalDate())
            .dayOfWeek((short) now.getDayOfWeek().getValue())
            .timeBucket(timeBucket(now.getHour(), now.getMinute()))
            .build());
    }

    @Transactional
    public void recordZeroResult(Community community, String rawQuery, String normalisedQuery) {
        LocalDate today = LocalDate.now(IST);
        zeroResultRepo.findByCommunityIdAndNormalisedQuery(community.getId(), normalisedQuery)
            .ifPresentOrElse(
                z -> { z.setOccurrenceCount(z.getOccurrenceCount() + 1); z.setLastSeen(today); zeroResultRepo.save(z); },
                () -> zeroResultRepo.save(ZeroResultLog.builder()
                    .community(community).rawQuery(rawQuery).normalisedQuery(normalisedQuery)
                    .occurrenceCount(1).firstSeen(today).lastSeen(today).build()));
    }

    @Transactional
    public void recordImpressions(SearchEvent event, List<MatchedProvider> matched) {
        LocalDate today = LocalDate.now(IST);
        for (int i = 0; i < matched.size(); i++) {
            MatchedProvider mp = matched.get(i);
            Provider pRef = new Provider(); pRef.setId(mp.id());
            impressionRepo.save(SearchResultImpression.builder()
                .searchEvent(event).provider(pRef).rankShown(i + 1)
                .matchReason(mp.matchReason()).build());
            providerAnalyticsRepo.findByProviderIdAndAnalyticsDate(mp.id(), today)
                .ifPresentOrElse(
                    pad -> { pad.setImpressions(pad.getImpressions() + 1); providerAnalyticsRepo.save(pad); },
                    () -> providerAnalyticsRepo.save(ProviderAnalyticsDaily.builder()
                        .provider(pRef).analyticsDate(today).impressions(1).build()));
        }
    }

    @Transactional
    public void recordClick(UUID searchEventId, UUID providerId, String clickType) {
        SearchEvent eRef = new SearchEvent(); eRef.setId(searchEventId);
        Provider pRef = new Provider(); pRef.setId(providerId);
        clickRepo.save(ProviderClickEvent.builder()
            .searchEvent(eRef).provider(pRef)
            .clickType(ProviderClickEvent.ClickType.valueOf(clickType)).build());
        if ("whatsapp_tap".equals(clickType)) {
            LocalDate today = LocalDate.now(IST);
            providerAnalyticsRepo.findByProviderIdAndAnalyticsDate(providerId, today)
                .ifPresentOrElse(
                    pad -> { pad.setWhatsappClicks(pad.getWhatsappClicks() + 1); providerAnalyticsRepo.save(pad); },
                    () -> providerAnalyticsRepo.save(ProviderAnalyticsDaily.builder()
                        .provider(pRef).analyticsDate(today).whatsappClicks(1).build()));
        }
    }

    @Scheduled(cron = "${apna.analytics.insight-generation-cron:0 0 2 * * MON}")
    @Transactional
    public void generateWeeklyInsights() {
        LocalDate weekOf = LocalDate.now(IST);
        communityRepo.findAll().forEach(c -> {
            zeroResultRepo.findTopUnmetDemand(c.getId()).stream()
                .filter(z -> z.getOccurrenceCount() >= 5).limit(10)
                .forEach(z -> insightRepo.save(DemandInsight.builder()
                    .community(c).insightType(DemandInsight.InsightType.unmet_demand)
                    .insightMessage(String.format("'%s' searched %d times with zero results. Onboard a seller.",
                        z.getRawQuery(), z.getOccurrenceCount()))
                    .supportingData(Map.of("query", z.getRawQuery(), "occurrences", z.getOccurrenceCount()))
                    .weekOf(weekOf).build()));
        });
    }

    public DashboardResponse getCommunityDashboard(String communitySlug, LocalDate from, LocalDate to) {
        Community community = communityRepo.findBySlug(communitySlug)
            .orElseThrow(() -> new RuntimeException("Community not found: " + communitySlug));
        LocalDate today = LocalDate.now(IST);
        long totalToday = searchEventRepo.countByCommunityIdAndQueryDate(community.getId(), today);
        long zeroResults = searchEventRepo.countByCommunityIdAndHadResults(community.getId(), false);
        List<QueryCount> topQ = searchEventRepo.findTopQueries(community.getId(), from, to, 20)
            .stream().map(r -> new QueryCount((String) r[0], ((Number) r[1]).longValue())).toList();
        List<UnmetDemandItem> unmet = zeroResultRepo.findTopUnmetDemand(community.getId()).stream()
            .map(z -> new UnmetDemandItem(z.getRawQuery(), z.getOccurrenceCount(),
                z.getFirstSeen().toString(), z.getStatus().name())).toList();
        List<InsightItem> insights = insightRepo.findUnactioned(community.getId()).stream()
            .map(i -> new InsightItem(i.getInsightType().name(), i.getInsightMessage(), i.getSupportingData())).toList();
        List<ProviderPerformance> topP = providerAnalyticsRepo.findTopProvidersByClicks(community.getId(), from, to)
            .stream().map(r -> {
                UUID pid = UUID.fromString(r[0].toString());
                String name = providerRepo.findById(pid).map(Provider::getName).orElse("Unknown");
                return new ProviderPerformance(pid, name, ((Number) r[1]).longValue());
            }).toList();
        int active = providerRepo.findByCommunityIdAndStatus(community.getId(), Provider.ProviderStatus.active).size();
        return new DashboardResponse(totalToday, totalToday, zeroResults, active, topQ, unmet, insights, topP);
    }

    private SearchEvent.TimeBucket timeBucket(int h, int m) {
        if (h < 6) return SearchEvent.TimeBucket.early_morning;
        if (h == 6) return SearchEvent.TimeBucket.morning_630;
        if (h == 7 && m < 30) return SearchEvent.TimeBucket.morning_7;
        if (h == 7) return SearchEvent.TimeBucket.morning_730;
        if (h == 8) return SearchEvent.TimeBucket.morning_8;
        if (h == 9) return SearchEvent.TimeBucket.morning_9;
        if (h <= 10) return SearchEvent.TimeBucket.mid_morning;
        if (h == 11) return SearchEvent.TimeBucket.lunch_11;
        if (h == 12) return SearchEvent.TimeBucket.lunch_12;
        if (h == 13) return SearchEvent.TimeBucket.afternoon_1;
        if (h == 14) return SearchEvent.TimeBucket.afternoon_2;
        if (h == 16) return SearchEvent.TimeBucket.evening_4;
        if (h == 17) return SearchEvent.TimeBucket.evening_5;
        if (h == 18) return SearchEvent.TimeBucket.evening_6;
        if (h == 19) return SearchEvent.TimeBucket.evening_7;
        if (h == 20) return SearchEvent.TimeBucket.night_8;
        if (h == 21) return SearchEvent.TimeBucket.night_9;
        return SearchEvent.TimeBucket.late_night;
    }
}
