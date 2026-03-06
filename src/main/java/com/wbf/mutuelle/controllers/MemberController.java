package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.services.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/mutuelle/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;


    /**
     * Profil du membre connecté
     */
    @GetMapping("/profile")
    public ResponseEntity<Member> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaim("email");
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
        String email = jwt.getClaim("email");
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
        String email = jwt.getClaim("email");
        Member member = memberService.getMemberByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        // Logique d'upload...
        return ResponseEntity.ok(Map.of("message", "Upload réussi"));
    }

    // ========== MEMBER ENDPOINTS ==========

    /**
     * Récupérer tous les membres
     * MODIFIÉ : maintenant accessible à tout utilisateur authentifié
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")  // ← MODIFICATION ICI
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PRESIDENT') or hasRole('SECRETARY')")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRESIDENT')")
    public Member updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
        return memberService.updateMember(id, memberDetails);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRESIDENT')")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/subscription")
    @PreAuthorize("hasRole('PRESIDENT') or hasRole('SECRETARY')")
    public ResponseEntity<Member> updateSubscription(@PathVariable Long id,
                                                     @RequestParam Boolean isRegular,
                                                     @RequestParam String subscriptionDate) {
        // Logique...
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/debt-status")
    @PreAuthorize("hasRole('PRESIDENT') or hasRole('TREASURER')")
    public ResponseEntity<Member> updateDebtStatus(@PathVariable Long id,
                                                   @RequestParam Boolean hasDebt) {
        // Logique...
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/can-request-loan")
    public ResponseEntity<Boolean> canRequestLoan(@PathVariable Long id) {
        boolean canRequest = memberService.validateMemberForLoan(id);
        return ResponseEntity.ok(canRequest);
    }
}