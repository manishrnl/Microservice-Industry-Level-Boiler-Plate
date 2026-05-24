package com.company.platform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class GatewayJwtConfig {
    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(@Value("${gateway.jwt.jwks-url:${AUTH_JWKS_URL:http://auth-service:8081/api/v1/auth/.well-known/jwks.json}}") String jwksUrl) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUrl).build();
    }
}
