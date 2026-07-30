package com.campusfruit.merchant.repository;

import com.campusfruit.merchant.entity.Store;
import com.campusfruit.merchant.enums.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByMerchantId(Long merchantId);

    List<Store> findByStatus(StoreStatus status);

    List<Store> findByLatitudeBetweenAndLongitudeBetween(
            Double latMin, Double latMax, Double lonMin, Double lonMax);

    List<Store> findByMerchantIdAndStatus(Long merchantId, StoreStatus status);
}
