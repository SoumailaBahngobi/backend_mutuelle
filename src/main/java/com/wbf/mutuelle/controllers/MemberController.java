package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.services.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@CrossOrigin(origins = "*", maxAge = 3000)
@RestController
@RequestMapping("mut/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/profile")
    public ResponseEntity<Member> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Member member = memberService.getAllMembers().stream()
                .filter(m -> m.getEmail().equals(email))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.ok(member);
    }

    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id).orElseThrow();
    }

    @PostMapping
    public Member createMember(@RequestBody Member member) {
        return memberService.createMember(member);
    }

    @PostMapping("/{id}")
    public Member updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
        return memberService.updateMember(id, memberDetails);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/subscription")
    public ResponseEntity<Member> updateSubscription(
            @PathVariable Long id,
            @RequestParam Boolean isRegular,
            @RequestParam String subscriptionDate) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(subscriptionDate);
            Member updatedMember = memberService.updateSubscriptionStatus(id, isRegular, date);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/debt-status")
    public ResponseEntity<Member> updateDebtStatus(
            @PathVariable Long id,
            @RequestParam Boolean hasDebt) {
        try {
            Member updatedMember = memberService.updateDebtStatus(id, hasDebt);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/can-request-loan")
    public ResponseEntity<Boolean> canRequestLoan(@PathVariable Long id) {
        try {
            boolean canRequest = memberService.validateMemberForLoan(id);
            return ResponseEntity.ok(canRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/current/can-request-loan")
    public ResponseEntity<Boolean> canCurrentUserRequestLoan(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            Member member = memberService.getMemberByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
            boolean canRequest = memberService.validateMemberForLoan(member.getId());
            return ResponseEntity.ok(canRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}