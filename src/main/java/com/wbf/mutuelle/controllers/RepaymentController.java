package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.services.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/mut/repayment")
@RequiredArgsConstructor
public class RepaymentController {

    private final RepaymentService repaymentService;

    @GetMapping
    public List<Repayment> getAllRepayments() {
        return repaymentService.getAllRepayments();
    }

    @GetMapping("/{id}")
    public Repayment getRepaymentById(@PathVariable Long id) {
        return repaymentService.getRepaymentById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));
    }

    @GetMapping("/loan-request/{loanRequestId}")
    public List<Repayment> getRepaymentsByLoanRequest(@PathVariable Long loanRequestId) {
        return repaymentService.getRepaymentsByLoanRequest(loanRequestId);
    }

    @PostMapping
    public Repayment createRepayment(@RequestBody Repayment repayment) {
        return repaymentService.createRepayment(repayment);
    }

    @PostMapping("/generate-for-loan/{loanId}")
    public void generateRepaymentScheduleForLoan(@PathVariable Long loanId) {
        repaymentService.createInstallmentsForLoan(loanId);
    }

    @PostMapping("/pay-full/{loanId}")
    public void payFullLoan(@PathVariable Long loanId,
                            @RequestParam(required = false) String paymentMethod,
                            @RequestParam(required = false) String transactionReference) {
        repaymentService.payFullLoan(loanId, paymentMethod, transactionReference);
    }
    @PostMapping("/{id}/process")
    public Repayment processRepayment(@PathVariable Long id, @RequestParam BigDecimal amountPaid) {
        return repaymentService.processRepayment(id, amountPaid);
    }

    @GetMapping("/loan-request/{loanRequestId}/total-repaid")
    public BigDecimal getTotalRepaidAmount(@PathVariable Long loanRequestId) {
        return repaymentService.getTotalRepaidAmount(loanRequestId);
    }

    @GetMapping("/loan-request/{loanRequestId}/remaining")
    public BigDecimal getRemainingAmount(@PathVariable Long loanRequestId) {
        return repaymentService.getRemainingAmount(loanRequestId);
    }

    @PutMapping("/{id}")
    public Repayment updateRepayment(@PathVariable Long id, @RequestBody Repayment repayment) {
        return repaymentService.updateRepayment(id, repayment);
    }

    @DeleteMapping("/{id}")
    public void deleteRepayment(@PathVariable Long id) {
        repaymentService.deleteRepayment(id);
    }
}