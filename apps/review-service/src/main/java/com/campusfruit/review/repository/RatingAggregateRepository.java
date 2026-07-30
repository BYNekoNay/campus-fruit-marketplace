package com.campusfruit.review.repository;

import com.campusfruit.review.entity.RatingAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingAggregateRepository extends JpaRepository<RatingAggregate, Long> {

    Optional<RatingAggregate> findByStoreId(Long storeId);
}
