package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.repositories.RepaymentRepository;
import com.wbf.mutuelle.repositories.LoanRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;
    private final NotificationService notificationService;

    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }

    public Optional<Repayment> getRepaymentById(Long id) {
        return repaymentRepository.findById(id);
    }

    public List<Repayment> getRepaymentsByLoanRequest(Long loanRequestId) {
        return repaymentRepository.findByLoanRequestId(loanRequestId);
    }

    @Transactional
    public Repayment createRepayment(Repayment repayment) {
        // Support repayment tied to either a LoanRequest or a Loan
        if (repayment.getLoanRequest() != null && repayment.getLoanRequest().getId() != null) {
            LoanRequest loanRequest = loanRequestRepository.findById(repayment.getLoanRequest().getId())
                    .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

            BigDecimal totalRepaid = repaymentRepository.getTotalRepaidAmount(loanRequest.getId());
            BigDecimal remainingAmount = loanRequest.getRequestAmount().subtract(totalRepaid != null ? totalRepaid : BigDecimal.ZERO);

            if (repayment.getAmount().compareTo(remainingAmount) > 0) {
                throw new RuntimeException("Le montant du remboursement dépasse le montant restant du prêt");
            }

            Repayment saved = repaymentRepository.save(repayment);
            // If fully repaid, update loanRequest
            if (getTotalRepaidAmount(loanRequest.getId()).compareTo(loanRequest.getRequestAmount()) >= 0) {
                loanRequest.setIsRepaid(true);
                loanRequestRepository.save(loanRequest);
                // notify
                if (loanRequest.getMember() != null) {
                    notificationService.notifyLoanStatusChange(loanRequest.getMember().getEmail(), "REPAID", "Demande de prêt remboursée");
                }
            }

            return saved;
        } else if (repayment.getLoan() != null && repayment.getLoan().getId() != null) {
            Loan loan = loanRepository.findById(repayment.getLoan().getId())
                    .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));

            BigDecimal remaining = loan.calculateRemainingBalance();
            if (repayment.getAmount().compareTo(remaining) > 0) {
                throw new RuntimeException("Le montant du remboursement dépasse le montant restant du prêt");
            }

            Repayment saved = repaymentRepository.save(repayment);

            // Update loan status
            loan.getRepayments().add(saved);
            loan.updateLoanStatus();
            loanRepository.save(loan);

            if (loan.isFullyPaid()) {
                // mark associated loan request if present
                if (loan.getLoanRequest() != null) {
                    LoanRequest lr = loan.getLoanRequest();
                    lr.setIsRepaid(true);
                    loanRequestRepository.save(lr);
                }
                if (loan.getMember() != null) {
                    notificationService.notifyLoanStatusChange(loan.getMember().getEmail(), "REPAID", "Prêt remboursé: " + loan.getId());
                }
            }

            return saved;
        }

        throw new RuntimeException("La demande de prêt ou le prêt doit être fourni pour créer un remboursement");
    }

    @Transactional
    public void generateRepaymentSchedule(LoanRequest loanRequest) {
        // Générer le plan de remboursement pour une demande de prêt approuvée
        //BigDecimal loanAmount = loanRequest.getAmount();
        BigDecimal loanAmount = loanRequest.getRequestAmount();
        Integer duration = loanRequest.getDuration(); // en mois
    BigDecimal monthlyAmount = loanAmount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date()); // Date de début = aujourd'hui

        for (int i = 1; i <= duration; i++) {
            calendar.add(Calendar.MONTH, 1);

            Repayment repayment = new Repayment();
            repayment.setAmount(monthlyAmount);
            repayment.setDueDate(calendar.getTime());
            repayment.setInstallmentNumber(i);
            repayment.setStatus("PENDING");
            repayment.setLoanRequest(loanRequest);

            repaymentRepository.save(repayment);
        }
    }

    @Transactional
    public Repayment processRepayment(Long repaymentId, BigDecimal amountPaid) {
        Repayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        if (!"PENDING".equals(repayment.getStatus()) && !"OVERDUE".equals(repayment.getStatus())) {
            throw new RuntimeException("Ce remboursement a déjà été traité");
        }

        if (amountPaid.compareTo(repayment.getAmount()) >= 0) {
            repayment.setStatus("PAID");
            repayment.setRepaymentDate(new Date());

            // Si paiement supérieur au montant dû, gérer l'excédent
            if (amountPaid.compareTo(repayment.getAmount()) > 0) {
                // Appliquer l'excédent au prochain remboursement en attente
                if (repayment.getLoanRequest() != null) {
                    applyOverpayment(repayment.getLoanRequest().getId(),
                            amountPaid.subtract(repayment.getAmount()));
                } else if (repayment.getLoan() != null) {
                    applyOverpaymentForLoan(repayment.getLoan().getId(),
                            amountPaid.subtract(repayment.getAmount()));
                }
            }
        } else {
            throw new RuntimeException("Le montant payé est inférieur au montant dû");
        }

        return repaymentRepository.save(repayment);
    }

    @Transactional
    public void createInstallmentsForLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));

        BigDecimal remaining = loan.calculateRemainingBalance();
        Integer duration = loan.getDuration() != null && loan.getDuration() > 0 ? loan.getDuration() : 1;
        BigDecimal monthly = remaining.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        for (int i = 1; i <= duration; i++) {
            calendar.add(Calendar.MONTH, 1);
            Repayment r = new Repayment();
            r.setAmount(monthly);
            r.setDueDate(calendar.getTime());
            r.setInstallmentNumber(i);
            r.setTotalInstallments(duration);
            r.setStatus("PENDING");
            r.setLoan(loan);
            r.setLoanRequest(loan.getLoanRequest());
            repaymentRepository.save(r);
        }
    }

    @Transactional
    public void payFullLoan(Long loanId, String paymentMethod, String transactionReference) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));

        BigDecimal remaining = loan.calculateRemainingBalance();
        Repayment repayment = new Repayment();
        repayment.setAmount(remaining);
        repayment.setRepaymentDate(new Date());
        repayment.setStatus("PAID");
        repayment.setPaymentMethod(paymentMethod);
        repayment.setTransactionReference(transactionReference);
        repayment.setLoan(loan);
        repayment.setLoanRequest(loan.getLoanRequest());

        repaymentRepository.save(repayment);

        loan.getRepayments().add(repayment);
        loan.updateLoanStatus();
        loanRepository.save(loan);

        if (loan.isFullyPaid()) {
            if (loan.getLoanRequest() != null) {
                LoanRequest lr = loan.getLoanRequest();
                lr.setIsRepaid(true);
                loanRequestRepository.save(lr);
            }
            if (loan.getMember() != null) {
                notificationService.notifyLoanStatusChange(loan.getMember().getEmail(), "REPAID", "Paiement intégral reçu: " + remaining);
            }
        }
    }

    private void applyOverpayment(Long loanRequestId, BigDecimal overpayment) {
        // Trouver le prochain remboursement en attente
        Optional<Repayment> nextRepayment = repaymentRepository
                .findFirstByLoanRequestIdAndStatusOrderByDueDateAsc(loanRequestId, "PENDING");

        if (nextRepayment.isPresent()) {
            Repayment repayment = nextRepayment.get();
            repayment.setAmount(repayment.getAmount().subtract(overpayment));

            if (repayment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                repayment.setStatus("PAID");
                repayment.setRepaymentDate(new Date());

                // Réappliquer l'excédent restant
                BigDecimal remainingOverpayment = repayment.getAmount().abs();
                if (remainingOverpayment.compareTo(BigDecimal.ZERO) > 0) {
                    applyOverpayment(loanRequestId, remainingOverpayment);
                }
            }

            repaymentRepository.save(repayment);
        }
    }

    private void applyOverpaymentForLoan(Long loanId, BigDecimal overpayment) {
        Optional<Repayment> nextRepayment = repaymentRepository
                .findFirstByLoanIdAndStatusOrderByDueDateAsc(loanId, "PENDING");

        if (nextRepayment.isPresent()) {
            Repayment repayment = nextRepayment.get();
            BigDecimal newAmount = repayment.getAmount().subtract(overpayment);
            // If overpayment covers this repayment
            if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                repayment.setStatus("PAID");
                repayment.setRepaymentDate(new Date());

                // Save and propagate remaining overpayment
                repaymentRepository.save(repayment);
                BigDecimal remainingOver = overpayment.subtract(repayment.getAmount());
                if (remainingOver.compareTo(BigDecimal.ZERO) > 0) {
                    applyOverpaymentForLoan(loanId, remainingOver);
                }
            } else {
                repayment.setAmount(newAmount);
                repaymentRepository.save(repayment);
            }
        }
    }

    public BigDecimal getTotalRepaidAmount(Long loanRequestId) {
        BigDecimal total = repaymentRepository.getTotalRepaidAmount(loanRequestId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getRemainingAmount(Long loanRequestId) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        BigDecimal totalRepaid = getTotalRepaidAmount(loanRequestId);
       // return loanRequest.getAmount().subtract(totalRepaid);
        return loanRequest.getRequestAmount().subtract(totalRepaid);
    }

    public Repayment updateRepayment(Long id, Repayment repaymentDetails) {
        Repayment repayment = repaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));

        repayment.setAmount(repaymentDetails.getAmount());
        repayment.setStatus(repaymentDetails.getStatus());
        repayment.setDueDate(repaymentDetails.getDueDate());
        repayment.setRepaymentDate(repaymentDetails.getRepaymentDate());

        return repaymentRepository.save(repayment);
    }

    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }
}