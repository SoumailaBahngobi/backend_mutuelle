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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloakAdmin;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

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

            // 2. Déterminer le rôle Keycloak (toujours MEMBER pour les utilisateurs normaux)
            String keycloakRole = "MEMBER";

            // 3. Créer l'utilisateur dans Keycloak avec le rôle MEMBER
            String userId = createUserInKeycloak(request, keycloakRole);

            // 4. Créer l'utilisateur dans la base locale
            Member member = new Member();
            member.setKeycloakId(userId);
            member.setEmail(request.getEmail());
            member.setName(request.getName());
            member.setFirstName(request.getFirstName());
            member.setPhone(request.getPhone());
            member.setNpi(request.getNpi());

            // Gérer le rôle local (peut être différent du rôle Keycloak)
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
            log.info("Utilisateur créé avec succès: {}, rôle Keycloak: {}, rôle DB: {}",
                    request.getEmail(), keycloakRole, role);

            // 5. Envoyer l'email de bienvenue
            sendWelcomeEmail(savedMember, request.getPassword());

            return savedMember;

        } catch (jakarta.ws.rs.NotAuthorizedException nae) {
            log.error("Accès refusé lors de l'inscription Keycloak - vérifiez les identifiants/admin roles", nae);
            throw new RuntimeException("Erreur lors de l'inscription: accès refusé (401). Vérifiez la configuration Keycloak.");
        } catch (Exception e) {
            log.error("Erreur lors de l'inscription", e);
            throw new RuntimeException("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    /**
     * Crée un utilisateur dans Keycloak avec le rôle spécifié
     * @param request les données d'inscription
     * @param keycloakRole le rôle à assigner dans Keycloak (MEMBER ou ADMIN)
     * @return l'ID de l'utilisateur créé
     */
    private String createUserInKeycloak(RegisterRequest request, String keycloakRole) {
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

        // Assigner le rôle spécifié dans Keycloak
        assignRoleToUser(userId, keycloakRole);

        log.debug("Utilisateur créé dans Keycloak avec ID: {}, rôle: {}", userId, keycloakRole);
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

    // ==================== INSCRIPTION ADMIN ====================

    @Transactional
    public Member registerAdmin(RegisterRequest request) {
        try {
            // Vérifier si l'utilisateur existe déjà
            if (userExists(request.getEmail())) {
                throw new RuntimeException("Un utilisateur avec cet email existe déjà");
            }

            // Créer dans Keycloak avec le rôle ADMIN
            String userId = createUserInKeycloak(request, "ADMIN");

            // Créer dans la base locale avec le rôle ADMIN
            Member member = new Member();
            member.setKeycloakId(userId);
            member.setEmail(request.getEmail());
            member.setName(request.getName());
            member.setFirstName(request.getFirstName());
            member.setPhone(request.getPhone() != null ? request.getPhone() : "Non renseigné");
            member.setNpi(request.getNpi() != null ? request.getNpi() : "ADMIN-" + System.currentTimeMillis());
            member.setRole(Role.ADMIN);
            member.setIsRegular(true);
            member.setHasPreviousDebt(false);
            member.setSubscriptionStatus("ACTIVE");

            Member savedMember = memberRepository.save(member);

            // Envoyer l'email de bienvenue
            sendWelcomeEmail(savedMember, request.getPassword());

            log.info("✅ Administrateur créé avec succès: {}, rôle Keycloak: ADMIN", request.getEmail());

            return savedMember;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'administrateur", e);
            throw new RuntimeException("Erreur lors de la création de l'administrateur: " + e.getMessage());
        }
    }

    // ==================== ENVOI D'EMAIL DE BIENVENUE ====================

    private void sendWelcomeEmail(Member member, String password) {
        try {
            String subject = "Bienvenue sur la Mutuelle WBF !";

            String body = String.format("""
                Bonjour %s %s,
                
                Votre compte a été créé avec succès sur la plateforme Mutuelle WBF.
                
                Vos informations de connexion :
                ────────────────────────────────
                Email : %s
                Mot de passe : %s
                ────────────────────────────────
                
                Accédez à votre espace : http://localhost:3000/login
                
                Pour votre sécurité, nous vous recommandons de changer votre mot de passe après votre première connexion.
                
                Besoin d'aide ? Contactez-nous à support@mutuelle-wbf.com
                
                Cordialement,
                L'équipe Mutuelle WBF
                """,
                    member.getFirstName(),
                    member.getName(),
                    member.getEmail(),
                    password
            );

            emailService.sendSimpleEmail(member.getEmail(), subject, body);
            log.info("Email de bienvenue envoyé à: {}", member.getEmail());

        } catch (Exception e) {
            // Ne pas bloquer l'inscription si l'email échoue, juste logger
            log.error("Erreur lors de l'envoi de l'email de bienvenue à {}: {}", member.getEmail(), e.getMessage());
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

            // Envoyer un email de confirmation
            sendResetPasswordConfirmationEmail(email);

            log.info("Email de réinitialisation envoyé à: {}", email);
            return true;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email", e);
            return false;
        }
    }

    private void sendResetPasswordConfirmationEmail(String email) {
        try {
            String subject = "Réinitialisation de votre mot de passe - Mutuelle WBF";
            String body = String.format("""
                Bonjour,
                
                Vous avez demandé la réinitialisation de votre mot de passe.
                
                Un email a été envoyé à %s avec un lien pour réinitialiser votre mot de passe.
                
                Si vous n'avez pas fait cette demande, veuillez ignorer cet email.
                
                Ce lien est valable 24 heures.
                Cordialement,
                L'équipe Mutuelle WBF
                """, email);

            emailService.sendSimpleEmail(email, subject, body);
            log.info("Email de confirmation de réinitialisation envoyé à: {}", email);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation à {}: {}", email, e.getMessage());
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

                        updateMemberAttributes(member, finalKcUser.getAttributes());
                    } else {
                        if (finalKcId != null && !finalKcId.isEmpty()) member.setKeycloakId(finalKcId);
                    }

                    log.debug("Member mis à jour lors de la synchronisation pour: {}", email);
                    return memberRepository.save(member);
                })
                .orElseGet(() -> createNewMember(email, finalKcId, finalKcUser));
    }

    private void updateMemberAttributes(Member member, Map<String, List<String>> attributes) {
        if (attributes == null) return;

        if (attributes.containsKey("phone") && !attributes.get("phone").isEmpty()) {
            member.setPhone(attributes.get("phone").get(0));
        }
        if (attributes.containsKey("npi") && !attributes.get("npi").isEmpty()) {
            member.setNpi(attributes.get("npi").get(0));
        }
        if (attributes.containsKey("profileImage") && !attributes.get("profileImage").isEmpty()) {
            member.setProfileImage(attributes.get("profileImage").get(0));
        }
    }

    private Member createNewMember(String email, String keycloakId, UserRepresentation kcUser) {
        Member newMember = new Member();
        newMember.setEmail(email);
        newMember.setKeycloakId(keycloakId == null ? (kcUser != null ? kcUser.getId() : null) : keycloakId);
        newMember.setRole(Role.MEMBER);

        if (kcUser != null) {
            newMember.setFirstName(kcUser.getFirstName());
            newMember.setName(kcUser.getLastName());
            updateMemberAttributes(newMember, kcUser.getAttributes());
        }

        newMember.setIsRegular(false);
        newMember.setHasPreviousDebt(false);
        newMember.setSubscriptionStatus("PENDING");

        if (newMember.getNpi() == null) newMember.setNpi("NPI-" + System.currentTimeMillis());
        if (newMember.getPhone() == null) newMember.setPhone("Non renseigné");

        Member saved = memberRepository.save(newMember);
        log.info("Nouveau membre créé lors de la synchronisation: {}", email);
        return saved;
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
                        .serverUrl(serverUrl)
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

    // ==================== GESTION DES RÔLES ====================

    @Transactional
    public void updateUserRole(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Récupérer les rôles actuels
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();

            // Supprimer tous les rôles existants
            if (!currentRoles.isEmpty()) {
                userResource.roles().realmLevel().remove(currentRoles);
                log.debug("Rôles existants supprimés pour l'utilisateur: {}", userId);
            }

            // Ajouter le nouveau rôle
            RoleRepresentation newRole = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(newRole));

            log.info("✅ Rôle {} attribué à l'utilisateur Keycloak {}", roleName, userId);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du rôle dans Keycloak pour {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Récupère le rôle Keycloak d'un utilisateur
     */
    public String getUserKeycloakRole(String userId) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();
            if (!roles.isEmpty()) {
                return roles.get(0).getName();
            }
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du rôle Keycloak pour {}", userId, e);
            return null;
        }
    }
}