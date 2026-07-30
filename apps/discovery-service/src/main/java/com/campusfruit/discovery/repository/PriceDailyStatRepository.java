package com.campusfruit.discovery.repository;

import com.campusfruit.discovery.entity.PriceDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceDailyStatRepository extends JpaRepository<PriceDailyStat, Long> {

    List<PriceDailyStat> findByCanonicalFruitIdAndStatDateBetween(Long canonicalFruitId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate);
}
