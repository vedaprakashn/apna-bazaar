package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "demand_insight")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandInsight {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id") private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id") private Provider provider;
    @Enumerated(EnumType.STRING) private InsightType insightType;
    @Column(name = "insight_message", columnDefinition = "TEXT") private String insightMessage;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "supporting_data", columnDefinition = "jsonb")
    private Map<String, Object> supportingData;
    private LocalDate weekOf;
    private Boolean isActioned = false;
    private Instant createdAt = Instant.now();
    public enum InsightType {
        peak_time_mismatch, unmet_demand, competitor_gap,
        pricing_signal, day_pattern, low_stock_miss, high_performer
    }
}
