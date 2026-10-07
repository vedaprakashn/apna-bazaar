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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
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
        SearchEvent event = SearchEvent.builder().id(UUID.randomUUID())
            .community(community).sessionId(sessionId)
            .rawQuery(rawQuery).normalisedQuery(normalisedQuery)
            .resultCount(resultCount).hadResults(resultCount > 0)
            .queryTime(now.toLocalTime()).queryDate(now.toLocalDate())
            .dayOfWeek((short) now.getDayOfWeek().getValue())
            .createdAt(Instant.now()).timeBucket(timeBucket(now.getHour(), now.getMinute())).build();
        // Bind LocalDate/LocalTime directly: Hibernate's JDBC timezone would shift
        // an already-local IST clock a second time.
        jdbc.update("""
            INSERT INTO search_event (id,community_id,session_id,raw_query,normalised_query,
                result_count,had_results,query_time,query_date,day_of_week,time_bucket)
            VALUES (?,?,?,?,?,?,?,?,?,?,?::time_bucket_enum)
            """, event.getId(), community.getId(), sessionId, rawQuery, normalisedQuery,
            resultCount, resultCount > 0, now.toLocalTime(), now.toLocalDate(),
            (short) now.getDayOfWeek().getValue(), event.getTimeBucket().name());
        return event;
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
            MatchedProvider provider = matched.get(i);
            jdbc.update("INSERT INTO search_result_impression (search_event_id,provider_id,rank_shown,match_reason) VALUES (?,?,?,?)",
                event.getId(), provider.id(), i + 1, provider.matchReason());
            jdbc.update("""
                INSERT INTO provider_analytics_daily (provider_id,analytics_date,impressions,searches_matched)
                VALUES (?,?,1,1) ON CONFLICT (provider_id,analytics_date)
                DO UPDATE SET impressions=provider_analytics_daily.impressions+1,
                              searches_matched=provider_analytics_daily.searches_matched+1
                """, provider.id(), today);
        }
    }

    @Transactional
    public void recordClick(UUID searchEventId, UUID providerId, String clickType) {
        ProviderClickEvent.ClickType.valueOf(clickType);
        Integer matches = jdbc.queryForObject("SELECT count(*) FROM search_result_impression WHERE search_event_id=? AND provider_id=?",
            Integer.class, searchEventId, providerId);
        if (matches == null || matches == 0) throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Provider must belong to these search results");
        jdbc.update("INSERT INTO provider_click_event (search_event_id,provider_id,click_type) VALUES (?,?,?::click_type_enum)",
            searchEventId, providerId, clickType);
        String column = "whatsapp_tap".equals(clickType) ? "whatsapp_clicks" : "profile_clicks";
        jdbc.update("INSERT INTO provider_analytics_daily (provider_id,analytics_date," + column + ") VALUES (?,?,1) "
            + "ON CONFLICT (provider_id,analytics_date) DO UPDATE SET " + column + "=provider_analytics_daily." + column + "+1",
            providerId, LocalDate.now(IST));
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
