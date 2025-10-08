package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.LoanRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mut/loan-validator")
public class LoanValidatorController {

    private final LoanRequestService loanRequestService;

    public LoanValidatorController(LoanRequestService loanRequestService) {
        this.loanRequestService = loanRequestService;
    }

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
}