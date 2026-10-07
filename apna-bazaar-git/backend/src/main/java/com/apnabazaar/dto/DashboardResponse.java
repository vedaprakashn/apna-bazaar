package com.apnabazaar.dto;
import java.util.List;
public record DashboardResponse(long totalSearchesToday, long totalSearchesWeek,
    long zeroResultQueries, int activeProvidersToday, List<QueryCount> topQueries,
    List<UnmetDemandItem> unmetDemand, List<InsightItem> pendingInsights,
    List<ProviderPerformance> topProviders) {}
