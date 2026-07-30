package com.campusfruit.identity;

import com.campusfruit.identity.dto.LoginRequest;
import com.campusfruit.identity.dto.LoginResponse;
import com.campusfruit.identity.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 完整认证流程集成测试：注册 -> 登录 -> 获取用户信息。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("identity_service")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        long timestamp = System.currentTimeMillis();
        testEmail = "test-" + timestamp + "@campusfruit.com";
        testPassword = "testPass123";
    }

    @Test
    @DisplayName("完整认证流程：注册 -> 登录 -> 获取用户信息")
    void shouldCompleteFullAuthFlow() {
        // Step 1: 注册新用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);
        registerRequest.setNickname("测试用户");

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/auth/register", registerRequest, String.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).contains("\"email\":\"" + testEmail + "\"");

        // Step 2: 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse loginBody = loginResponse.getBody();
        assertThat(loginBody).isNotNull();
        assertThat(loginBody.getAccessToken()).isNotBlank();
        assertThat(loginBody.getTokenType()).isEqualTo("Bearer");
        assertThat(loginBody.getUser()).isNotNull();
        assertThat(loginBody.getUser().getEmail()).isEqualTo(testEmail);

        // Step 3: 使用 access_token 获取当前用户信息
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginBody.getAccessToken());
        HttpEntity<Void> meRequest = new HttpEntity<>(headers);

        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, meRequest, String.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).contains("\"email\":\"" + testEmail + "\"");
    }

    @Test
    @DisplayName("使用错误密码登录应返回 401")
    void shouldRejectInvalidPassword() {
        // 先注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);
        restTemplate.postForEntity("/api/auth/register", registerRequest, String.class);

        // 使用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("wrongPassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("邮箱或密码不正确");
    }

    @Test
    @DisplayName("重复注册应返回 409")
    void shouldRejectDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // 第一次注册
        ResponseEntity<String> first = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 第二次注册相同邮箱
        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).contains("邮箱已被注册");
    }
}
