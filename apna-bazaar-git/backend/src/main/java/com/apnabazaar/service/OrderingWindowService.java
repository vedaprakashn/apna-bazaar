package com.apnabazaar.service;
import com.apnabazaar.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class OrderingWindowService {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    public OrderingStatus resolve(DailyLineItem item) {
        LocalDate today = LocalDate.now(IST);
        LocalDateTime now = LocalDateTime.now(IST);

        LocalTime servesFrom = coalesce(item.getServesFrom(), scheduleField(item, "from"));
        LocalTime servesTo   = coalesce(item.getServesTo(),   scheduleField(item, "to"));
        boolean acceptsPre   = Boolean.TRUE.equals(coalesce(item.getAcceptsPreorder(), true));
        boolean acceptsRT    = Boolean.TRUE.equals(coalesce(item.getAcceptsRealtime(),  true));
        LocalTime preClosed  = coalesce(item.getPreorderClosesAt(), scheduleField(item, "preorderCloses"));
        int rtCutoff         = coalesce(item.getRealtimeCutoffMinutes(), 0);
        OfferingSchedule.PreorderDayOffset offset =
            coalesce(item.getPreorderDayOffset(), OfferingSchedule.PreorderDayOffset.same_day);

        Instant preorderCutoff = computePreorderCutoff(today, preClosed, offset);
        Instant realtimeCutoff = servesTo != null
            ? LocalDateTime.of(today, servesTo).minusMinutes(rtCutoff).atZone(IST).toInstant()
            : null;
        Instant nowI = Instant.now();

        boolean servingNow = servesFrom != null && servesTo != null
            && !now.toLocalTime().isBefore(servesFrom)
            && now.toLocalTime().isBefore(servesTo);

        boolean preorderOpen = acceptsPre && preorderCutoff != null && nowI.isBefore(preorderCutoff);
        boolean rtOpen = acceptsRT && servingNow && (realtimeCutoff == null || nowI.isBefore(realtimeCutoff));

        if (rtOpen) {
            long mins = Duration.between(nowI, realtimeCutoff != null ? realtimeCutoff
                : LocalDateTime.of(today, servesTo).atZone(IST).toInstant()).toMinutes();
            String urgency = mins <= 15 ? " ⚡ Only " + mins + " mins left!" : "";
            return new OrderingStatus(State.REALTIME_OPEN,
                "🟢 Ordering open now" + urgency,
                servesTo != null ? "Serving until " + fmt(servesTo) : null,
                preorderOpen ? "Pre-orders also open until " + fmt(preClosed) : null);
        }
        if (preorderOpen) {
            String dayNote = offset == OfferingSchedule.PreorderDayOffset.day_before ? " (for tomorrow)" : "";
            String serving = servesFrom != null ? "Serving " + fmt(servesFrom) + "–" + fmt(servesTo) : "";
            return new OrderingStatus(State.PREORDER_OPEN,
                "📋 Pre-order open — closes " + fmt(preClosed) + dayNote, serving, null);
        }
        if (servingNow) {
            return new OrderingStatus(State.SERVING_NO_ORDERS,
                "🍽️ Serving now — orders closed", "Serving until " + fmt(servesTo), null);
        }

        String next = acceptsPre && preClosed != null
            ? "Pre-order " + (offset == OfferingSchedule.PreorderDayOffset.day_before
                ? "today by " + fmt(preClosed) + " for tomorrow"
                : "tomorrow by " + fmt(preClosed))
            : (acceptsRT && servesFrom != null ? "Opens tomorrow from " + fmt(servesFrom) : "Check back tomorrow");
        return new OrderingStatus(State.CLOSED, "🔴 Ordering closed", next, null);
    }

    private Instant computePreorderCutoff(LocalDate today, LocalTime closesAt,
                                           OfferingSchedule.PreorderDayOffset offset) {
        if (closesAt == null) return null;
        return LocalDateTime.of(today, closesAt).atZone(IST).toInstant();
    }

    private LocalTime scheduleField(DailyLineItem item, String field) {
        if (item.getOffering() == null || item.getOffering().getSchedules() == null) return null;
        return item.getOffering().getSchedules().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive())
                && s.appliesOn(LocalDate.now(IST).getDayOfWeek()))
            .findFirst()
            .map(s -> switch (field) {
                case "from" -> s.getServesFrom();
                case "to" -> s.getServesTo();
                case "preorderCloses" -> s.getPreorderClosesAt();
                default -> null;
            }).orElse(null);
    }

    private String fmt(LocalTime t) { return t != null ? t.format(TIME_FMT) : ""; }
    @SuppressWarnings("unchecked")
    private <T> T coalesce(T a, T b) { return a != null ? a : b; }

    public enum State { PREORDER_OPEN, REALTIME_OPEN, SERVING_NO_ORDERS, CLOSED }
    public record OrderingStatus(State state, String headline, String detail, String callToAction) {
        public String toChatLine() {
            var sb = new StringBuilder(headline);
            if (detail != null && !detail.isBlank()) sb.append(" · ").append(detail);
            if (callToAction != null) sb.append(" · ").append(callToAction);
            return sb.toString();
        }
    }
}
