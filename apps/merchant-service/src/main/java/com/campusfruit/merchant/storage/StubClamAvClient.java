package com.campusfruit.merchant.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * ClamAV 客户端 Stub 实现。
 * 首版直接返回 CLEAN，后续版本接入真实 ClamAV。
 */
@Component
public class StubClamAvClient implements ClamAvClient {

    private static final Logger log = LoggerFactory.getLogger(StubClamAvClient.class);

    @Override
    public ScanResult scan(InputStream data) {
        log.debug("ClamAV stub: returning CLEAN");
        return ScanResult.CLEAN;
    }
}
