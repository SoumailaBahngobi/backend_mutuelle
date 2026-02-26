package com.wbf.mutuelle.services;

import com.wbf.mutuelle.dto.RegisterRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.repositories.MemberRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloakAdmin;
    private final MemberRepository memberRepository;

    @Value("${keycloak.realm}")
    private String realm;

    // ==================== INSCRIPTION ====================

    @Transactional
    public Member registerUser(RegisterRequest request) {
        try {
            // 1. Vérifier si l'utilisateur existe déjà
            if (userExists(request.getEmail())) {
                throw new RuntimeException("Un utilisateur avec cet email existe déjà");
            }

            // 2. Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(request);

            // 3. Assigner le rôle par défaut (MEMBER)
            assignRoleToUser(userId, "MEMBER");

            // 4. Créer l'utilisateur dans la base locale (sans mot de passe)
            Member member = new Member();
            member.setKeycloakId(userId);
            member.setEmail(request.getEmail());
            member.setName(request.getName());
            member.setFirstName(request.getFirstName());
            member.setPhone(request.getPhone());
            member.setNpi(request.getNpi());
            member.setRole(Role.MEMBER); // Rôle par défaut
            member.setIsRegular(false);
            member.setHasPreviousDebt(false);
            member.setSubscriptionStatus("PENDING");

            Member savedMember = memberRepository.save(member);
            log.info("Utilisateur créé avec succès: {}", request.getEmail());

            return savedMember;

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
    }

    private void assignRoleToUser(String userId, String roleName) {
        RealmResource realmResource = keycloakAdmin.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(role));
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
            log.error("Erreur recherche utilisateur", e);
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
            log.error("Erreur recherche utilisateur par ID", e);
            return null;
        }
    }

    // ==================== SYNC AVEC BASE LOCALE ====================

    @Transactional
    public Member syncUserWithDatabase(String email, String keycloakId) {
        return memberRepository.findByEmail(email)
                .map(member -> {
                    member.setKeycloakId(keycloakId);
                    return memberRepository.save(member);
                })
                .orElseGet(() -> {
                    // Créer un membre si inexistant
                    Member newMember = new Member();
                    newMember.setEmail(email);
                    newMember.setKeycloakId(keycloakId);
                    newMember.setRole(Role.MEMBER);
                    newMember.setIsRegular(false);
                    newMember.setHasPreviousDebt(false);
                    newMember.setSubscriptionStatus("PENDING");
                    return memberRepository.save(newMember);
                });
    }
}