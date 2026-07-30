package com.campusfruit.discovery.dto;

import java.time.Instant;

public class RebuildStatusDTO {

    private String status;
    private String currentSource;
    private String message;
    private Long recordsWritten;
    private Long deltaApplied;
    private String snapshotToken;
    private Long highWatermark;
    private boolean inProgress;
    private boolean sequenceGapDetected;
    private Instant startedAt;
    private Instant lastUpdateAt;

    public static RebuildStatusDTO pending() {
        RebuildStatusDTO dto = new RebuildStatusDTO();
        dto.status = "PENDING";
        dto.inProgress = false;
        return dto;
    }

    public static RebuildStatusDTO inProgress(String source) {
        RebuildStatusDTO dto = new RebuildStatusDTO();
        dto.status = "IN_PROGRESS";
        dto.currentSource = source;
        dto.inProgress = true;
        dto.startedAt = Instant.now();
        return dto;
    }

    public static RebuildStatusDTO completed(String source, long records, long delta) {
        RebuildStatusDTO dto = new RebuildStatusDTO();
        dto.status = "COMPLETED";
        dto.currentSource = source;
        dto.recordsWritten = records;
        dto.deltaApplied = delta;
        dto.inProgress = false;
        dto.lastUpdateAt = Instant.now();
        return dto;
    }

    public static RebuildStatusDTO failed(String message) {
        RebuildStatusDTO dto = new RebuildStatusDTO();
        dto.status = "FAILED";
        dto.message = message;
        dto.inProgress = false;
        dto.lastUpdateAt = Instant.now();
        return dto;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentSource() { return currentSource; }
    public void setCurrentSource(String currentSource) { this.currentSource = currentSource; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getRecordsWritten() { return recordsWritten; }
    public void setRecordsWritten(Long recordsWritten) { this.recordsWritten = recordsWritten; }

    public Long getDeltaApplied() { return deltaApplied; }
    public void setDeltaApplied(Long deltaApplied) { this.deltaApplied = deltaApplied; }

    public String getSnapshotToken() { return snapshotToken; }
    public void setSnapshotToken(String snapshotToken) { this.snapshotToken = snapshotToken; }

    public Long getHighWatermark() { return highWatermark; }
    public void setHighWatermark(Long highWatermark) { this.highWatermark = highWatermark; }

    public boolean isInProgress() { return inProgress; }
    public void setInProgress(boolean inProgress) { this.inProgress = inProgress; }

    public boolean isSequenceGapDetected() { return sequenceGapDetected; }
    public void setSequenceGapDetected(boolean sequenceGapDetected) { this.sequenceGapDetected = sequenceGapDetected; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getLastUpdateAt() { return lastUpdateAt; }
    public void setLastUpdateAt(Instant lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }
}
