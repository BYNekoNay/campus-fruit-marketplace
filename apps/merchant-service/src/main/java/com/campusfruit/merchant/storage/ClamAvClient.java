package com.campusfruit.merchant.storage;

/**
 * ClamAV 病毒扫描客户端接口。
 * 首版为 stub 实现，总是返回 CLEAN。
 */
public interface ClamAvClient {

    enum ScanResult {
        CLEAN,
        INFECTED,
        ERROR
    }

    /**
     * 扫描输入流中的数据，返回扫描结果。
     */
    ScanResult scan(java.io.InputStream data);
}
