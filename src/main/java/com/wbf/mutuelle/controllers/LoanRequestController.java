package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.LoanRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoanRequest(@PathVariable Long id) {
        try {
            LoanRequest rejectedRequest = loanRequestService.rejectLoanRequest(id);
            return ResponseEntity.ok(rejectedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}