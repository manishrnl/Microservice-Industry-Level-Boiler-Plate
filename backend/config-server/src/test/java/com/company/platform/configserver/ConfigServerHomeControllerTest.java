package com.company.platform.configserver;

import org.testng.annotations.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mockStatic;

class ConfigServerHomeControllerTest {

    @Test
    void homeReturnsUsefulConfigServerLinks() {
        Map<String, Object> home = new ConfigServerHomeController().home();

        assertEquals(home.get("service"), "config-server");
        assertEquals(home.get("status"), "running");
        assertEquals(home.get("health"), "/actuator/health");
        assertEquals(home.get("metrics"), "/actuator/prometheus");
        @SuppressWarnings("unchecked")
        List<String> examples = (List<String>) home.get("examples");
        assertTrue(String.valueOf(examples).contains(String.valueOf("/auth-service/default")));
        assertTrue(String.valueOf(examples).contains(String.valueOf("/user-service/default")));
    }

    @Test
    void applicationIsMarkedAsSpringConfigServer() {
        assertTrue(ConfigServerApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(ConfigServerApplication.class.isAnnotationPresent(EnableConfigServer.class));
    }

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (var springApplication = mockStatic(SpringApplication.class)) {
            ConfigServerApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ConfigServerApplication.class, args));
        }
    }
}
