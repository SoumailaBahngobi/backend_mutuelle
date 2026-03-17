package com.wbf.mutuelle.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KkiapayConfig {

    @Value("${kkiapay.base-url}")
    private String baseUrl;

    @Value("${kkiapay.api-key}")
    private String apiKey;

    @Value("${kkiapay.secret-key}")
    private String secretKey;

    @Value("${kkiapay.private-key}")
    private String privateKey;

    @Value("${kkiapay.public-key}")
    private String publicKey;

    @Value("${kkiapay.callback-url}")
    private String callbackUrl;

    @Value("${kkiapay.success-url}")
    private String successUrl;

    @Value("${kkiapay.error-url}")
    private String errorUrl;

    @Value("${kkiapay.sandbox:true}")
    private boolean sandbox;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Getters
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getSecretKey() { return secretKey; }
    public String getPrivateKey() { return privateKey; }
    public String getPublicKey() { return publicKey; }
    public String getCallbackUrl() { return callbackUrl; }
    public String getSuccessUrl() { return successUrl; }
    public String getErrorUrl() { return errorUrl; }
    public boolean isSandbox() { return sandbox; }
}