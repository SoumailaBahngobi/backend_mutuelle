package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ApiResponse;
import com.wbf.mutuelle.dto.ChangePasswordRequest;
import com.wbf.mutuelle.dto.ForgotPasswordRequest;
import com.wbf.mutuelle.dto.RegisterRequest;
import com.wbf.mutuelle.dto.ResetPasswordRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.services.KeycloakUserService;
import com.wbf.mutuelle.services.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mutuelle/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakUserService keycloakUserService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    /**
     * Inscription via Keycloak
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Member member = keycloakUserService.registerUser(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Inscription réussie ! Vous pouvez maintenant vous connecter.",
                            Map.of("email", member.getEmail(), "id", member.getId())));
        } catch (Exception e) {
            log.error("Erreur inscription", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Récupérer les informations de l'utilisateur connecté
     */
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String email = jwt.getClaim("email");
        String keycloakId = jwt.getSubject();

        // Chercher dans la base locale
        Member member = memberRepository.findByEmail(email)
                .orElse(null);

        // Si pas trouvé, synchroniser automatiquement
        if (member == null) {
            member = new Member();
            member.setEmail(email);
            member.setFirstName(jwt.getClaim("given_name"));
            member.setName(jwt.getClaim("family_name"));
            member.setKeycloakId(keycloakId);
            member.setRole(Role.MEMBER);
            member.setIsRegular(false);
            member.setHasPreviousDebt(false);
            member.setSubscriptionStatus("PENDING");
            member.setNpi("NPI-" + System.currentTimeMillis());
            member.setPhone("Non renseigné");

            member = memberRepository.save(member);
            log.info("Nouvel utilisateur synchronisé via user-info: {}", email);
        } else {
            if (member.getKeycloakId() == null) {
                member.setKeycloakId(keycloakId);
                member = memberRepository.save(member);
                log.info("KeycloakId mis à jour pour: {}", email);
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("email", email);
        userInfo.put("name", member.getName());
        userInfo.put("firstName", member.getFirstName());
        userInfo.put("id", member.getId());
        userInfo.put("role", member.getRole());
        userInfo.put("npi", member.getNpi());
        userInfo.put("phone", member.getPhone());
        userInfo.put("isRegular", member.getIsRegular());
        userInfo.put("subscriptionStatus", member.getSubscriptionStatus());

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            userInfo.put("keycloakRoles", realmAccess.get("roles"));
        }

        return ResponseEntity.ok(userInfo);
    }

    /**
     * URL de connexion Keycloak (pour le frontend)
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String loginUrl = "http://localhost:8088/realms/mutuelle-realm/protocol/openid-connect/auth" +
                "?client_id=mutuelle-client" +
                "&response_type=code" +
                "&redirect_uri=http://localhost:3000" +
                "&scope=openid%20profile%20email";

        return ResponseEntity.ok(Map.of("loginUrl", loginUrl));
    }

    /**
     * URL de déconnexion Keycloak
     */
    @GetMapping("/logout-url")
    public ResponseEntity<Map<String, String>> getLogoutUrl() {
        String logoutUrl = "http://localhost:8088/realms/mutuelle-realm/protocol/openid-connect/logout" +
                "?redirect_uri=http://localhost:3000";

        return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
    }

    /**
     * URL de réinitialisation Keycloak
     */
    @GetMapping("/reset-password-url")
    public ResponseEntity<Map<String, String>> getResetPasswordUrl() {
        String resetUrl = "http://localhost:8088/realms/mutuelle-realm/login-actions/reset-credentials";
        return ResponseEntity.ok(Map.of("resetUrl", resetUrl));
    }

    /**
     * Mot de passe oublié - Demande d'envoi d'email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            boolean sent = keycloakUserService.sendResetPasswordEmail(request.getEmail());

            if (sent) {
                return ResponseEntity.ok(new ApiResponse(true,
                        "Email de réinitialisation envoyé. Vérifiez votre boîte de réception.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Aucun compte trouvé avec cet email.", null));
            }
        } catch (Exception e) {
            log.error("Erreur forgot password", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Erreur lors de l'envoi de l'email.", null));
        }
    }

    /**
     * Réinitialisation du mot de passe avec le token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            boolean reset = keycloakUserService.resetPassword(request.getUserId(), request.getNewPassword());

            if (reset) {
                return ResponseEntity.ok(new ApiResponse(true,
                        "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Échec de la réinitialisation du mot de passe.", null));
            }
        } catch (Exception e) {
            log.error("Erreur reset password", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Erreur lors de la réinitialisation.", null));
        }
    }

    /**
     * Changement de mot de passe (utilisateur connecté)
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {

        try {
            String keycloakId = jwt.getSubject();
            boolean changed = keycloakUserService.changePassword(keycloakId,
                    request.getCurrentPassword(), request.getNewPassword());

            if (changed) {
                return ResponseEntity.ok(new ApiResponse(true,
                        "Mot de passe modifié avec succès.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Mot de passe actuel incorrect.", null));
            }
        } catch (Exception e) {
            log.error("Erreur change password", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Erreur lors du changement de mot de passe.", null));
        }
    }
}