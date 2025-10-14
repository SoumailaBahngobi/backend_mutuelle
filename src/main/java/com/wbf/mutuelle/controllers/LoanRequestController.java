package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ApprovalRequest;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.services.LoanRequestService;
import org.springframework.http.HttpStatus;
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

    // GET toutes les demandes de prêt
    @GetMapping
    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestService.getAllLoanRequests();
    }

    // GET une demande spécifique par ID
    @GetMapping("/{id}")
    public ResponseEntity<LoanRequest> getLoanRequestById(@PathVariable Long id) {
        return loanRequestService.getLoanRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET demandes par membre ID
    @GetMapping("/member/{memberId}")
    public List<LoanRequest> getLoanRequestsByMember(@PathVariable Long memberId) {
        return loanRequestService.getLoanRequestsByMemberId(memberId);
    }

    // CORRECTION : Mes demandes de prêt
    @GetMapping("/my-requests")
<<<<<<< HEAD
    public ResponseEntity<List<LoanRequest>> getMyLoanRequests(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<LoanRequest> myRequests = loanRequestService.getLoanRequestsByMemberEmail(userDetails.getUsername());
            return ResponseEntity.ok(myRequests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
=======
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
>>>>>>> cab43455d1c7321b3be4720b9866b944178a04ff
    }

    // POST créer une nouvelle demande
    @PostMapping
    public ResponseEntity<?> createLoanRequest(@RequestBody LoanRequest loanRequest,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoanRequest createdRequest = loanRequestService.createLoanRequest(loanRequest, userDetails.getUsername());
            return ResponseEntity.ok(createdRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoints d'approbation
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

    // Endpoint de rejet
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoanRequest(@PathVariable Long id,
                                               @RequestBody Map<String, String> request) {
        try {
            String rejectionReason = request.get("rejectionReason");
            String rejectedByRole = request.get("rejectedByRole");
            LoanRequest rejectedRequest = loanRequestService.rejectLoanRequest(id, rejectionReason, rejectedByRole);
            return ResponseEntity.ok(rejectedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint pour réinitialiser une approbation
    @PostMapping("/{id}/reset-approval")
    public ResponseEntity<?> resetApproval(@PathVariable Long id,
                                           @RequestBody Map<String, String> request) {
        try {
            String role = request.get("role");
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

    // Endpoints pour les responsables
    @GetMapping("/all-with-approval")
    public List<LoanRequest> getAllLoanRequestsWithApprovalDetails() {
        return loanRequestService.getAllLoanRequestsWithApprovalDetails();
    }

    @GetMapping("/my-pending-approvals")
    public List<LoanRequest> getMyPendingApprovals(@AuthenticationPrincipal UserDetails userDetails) {
        return loanRequestService.getPendingApprovalsForCurrentUser(userDetails.getUsername());
    }

    @GetMapping("/validator-dashboard")
    public Map<String, Object> getValidatorDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return loanRequestService.getValidatorDashboard(userDetails.getUsername());
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
   /* public void generateRepaymentSchedule(@PathVariable Long id) {
        LoanRequest loanRequest = loanRequestService.getLoanRequestById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        if (!"APPROVED".equals(loanRequest.getStatus())) {
            throw new RuntimeException("Seules les demandes de prêt approuvées peuvent avoir un plan de remboursement");
        }

        LoanRequestController repaymentService;
        repaymentService.generateRepaymentSchedule(loanRequest);
    }*/
    public void generateRepaymentSchedule(@PathVariable Long id) {
        loanRequestService.generateRepaymentScheduleForLoanRequest(id);
    }
    

    @GetMapping("/{id}/repayments")
    /*public List<Repayment> getLoanRequestRepayments(@PathVariable Long id) {
        return repaymentService.getRepaymentsByLoanRequest(id);
    }*/
    public List<Repayment> getLoanRequestRepayments(@PathVariable Long id) {
        return loanRequestService.getRepaymentsByLoanRequest(id);
    }

    @GetMapping("/approved")
    public List<LoanRequest> getApprovedLoans() {
        return loanRequestService.getApprovedLoans();
    }

}