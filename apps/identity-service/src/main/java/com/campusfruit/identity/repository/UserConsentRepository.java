package com.campusfruit.identity.repository;

import com.campusfruit.identity.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    Optional<UserConsent> findByUserIdAndConsentType(Long userId, String consentType);

    List<UserConsent> findByUserIdAndDeletedFalse(Long userId);
}
