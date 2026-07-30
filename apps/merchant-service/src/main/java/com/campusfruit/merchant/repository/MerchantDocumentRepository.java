package com.campusfruit.merchant.repository;

import com.campusfruit.merchant.entity.MerchantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantDocumentRepository extends JpaRepository<MerchantDocument, Long> {

    List<MerchantDocument> findByMerchantId(Long merchantId);
}
