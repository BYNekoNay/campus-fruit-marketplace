package com.campusfruit.discovery.repository;

import com.campusfruit.discovery.entity.ProjectionCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectionCheckpointRepository extends JpaRepository<ProjectionCheckpoint, Long> {

    Optional<ProjectionCheckpoint> findBySourceService(String sourceService);
}
