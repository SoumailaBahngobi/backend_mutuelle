package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ApprovalRequest;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.services.LoanRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3000)
@RestController
@RequestMapping("/mut/loan_request")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

    public LoanRequestController(LoanRequestService loanRequestService) {
        this.loanRequestService = loanRequestService;
    }

    @GetMapping
    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestService.getAllLoanRequests();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanRequest> getLoanRequestById(@PathVariable Long id) {
        return loanRequestService.getLoanRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/member/{memberId}")
    public List<LoanRequest> getLoanRequestsByMember(@PathVariable Long memberId) {
        return loanRequestService.getLoanRequestsByMemberId(memberId);
    }

    @GetMapping("/my-requests")
    public List<LoanRequest> getMyLoanRequests(@AuthenticationPrincipal UserDetails userDetails) {
        String username = null;
        if (userDetails != null) {
            username = userDetails.getUsername();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                username = auth.getName();
            }
        }

        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        return loanRequestService.getLoanRequestsByMemberEmail(username);
    }

    @PostMapping
    public ResponseEntity<?> createLoanRequest(@RequestBody LoanRequest loanRequest,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String username = null;
            if (userDetails != null) {
                username = userDetails.getUsername();
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                    username = auth.getName();
                }
            }

            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilisateur non authentifié");
            }

            LoanRequest createdRequest = loanRequestService.createLoanRequest(loanRequest, username);
            return ResponseEntity.ok(createdRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoints d'approbation AVEC commentaire (version recommandée)
    @PostMapping("/{id}/approve/president")
    public ResponseEntity<?> approveByPresident(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByPresident(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve/secretary")
    public ResponseEntity<?> approveBySecretary(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveBySecretary(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve/treasurer")
    public ResponseEntity<?> approveByTreasurer(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByTreasurer(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint de rejet avec raison
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoanRequest(@PathVariable Long id,
                                               @RequestBody Map<String, String> request) {
        try {
            String rejectionReason = request.get("rejectionReason");
            String rejectedByRole = request.get("rejectedByRole"); // PRESIDENT, SECRETARY, TREASURER
            LoanRequest rejectedRequest = loanRequestService.rejectLoanRequest(id, rejectionReason, rejectedByRole);
            return ResponseEntity.ok(rejectedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint pour réinitialiser une approbation (admin seulement)
    @PostMapping("/{id}/reset-approval")
    public ResponseEntity<?> resetApproval(@PathVariable Long id,
                                           @RequestBody Map<String, String> request) {
        try {
            String role = request.get("role"); // PRESIDENT, SECRETARY, TREASURER
            LoanRequest updatedRequest = loanRequestService.resetApproval(id, role);
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoints de consultation par statut
    @GetMapping("/status/pending")
    public List<LoanRequest> getPendingRequests() {
        return loanRequestService.getPendingRequests();
    }

    @GetMapping("/status/in-review")
    public List<LoanRequest> getInReviewRequests() {
        return loanRequestService.getInReviewRequests();
    }

    @GetMapping("/status/approved")
    public List<LoanRequest> getApprovedRequests() {
        return loanRequestService.getApprovedRequests();
    }

    @GetMapping("/status/rejected")
    public List<LoanRequest> getRejectedRequests() {
        return loanRequestService.getRejectedRequests();
    }

    // NOUVEAUX ENDPOINTS POUR LES RESPONSABLES
    @GetMapping("/all-with-approval")
    public List<LoanRequest> getAllLoanRequestsWithApprovalDetails() {
        return loanRequestService.getAllLoanRequestsWithApprovalDetails();
    }

    @GetMapping("/my-pending-approvals")
    public List<LoanRequest> getMyPendingApprovals(@AuthenticationPrincipal UserDetails userDetails) {
        String username = null;
        if (userDetails != null) {
            username = userDetails.getUsername();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                username = auth.getName();
            }
        }

        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        return loanRequestService.getPendingApprovalsForCurrentUser(username);
    }

    @GetMapping("/validator-dashboard")
    public Map<String, Object> getValidatorDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        String username = null;
        if (userDetails != null) {
            username = userDetails.getUsername();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                username = auth.getName();
            }
        }

        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        return loanRequestService.getValidatorDashboard(username);
    }

    @GetMapping("/{id}/approval-status")
    public ResponseEntity<Map<String, Object>> getLoanRequestApprovalStatus(@PathVariable Long id) {
        try {
            Map<String, Object> approvalStatus = loanRequestService.getLoanRequestApprovalStatus(id);
            return ResponseEntity.ok(approvalStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/generate-repayment-schedule")
    public ResponseEntity<?> generateRepaymentSchedule(@PathVariable Long id) {
        try {
            loanRequestService.generateRepaymentScheduleForLoanRequest(id);
            return ResponseEntity.ok().body(Map.of("message", "Calendrier de remboursement généré"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/{id}/repayments")
    public List<Repayment> getLoanRequestRepayments(@PathVariable Long id) {
        return loanRequestService.getRepaymentsByLoanRequest(id);
    }

    @GetMapping("/approved")

    public List<LoanRequest> getApprovedLoans() {
        return loanRequestService.getApprovedLoans();
    }

    @PostMapping("/{id}/force-create-loan")
    public ResponseEntity<?> forceCreateLoan(@PathVariable Long id) {
        try {
            loanRequestService.forceCreateLoanFromRequest(id);
            return ResponseEntity.ok().body(Map.of("message", "Prêt créé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ NOUVEAUX ENDPOINTS POUR LE TRÉSORIER
    @GetMapping("/treasurer/pending-grant")
    public ResponseEntity<List<LoanRequest>> getApprovedPendingGrant() {
        try {
            List<LoanRequest> pendingGrants = loanRequestService.getApprovedPendingGrant();
            return ResponseEntity.ok(pendingGrants);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/treasurer/granted-loans")
    public ResponseEntity<List<Loan>> getGrantedLoans() {
        try {
            List<Loan> grantedLoans = loanRequestService.getGrantedLoans();
            return ResponseEntity.ok(grantedLoans);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/treasurer/grant")
    public ResponseEntity<?> grantLoan(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String comment = request.get("comment");
            Loan grantedLoan = loanRequestService.grantLoan(id, comment);
            return ResponseEntity.ok(grantedLoan);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/treasurer/cancel-grant")
    public ResponseEntity<?> cancelLoanGrant(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            loanRequestService.cancelLoanGrant(id, reason);
            return ResponseEntity.ok().body(Map.of("message", "Accord de prêt annulé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/treasurer/dashboard")
    public ResponseEntity<Map<String, Object>> getTreasurerDashboard() {
        try {
            Map<String, Object> dashboard = Map.of(
                    "pendingGrants", loanRequestService.getApprovedPendingGrant().size(),
                    "grantedLoans", loanRequestService.getGrantedLoans().size(),
                    "pendingGrantsList", loanRequestService.getApprovedPendingGrant(),
                    "grantedLoansList", loanRequestService.getGrantedLoans()
            );
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}