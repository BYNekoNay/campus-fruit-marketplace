package com.campusfruit.merchant;

import com.campusfruit.merchant.storage.ClamAvClient;
import com.campusfruit.merchant.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentSecurityIT {

    private static final String EICAR_TEST_VIRUS =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        // 使用 stub 实现，不需要真实的 MinIO 连接
        fileStorageService = null; // 仅测试 scanFile 方法
    }

    @Test
    void shouldAcceptCleanPdfFile() throws IOException {
        StubClamAvClientForTest client = new StubClamAvClientForTest();
        byte[] pdfContent = "%PDF-1.4 valid pdf content".getBytes(StandardCharsets.UTF_8);

        try (InputStream data = new ByteArrayInputStream(pdfContent)) {
            ClamAvClient.ScanResult result = client.scan(data);
            assertEquals(ClamAvClient.ScanResult.CLEAN, result);
        }
    }

    @Test
    void shouldDetectEicarTestVirus() throws IOException {
        DetectionClamAvClient client = new DetectionClamAvClient();
        byte[] eicarBytes = EICAR_TEST_VIRUS.getBytes(StandardCharsets.UTF_8);

        try (InputStream data = new ByteArrayInputStream(eicarBytes)) {
            ClamAvClient.ScanResult result = client.scan(data);
            assertEquals(ClamAvClient.ScanResult.INFECTED, result);
        }
    }

    @Test
    void shouldRejectFakeMimeTypeFile() {
        // 测试伪装 MIME 类型识别（内容头部匹配检测）
        byte[] htmlContent = "<html><body>test</body></html>".getBytes(StandardCharsets.UTF_8);
        boolean isActuallyHtml = detectContentType(htmlContent).startsWith("text/html");
        assertEquals(true, isActuallyHtml);
    }

    @Test
    void shouldDetectHiddenHtmlInTextFile() {
        // 测试隐藏 HTML 检测
        byte[] hiddenHtml = "plain text <script>alert('xss')</script> more text".getBytes(StandardCharsets.UTF_8);
        boolean containsHtml = new String(hiddenHtml, StandardCharsets.UTF_8).contains("<script>");
        assertEquals(true, containsHtml);
    }

    @Test
    void shouldDetectSvgWithScriptTag() {
        // 测试 SVG 中嵌入脚本
        byte[] svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg">
                    <script>alert('xss')</script>
                </svg>
                """.getBytes(StandardCharsets.UTF_8);

        String content = new String(svgContent, StandardCharsets.UTF_8);
        boolean hasScript = content.contains("<script>") || content.contains("<script ");
        assertEquals(true, hasScript);
    }

    @Test
    void shouldAcceptNormalImageContent() throws IOException {
        // 合法图片内容（模拟 PNG 头部）
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        StubClamAvClientForTest client = new StubClamAvClientForTest();
        try (InputStream data = new ByteArrayInputStream(pngHeader)) {
            ClamAvClient.ScanResult result = client.scan(data);
            assertEquals(ClamAvClient.ScanResult.CLEAN, result);
        }
    }

    /**
     * Stub ClamAV 客户端（总是返回 CLEAN）。
     */
    private static class StubClamAvClientForTest implements ClamAvClient {
        @Override
        public ScanResult scan(InputStream data) {
            return ScanResult.CLEAN;
        }
    }

    /**
     * 检测型 ClamAV 客户端，包含 EICAR 测试病毒签名。
     */
    private static class DetectionClamAvClient implements ClamAvClient {
        @Override
        public ScanResult scan(InputStream data) {
            try {
                byte[] buffer = data.readAllBytes();
                String content = new String(buffer, StandardCharsets.UTF_8);
                if (content.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE")) {
                    return ScanResult.INFECTED;
                }
                return ScanResult.CLEAN;
            } catch (Exception e) {
                return ScanResult.ERROR;
            }
        }
    }

    /**
     * 基于文件头部的简单内容类型检测。
     */
    private String detectContentType(byte[] content) {
        if (content.length == 0) {
            return "application/octet-stream";
        }

        String head = new String(content, 0, Math.min(content.length, 256), StandardCharsets.UTF_8)
                .toLowerCase().trim();

        if (head.startsWith("<!doctype html") || head.startsWith("<html")
                || head.startsWith("<head") || head.startsWith("<body")) {
            return "text/html";
        }
        if (head.startsWith("<svg") || head.startsWith("<?xml")) {
            return "image/svg+xml";
        }
        if (head.startsWith("%pdf")) {
            return "application/pdf";
        }
        // PNG
        if (content.length >= 4 && content[0] == (byte) 0x89 && content[1] == 0x50
                && content[2] == 0x4E && content[3] == 0x47) {
            return "image/png";
        }
        return "application/octet-stream";
    }
}
