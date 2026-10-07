package com.apnabazaar.repository;
import com.apnabazaar.entity.ZeroResultLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZeroResultLogRepository extends JpaRepository<ZeroResultLog, UUID> {

    @Query(value = """
        SELECT * FROM zero_result_log
        WHERE community_id = :communityId AND status = 'new_'
        ORDER BY occurrence_count DESC LIMIT 20
        """, nativeQuery = true)
    List<ZeroResultLog> findTopUnmetDemand(@Param("communityId") UUID communityId);

    Optional<ZeroResultLog> findByCommunityIdAndNormalisedQuery(UUID communityId, String query);
}
