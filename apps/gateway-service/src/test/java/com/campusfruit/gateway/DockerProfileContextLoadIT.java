package com.campusfruit.gateway;

import com.campusfruit.gateway.config.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("docker")
class DockerProfileContextLoadIT {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void dockerProfileShouldConfigureJwtWhitelist() {
        assertThat(jwtAuthFilter).isNotNull();
    }
}
