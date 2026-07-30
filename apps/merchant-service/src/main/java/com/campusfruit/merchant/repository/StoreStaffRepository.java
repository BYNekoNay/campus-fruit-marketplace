package com.campusfruit.merchant.repository;

import com.campusfruit.merchant.entity.StoreStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    List<StoreStaff> findByStoreId(Long storeId);

    List<StoreStaff> findByUserId(Long userId);

    Optional<StoreStaff> findByStoreIdAndUserId(Long storeId, Long userId);

    @Modifying
    @Transactional
    void deleteByStoreIdAndUserId(Long storeId, Long userId);
}
