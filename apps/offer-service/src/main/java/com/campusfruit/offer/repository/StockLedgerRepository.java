package com.campusfruit.offer.repository;

import com.campusfruit.offer.entity.StockLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    List<StockLedger> findByOfferIdOrderByCreatedAtDesc(Long offerId);

    List<StockLedger> findByReferenceId(String referenceId);
}
