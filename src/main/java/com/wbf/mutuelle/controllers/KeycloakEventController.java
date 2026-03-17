package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.services.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mutuelle/keycloak")
@RequiredArgsConstructor
@Slf4j
public class KeycloakEventController {

    private final KeycloakUserService keycloakUserService;

    @Value("${keycloak.event-secret:}")
    private String eventSecret;

    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(@RequestHeader(value = "X-Keycloak-Event-Secret", required = false) String secret,
                                              @RequestBody Map<String, Object> payload) {

        if (eventSecret != null && !eventSecret.isEmpty()) {
            if (secret == null || !secret.equals(eventSecret)) {
                log.warn("Requête webhook Keycloak refusée, secret invalide");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid secret");
            }
        }

        try {
            // Tentative d'extraction des données utilisateur
            String userId = null;
            String email = null;

            // Cas: payload contient directement userId / email
            if (payload.get("userId") != null) {
                userId = String.valueOf(payload.get("userId"));
            }
            if (payload.get("email") != null) {
                email = String.valueOf(payload.get("email"));
            }

            // Cas: payload.user existe
            if ((userId == null || email == null) && payload.get("user") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = (Map<String, Object>) payload.get("user");
                if (user.get("id") != null) userId = String.valueOf(user.get("id"));
                if (user.get("email") != null) email = String.valueOf(user.get("email"));
            }

            // Cas: payload содержит 'representation' (admin events)
            if ((userId == null || email == null) && payload.get("representation") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rep = (Map<String, Object>) payload.get("representation");
                if (rep.get("id") != null) userId = String.valueOf(rep.get("id"));
                if (rep.get("email") != null) email = String.valueOf(rep.get("email"));
                if (rep.get("username") != null && email == null) email = String.valueOf(rep.get("username"));
            }

            if (email == null && payload.get("username") != null) {
                email = String.valueOf(payload.get("username"));
            }

            if (email == null) {
                log.warn("Événement Keycloak reçu sans email — payload: {}", payload);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No email in payload");
            }

            // Appel de la synchronisation (upsert)
            keycloakUserService.syncUserWithDatabase(email, userId != null ? userId : "");

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Erreur traitement webhook Keycloak", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
