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

    // ==================== CRUD MEMBRES ====================

    @GetMapping("/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Member>> getAllMembers() {
        log.info("🔍 ADMIN - Récupération de tous les membres");
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMemberById(@PathVariable Long id) {
        log.info("🔍 ADMIN - Récupération du membre ID: {}", id);
        try {
            Member member = memberService.getMemberById(id)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
        log.info("🔍 ADMIN - Mise à jour du membre ID: {}", id);
        log.info("📝 Données reçues: {}", memberDetails);

        try {
            Member updatedMember = memberService.updateMember(id, memberDetails);
            log.info("✅ Membre {} mis à jour avec succès", id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Membre mis à jour avec succès",
                    "member", updatedMember
            ));
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMember(@PathVariable Long id) {
        log.info("🔍 ADMIN - Suppression du membre ID: {}", id);
        try {
            memberService.deleteMember(id);
            log.info("✅ Membre {} supprimé avec succès", id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Membre supprimé avec succès"
            ));
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== GESTION DES RÔLES ====================

    @PutMapping("/members/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        log.info("🔍 ADMIN - Attribution du rôle {} au membre ID: {}", request.getRole(), id);
        try {
            Member member = memberService.assignRole(id, request.getRole());

            // Mettre à jour le rôle dans Keycloak également
            if (member.getKeycloakId() != null) {
                keycloakUserService.updateUserRole(member.getKeycloakId(), request.getRole().name());
            }

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
        log.info("🔍 ADMIN - Récupération des membres avec rôle: {}", role);
        return ResponseEntity.ok(memberService.getMembersByRole(role));
    }
}