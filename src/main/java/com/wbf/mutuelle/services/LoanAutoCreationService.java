package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.repositories.LoanRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanAutoCreationService {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;

    /**
     * Vérifie si une demande est entièrement approuvée et crée automatiquement le prêt
     */
    @Transactional
    public void checkAndCreateLoan(Long loanRequestId) {
        Optional<LoanRequest> loanRequestOpt = loanRequestRepository.findById(loanRequestId);

        if (loanRequestOpt.isEmpty()) {
            log.warn("Demande de prêt non trouvée avec l'ID: {}", loanRequestId);
            return;
        }

        LoanRequest loanRequest = loanRequestOpt.get();

        // Vérifier si la demande est entièrement approuvée
        if (isFullyApproved(loanRequest)) {
            // Vérifier si un prêt existe déjà pour cette demande
            boolean loanExists = loanRepository.findByLoanRequestId(loanRequestId).isPresent();
            boolean loanAlreadyCreated = Boolean.TRUE.equals(loanRequest.getLoanCreated());

            if (!loanExists && !loanAlreadyCreated) {
                try {
                    // Créer le prêt automatiquement
                    createLoanFromApprovedRequest(loanRequest);
                    log.info("✅ Prêt créé automatiquement pour la demande approuvée ID: {}", loanRequestId);
                } catch (Exception e) {
                    log.error("❌ Erreur lors de la création automatique du prêt: {}", e.getMessage());
                    throw new RuntimeException("Erreur lors de la création automatique du prêt: " + e.getMessage());
                }
            } else {
                log.info("ℹ️ Prêt déjà créé ou existant pour la demande ID: {}", loanRequestId);
            }
        } else {
            log.debug("Demande ID: {} pas encore entièrement approuvée", loanRequestId);
        }
    }

    /**
     * Vérifie si une demande est entièrement approuvée
     */
    public boolean isFullyApproved(LoanRequest loanRequest) {
    // Only consider the three approval flags. The status is set separately by
    // the LoanRequestService when the request is fully approved.
    return Boolean.TRUE.equals(loanRequest.getPresidentApproved()) &&
        Boolean.TRUE.equals(loanRequest.getSecretaryApproved()) &&
        Boolean.TRUE.equals(loanRequest.getTreasurerApproved());
    }

    /**
     * Crée un prêt à partir d'une demande approuvée
     */
    private Loan createLoanFromApprovedRequest(LoanRequest loanRequest) {
        // Calculer le montant à rembourser
        BigDecimal repaymentAmount = calculateRepaymentAmount(
                loanRequest.getRequestAmount(),
                loanRequest.getInterestRate(),
                loanRequest.getDuration()
        );

        // Calculer la date de fin
        Date endDate = calculateEndDate(loanRequest.getDuration());

        // Créer le prêt
        Loan loan = new Loan();
        loan.setAmount(loanRequest.getRequestAmount());
        loan.setDuration(loanRequest.getDuration());
        loan.setBeginDate(new Date());
        loan.setEndDate(endDate);
        loan.setRepaymentAmount(repaymentAmount);
        loan.setInterestRate(loanRequest.getInterestRate());
        loan.setMember(loanRequest.getMember());
        loan.setLoanRequest(loanRequest);
        loan.setIsRepaid(false);

        Loan savedLoan = loanRepository.save(loan);

        // Marquer la demande comme ayant un prêt créé
        loanRequest.setLoanCreated(true);
        loanRequestRepository.save(loanRequest);

        log.info("🎉 Prêt créé avec succès pour le membre: {} - Montant: {} FCFA",
                loanRequest.getMember().getId(), loanRequest.getRequestAmount());

        return savedLoan;
    }

    private BigDecimal calculateRepaymentAmount(BigDecimal amount, BigDecimal interestRate, Integer duration) {
        if (amount == null || interestRate == null || duration == null) {
            throw new RuntimeException("Données manquantes pour le calcul du remboursement");
        }

        // Formule : montant + (montant * taux * durée en mois / 12)
        BigDecimal monthlyInterestRate = interestRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal totalInterest = amount.multiply(monthlyInterestRate)
                .multiply(BigDecimal.valueOf(duration));

        return amount.add(totalInterest).setScale(2, RoundingMode.HALF_UP);
    }

    private Date calculateEndDate(Integer durationInMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, durationInMonths);
        return calendar.getTime();
    }

    /**
     * Méthode pour forcer la création d'un prêt (en cas de problème)
     */
    @Transactional
    public Loan forceCreateLoan(Long loanRequestId) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        if (!isFullyApproved(loanRequest)) {
            throw new RuntimeException("La demande n'est pas entièrement approuvée");
        }

        return createLoanFromApprovedRequest(loanRequest);
    }
}