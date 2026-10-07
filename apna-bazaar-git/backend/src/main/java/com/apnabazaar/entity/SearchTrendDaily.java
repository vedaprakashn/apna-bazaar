package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "search_trend_daily",
       uniqueConstraints = @UniqueConstraint(columnNames = {"community_id","category_id","trend_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchTrendDaily {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id") private Category category;
    private LocalDate trendDate;
    private Integer searchCount = 0;
    private Integer uniqueSearchers = 0;
    private Integer resultClicks = 0;
    private BigDecimal clickThroughRate = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) private SearchEvent.TimeBucket peakTimeBucket;
}
