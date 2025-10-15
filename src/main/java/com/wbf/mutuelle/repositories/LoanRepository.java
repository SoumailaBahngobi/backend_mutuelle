package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Trouver tous les prêts d'un membre
    List<Loan> findByMember(Member member);

    // Trouver les prêts par statut de remboursement
    List<Loan> findByIsRepaid(Boolean isRepaid);

    // Trouver un prêt par la demande de prêt associée
    Optional<Loan> findByLoanRequestId(Long loanRequestId);

    // Compter les prêts non remboursés d'un membre
    Long countByMemberAndIsRepaid(Member member, Boolean isRepaid);

    // Trouver les prêts avec montant supérieur à une valeur
    List<Loan> findByAmountGreaterThan(BigDecimal amount);

    // Requête personnalisée pour les prêts en cours (non remboursés et date de fin non dépassée)
    @Query("SELECT l FROM Loan l WHERE l.isRepaid = false AND l.endDate > CURRENT_DATE")
    List<Loan> findActiveLoans();

    // Requête pour les prêts échus mais non remboursés
    @Query("SELECT l FROM Loan l WHERE l.isRepaid = false AND l.endDate <= CURRENT_DATE")
    List<Loan> findOverdueLoans();

    // Trouver les prêts par taux d'intérêt
    List<Loan> findByInterestRateGreaterThan(BigDecimal interestRate);
}