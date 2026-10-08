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
    @Convert(converter = TypeConverter.class)
    @org.hibernate.annotations.ColumnTransformer(write = "CAST(? AS offering_type_enum)")
    @Column(name = "offering_type", columnDefinition = "offering_type_enum")
    private OfferingType offeringType = OfferingType.food_item;
    private BigDecimal basePrice;
    private String unit;
    private Boolean isAvailable = true;
    private String liveStatus = "unconfirmed";
    private Instant availabilityUpdatedAt;
    private Integer sortOrder = 0;
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default private List<OfferingSchedule> schedules = new ArrayList<>();
    @Converter
    public static class TypeConverter implements AttributeConverter<OfferingType, String> {
        public String convertToDatabaseColumn(OfferingType value) {
            return value == null ? null : value == OfferingType.class_ ? "class" : value.name();
        }
        public OfferingType convertToEntityAttribute(String value) {
            return value == null ? null : "class".equals(value) ? OfferingType.class_ : OfferingType.valueOf(value);
        }
    }
    public enum OfferingType { food_item, grocery_item, service_package, class_, other }
}
