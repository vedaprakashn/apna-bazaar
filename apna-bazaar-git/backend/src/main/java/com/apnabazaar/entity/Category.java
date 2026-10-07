package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Category parent;
    @Column(nullable = false) private String name;
    private String iconEmoji;
    @Enumerated(EnumType.STRING) private Domain domain;
    private Integer sortOrder = 0;
    private Instant createdAt = Instant.now();
    public enum Domain { food, grocery, services, education, other }
}
