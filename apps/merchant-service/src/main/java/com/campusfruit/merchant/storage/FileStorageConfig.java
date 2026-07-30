package com.campusfruit.merchant.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("app.storage")
public class FileStorageConfig {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String quarantineBucket = "merchant-quarantine";
    private String approvedBucket = "merchant-documents";
    private boolean useSsl = false;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getQuarantineBucket() { return quarantineBucket; }
    public void setQuarantineBucket(String quarantineBucket) { this.quarantineBucket = quarantineBucket; }

    public String getApprovedBucket() { return approvedBucket; }
    public void setApprovedBucket(String approvedBucket) { this.approvedBucket = approvedBucket; }

    public boolean isUseSsl() { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
}
