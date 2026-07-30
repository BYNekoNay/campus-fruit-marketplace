package com.campusfruit.merchant.storage;

import com.campusfruit.merchant.entity.Merchant;
import com.campusfruit.merchant.entity.MerchantDocument;
import com.campusfruit.merchant.enums.DocumentScanStatus;
import com.campusfruit.merchant.enums.DocumentType;
import com.campusfruit.merchant.repository.MerchantDocumentRepository;
import com.campusfruit.merchant.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final FileStorageService fileStorageService;
    private final FileStorageConfig config;
    private final MerchantDocumentRepository documentRepository;
    private final MerchantRepository merchantRepository;

    public DocumentService(FileStorageService fileStorageService,
                           FileStorageConfig config,
                           MerchantDocumentRepository documentRepository,
                           MerchantRepository merchantRepository) {
        this.fileStorageService = fileStorageService;
        this.config = config;
        this.documentRepository = documentRepository;
        this.merchantRepository = merchantRepository;
    }

    /**
     * 上传文档到隔离桶，执行扫描，创建数据库记录。
     */
    @Transactional
    public MerchantDocument uploadDocument(Long merchantId, DocumentType docType, MultipartFile file)
            throws Exception {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("商家不存在: " + merchantId));

        String originalFilename = file.getOriginalFilename();
        String objectPath = merchantId + "/" + docType.name().toLowerCase() + "/"
                + UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename : "unknown");

        // 上传到隔离桶
        try (InputStream data = file.getInputStream()) {
            fileStorageService.uploadFile(config.getQuarantineBucket(), objectPath, data,
                    file.getContentType(), file.getSize());

            // ClamAV 扫描
            ClamAvClient.ScanResult scanResult = fileStorageService.scanFile(file.getInputStream());

            // 创建数据库记录
            MerchantDocument doc = new MerchantDocument();
            doc.setMerchant(merchant);
            doc.setDocType(docType);
            doc.setFileName(originalFilename);
            doc.setFilePath(objectPath);
            doc.setFileSize(file.getSize());
            doc.setMimeType(file.getContentType());

            if (scanResult == ClamAvClient.ScanResult.CLEAN) {
                doc.setScanStatus(DocumentScanStatus.CLEAN);
                doc.setScanResult("CLEAN (stub)");
            } else if (scanResult == ClamAvClient.ScanResult.INFECTED) {
                doc.setScanStatus(DocumentScanStatus.INFECTED);
                doc.setScanResult("INFECTED (stub)");
            } else {
                doc.setScanStatus(DocumentScanStatus.ERROR);
                doc.setScanResult("SCAN_ERROR (stub)");
            }

            return documentRepository.save(doc);
        }
    }

    /**
     * 审核通过文档：从隔离桶迁移到审核桶。
     */
    @Transactional
    public MerchantDocument approveDocument(Long documentId) throws Exception {
        MerchantDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        if (doc.getScanStatus() != DocumentScanStatus.CLEAN) {
            throw new IllegalStateException("只能批准扫描通过的文档，当前状态: " + doc.getScanStatus());
        }

        // 从隔离桶迁移到审核桶
        fileStorageService.moveFile(config.getQuarantineBucket(), doc.getFilePath(),
                config.getApprovedBucket(), doc.getFilePath());
        doc.setScanStatus(DocumentScanStatus.CLEAN);

        return documentRepository.save(doc);
    }

    /**
     * 删除文档及其存储文件。
     */
    @Transactional
    public void deleteDocument(Long documentId) throws Exception {
        MerchantDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        // 尝试从隔离桶和审核桶中删除
        try {
            fileStorageService.deleteFile(config.getQuarantineBucket(), doc.getFilePath());
        } catch (Exception e) {
            log.debug("File not found in quarantine bucket: {}", doc.getFilePath());
        }
        try {
            fileStorageService.deleteFile(config.getApprovedBucket(), doc.getFilePath());
        } catch (Exception e) {
            log.debug("File not found in approved bucket: {}", doc.getFilePath());
        }

        documentRepository.delete(doc);
        log.info("Deleted document {} and its storage files", documentId);
    }

    /**
     * 获取文档的预签名访问 URL。
     */
    public String getDocumentPresignedUrl(Long documentId, Duration expiry) throws Exception {
        MerchantDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        String bucket = doc.getScanStatus() == DocumentScanStatus.CLEAN
                ? config.getApprovedBucket()
                : config.getQuarantineBucket();

        return fileStorageService.getPresignedUrl(bucket, doc.getFilePath(), expiry);
    }
}
