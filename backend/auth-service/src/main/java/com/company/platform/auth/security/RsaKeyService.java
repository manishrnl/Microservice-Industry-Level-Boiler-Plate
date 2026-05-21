package com.company.platform.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;

@Service
public class RsaKeyService {
    private final RSAKey rsaKey;
    private final Map<String, Object> publicJwks;

    public RsaKeyService(@Value("${security.jwt.key-id}") String keyId) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            this.rsaKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(keyId + "-" + UUID.randomUUID())
                    .build();
            this.publicJwks = new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create RSA key", ex);
        }
    }

    public RSAKey rsaKey() {
        return rsaKey;
    }

    public Map<String, Object> jwks() {
        return publicJwks;
    }
}
