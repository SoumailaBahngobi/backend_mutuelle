package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ApiResponse;
import com.wbf.mutuelle.dto.RegisterRequest;
import com.wbf.mutuelle.entities.Member;
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

        // Synchroniser avec la base locale
        Member member = keycloakUserService.syncUserWithDatabase(email, keycloakId);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("email", email);
        userInfo.put("name", jwt.getClaim("family_name"));
        userInfo.put("firstName", jwt.getClaim("given_name"));
        userInfo.put("preferred_username", jwt.getClaim("preferred_username"));
        userInfo.put("memberId", member.getId());
        userInfo.put("role", member.getRole());

        // Extraire les rôles Keycloak
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            userInfo.put("roles", realmAccess.get("roles"));
        }

        return ResponseEntity.ok(userInfo);
    }

    /**
     * URL de connexion Keycloak (pour le frontend)
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String loginUrl = "http://localhost:8080/realms/mutuelle-realm/protocol/openid-connect/auth" +
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
        String logoutUrl = "http://localhost:8080/realms/mutuelle-realm/protocol/openid-connect/logout" +
                "?redirect_uri=http://localhost:3000";

        return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
    }

    /**
     * Mot de passe oublié
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestParam String email) {
        try {
            boolean sent = keycloakUserService.sendResetPasswordEmail(email);

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
     * URL de réinitialisation Keycloak
     */
    @GetMapping("/reset-password-url")
    public ResponseEntity<Map<String, String>> getResetPasswordUrl() {
        String resetUrl = "http://localhost:8080/realms/mutuelle-realm/login-actions/reset-credentials";
        return ResponseEntity.ok(Map.of("resetUrl", resetUrl));
    }
}