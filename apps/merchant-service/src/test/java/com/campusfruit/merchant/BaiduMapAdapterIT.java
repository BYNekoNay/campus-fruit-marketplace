package com.campusfruit.merchant;

import com.campusfruit.merchant.integration.baidu.BaiduGeocodingService;
import com.campusfruit.merchant.integration.baidu.BaiduMapConfig;
import com.campusfruit.merchant.integration.baidu.model.BaiduGeoResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BaiduMapAdapterIT {

    private static WireMockServer wireMockServer;

    @Autowired
    private RestClient baiduMapRestClient;

    private BaiduGeocodingService geocodingService;
    private BaiduMapConfig config;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.baidu.map.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("app.baidu.map.server-ak", () -> "test-ak");
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        config = new BaiduMapConfig();
        config.setBaseUrl("http://localhost:" + wireMockServer.port());
        config.setServerAk("test-ak");
        geocodingService = new BaiduGeocodingService(baiduMapRestClient, config);
        WireMock.reset();
    }

    @Test
    void shouldGeocodeSuccessfully() throws Exception {
        stubFor(get(urlPathEqualTo("/geocoding/v3"))
                .withQueryParam("address", equalTo("北京市海淀区"))
                .withQueryParam("ak", equalTo("test-ak"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "status": 0,
                                    "result": {
                                        "location": { "lat": 39.960000, "lng": 116.300000 },
                                        "precise": 1,
                                        "confidence": 80,
                                        "level": "区县",
                                        "formatted_address": "北京市海淀区"
                                    }
                                }
                                """)));

        CompletableFuture<Optional<BaiduGeoResult>> future = geocodingService.geocode("北京市海淀区");
        Optional<BaiduGeoResult> result = future.get(10, TimeUnit.SECONDS);

        assertTrue(result.isPresent());
        assertEquals(0, result.get().getStatus());
        assertEquals(39.960000, result.get().getLat(), 0.0001);
        assertEquals(116.300000, result.get().getLng(), 0.0001);
        assertEquals("北京市海淀区", result.get().getFormattedAddress());
    }

    @Test
    void shouldReturnEmptyOnApiError() throws Exception {
        stubFor(get(urlPathEqualTo("/geocoding/v3"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 500, \"message\": \"Internal error\"}")));

        CompletableFuture<Optional<BaiduGeoResult>> future = geocodingService.geocode("北京市海淀区");
        Optional<BaiduGeoResult> result = future.get(10, TimeUnit.SECONDS);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyOnTimeout() throws Exception {
        stubFor(get(urlPathEqualTo("/geocoding/v3"))
                .willReturn(aResponse()
                        .withFixedDelay(6000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": 0, \"result\": { \"location\": { \"lat\": 39.96, \"lng\": 116.30 }}}")));

        CompletableFuture<Optional<BaiduGeoResult>> future = geocodingService.geocode("北京市海淀区");
        Optional<BaiduGeoResult> result = future.get(10, TimeUnit.SECONDS);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyOnNonZeroStatus() throws Exception {
        stubFor(get(urlPathEqualTo("/geocoding/v3"))
                .withQueryParam("address", equalTo("invalid-address"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "status": 1,
                                    "message": "AK invalid or expired"
                                }
                                """)));

        CompletableFuture<Optional<BaiduGeoResult>> future = geocodingService.geocode("invalid-address");
        Optional<BaiduGeoResult> result = future.get(10, TimeUnit.SECONDS);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldReverseGeocodeSuccessfully() throws Exception {
        stubFor(get(urlPathEqualTo("/reverse_geocoding/v3"))
                .withQueryParam("location", equalTo("39.96,116.30"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "status": 0,
                                    "result": {
                                        "location": { "lat": 39.960000, "lng": 116.300000 },
                                        "formatted_address": "北京市海淀区中关村",
                                        "business": "中关村"
                                    }
                                }
                                """)));

        CompletableFuture<Optional<BaiduGeoResult>> future = geocodingService.reverseGeocode(39.96, 116.30);
        Optional<BaiduGeoResult> result = future.get(10, TimeUnit.SECONDS);

        assertTrue(result.isPresent());
        assertEquals("北京市海淀区中关村", result.get().getFormattedAddress());
    }
}
