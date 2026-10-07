package com.apnabazaar.repository;
import com.apnabazaar.entity.SearchResultImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SearchResultImpressionRepository extends JpaRepository<SearchResultImpression, UUID> {}
