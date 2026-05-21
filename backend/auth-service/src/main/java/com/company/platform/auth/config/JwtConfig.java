package com.company.platform.auth.config;

import com.company.platform.auth.security.RsaKeyService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {
    @Bean
    JwtDecoder jwtDecoder(RsaKeyService rsaKeyService) throws Exception {
        RSAKey key = rsaKeyService.rsaKey();
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) key.toPublicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeyService rsaKeyService) {
        RSAKey key = rsaKeyService.rsaKey();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
