package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.services.TreasurerLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mutuelle/treasurer/loans")
@RequiredArgsConstructor
public class TreasurerLoanController {

    private final TreasurerLoanService treasurerLoanService;

    /**
     * Accorder un prêt approuvé
     */
    @PostMapping("/grant/{loanRequestId}")
    public ResponseEntity<?> grantLoan(@PathVariable Long loanRequestId,
                                       @RequestBody Map<String, String> request) {
        try {
            String comment = request.get("comment");
            Loan loan = treasurerLoanService.grantApprovedLoan(loanRequestId, comment);
            return ResponseEntity.ok(loan);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupérer les prêts accordés
     */
    @GetMapping("/granted")
    public ResponseEntity<List<Loan>> getGrantedLoans() {
        List<Loan> loans = treasurerLoanService.getGrantedLoans();
        return ResponseEntity.ok(loans);
    }

    /**
     * Récupérer les demandes approuvées en attente d'accord
     */
    @GetMapping("/pending-grant")
    public ResponseEntity<List<LoanRequest>> getApprovedPendingGrant() {
        List<LoanRequest> requests = treasurerLoanService.getApprovedPendingGrant();
        return ResponseEntity.ok(requests);
    }

    /**
     * Annuler l'accord d'un prêt
     */
    @PostMapping("/cancel-grant/{loanRequestId}")
    public ResponseEntity<?> cancelLoanGrant(@PathVariable Long loanRequestId,
                                             @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            treasurerLoanService.cancelLoanGrant(loanRequestId, reason);
            return ResponseEntity.ok().body(Map.of("message", "Accord de prêt annulé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tableau de bord du trésorier
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getTreasurerDashboard() {
        try {
            Map<String, Object> dashboard = Map.of(
                    "pendingGrants", treasurerLoanService.getApprovedPendingGrant().size(),
                    "grantedLoans", treasurerLoanService.getGrantedLoans().size(),
                    "pendingGrantsList", treasurerLoanService.getApprovedPendingGrant(),
                    "grantedLoansList", treasurerLoanService.getGrantedLoans()
            );
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}