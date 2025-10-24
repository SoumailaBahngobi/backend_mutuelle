package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.services.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/mut/repayment")
@RequiredArgsConstructor
public class RepaymentController {

    private final RepaymentService repaymentService;

    // Version paginée pour éviter les gros JSON
    @GetMapping
    public ResponseEntity<Page<Repayment>> getAllRepayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Repayment> repayments = repaymentService.getAllRepayments(pageable);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Repayment> getRepaymentById(@PathVariable Long id) {
        try {
            return repaymentService.getRepaymentById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/loan-request/{loanRequestId}")
    public ResponseEntity<List<Repayment>> getRepaymentsByLoanRequest(@PathVariable Long loanRequestId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentsByLoanRequest(loanRequestId);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Repayment>> getRepaymentsByLoan(@PathVariable Long loanId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentsByLoan(loanId);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Nouveaux endpoints pour l'historique et l'exportation
    @GetMapping("/history")
    public ResponseEntity<List<Repayment>> getRepaymentHistory(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Long memberId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentHistory(statuses, memberId);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(@RequestParam(required = false) List<String> statuses,
                                              @RequestParam(required = false) Long memberId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentHistory(statuses, memberId);
            byte[] pdfBytes = repaymentService.exportRepaymentsToPdf(repayments);

            String filename = "historique-remboursements-" +
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) List<String> statuses,
                                                @RequestParam(required = false) Long memberId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentHistory(statuses, memberId);
            byte[] excelBytes = repaymentService.exportRepaymentsToExcel(repayments);

            String filename = "historique-remboursements-" +
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv(@RequestParam(required = false) List<String> statuses,
                                              @RequestParam(required = false) Long memberId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentHistory(statuses, memberId);
            byte[] csvBytes = repaymentService.exportRepaymentsToCsv(repayments);

            String filename = "historique-remboursements-" +
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Repayment> createRepayment(@RequestBody Repayment repayment) {
        try {
            Repayment createdRepayment = repaymentService.createRepayment(repayment);
            return ResponseEntity.ok(createdRepayment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/generate-for-loan/{loanId}")
    public ResponseEntity<String> generateRepaymentScheduleForLoan(@PathVariable Long loanId) {
        try {
            repaymentService.createInstallmentsForLoan(loanId);
            return ResponseEntity.ok("Plan de remboursement généré avec succès pour le prêt " + loanId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la génération du plan de remboursement: " + e.getMessage());
        }
    }

    @PostMapping("/pay-full/{loanId}")
    public ResponseEntity<String> payFullLoan(@PathVariable Long loanId,
                                              @RequestParam(required = false) String paymentMethod,
                                              @RequestParam(required = false) String transactionReference) {
        try {
            repaymentService.payFullLoan(loanId,
                    paymentMethod != null ? paymentMethod : "CASH",
                    transactionReference != null ? transactionReference : "MANUAL_" + System.currentTimeMillis());
            return ResponseEntity.ok("Paiement intégral effectué avec succès pour le prêt " + loanId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du paiement intégral: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Repayment> processRepayment(@PathVariable Long id,
                                                      @RequestParam BigDecimal amountPaid,
                                                      @RequestParam(required = false) String paymentMethod,
                                                      @RequestParam(required = false) String transactionReference) {
        try {
            Repayment processedRepayment = repaymentService.processRepayment(id, amountPaid,
                    paymentMethod != null ? paymentMethod : "CASH",
                    transactionReference != null ? transactionReference : "MANUAL_" + System.currentTimeMillis());
            return ResponseEntity.ok(processedRepayment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/loan-request/{loanRequestId}/total-repaid")
    public ResponseEntity<BigDecimal> getTotalRepaidAmount(@PathVariable Long loanRequestId) {
        try {
            BigDecimal total = repaymentService.getTotalRepaidAmount(loanRequestId);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/loan-request/{loanRequestId}/remaining")
    public ResponseEntity<BigDecimal> getRemainingAmount(@PathVariable Long loanRequestId) {
        try {
            BigDecimal remaining = repaymentService.getRemainingAmount(loanRequestId);
            return ResponseEntity.ok(remaining);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Repayment> updateRepayment(@PathVariable Long id, @RequestBody Repayment repayment) {
        try {
            Repayment updatedRepayment = repaymentService.updateRepayment(id, repayment);
            return ResponseEntity.ok(updatedRepayment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRepayment(@PathVariable Long id) {
        try {
            repaymentService.deleteRepayment(id);
            return ResponseEntity.ok("Remboursement supprimé avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du remboursement: " + e.getMessage());
        }
    }

    // Nouveaux endpoints pour les fonctionnalités avancées
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Repayment>> getRepaymentsByMember(@PathVariable Long memberId) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentsByMember(memberId);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Repayment>> getRepaymentsByStatus(@PathVariable String status) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentsByStatus(status);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Repayment>> getOverdueRepayments() {
        try {
            List<Repayment> repayments = repaymentService.getOverdueRepayments();
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/mark-overdue")
    public ResponseEntity<String> markOverdueRepayments() {
        try {
            repaymentService.markOverdueRepayments();
            return ResponseEntity.ok("Remboursements en retard marqués avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du marquage des remboursements en retard: " + e.getMessage());
        }
    }

    @GetMapping("/member/{memberId}/total-repaid")
    public ResponseEntity<BigDecimal> getTotalRepaidAmountByMember(@PathVariable Long memberId) {
        try {
            BigDecimal total = repaymentService.getTotalRepaidAmountByMember(memberId);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/member/{memberId}/pending-amount")
    public ResponseEntity<BigDecimal> getPendingAmountByMember(@PathVariable Long memberId) {
        try {
            BigDecimal pending = repaymentService.getPendingAmountByMember(memberId);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/member/{memberId}/next-due")
    public ResponseEntity<Repayment> getNextDueRepaymentByMember(@PathVariable Long memberId) {
        try {
            return repaymentService.getNextDueRepaymentByMember(memberId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/filters")
    public ResponseEntity<Page<Repayment>> getRepaymentsWithFilters(
            @RequestParam(required = false) Long loanRequestId,
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Repayment> repayments = repaymentService.getRepaymentsWithFilters(loanRequestId, loanId, status, memberId, pageable);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats/period")
    public ResponseEntity<BigDecimal> getTotalRepaidAmountByPeriod(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            BigDecimal total = repaymentService.getTotalRepaidAmountByPeriod(start, end);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/transaction/{reference}/exists")
    public ResponseEntity<Boolean> isTransactionReferenceExists(@PathVariable String reference) {
        try {
            boolean exists = repaymentService.isTransactionReferenceExists(reference);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/loan-request/{loanRequestId}/status/{status}")
    public ResponseEntity<List<Repayment>> getRepaymentsByLoanRequestAndStatus(@PathVariable Long loanRequestId,
                                                                               @PathVariable String status) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentsByLoanRequestAndStatus(loanRequestId, status);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/loan/{loanId}/status/{status}")
    public ResponseEntity<List<Repayment>> getRepaymentsByLoanAndStatus(@PathVariable Long loanId,
                                                                        @PathVariable String status) {
        try {
            List<Repayment> repayments = repaymentService.getRepaymentsByLoanAndStatus(loanId, status);
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats/count-by-status/{status}")
    public ResponseEntity<Long> countRepaymentsByStatus(@PathVariable String status) {
        try {
            Long count = repaymentService.countRepaymentsByStatus(status);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint de test pour diagnostiquer les problèmes
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        try {
            long count = repaymentService.countRepaymentsByStatus("PENDING");
            return ResponseEntity.ok("Service OK - " + count + " remboursements PENDING trouvés");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Endpoint simplifié sans pagination pour le frontend
    @GetMapping("/simple")
    public ResponseEntity<List<Repayment>> getAllRepaymentsSimple() {
        try {
            List<Repayment> repayments = repaymentService.getAllRepaymentsSimple();
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    ////////////////////////////


}