package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "broadcast")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Broadcast {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id") private Provider provider;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Enumerated(EnumType.STRING) @Column(name = "broadcast_type") private BroadcastType broadcastType = BroadcastType.organic;
    @Enumerated(EnumType.STRING) private BroadcastStatus status = BroadcastStatus.draft;
    @Enumerated(EnumType.STRING) private BroadcastChannel channel = BroadcastChannel.chatbot_digest;
    @Column(name = "scheduled_at") private LocalDateTime scheduledAt;
    @Column(name = "sent_at") private Instant sentAt;
    private String recurrencePattern;
    private Boolean isRecurring = false;
    private Integer promoAmount;
    private String paymentStatus;
    private Integer impressions = 0;
    private Integer searchesTriggered = 0;
    private Integer whatsappTaps = 0;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private Map<String, Object> metadata;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    public enum BroadcastType { organic, sponsored, ai_generated }
    public enum BroadcastStatus { draft, scheduled, sent, cancelled, recurring_active, recurring_paused }
    public enum BroadcastChannel { chatbot_digest, whatsapp_push, both }
}
