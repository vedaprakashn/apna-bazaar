package com.apnabazaar.dto;
import java.util.UUID;
public record ClickRequest(UUID searchEventId, UUID providerId, String clickType) {}
