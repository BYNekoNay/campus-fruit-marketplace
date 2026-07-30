package com.campusfruit.discovery.rebuild;

import com.campusfruit.discovery.dto.RebuildStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/discovery/rebuild")
public class ProjectionRebuildController {

    private static final Logger log = LoggerFactory.getLogger(ProjectionRebuildController.class);

    private final ProjectionRebuildService rebuildService;
    private final String internalApiKey;

    public ProjectionRebuildController(ProjectionRebuildService rebuildService,
                                        @Value("${discovery.internal-api-key:}") String internalApiKey) {
        this.rebuildService = rebuildService;
        this.internalApiKey = internalApiKey;
    }

    /**
     * 重建所有源服务的投影。
     */
    @PostMapping
    public ResponseEntity<?> rebuildAll(@RequestHeader(value = "X-Internal-API-Key", required = false) String apiKey) {
        if (!validateApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Internal API Key");
        }

        RebuildStatusDTO status = rebuildService.getStatus();
        if (status.isInProgress()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Rebuild already in progress for: " + status.getCurrentSource());
        }

        rebuildService.rebuildAll();
        return ResponseEntity.accepted().body("Rebuild triggered for all source services");
    }

    /**
     * 重建指定源服务的投影。
     */
    @PostMapping("/{sourceService}")
    public ResponseEntity<?> rebuildSource(@PathVariable String sourceService,
                                            @RequestHeader(value = "X-Internal-API-Key", required = false) String apiKey) {
        if (!validateApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Internal API Key");
        }

        if (!"merchant-service".equals(sourceService) && !"offer-service".equals(sourceService)) {
            return ResponseEntity.badRequest()
                    .body("Unknown sourceService: " + sourceService + ". Supported: merchant-service, offer-service");
        }

        RebuildStatusDTO status = rebuildService.getStatus();
        if (status.isInProgress()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Rebuild already in progress for: " + status.getCurrentSource());
        }

        rebuildService.rebuildFromSource(sourceService);
        return ResponseEntity.accepted().body("Rebuild triggered for: " + sourceService);
    }

    /**
     * 查询重建状态。
     */
    @GetMapping("/status")
    public ResponseEntity<RebuildStatusDTO> getStatus(
            @RequestHeader(value = "X-Internal-API-Key", required = false) String apiKey) {
        if (!validateApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(rebuildService.getStatus());
    }

    private boolean validateApiKey(String apiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.warn("Internal API Key not configured, refusing request");
            return false;
        }
        return internalApiKey.equals(apiKey);
    }
}
