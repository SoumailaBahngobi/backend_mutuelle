package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.services.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LoanAliasController {
    private final LoanService loanService;

    // Alias endpoint to support frontend calling /mut/loan (singular)
    @GetMapping("/mut/loan")
    public ResponseEntity<List<Loan>> getLoansAlias() {
        List<Loan> loans = loanService.getAllLoans();
        return ResponseEntity.ok(loans);
    }
}
