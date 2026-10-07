package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "provider_analytics_daily",
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id","analytics_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderAnalyticsDaily {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", nullable = false) private Provider provider;
    private LocalDate analyticsDate;
    private Integer impressions = 0;
    private Integer profileClicks = 0;
    private Integer whatsappClicks = 0;
    private Integer searchesMatched = 0;
    private BigDecimal impressionToClickRate = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) private SearchEvent.TimeBucket peakDemandTime;
    private Integer zeroStockMisses = 0;
}
