package com.wbf.mutuelle.configuration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    @Bean
    public Keycloak keycloakAdmin() {
        log.info("Initialisation du client Keycloak Admin sur: {}", serverUrl);

        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm("master")  // Toujours "master" pour l'administration
                    .username(adminUsername)
                    .password(adminPassword)
                    .clientId("admin-cli")  // Client par défaut
                    .build();

            log.info("Client Keycloak Admin initialisé (connexion testée à la première utilisation)");
            return keycloak;
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du client Keycloak Admin", e);
            throw e;
        }
    }
}