package com.apnabazaar.repository;
import com.apnabazaar.entity.SearchTrendDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SearchTrendDailyRepository extends JpaRepository<SearchTrendDaily, UUID> {}
