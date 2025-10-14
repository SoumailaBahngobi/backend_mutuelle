package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.services.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/mut/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

     // Créer un prêt à partir d'une demande approuvée
    @PostMapping("/from-request/{loanRequestId}")
    public ResponseEntity<?> createLoanFromRequest(@PathVariable Long loanRequestId) {
        try {
            Loan loan = loanService.createLoanFromRequest(loanRequestId);
            return ResponseEntity.ok(loan);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
// Récupérer tous les prêts
    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        return ResponseEntity.ok(loans);
    }
     // Récupérer un prêt par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        Optional<Loan> loan = loanService.getLoanById(id);
        return loan.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
     // Récupérer les prêts d'un membre
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Loan>> getLoansByMember(@PathVariable Long memberId) {
        try {
            List<Loan> loans = loanService.getLoansByMember(memberId);
            return ResponseEntity.ok(loans);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
     // Récupérer les prêts non remboursés
    @GetMapping("/unpaid")
    public ResponseEntity<List<Loan>> getUnpaidLoans() {
        List<Loan> loans = loanService.getUnpaidLoans();
        return ResponseEntity.ok(loans);
    }
 //Récupérer les prêts remboursés

    @GetMapping("/paid")
    public ResponseEntity<List<Loan>> getPaidLoans() {
        List<Loan> loans = loanService.getPaidLoans();
        return ResponseEntity.ok(loans);
    }
    // * Récupérer les prêts en cours
    @GetMapping("/active")
    public ResponseEntity<List<Loan>> getActiveLoans() {
        List<Loan> loans = loanService.getActiveLoans();
        return ResponseEntity.ok(loans);
    }
     // Récupérer les prêts échus
    @GetMapping("/overdue")
    public ResponseEntity<List<Loan>> getOverdueLoans() {
        List<Loan> loans = loanService.getOverdueLoans();
        return ResponseEntity.ok(loans);
    }
    // * Marquer un prêt comme remboursé
    @PutMapping("/{id}/mark-repaid")
    public ResponseEntity<?> markAsRepaid(@PathVariable Long id) {
        try {
            Loan loan = loanService.markAsRepaid(id);
            return ResponseEntity.ok(loan);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
     //Mettre à jour un prêt

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLoan(@PathVariable Long id, @RequestBody Loan loanDetails) {
        try {
            Loan updatedLoan = loanService.updateLoan(id, loanDetails);
            return ResponseEntity.ok(updatedLoan);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Supprimer un prêt
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoan(@PathVariable Long id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
// Vérifier si un membre a des prêts en cours

    @GetMapping("/member/{memberId}/has-active-loans")
    public ResponseEntity<Boolean> hasActiveLoans(@PathVariable Long memberId) {
        boolean hasActiveLoans = loanService.hasActiveLoans(memberId);
        return ResponseEntity.ok(hasActiveLoans);
    }
}