package com.apnabazaar.repository;
import com.apnabazaar.entity.Offering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OfferingRepository extends JpaRepository<Offering, UUID> {
    List<Offering> findByProviderIdAndIsAvailable(UUID providerId, boolean available);
}
