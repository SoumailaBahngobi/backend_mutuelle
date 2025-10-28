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
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanRequestRepository loanRequestRepository;

    @Transactional
    public Loan createLoanFromRequest(Long loanRequestId) {
        Optional<LoanRequest> loanRequestOpt = loanRequestRepository.findById(loanRequestId);

        if (loanRequestOpt.isEmpty()) {
            throw new RuntimeException("Demande de prêt non trouvée avec l'ID: " + loanRequestId);
        }

        LoanRequest loanRequest = loanRequestOpt.get();

        // Vérifier si la demande est approuvée
        if (!loanRequest.isFullyApproved()) {
            throw new RuntimeException("La demande de prêt n'est pas encore approuvée");
        }

        // Vérifier si un prêt existe déjà pour cette demande
        Optional<Loan> existingLoan = loanRepository.findByLoanRequestId(loanRequestId);
        if (existingLoan.isPresent()) {
            throw new RuntimeException("Un prêt existe déjà pour cette demande");
        }

        // Calculer le montant à rembourser par le membre
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
        loan.setInterestRate(loanRequest.getInterestRate()); // Ajout du taux d'intérêt
        loan.setMember(loanRequest.getMember());
        loan.setLoanRequest(loanRequest);
        loan.setIsRepaid(false);

        Loan savedLoan = loanRepository.save(loan);
        log.info("Prêt créé avec succès pour le membre: {}", loanRequest.getMember().getId());

        return savedLoan;
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    public List<Loan> getLoansByMember(Long memberId) {
        Optional<Member> member = memberRepository.findById(memberId);
        if (member.isEmpty()) {
            throw new RuntimeException("Membre non trouvé avec l'ID: " + memberId);
        }
        return loanRepository.findByMember(member.get());
    }

    public List<Loan> getUnpaidLoans() {
        return loanRepository.findByIsRepaid(false);
    }

    public List<Loan> getPaidLoans() {
        return loanRepository.findByIsRepaid(true);
    }

    @Transactional
    public Loan markAsRepaid(Long loanId) {
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (loanOpt.isEmpty()) {
            throw new RuntimeException("Prêt non trouvé avec l'ID: " + loanId);
        }

        Loan loan = loanOpt.get();
        loan.setIsRepaid(true);

        // Marquer également la demande de prêt comme remboursée
        if (loan.getLoanRequest() != null) {
            loan.getLoanRequest().setIsRepaid(true);
        }

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan updateLoan(Long loanId, Loan loanDetails) {
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (loanOpt.isEmpty()) {
            throw new RuntimeException("Prêt non trouvé avec l'ID: " + loanId);
        }

        Loan loan = loanOpt.get();

        // Mettre à jour les champs modifiables
        if (loanDetails.getAmount() != null) {
            loan.setAmount(loanDetails.getAmount());
        }
        if (loanDetails.getDuration() != null) {
            loan.setDuration(loanDetails.getDuration());
            // Recalculer la date de fin si la durée change
            loan.setEndDate(calculateEndDate(loanDetails.getDuration()));
        }
        if (loanDetails.getInterestRate() != null) {
            loan.setInterestRate(loanDetails.getInterestRate());
            // Recalculer le montant de remboursement si le taux change
            loan.setRepaymentAmount(calculateRepaymentAmount(
                    loan.getAmount(),
                    loanDetails.getInterestRate(),
                    loan.getDuration()
            ));
        }
        if (loanDetails.getIsRepaid() != null) {
            loan.setIsRepaid(loanDetails.getIsRepaid());
        }

        return loanRepository.save(loan);
    }

    @Transactional
    public void deleteLoan(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new RuntimeException("Prêt non trouvé avec l'ID: " + loanId);
        }
        loanRepository.deleteById(loanId);
    }

    private BigDecimal calculateRepaymentAmount(BigDecimal amount, BigDecimal interestRate, Integer duration) {
        // Formule corrigée : montant + (montant * taux * durée en mois / 12)
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

    public List<Loan> getActiveLoans() {
        return loanRepository.findActiveLoans();
    }

    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans();
    }

    public boolean hasActiveLoans(Long memberId) {
        Optional<Member> member = memberRepository.findById(memberId);
        if (member.isEmpty()) {
            return false;
        }
        Long activeLoansCount = loanRepository.countByMemberAndIsRepaid(member.get(), false);
        return activeLoansCount > 0;
    }
}