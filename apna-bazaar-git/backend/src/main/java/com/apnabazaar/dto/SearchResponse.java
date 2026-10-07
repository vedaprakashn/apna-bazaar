package com.apnabazaar.dto;
import lombok.Builder;
import java.util.List;
import java.util.UUID;
@Builder
public record SearchResponse(String intro, List<MatchedProvider> providers, UUID sessionId, int totalResults, String searchIntent, List<java.util.Map<String,Object>> contacts, boolean urgentHelp) {}
