package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.RoleUpdateRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.services.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/mutuelle/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    /**
     * Profil du membre connecté
     */
    @GetMapping("/profile")
    public ResponseEntity<Member> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Member member = memberService.getMemberByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        return ResponseEntity.ok(member);
    }

    /**
     * Mettre à jour le profil
     */
    @PutMapping("/profile")
    public ResponseEntity<Member> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody Member memberDetails) {
        String email = jwt.getClaimAsString("email");
        Member member = memberService.getMemberByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        member.setName(memberDetails.getName());
        member.setFirstName(memberDetails.getFirstName());
        member.setPhone(memberDetails.getPhone());
        member.setNpi(memberDetails.getNpi());

        Member updated = memberService.updateMember(member.getId(), member);
        return ResponseEntity.ok(updated);
    }

    /**
     * Upload photo de profil
     */
    @PostMapping("/upload-profile")
    public ResponseEntity<?> uploadProfileImage(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam("file") MultipartFile file) {
        String email = jwt.getClaimAsString("email");
        Member member = memberService.getMemberByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        return ResponseEntity.ok(Map.of("message", "Upload réussi"));
    }

    // ========== MEMBER ENDPOINTS ==========

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PRESIDENT') or hasRole('SECRETARY') or hasRole('ADMIN') or hasRole('TREASURER')")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRESIDENT')")
    public Member updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
        return memberService.updateMember(id, memberDetails);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/subscription")
    @PreAuthorize("hasRole('PRESIDENT') or hasRole('SECRETARY')")
    public ResponseEntity<Member> updateSubscription(@PathVariable Long id,
                                                     @RequestParam Boolean isRegular,
                                                     @RequestParam String subscriptionDate) {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/debt-status")
    @PreAuthorize("hasRole('PRESIDENT') or hasRole('TREASURER')")
    public ResponseEntity<Member> updateDebtStatus(@PathVariable Long id,
                                                   @RequestParam Boolean hasDebt) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/can-request-loan")
    public ResponseEntity<Boolean> canRequestLoan(@PathVariable Long id) {
        boolean canRequest = memberService.validateMemberForLoan(id);
        return ResponseEntity.ok(canRequest);
    }

    @PostMapping("/link-keycloak")
    public ResponseEntity<?> linkKeycloakAccount(@RequestBody Map<String, Object> request,
                                                 Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");

            Long memberId = Long.parseLong(request.get("memberId").toString());
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'ID: " + memberId));

            member.setKeycloakId(keycloakId);
            if (email != null) {
                member.setEmail(email);
            }

            memberRepository.save(member);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Compte Keycloak lié avec succès",
                    "memberId", member.getId(),
                    "keycloakId", keycloakId,
                    "email", email
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/create-from-keycloak")
    public ResponseEntity<?> createMemberFromKeycloak(Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            String name = firstName + " " + lastName;

            Optional<Member> existingByEmail = memberRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                Member member = existingByEmail.get();
                if (member.getKeycloakId() == null) {
                    member.setKeycloakId(keycloakId);
                    memberRepository.save(member);
                }
                return ResponseEntity.ok(Map.of(
                        "message", "Membre existant mis à jour avec keycloakId",
                        "member", member
                ));
            }

            Member newMember = new Member();
            newMember.setKeycloakId(keycloakId);
            newMember.setEmail(email);
            newMember.setFirstName(firstName);
            newMember.setName(name);
            newMember.setRole(Role.MEMBER);
            newMember.setIsRegular(false);
            newMember.setHasPreviousDebt(false);
            newMember.setSubscriptionStatus("PENDING");

            Member saved = memberRepository.save(newMember);

            return ResponseEntity.ok(Map.of(
                    "message", "Membre créé avec succès",
                    "member", saved
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/auto-link")
    public ResponseEntity<?> autoLinkMember(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            Optional<Member> existingByKeycloak = memberRepository.findByKeycloakId(keycloakId);
            if (existingByKeycloak.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Membre déjà lié",
                        "member", existingByKeycloak.get()
                ));
            }

            Optional<Member> existingByEmail = memberRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                Member member = existingByEmail.get();
                member.setKeycloakId(keycloakId);
                memberRepository.save(member);
                return ResponseEntity.ok(Map.of(
                        "message", "Membre existant lié avec succès",
                        "member", member
                ));
            }

            Member newMember = new Member();
            newMember.setKeycloakId(keycloakId);
            newMember.setEmail(email);
            newMember.setFirstName(firstName);
            newMember.setName(firstName + " " + lastName);
            newMember.setRole(Role.MEMBER);
            newMember.setIsRegular(false);
            newMember.setHasPreviousDebt(false);
            newMember.setSubscriptionStatus("PENDING");

            Member saved = memberRepository.save(newMember);

            return ResponseEntity.ok(Map.of(
                    "message", "Nouveau membre créé avec succès",
                    "member", saved
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS ADMIN ====================

    /**
     * Récupérer tous les membres (admin)
     */
    @GetMapping("/admin/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Member>> getAllMembersAdmin() {
        log.info("🔍 ADMIN - Récupération de tous les membres");
        List<Member> members = memberService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    /**
     * Récupérer un membre par ID (admin)
     */
    @GetMapping("/admin/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMemberByIdAdmin(@PathVariable Long id) {
        log.info(" ADMIN - Récupération du membre ID: {}", id);
        try {
            Member member = memberService.getMemberById(id)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            log.error(" Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/admin/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMemberAdmin(@PathVariable Long id, @RequestBody Member memberDetails) {
        log.info(" ADMIN - Mise à jour du membre ID: {}", id);
        log.info(" Données reçues: {}", memberDetails);

        try {
            Member updatedMember = memberService.updateMember(id, memberDetails);
            log.info(" Membre {} mis à jour avec succès", id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Membre mis à jour avec succès",
                    "member", updatedMember
            ));
        } catch (Exception e) {
            log.error(" Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Supprimer un membre (admin)
     */
    @DeleteMapping("/admin/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMemberAdmin(@PathVariable Long id) {
        log.info(" ADMIN - Suppression du membre ID: {}", id);
        try {
            memberService.deleteMember(id);
            log.info(" Membre {} supprimé avec succès", id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Membre supprimé avec succès"
            ));
        } catch (Exception e) {
            log.error(" Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Attribuer un rôle à un utilisateur
     */
    @PutMapping("/admin/members/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        log.info(" ADMIN - Attribution du rôle {} au membre ID: {}", request.getRole(), id);
        try {
            Member updatedMember = memberService.assignRole(id, request.getRole());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rôle attribué avec succès",
                    "member", updatedMember
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Récupérer les membres par rôle
     */
    @GetMapping("/admin/by-role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Member>> getMembersByRole(@PathVariable Role role) {
        List<Member> members = memberService.getMembersByRole(role);
        return ResponseEntity.ok(members);
    }

    /**
     * Récupérer les membres sans rôle spécifique
     */
    @GetMapping("/admin/pending-roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Member>> getMembersWithoutRoles() {
        List<Member> members = memberService.getMembersWithoutSpecificRoles();
        return ResponseEntity.ok(members);
    }

    @GetMapping("/test")
    public String test() {
        return "MemberController fonctionne";
    }
}