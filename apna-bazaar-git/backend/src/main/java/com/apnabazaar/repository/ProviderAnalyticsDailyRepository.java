package com.apnabazaar.repository;
import com.apnabazaar.entity.ProviderAnalyticsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderAnalyticsDailyRepository extends JpaRepository<ProviderAnalyticsDaily, UUID> {
    Optional<ProviderAnalyticsDaily> findByProviderIdAndAnalyticsDate(UUID providerId, LocalDate date);

    @Query(value = """
        SELECT provider_id, SUM(whatsapp_clicks) as clicks
        FROM provider_analytics_daily
        WHERE provider_id IN (SELECT id FROM provider WHERE community_id = :communityId)
        AND analytics_date BETWEEN :from AND :to
        GROUP BY provider_id ORDER BY clicks DESC
        """, nativeQuery = true)
    List<Object[]> findTopProvidersByClicks(
        @Param("communityId") UUID communityId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
