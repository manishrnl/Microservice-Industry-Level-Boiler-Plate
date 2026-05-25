package com.company.platform.discovery;

import org.testng.annotations.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mockStatic;

class DiscoveryServerApplicationTest {

    @Test
    void applicationIsMarkedAsEurekaServer() {
        assertTrue(DiscoveryServerApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(DiscoveryServerApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }

    @Test
    void securityConfigDeclaresFilterChainBean() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(Configuration.class));
        boolean hasFilterChainBean = Arrays.stream(SecurityConfig.class.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Bean.class) && method.getName().equals("securityFilterChain"));

        assertTrue(hasFilterChainBean);
    }

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (var springApplication = mockStatic(SpringApplication.class)) {
            DiscoveryServerApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(DiscoveryServerApplication.class, args));
        }
    }
}
