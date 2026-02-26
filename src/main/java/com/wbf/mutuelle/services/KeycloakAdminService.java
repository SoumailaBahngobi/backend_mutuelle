package com.wbf.mutuelle.services;

import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KeycloakAdminService {

    @Autowired
    private Keycloak keycloakAdmin;

    @Value("${keycloak.realm:mutuelle-realm}")
    private String realm;

    /**
     * Récupère un utilisateur par son email
     */
    public UserRepresentation getUserByEmail(String email) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.searchByEmail(email, true);

            if (users == null || users.isEmpty()) {
                return null;
            }

            return users.get(0);
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de l'utilisateur par email: {}", email, e);
            return null;
        }
    }

    /**
     * Récupère un utilisateur par son username
     */
    public UserRepresentation getUserByUsername(String username) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.searchByUsername(username, true);

            if (users == null || users.isEmpty()) {
                return null;
            }

            return users.get(0);
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de l'utilisateur par username: {}", username, e);
            return null;
        }
    }

    /**
     * Met à jour le mot de passe d'un utilisateur Keycloak
     */
    public boolean updateUserPassword(String userId, String newPassword) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            userResource.resetPassword(credential);

            log.info("Mot de passe mis à jour pour l'utilisateur ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du mot de passe pour l'utilisateur ID: {}", userId, e);
            return false;
        }
    }

    /**
     * Envoie un email de réinitialisation de mot de passe via Keycloak
     */
    public boolean sendResetPasswordEmail(String userId) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            userResource.executeActionsEmail(List.of("UPDATE_PASSWORD"));

            log.info("Email de réinitialisation envoyé pour l'utilisateur ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation", e);
            return false;
        }
    }

    /**
     * Crée un nouvel utilisateur dans Keycloak
     */
    public String createUser(String username, String email, String password) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setEnabled(true);
            user.setEmailVerified(true);

            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();

            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

                // Définir le mot de passe
                updateUserPassword(userId, password);

                log.info("Utilisateur créé dans Keycloak avec ID: {}", userId);
                return userId;
            }

            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur", e);
            return null;
        }
    }
}