package com.apnabazaar.service;

import java.time.*;

public final class PushPolicy {
    public static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private PushPolicy() {}
    public static boolean eligible(String daypart, Instant now) {
        int hour = now.atZone(INDIA).getHour();
        if (hour < 8 || hour >= 22) return false;
        return switch (daypart) {
            case "morning" -> hour < 11;
            case "lunch" -> hour >= 11 && hour < 15;
            case "afternoon" -> hour >= 15 && hour < 17;
            case "evening" -> hour >= 17;
            case "anytime" -> true;
            default -> false;
        };
    }
}
