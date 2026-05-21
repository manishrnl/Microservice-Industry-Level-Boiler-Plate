package com.company.platform.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProviderProperties {
    private String redirectBaseUrl;
    private String frontendRedirectUrl;
    private Map<String, Provider> providers = new HashMap<>();

    @Data
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String scopes;
    }
}
