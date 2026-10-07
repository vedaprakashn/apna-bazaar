package com.apnabazaar.repository;
import com.apnabazaar.entity.SearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SearchEventRepository extends JpaRepository<SearchEvent, UUID> {

    @Query(value = """
        SELECT normalised_query, COUNT(*) as cnt
        FROM search_event
        WHERE community_id = :communityId
        AND query_date BETWEEN :from AND :to
        GROUP BY normalised_query
        ORDER BY cnt DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<Object[]> findTopQueries(
        @Param("communityId") UUID communityId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("lim") int limit
    );

    long countByCommunityIdAndQueryDate(UUID communityId, LocalDate date);
    long countByCommunityIdAndHadResults(UUID communityId, boolean hadResults);
}
