package com.apnabazaar.repository;
import com.apnabazaar.entity.DailyPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyPostRepository extends JpaRepository<DailyPost, UUID> {
    List<DailyPost> findByProviderIdAndPostDate(UUID providerId, LocalDate date);

    @Query("""
        SELECT dp FROM DailyPost dp
        JOIN FETCH dp.lineItems
        JOIN FETCH dp.provider p
        WHERE dp.postDate = :date
        AND p.community.id = :communityId
        AND dp.isActive = true
        """)
    List<DailyPost> findActiveTodayForCommunity(
        @Param("communityId") UUID communityId,
        @Param("date") LocalDate date
    );
}
