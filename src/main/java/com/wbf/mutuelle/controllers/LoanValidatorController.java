package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.LoanRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "*", maxAge = 3000)
@RestController
@RequestMapping("/mutuelle/loan-validator")
@RequiredArgsConstructor
public class LoanValidatorController {

    private final LoanRequestService loanRequestService;
    // Tableau de bord pour les validateurs
    @GetMapping("/dashboard")
    public ResponseEntity<?> getValidatorDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> dashboard = loanRequestService.getValidatorDashboard(userDetails.getUsername());
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Demandes en attente de validation par l'utilisateur courant
    @GetMapping("/my-pending-approvals")
    public List<LoanRequest> getMyPendingApprovals(@AuthenticationPrincipal UserDetails userDetails) {
        return loanRequestService.getPendingApprovalsForCurrentUser(userDetails.getUsername());
    }
    // Historique des validations faites par l'utilisateur
    @GetMapping("/my-approval-history")
    public List<LoanRequest> getMyApprovalHistory(@AuthenticationPrincipal UserDetails userDetails) {
        // Implémentez cette méthode selon vos besoins
        return List.of(); // À compléter
    }
}