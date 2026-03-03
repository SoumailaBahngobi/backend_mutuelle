package com.wbf.mutuelle.services;

import com.wbf.mutuelle.dto.RegisterRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.repositories.MemberRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloakAdmin;
    private final MemberRepository memberRepository;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;  // ← AJOUTÉ

    @Value("${keycloak.realm}")
    private String realm;

    // ==================== INSCRIPTION ====================

    @Transactional
    public Member registerUser(RegisterRequest request) {
        try {
            // 1. Vérifier si l'utilisateur existe déjà dans Keycloak
            if (userExists(request.getEmail())) {
                throw new RuntimeException("Un utilisateur avec cet email existe déjà");
            }

            // 2. Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(request);

            // 3. Créer l'utilisateur dans la base locale
            Member member = new Member();
            member.setKeycloakId(userId);
            member.setEmail(request.getEmail());
            member.setName(request.getName());
            member.setFirstName(request.getFirstName());
            member.setPhone(request.getPhone());
            member.setNpi(request.getNpi());

            // Gérer le rôle : si null ou vide, mettre MEMBER par défaut
            Role role = Role.MEMBER;
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                try {
                    role = Role.valueOf(request.getRole().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Rôle invalide: {}, utilisation de MEMBER par défaut", request.getRole());
                }
            }
            member.setRole(role);

            member.setIsRegular(false);
            member.setHasPreviousDebt(false);
            member.setSubscriptionStatus("PENDING");

            Member savedMember = memberRepository.save(member);
            log.info("Utilisateur créé avec succès dans Keycloak et DB: {}, rôle: {}",
                    request.getEmail(), role);

            return savedMember;

        } catch (jakarta.ws.rs.NotAuthorizedException nae) {
            log.error("Accès refusé lors de l'inscription Keycloak - vérifiez les identifiants/admin roles", nae);
            throw new RuntimeException("Erreur lors de l'inscription: accès refusé (401). Vérifiez la configuration Keycloak.");
        } catch (Exception e) {
            log.error("Erreur lors de l'inscription", e);
            throw new RuntimeException("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    private String createUserInKeycloak(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        RealmResource realmResource = keycloakAdmin.realm(realm);
        UsersResource usersResource = realmResource.users();

        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Erreur Keycloak: " + response.getStatus());
        }

        // Récupérer l'ID de l'utilisateur créé
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // Définir le mot de passe
        setUserPassword(userId, request.getPassword());

        // Assigner le rôle dans Keycloak (toujours MEMBER pour l'authentification)
        assignRoleToUser(userId, "MEMBER");

        return userId;
    }

    private void setUserPassword(String userId, String password) {
        RealmResource realmResource = keycloakAdmin.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        userResource.resetPassword(credential);
        log.debug("Mot de passe défini pour l'utilisateur: {}", userId);
    }

    private void assignRoleToUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(role));
            log.debug("Rôle {} assigné à l'utilisateur: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Erreur lors de l'assignation du rôle {} à l'utilisateur {}", roleName, userId, e);
        }
    }

    // ==================== MOT DE PASSE OUBLIÉ ====================

    public boolean sendResetPasswordEmail(String email) {
        try {
            UserRepresentation user = getUserByEmail(email);
            if (user == null) {
                log.warn("Utilisateur non trouvé: {}", email);
                return false;
            }

            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(user.getId());

            // Envoyer l'email de réinitialisation Keycloak
            userResource.executeActionsEmail(List.of("UPDATE_PASSWORD"));

            log.info("Email de réinitialisation envoyé à: {}", email);
            return true;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email", e);
            return false;
        }
    }

    // ==================== UTILITAIRES ====================

    public UserRepresentation getUserByEmail(String email) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            List<UserRepresentation> users = realmResource.users().searchByEmail(email, true);
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            log.error("Erreur recherche utilisateur par email: {}", email, e);
            return null;
        }
    }

    public boolean userExists(String email) {
        return getUserByEmail(email) != null;
    }

    public UserRepresentation getUserById(String userId) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            return realmResource.users().get(userId).toRepresentation();
        } catch (Exception e) {
            log.error("Erreur recherche utilisateur par ID: {}", userId, e);
            return null;
        }
    }

    // ==================== SYNC AVEC BASE LOCALE ====================

    @Transactional
    public Member syncUserWithDatabase(String email, String keycloakId) {
        String finalKeycloakId = keycloakId;

        UserRepresentation kcUser = null;
        if (finalKeycloakId != null && !finalKeycloakId.isEmpty()) {
            kcUser = getUserById(finalKeycloakId);
        }

        if (kcUser == null) {
            kcUser = getUserByEmail(email);
            if (kcUser != null && (finalKeycloakId == null || finalKeycloakId.isEmpty())) {
                finalKeycloakId = kcUser.getId();
            }
        }

        final UserRepresentation finalKcUser = kcUser;
        final String finalKcId = finalKeycloakId;

        return memberRepository.findByEmail(email)
                .map(member -> {
                    if (finalKcUser != null) {
                        member.setKeycloakId(finalKcUser.getId());
                        member.setFirstName(finalKcUser.getFirstName());
                        member.setName(finalKcUser.getLastName());
                        if (finalKcUser.getEmail() != null) member.setEmail(finalKcUser.getEmail());

                        if (finalKcUser.getAttributes() != null) {
                            Object phoneAttr = finalKcUser.getAttributes().get("phone");
                            if (phoneAttr instanceof java.util.List && !((java.util.List<?>) phoneAttr).isEmpty()) {
                                member.setPhone(String.valueOf(((java.util.List<?>) phoneAttr).get(0)));
                            }
                            Object npiAttr = finalKcUser.getAttributes().get("npi");
                            if (npiAttr instanceof java.util.List && !((java.util.List<?>) npiAttr).isEmpty()) {
                                member.setNpi(String.valueOf(((java.util.List<?>) npiAttr).get(0)));
                            }
                            Object imgAttr = finalKcUser.getAttributes().get("profileImage");
                            if (imgAttr instanceof java.util.List && !((java.util.List<?>) imgAttr).isEmpty()) {
                                member.setProfileImage(String.valueOf(((java.util.List<?>) imgAttr).get(0)));
                            }
                        }
                    } else {
                        if (finalKcId != null && !finalKcId.isEmpty()) member.setKeycloakId(finalKcId);
                    }

                    log.debug("Member mis à jour lors de la synchronisation pour: {}", email);
                    return memberRepository.save(member);
                })
                .orElseGet(() -> {
                    Member newMember = new Member();
                    newMember.setEmail(email);
                    newMember.setKeycloakId(finalKcId == null ? (finalKcUser != null ? finalKcUser.getId() : null) : finalKcId);
                    newMember.setRole(Role.MEMBER);

                    if (finalKcUser != null) {
                        newMember.setFirstName(finalKcUser.getFirstName());
                        newMember.setName(finalKcUser.getLastName());
                        if (finalKcUser.getAttributes() != null) {
                            Object phoneAttr = finalKcUser.getAttributes().get("phone");
                            if (phoneAttr instanceof java.util.List && !((java.util.List<?>) phoneAttr).isEmpty()) {
                                newMember.setPhone(String.valueOf(((java.util.List<?>) phoneAttr).get(0)));
                            }
                            Object npiAttr = finalKcUser.getAttributes().get("npi");
                            if (npiAttr instanceof java.util.List && !((java.util.List<?>) npiAttr).isEmpty()) {
                                newMember.setNpi(String.valueOf(((java.util.List<?>) npiAttr).get(0)));
                            }
                            Object imgAttr = finalKcUser.getAttributes().get("profileImage");
                            if (imgAttr instanceof java.util.List && !((java.util.List<?>) imgAttr).isEmpty()) {
                                newMember.setProfileImage(String.valueOf(((java.util.List<?>) imgAttr).get(0)));
                            }
                        }
                    }

                    newMember.setIsRegular(false);
                    newMember.setHasPreviousDebt(false);
                    newMember.setSubscriptionStatus("PENDING");

                    if (newMember.getNpi() == null) newMember.setNpi("NPI-" + System.currentTimeMillis());
                    if (newMember.getPhone() == null) newMember.setPhone("Non renseigné");

                    Member saved = memberRepository.save(newMember);
                    log.info("Nouveau membre créé lors de la synchronisation: {}", email);
                    return saved;
                });
    }

    // ==================== GESTION DES MOTS DE PASSE ====================

    public boolean resetPassword(String userId, String newPassword) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            userResource.resetPassword(credential);

            log.info("Mot de passe réinitialisé pour l'utilisateur ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du mot de passe", e);
            return false;
        }
    }

    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        try {
            // 1. Vérifier d'abord que l'ancien mot de passe est correct
            String email = getUserById(userId).getEmail();

            // Tenter une authentification avec l'ancien mot de passe
            try {
                Keycloak userKeycloak = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)  // ← CORRIGÉ : utilisation de la variable
                        .realm(realm)
                        .username(email)
                        .password(currentPassword)
                        .clientId("mutuelle-client")
                        .build();

                // Si ça échoue, une exception sera levée
                userKeycloak.tokenManager().getAccessToken();
            } catch (Exception e) {
                log.warn("Ancien mot de passe incorrect pour: {}", email);
                return false;
            }

            // 2. Si l'ancien mot de passe est correct, on peut changer
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            userResource.resetPassword(credential);

            log.info("Mot de passe changé pour l'utilisateur ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Erreur lors du changement de mot de passe", e);
            return false;
        }
    }


}