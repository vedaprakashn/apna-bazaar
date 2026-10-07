package com.apnabazaar.repository;
import com.apnabazaar.entity.OfferingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OfferingScheduleRepository extends JpaRepository<OfferingSchedule, UUID> {
    List<OfferingSchedule> findByOfferingIdAndIsActive(UUID offeringId, boolean active);
}
