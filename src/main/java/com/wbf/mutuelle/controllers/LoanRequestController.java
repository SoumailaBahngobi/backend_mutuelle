package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.LoanRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
        return loanRequestService.getLoanRequestsByMemberEmail(userDetails.getUsername());
    }

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

    @PostMapping("/{id}/approve/president")
    public ResponseEntity<?> approveByPresident(@PathVariable Long id) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByPresident(id);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve/secretary")
    public ResponseEntity<?> approveBySecretary(@PathVariable Long id) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveBySecretary(id);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve/treasurer")
    public ResponseEntity<?> approveByTreasurer(@PathVariable Long id) {
        try {
            LoanRequest approvedRequest = loanRequestService.approveByTreasurer(id);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
/*
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoanRequest(@PathVariable Long id) {
        try {
            LoanRequest rejectedRequest = loanRequestService.rejectLoanRequest(id);
            return ResponseEntity.ok(rejectedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }*/

    //endpoint pour les approbations

    /*@PostMapping("/{id}/approve/president")
    public ResponseEntity<?> approveByPresident(@PathVariable Long id,
                                                @RequestBody Map<String, String> request) {
        try {
            String comment = request.get("comment");
            LoanRequest approvedRequest = loanRequestService.approveByPresident(id, comment);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
*/
   /* @PostMapping("/{id}/approve/secretary")
    public ResponseEntity<?> approveBySecretary(@PathVariable Long id,
                                                @RequestBody Map<String, String> request) {
        try {
            String comment = request.get("comment");
            LoanRequest approvedRequest = loanRequestService.approveBySecretary(id, comment);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }*/
/*
    @PostMapping("/{id}/approve/treasurer")
    public ResponseEntity<?> approveByTreasurer(@PathVariable Long id,
                                                @RequestBody Map<String, String> request) {
        try {
            String comment = request.get("comment");
            LoanRequest approvedRequest = loanRequestService.approveByTreasurer(id, comment);
            return ResponseEntity.ok(approvedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }*/

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


}