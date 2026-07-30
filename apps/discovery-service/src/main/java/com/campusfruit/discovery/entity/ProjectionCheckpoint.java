package com.campusfruit.discovery.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projection_checkpoints")
public class ProjectionCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service", nullable = false, length = 50)
    private String sourceService;

    @Column(name = "last_event_id", length = 100)
    private String lastEventId;

    @Column(name = "last_source_sequence")
    private Long lastSourceSequence = 0L;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public ProjectionCheckpoint() {
    }

    public ProjectionCheckpoint(String sourceService) {
        this.sourceService = sourceService;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getLastEventId() { return lastEventId; }
    public void setLastEventId(String lastEventId) { this.lastEventId = lastEventId; }

    public Long getLastSourceSequence() { return lastSourceSequence; }
    public void setLastSourceSequence(Long lastSourceSequence) { this.lastSourceSequence = lastSourceSequence; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
