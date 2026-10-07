package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "zero_result_log",
       uniqueConstraints = @UniqueConstraint(columnNames = {"community_id","normalised_query"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ZeroResultLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    private String rawQuery;
    @Column(name = "normalised_query") private String normalisedQuery;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "closest_category_id") private Category closestCategory;
    private Integer occurrenceCount = 1;
    private LocalDate firstSeen;
    private LocalDate lastSeen;
    @Enumerated(EnumType.STRING) private ZeroResultStatus status = ZeroResultStatus.new_;
    public enum ZeroResultStatus { new_, reviewing, provider_sought, fulfilled, declined }
}
