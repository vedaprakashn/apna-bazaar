package com.apnabazaar.repository;
import com.apnabazaar.entity.DemandInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DemandInsightRepository extends JpaRepository<DemandInsight, UUID> {

    @Query("""
        SELECT d FROM DemandInsight d
        WHERE d.community.id = :communityId AND d.isActioned = false
        ORDER BY d.createdAt DESC
        """)
    List<DemandInsight> findUnactioned(@Param("communityId") UUID communityId);
}
