package com.campusfruit.merchant.repository;

import com.campusfruit.merchant.entity.Merchant;
import com.campusfruit.merchant.enums.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByOwnerUserId(Long ownerUserId);

    Page<Merchant> findByStatus(MerchantStatus status, Pageable pageable);

    boolean existsByOwnerUserIdAndStatus(Long ownerUserId, MerchantStatus status);

    Page<Merchant> findByIdGreaterThan(Long id, Pageable pageable);

    Page<Merchant> findAllByOrderByIdAsc(Pageable pageable);
}
