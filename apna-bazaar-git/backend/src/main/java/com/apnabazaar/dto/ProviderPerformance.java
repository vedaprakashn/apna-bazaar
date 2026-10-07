package com.apnabazaar.dto;
import java.util.UUID;
public record ProviderPerformance(UUID providerId, String name, long clicks) {}
