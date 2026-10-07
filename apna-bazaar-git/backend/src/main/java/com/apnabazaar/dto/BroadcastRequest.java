package com.apnabazaar.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record BroadcastRequest(String message, String type, LocalDateTime scheduledAt,
    String recurrencePattern, UUID providerId, Integer promoAmountPaise) {}
