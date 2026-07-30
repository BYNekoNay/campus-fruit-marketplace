package com.campusfruit.discovery.repository;

import com.campusfruit.discovery.entity.ProjectionChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectionChangeLogRepository extends JpaRepository<ProjectionChangeLog, Long> {

    List<ProjectionChangeLog> findBySourceServiceAndSequenceGreaterThanOrderBySequenceAsc(
            String sourceService, Long sequence);

    @Query("SELECT MAX(c.sequence) FROM ProjectionChangeLog c WHERE c.sourceService = :sourceService")
    Long findMaxSequenceBySourceService(@Param("sourceService") String sourceService);

    List<ProjectionChangeLog> findBySourceServiceOrderBySequenceAsc(String sourceService);
}
