package com.apnabazaar.repository;
import com.apnabazaar.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {
    Optional<Community> findBySlug(String slug);
}
