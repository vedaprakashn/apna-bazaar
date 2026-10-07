package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "provider_click_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderClickEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "search_event_id", nullable = false) private SearchEvent searchEvent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", nullable = false) private Provider provider;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offering_id") private Offering offering;
    @Enumerated(EnumType.STRING) private ClickType clickType;
    private Instant clickedAt = Instant.now();
    public enum ClickType { whatsapp_tap, profile_view, menu_expand }
}
