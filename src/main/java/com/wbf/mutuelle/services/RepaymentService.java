package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.repositories.RepaymentRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRequestRepository loanRequestRepository;

    public RepaymentService(RepaymentRepository repaymentRepository,
                            LoanRequestRepository loanRequestRepository) {
        this.repaymentRepository = repaymentRepository;
        this.loanRequestRepository = loanRequestRepository;
    }

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
        // Validation du montant et de la demande de prêt
        if (repayment.getLoanRequest() == null || repayment.getLoanRequest().getId() == null) {
            throw new RuntimeException("La demande de prêt est obligatoire");
        }

        LoanRequest loanRequest = loanRequestRepository.findById(repayment.getLoanRequest().getId())
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier que le montant ne dépasse pas le montant restant du prêt
        BigDecimal totalRepaid = repaymentRepository.getTotalRepaidAmount(loanRequest.getId());
        //BigDecimal remainingAmount = loanRequest.getAmount().subtract(totalRepaid != null ? totalRepaid : BigDecimal.ZERO);
        BigDecimal remainingAmount = loanRequest.getRequestAmount().subtract(totalRepaid != null ? totalRepaid : BigDecimal.ZERO);

        if (repayment.getAmount().compareTo(remainingAmount) > 0) {
            throw new RuntimeException("Le montant du remboursement dépasse le montant restant du prêt");
        }

        return repaymentRepository.save(repayment);
    }

    @Transactional
    public void generateRepaymentSchedule(LoanRequest loanRequest) {
        // Générer le plan de remboursement pour une demande de prêt approuvée
        //BigDecimal loanAmount = loanRequest.getAmount();
        BigDecimal loanAmount = loanRequest.getRequestAmount();
        Integer duration = loanRequest.getDuration(); // en mois
        BigDecimal monthlyAmount = loanAmount.divide(BigDecimal.valueOf(duration), 2, BigDecimal.ROUND_HALF_UP);

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
                applyOverpayment(repayment.getLoanRequest().getId(),
                        amountPaid.subtract(repayment.getAmount()));
            }
        } else {
            throw new RuntimeException("Le montant payé est inférieur au montant dû");
        }

        return repaymentRepository.save(repayment);
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