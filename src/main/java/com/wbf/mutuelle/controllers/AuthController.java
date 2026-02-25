package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mutuelle/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        // Extraire les informations du token Keycloak
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("email", oidcUser.getEmail());
        userInfo.put("name", oidcUser.getFullName());
        userInfo.put("firstName", oidcUser.getGivenName());
        userInfo.put("lastName", oidcUser.getFamilyName());
        userInfo.put("preferred_username", oidcUser.getPreferredUsername());

        // Extraire les rôles
        Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            userInfo.put("roles", realmAccess.get("roles"));
        }

        // Synchroniser avec la base de données locale
        syncUserWithDatabase(oidcUser);

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        // URL de déconnexion Keycloak
        String logoutUrl = "http://localhost:8080/realms/mutuelle-realm/protocol/openid-connect/logout" +
                "?redirect_uri=http://localhost:3000";

        return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
    }

    private void syncUserWithDatabase(OidcUser oidcUser) {
        String email = oidcUser.getEmail();

        if (!memberRepository.findByEmail(email).isPresent()) {
            // Créer un nouvel utilisateur dans la base de données locale
            Member newMember = new Member();
            newMember.setEmail(email);
            newMember.setName(oidcUser.getFamilyName());
            newMember.setFirstName(oidcUser.getGivenName());
            // Définir les valeurs par défaut pour les champs obligatoires
            newMember.setNpi("NPI-" + System.currentTimeMillis()); // À adapter
            newMember.setPhone("Non renseigné");

            memberRepository.save(newMember);
            log.info("Nouvel utilisateur synchronisé depuis Keycloak: {}", email);
        }
    }
}