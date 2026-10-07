package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "community")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Community {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String city;
    private Integer totalFlats;
    private String status = "active";
    @Column(unique = true, nullable = false) private String slug;
    private Instant createdAt = Instant.now();
}
