package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "provider")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Provider {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    @Column(nullable = false) private String name;
    private String flatNumber;
    private String whatsappNumber;
    private String shopName;
    @Enumerated(EnumType.STRING) @Column(name = "provider_type") private ProviderType providerType = ProviderType.food_seller;
    @Enumerated(EnumType.STRING) private ProviderStatus status = ProviderStatus.pending;
    private Boolean isVerified = false;
    private BigDecimal rating = BigDecimal.ZERO;
    private Integer reviewCount = 0;
    private Instant joinedAt = Instant.now();
    @UpdateTimestamp private Instant updatedAt;
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default private List<Offering> offerings = new ArrayList<>();
    public enum ProviderType { food_seller, grocery, service, tuition, other }
    public enum ProviderStatus { active, pending, suspended, inactive }
}
