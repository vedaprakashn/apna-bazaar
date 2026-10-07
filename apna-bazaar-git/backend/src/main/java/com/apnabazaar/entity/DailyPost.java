package com.apnabazaar.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "daily_post")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyPost {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", nullable = false) private Provider provider;
    @Column(name = "post_date", nullable = false) private LocalDate postDate;
    @Column(columnDefinition = "TEXT") private String rawMessage;
    @Enumerated(EnumType.STRING) private PostSource source = PostSource.excel_upload;
    private Boolean isActive = true;
    private Instant ingestedAt = Instant.now();
    @OneToMany(mappedBy = "dailyPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default private List<DailyLineItem> lineItems = new ArrayList<>();
    public enum PostSource { whatsapp_manual, whatsapp_auto, app_post, excel_upload }
}
