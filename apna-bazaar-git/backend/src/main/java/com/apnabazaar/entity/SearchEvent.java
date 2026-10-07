package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "search_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    private UUID sessionId;
    @Column(name = "raw_query", nullable = false) private String rawQuery;
    private String normalisedQuery;
    @Column(columnDefinition = "text") private String englishIntent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "matched_category_id") private Category matchedCategory;
    private Integer resultCount = 0;
    private Boolean hadResults = false;
    @Column(name = "query_time", nullable = false) private LocalTime queryTime;
    @Column(name = "query_date", nullable = false) private LocalDate queryDate;
    private Short dayOfWeek;
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "time_bucket_enum")
    private TimeBucket timeBucket;
    private String deviceType;
    private Instant createdAt = Instant.now();
    public enum TimeBucket {
        early_morning, morning_630, morning_7, morning_730, morning_8, morning_9,
        mid_morning, lunch_11, lunch_12, afternoon_1, afternoon_2,
        evening_4, evening_5, evening_6, evening_7, night_8, night_9, late_night
    }
}
