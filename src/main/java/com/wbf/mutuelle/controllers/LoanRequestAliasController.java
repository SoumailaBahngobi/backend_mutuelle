package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.LoanRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LoanRequestAliasController {
    private final LoanRequestService loanRequestService;

    // Alias endpoint to support frontend calling /mut/loan-requests/approved (hyphen plural)
    @GetMapping("/mut/loan-requests/approved")
    public ResponseEntity<List<LoanRequest>> getApprovedLoanRequestsAlias() {
        List<LoanRequest> approved = loanRequestService.getApprovedLoans();
        return ResponseEntity.ok(approved);
    }
}
