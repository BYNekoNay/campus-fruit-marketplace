package com.campusfruit.merchant.storage;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final MinioClient minioClient;
    private final FileStorageConfig config;
    private final ClamAvClient clamAvClient;

    public FileStorageService(FileStorageConfig config, ClamAvClient clamAvClient) {
        this.config = config;
        this.clamAvClient = clamAvClient;
        this.minioClient = MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
        ensureBuckets();
    }

    private void ensureBuckets() {
        try {
            for (String bucket : new String[]{config.getQuarantineBucket(), config.getApprovedBucket()}) {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket: {}", bucket);
                }
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO buckets exist", e);
        }
    }

    /**
     * 上传文件到指定桶。
     */
    public void uploadFile(String bucket, String objectPath, InputStream data, String contentType, long size)
            throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .stream(data, size, -1)
                        .contentType(contentType)
                        .build()
        );
        log.info("Uploaded file to {}: {}", bucket, objectPath);
    }

    /**
     * 生成预签名 URL（有效期默认 10 分钟）。
     */
    public String getPresignedUrl(String bucket, String objectPath, Duration expiry)
            throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectPath)
                        .expiry((int) expiry.getSeconds())
                        .build()
        );
    }

    /**
     * 删除文件。
     */
    public void deleteFile(String bucket, String objectPath) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()
        );
        log.info("Deleted file from {}: {}", bucket, objectPath);
    }

    /**
     * 从隔离桶迁移文件到审核桶（复制后删除源）。
     */
    public void moveFile(String sourceBucket, String sourcePath,
                          String targetBucket, String targetPath) throws Exception {
        // Copy to target
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .source(CopySource.builder()
                                .bucket(sourceBucket)
                                .object(sourcePath)
                                .build())
                        .bucket(targetBucket)
                        .object(targetPath)
                        .build()
        );
        // Delete from source
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(sourceBucket)
                        .object(sourcePath)
                        .build()
        );
        log.info("Moved file from {}/{} to {}/{}", sourceBucket, sourcePath, targetBucket, targetPath);
    }

    /**
     * ClamAV 扫描（stub 实现，总是返回 CLEAN）。
     */
    public ClamAvClient.ScanResult scanFile(InputStream data) {
        return clamAvClient.scan(data);
    }
}
