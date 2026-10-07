package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "order_slot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderSlot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "daily_line_item_id", nullable = false) private DailyLineItem dailyLineItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    private String buyerName;
    private String buyerFlat;
    private String buyerWhatsapp;
    private Integer quantity = 1;
    @Enumerated(EnumType.STRING) private OrderType orderType;
    private Instant orderedAt = Instant.now();
    @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.placed;
    private String notes;
    private Instant createdAt = Instant.now();
    public enum OrderType { preorder, realtime }
    public enum OrderStatus { placed, confirmed, ready, completed, cancelled }
}
