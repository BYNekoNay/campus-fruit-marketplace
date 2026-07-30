package com.campusfruit.identity.repository;

import com.campusfruit.identity.entity.UserAppeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAppealRepository extends JpaRepository<UserAppeal, Long> {

    List<UserAppeal> findByUserId(Long userId);

    List<UserAppeal> findByStatus(String status);
}
