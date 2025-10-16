package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.LoanRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TreasurerLoanService {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;

    /**
     * Accorder un prêt approuvé (action du trésorier)
     */
    @Transactional
    public Loan grantApprovedLoan(Long loanRequestId, String treasurerComment) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier que la demande est approuvée
        if (!"APPROVED".equals(loanRequest.getStatus())) {
            throw new RuntimeException("Seules les demandes approuvées peuvent être accordées");
        }

        // Vérifier que le prêt n'a pas déjà été accordé
        if (Boolean.TRUE.equals(loanRequest.getLoanGranted())) {
            throw new RuntimeException("Ce prêt a déjà été accordé");
        }

        // Vérifier qu'un prêt n'existe pas déjà
        Optional<Loan> existingLoan = loanRepository.findByLoanRequestId(loanRequestId);
        if (existingLoan.isPresent()) {
            throw new RuntimeException("Un prêt existe déjà pour cette demande");
        }

        try {
            // Créer le prêt
            Loan loan = createLoanFromApprovedRequest(loanRequest);

            // Marquer la demande comme accordée
            loanRequest.setLoanGranted(true);
            loanRequest.setLoanGrantedDate(new Date());
            loanRequest.setTreasurerGrantComment(treasurerComment);
            loanRequestRepository.save(loanRequest);

            log.info("✅ Prêt accordé par le trésorier pour la demande ID: {}", loanRequestId);
            return loan;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'accord du prêt: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'accord du prêt: " + e.getMessage());
        }
    }

    /**
     * Créer un prêt à partir d'une demande approuvée
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

        return loanRepository.save(loan);
    }

    /**
     * Récupérer tous les prêts accordés par le trésorier
     */
    public List<Loan> getGrantedLoans() {
        return loanRepository.findGrantedLoans();
    }

    /**
     * Récupérer les demandes approuvées en attente d'accord du trésorier
     */
    public List<LoanRequest> getApprovedPendingGrant() {
        return loanRequestRepository.findApprovedPendingGrant();
    }

    /**
     * Annuler l'accord d'un prêt (seulement si non remboursé)
     */
    @Transactional
    public void cancelLoanGrant(Long loanRequestId, String reason) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        if (!Boolean.TRUE.equals(loanRequest.getLoanGranted())) {
            throw new RuntimeException("Ce prêt n'a pas été accordé");
        }

        // Vérifier si le prêt a déjà des remboursements
        Optional<Loan> loanOpt = loanRepository.findByLoanRequestId(loanRequestId);
        if (loanOpt.isPresent()) {
            Loan loan = loanOpt.get();
            if (loan.getRepayments() != null && !loan.getRepayments().isEmpty()) {
                throw new RuntimeException("Impossible d'annuler: des remboursements existent déjà");
            }

            // Supprimer le prêt
            loanRepository.delete(loan);
        }

        // Réinitialiser l'accord
        loanRequest.setLoanGranted(false);
        loanRequest.setLoanGrantedDate(null);
        loanRequest.setTreasurerGrantComment("Accord annulé: " + reason);
        loanRequestRepository.save(loanRequest);

        log.info("❌ Accord de prêt annulé pour la demande ID: {}", loanRequestId);
    }

    // Méthodes utilitaires de calcul
    private BigDecimal calculateRepaymentAmount(BigDecimal amount, BigDecimal interestRate, Integer duration) {
        if (amount == null || interestRate == null || duration == null) {
            throw new RuntimeException("Données manquantes pour le calcul du remboursement");
        }

        // Formule : montant + (montant * taux * durée en mois / 12)
        BigDecimal yearlyInterestRate = interestRate.divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalInterest = amount.multiply(yearlyInterestRate)
                .multiply(BigDecimal.valueOf(duration))
                .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);

        return amount.add(totalInterest);
    }

    private Date calculateEndDate(Integer durationInMonths) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(java.util.Calendar.MONTH, durationInMonths);
        return calendar.getTime();
    }
}