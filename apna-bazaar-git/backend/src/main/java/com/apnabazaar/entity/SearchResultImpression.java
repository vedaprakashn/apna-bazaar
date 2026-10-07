package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "search_result_impression")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchResultImpression {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "search_event_id", nullable = false) private SearchEvent searchEvent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", nullable = false) private Provider provider;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offering_id") private Offering offering;
    private Integer rankShown;
    private String matchReason;
    private Instant createdAt = Instant.now();
}
