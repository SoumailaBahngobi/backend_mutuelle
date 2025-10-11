package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ApprovalRequest;
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
@RequestMapping("/mut/loan-validator")
@RequiredArgsConstructor
public class LoanValidatorController {

    private final LoanRequestService loanRequestService;

    // Tableau de bord complet pour les validateurs
    @GetMapping("/dashboard")
    public ResponseEntity<?> getCompleteValidatorDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> dashboard = loanRequestService.getCompleteValidatorDashboard(userDetails.getUsername());
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Toutes les demandes avec filtres optionnels
    @GetMapping("/all-requests")
    public ResponseEntity<List<LoanRequest>> getAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long memberId) {
        List<LoanRequest> requests = loanRequestService.getAllLoanRequestsWithFilters(status, memberId);
        return ResponseEntity.ok(requests);
    }

    // Demandes en attente de validation par l'utilisateur courant
    @GetMapping("/my-pending-approvals")
    public List<LoanRequest> getMyPendingApprovals(@AuthenticationPrincipal UserDetails userDetails) {
        return loanRequestService.getPendingApprovalsForCurrentUser(userDetails.getUsername());
    }

    // Historique des validations faites par l'utilisateur
    @GetMapping("/my-approval-history")
    public List<LoanRequest> getMyApprovalHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return loanRequestService.getValidationHistoryByUser(userDetails.getUsername());
    }

    // Endpoints d'approbation dédiés aux validateurs
    @PostMapping("/{id}/approve/president")
    public ResponseEntity<?> approveByPresident(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByPresident(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve/secretary")
    public ResponseEntity<?> approveBySecretary(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveBySecretary(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve/treasurer")
    public ResponseEntity<?> approveByTreasurer(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByTreasurer(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint de rejet
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoanRequest(@PathVariable Long id,
                                               @RequestBody Map<String, String> request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String rejectionReason = request.get("rejectionReason");
            String rejectedByRole = request.get("rejectedByRole");
            LoanRequest rejectedRequest = loanRequestService.rejectLoanRequest(id, rejectionReason, rejectedByRole);
            return ResponseEntity.ok(rejectedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Détails d'approbation d'une demande spécifique
    @GetMapping("/{id}/approval-details")
    public ResponseEntity<Map<String, Object>> getLoanRequestApprovalDetails(@PathVariable Long id) {
        try {
            Map<String, Object> approvalDetails = loanRequestService.getLoanRequestApprovalStatus(id);
            return ResponseEntity.ok(approvalDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}