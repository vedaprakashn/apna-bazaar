package com.apnabazaar.repository;
import com.apnabazaar.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BroadcastRepository extends JpaRepository<Broadcast, UUID> {

    @Query("""
        SELECT b FROM Broadcast b
        WHERE b.status = com.apnabazaar.entity.Broadcast.BroadcastStatus.scheduled
        AND b.scheduledAt <= :now ORDER BY b.scheduledAt ASC
        """)
    List<Broadcast> findDueForSending(@Param("now") LocalDateTime now);

    @Query(value = """
        SELECT * FROM broadcast
        WHERE community_id = (SELECT id FROM community WHERE slug = :slug)
        AND status = 'sent'
        AND DATE(sent_at AT TIME ZONE 'Asia/Kolkata') = :date
        ORDER BY sent_at DESC LIMIT 1
        """, nativeQuery = true)
    Optional<Broadcast> findLatestSentForCommunity(
        @Param("slug") String slug, @Param("date") LocalDate date);

    @Query(value = """
        SELECT * FROM broadcast
        WHERE community_id = (SELECT id FROM community WHERE slug = :slug)
        AND status = 'sent' ORDER BY sent_at DESC LIMIT :lim
        """, nativeQuery = true)
    List<Broadcast> findSentHistory(@Param("slug") String slug, @Param("lim") int limit);

    @Query("""
        SELECT b FROM Broadcast b WHERE b.community.slug = :slug
        AND b.status IN (
            com.apnabazaar.entity.Broadcast.BroadcastStatus.scheduled,
            com.apnabazaar.entity.Broadcast.BroadcastStatus.recurring_active
        ) ORDER BY b.scheduledAt ASC
        """)
    List<Broadcast> findScheduled(@Param("slug") String slug);
}
