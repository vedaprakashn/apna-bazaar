package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "daily_line_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyLineItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "daily_post_id", nullable = false) private DailyPost dailyPost;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offering_id") private Offering offering;
    @Column(name = "item_name", nullable = false) private String itemName;
    private BigDecimal price;
    private String pickupTime;
    private String pickupLocation;
    @Enumerated(EnumType.STRING) private DeliveryType deliveryType = DeliveryType.pickup;
    private Integer quantityAvailable;
    private String notes;
    private Instant createdAt = Instant.now();
    private LocalTime servesFrom;
    private LocalTime servesTo;
    private Boolean acceptsPreorder;
    private LocalTime preorderClosesAt;
    @Enumerated(EnumType.STRING) private OfferingSchedule.PreorderDayOffset preorderDayOffset;
    private Boolean acceptsRealtime;
    private Integer realtimeCutoffMinutes;
    private Integer quantityOrdered = 0;
    @Column(name = "order_cutoff_at") private Instant orderCutoffAt;
    public enum DeliveryType { pickup, home_delivery, both }
}
