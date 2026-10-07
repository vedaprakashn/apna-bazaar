package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "offering_schedule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferingSchedule {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offering_id", nullable = false) private Offering offering;
    @Enumerated(EnumType.STRING) @Column(name = "day_scope") private DayScope dayScope = DayScope.daily;
    private String daysOfWeek;
    @Column(name = "serves_from", nullable = false) private LocalTime servesFrom;
    @Column(name = "serves_to",   nullable = false) private LocalTime servesTo;
    private Boolean acceptsPreorder = true;
    @Enumerated(EnumType.STRING) @Column(name = "preorder_day_offset") private PreorderDayOffset preorderDayOffset = PreorderDayOffset.same_day;
    private LocalTime preorderOpensAt;
    private LocalTime preorderClosesAt;
    private Boolean acceptsRealtime = true;
    private Integer realtimeCutoffMinutes = 0;
    private Integer maxOrdersPerSlot;
    private Boolean isActive = true;
    private Instant createdAt = Instant.now();

    public enum DayScope { daily, weekdays, weekends, specific_days, sunday_only, saturday_only }
    public enum PreorderDayOffset { same_day, day_before, two_days_prior }

    public boolean appliesOn(DayOfWeek day) {
        return switch (dayScope) {
            case daily -> true;
            case weekdays -> day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
            case weekends -> day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
            case sunday_only -> day == DayOfWeek.SUNDAY;
            case saturday_only -> day == DayOfWeek.SATURDAY;
            case specific_days -> daysOfWeek != null && daysOfWeek.contains(day.name().substring(0, 3));
        };
    }

    public Instant computePreorderCutoff(LocalDate servingDate, ZoneId zone) {
        if (!Boolean.TRUE.equals(acceptsPreorder) || preorderClosesAt == null) return null;
        LocalDate cutoffDate = switch (preorderDayOffset) {
            case same_day -> servingDate;
            case day_before -> servingDate.minusDays(1);
            case two_days_prior -> servingDate.minusDays(2);
        };
        return LocalDateTime.of(cutoffDate, preorderClosesAt).atZone(zone).toInstant();
    }

    public Instant computeRealtimeCutoff(LocalDate servingDate, ZoneId zone) {
        if (!Boolean.TRUE.equals(acceptsRealtime) || servesTo == null) return null;
        return LocalDateTime.of(servingDate, servesTo)
            .minusMinutes(realtimeCutoffMinutes != null ? realtimeCutoffMinutes : 0)
            .atZone(zone).toInstant();
    }
}
