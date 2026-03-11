package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ApprovalRequest;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.repositories.LoanRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.services.LoanRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;  // ✅ Importer celle-ci
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3000)
@RestController
@RequestMapping("/mutuelle/loan_request")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final MemberRepository memberRepository;  // Ajoutez @Autowired si nécessaire
    private final LoanRequestRepository loanRequestRepository;  // Ajoutez @Autowired si nécessaire

    public LoanRequestController(LoanRequestService loanRequestService,
                                 MemberRepository memberRepository,
                                 LoanRequestRepository loanRequestRepository) {
        this.loanRequestService = loanRequestService;
        this.memberRepository = memberRepository;
        this.loanRequestRepository = loanRequestRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestService.getAllLoanRequests();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createLoanRequest(@RequestBody LoanRequest loanRequest,
                                               @AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Map<String, Object> claims = jwt.getClaims();
            String email = (String) claims.getOrDefault("email",
                    claims.getOrDefault("preferred_username", keycloakId));

            LoanRequest createdRequest = loanRequestService.createLoanRequest(
                    loanRequest,
                    email,
                    keycloakId
            );

            return ResponseEntity.ok(createdRequest);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<LoanRequest> getLoanRequestById(@PathVariable Long id) {
        return loanRequestService.getLoanRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/member/{memberId}")
    @Transactional(readOnly = true)
    public List<LoanRequest> getLoanRequestsByMember(@PathVariable Long memberId) {
        return loanRequestService.getLoanRequestsByMemberId(memberId);
    }

    @GetMapping("/my-requests")
    @Transactional(readOnly = true)  // ✅ Maintenant ça fonctionne
    public List<LoanRequest> getMyLoanRequests(Authentication authentication) {
        String username = extractUsername(authentication);
        return loanRequestService.getLoanRequestsByMemberEmail(username);
    }

    @PostMapping("/{id}/approve/president")
    @Transactional
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
    @Transactional
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
    @Transactional
    public ResponseEntity<?> approveByTreasurer(@PathVariable Long id,
                                                @RequestBody ApprovalRequest approvalRequest) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByTreasurer(id, approvalRequest.getComment());
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    @Transactional
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

    @PostMapping("/{id}/reset-approval")
    @Transactional
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

    @GetMapping("/status/pending")
    @Transactional(readOnly = true)
    public List<LoanRequest> getPendingRequests() {
        return loanRequestService.getPendingRequests();
    }

    @GetMapping("/status/in-review")
    @Transactional(readOnly = true)
    public List<LoanRequest> getInReviewRequests() {
        return loanRequestService.getInReviewRequests();
    }

    @GetMapping("/status/approved")
    @Transactional(readOnly = true)
    public List<LoanRequest> getApprovedRequests() {
        return loanRequestService.getApprovedRequests();
    }

    @GetMapping("/status/rejected")
    @Transactional(readOnly = true)
    public List<LoanRequest> getRejectedRequests() {
        return loanRequestService.getRejectedRequests();
    }

    @GetMapping("/all-with-approval")
    @Transactional(readOnly = true)  // ✅ Ajoutez ceci aussi
    public List<LoanRequest> getAllLoanRequestsWithApprovalDetails() {
        return loanRequestService.getAllLoanRequestsWithApprovalDetails();
    }

    @GetMapping("/my-pending-approvals")
    @Transactional(readOnly = true)
    public List<LoanRequest> getMyPendingApprovals(Authentication authentication) {
        String username = extractUsername(authentication);
        return loanRequestService.getPendingApprovalsForCurrentUser(username);
    }

    @GetMapping("/validator-dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> getValidatorDashboard(Authentication authentication) {
        String username = extractUsername(authentication);
        return loanRequestService.getValidatorDashboard(username);
    }

    @GetMapping("/{id}/approval-status")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getLoanRequestApprovalStatus(@PathVariable Long id) {
        try {
            Map<String, Object> approvalStatus = loanRequestService.getLoanRequestApprovalStatus(id);
            return ResponseEntity.ok(approvalStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/generate-repayment-schedule")
    @Transactional
    public ResponseEntity<?> generateRepaymentSchedule(@PathVariable Long id) {
        try {
            loanRequestService.generateRepaymentScheduleForLoanRequest(id);
            return ResponseEntity.ok().body(Map.of("message", "Calendrier de remboursement généré"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/repayments")
    @Transactional(readOnly = true)
    public List<Repayment> getLoanRequestRepayments(@PathVariable Long id) {
        return loanRequestService.getRepaymentsByLoanRequest(id);
    }

    @GetMapping("/approved")
    @Transactional(readOnly = true)
    public List<LoanRequest> getApprovedLoans() {
        return loanRequestService.getApprovedLoans();
    }

    @PostMapping("/{id}/force-create-loan")
    @Transactional
    public ResponseEntity<?> forceCreateLoan(@PathVariable Long id) {
        try {
            loanRequestService.forceCreateLoanFromRequest(id);
            return ResponseEntity.ok().body(Map.of("message", "Prêt créé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/treasurer/pending-grant")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LoanRequest>> getApprovedPendingGrant() {
        try {
            List<LoanRequest> pendingGrants = loanRequestService.getApprovedPendingGrant();
            return ResponseEntity.ok(pendingGrants);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/treasurer/granted-loans")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Loan>> getGrantedLoans() {
        try {
            List<Loan> grantedLoans = loanRequestService.getGrantedLoans();
            return ResponseEntity.ok(grantedLoans);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/treasurer/grant")
    @Transactional
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
    @Transactional
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
    @Transactional(readOnly = true)
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

    private String extractUsername(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Map<String, Object> claims = jwt.getClaims();
            if (claims.containsKey("email")) {
                return claims.get("email").toString();
            }
            if (claims.containsKey("preferred_username")) {
                return claims.get("preferred_username").toString();
            }
            return jwt.getSubject();
        }

        return authentication.getName();
    }
}