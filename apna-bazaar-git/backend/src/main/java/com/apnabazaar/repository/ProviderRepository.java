package com.apnabazaar.repository;
import com.apnabazaar.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {
    List<Provider> findByCommunityIdAndStatus(UUID communityId, Provider.ProviderStatus status);
    Optional<Provider> findByCommunityIdAndWhatsappNumber(UUID communityId, String whatsappNumber);

    @Query("""
        SELECT DISTINCT p FROM Provider p
        LEFT JOIN FETCH p.offerings o
        LEFT JOIN FETCH o.schedules
        WHERE p.community.id = :communityId AND p.status = 'active'
        """)
    List<Provider> findActiveWithOfferings(@Param("communityId") UUID communityId);
}
