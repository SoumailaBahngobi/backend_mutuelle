/*package com.wbf.mutuelle.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.LoanRequestService;

@RestController
@RequestMapping("/mut/loan_request")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

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

    @GetMapping("/my_requests")
    public List<LoanRequest> getMyLoanRequests(@AuthenticationPrincipal UserDetails userDetails) {
        // Cette méthode nécessiterait une relation entre UserDetails et Member
        // Pour simplifier, nous allons créer un endpoint pour récupérer par email
        // Dans une implémentation réelle, vous devriez avoir un service pour récupérer le membre par email
        return List.of(); // Placeholder
    }


    @PutMapping("/{id}")
    public ResponseEntity<LoanRequest> updateLoanRequest(@PathVariable Long id, @RequestBody LoanRequest loanRequest) {
        try {
            LoanRequest updatedRequest = loanRequestService.updateLoanRequest(id, loanRequest);
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoanRequest(@PathVariable Long id) {
        try {
            loanRequestService.deleteLoanRequest(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanRequest> rejectLoanRequest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(loanRequestService.rejectLoanRequest(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public LoanRequestController(LoanRequestService loanRequestService) {
        this.loanRequestService = loanRequestService;
    }

    @PostMapping
    public ResponseEntity<LoanRequest> createLoanRequest(@RequestBody LoanRequest loanRequest,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            LoanRequest createdRequest = loanRequestService.createLoanRequest(loanRequest, userDetails.getUsername());
            return ResponseEntity.ok(createdRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanRequest> approveLoanRequest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(loanRequestService.approveLoanRequest(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<LoanRequest>> getLoanRequestsByMember(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(loanRequestService.getLoanRequestsByMemberId(memberId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
 */

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