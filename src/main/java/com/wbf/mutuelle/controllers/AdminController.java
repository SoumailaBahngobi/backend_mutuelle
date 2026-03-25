package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.RegisterRequest;
import com.wbf.mutuelle.dto.RoleUpdateRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.services.KeycloakUserService;
import com.wbf.mutuelle.services.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mutuelle/admin")
@RequiredArgsConstructor
public class AdminController {

    private final KeycloakUserService keycloakUserService;
    private final MemberService memberService;

    @Value("${admin.registration.secret:SUPER_SECRET_2024}")
    private String adminSecret;

    // ==================== INSCRIPTION ADMIN ====================

    @PostMapping("/verify-secret")
    public ResponseEntity<?> verifySecret(@RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        boolean isValid = secret != null && secret.equals(adminSecret);
        log.info("🔑 Vérification code secret: {}", isValid ? "✅ VALIDE" : "❌ INVALIDE");
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterRequest request,
                                           @RequestHeader(value = "X-Admin-Secret", required = false) String secret) {

        if (secret == null || !secret.equals(adminSecret)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Code secret invalide"
            ));
        }

        try {
            Member admin = keycloakUserService.registerAdmin(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Administrateur créé avec succès",
                    "member", Map.of(
                            "id", admin.getId(),
                            "email", admin.getEmail(),
                            "firstName", admin.getFirstName(),
                            "name", admin.getName(),
                            "role", admin.getRole()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== GESTION DES RÔLES ====================

    @GetMapping("/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Member>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @PutMapping("/members/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        try {
            Member member = memberService.assignRole(id, request.getRole());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rôle attribué avec succès",
                    "member", member
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/members/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Member>> getMembersByRole(@PathVariable Role role) {
        return ResponseEntity.ok(memberService.getMembersByRole(role));
    }
}