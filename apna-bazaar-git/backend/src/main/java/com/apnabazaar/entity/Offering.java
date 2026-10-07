package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "offering")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Offering {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", nullable = false) private Provider provider;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id") private Category category;
    @Column(nullable = false) private String name;
    private String description;
    @Enumerated(EnumType.STRING) @Column(name = "offering_type") private OfferingType offeringType = OfferingType.food_item;
    private BigDecimal basePrice;
    private String unit;
    private Boolean isAvailable = true;
    private Integer sortOrder = 0;
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default private List<OfferingSchedule> schedules = new ArrayList<>();
    public enum OfferingType { food_item, grocery_item, service_package, class_, other }
}
