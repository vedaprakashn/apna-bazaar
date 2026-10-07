package com.apnabazaar.repository;
import com.apnabazaar.entity.ProviderClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProviderClickEventRepository extends JpaRepository<ProviderClickEvent, UUID> {}
