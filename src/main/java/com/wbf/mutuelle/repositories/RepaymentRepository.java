package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    // Méthodes existantes pour LoanRequest
    List<Repayment> findByLoanRequestId(Long loanRequestId);
    List<Repayment> findByLoanRequestIdAndStatus(Long loanRequestId, String status);
    Optional<Repayment> findFirstByLoanRequestIdAndStatusOrderByDueDateAsc(Long loanRequestId, String status);

    // Méthodes existantes pour Loan
    List<Repayment> findByLoanId(Long loanId);
    Optional<Repayment> findFirstByLoanIdAndStatusOrderByDueDateAsc(Long loanId, String status);

    // Méthodes de statut
    List<Repayment> findByStatus(String status);
    List<Repayment> findByStatusIn(List<String> statuses);

    @Query("SELECT r FROM Repayment r WHERE r.status IN :statuses ORDER BY r.dueDate DESC")
    List<Repayment> findByStatusesOrdered(@Param("statuses") List<String> statuses);

    // Méthodes pour membre (LoanRequest)
    List<Repayment> findByLoanRequestMemberIdAndStatusIn(Long memberId, List<String> statuses);

    @Query("SELECT r FROM Repayment r WHERE r.loan.member.id = :memberId AND r.status IN :statuses ORDER BY r.dueDate DESC")
    List<Repayment> findByLoanMemberIdAndStatuses(@Param("memberId") Long memberId, @Param("statuses") List<String> statuses);

    // Méthodes de calcul des montants
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Repayment r WHERE r.loanRequest.id = :loanRequestId AND r.status = 'PAID'")
    BigDecimal getTotalRepaidAmount(@Param("loanRequestId") Long loanRequestId);

    // Nouvelles méthodes pour supporter les fonctionnalités avancées

    // Recherche par membre (LoanRequest ou Loan)
    @Query("SELECT r FROM Repayment r WHERE r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId")
    List<Repayment> findByLoanRequestMemberIdOrLoanMemberId(@Param("memberId") Long memberId);

    // Recherche par membre avec statuts spécifiques - CORRIGÉ
    @Query("SELECT r FROM Repayment r WHERE (r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId) AND r.status IN :statuses")
    List<Repayment> findByStatusInAndLoanRequestMemberIdOrLoanMemberId(
            @Param("statuses") List<String> statuses,
            @Param("memberId") Long memberId);

    // Recherche par statut et date d'échéance dépassée
    List<Repayment> findByStatusAndDueDateBefore(String status, Date date);

    // Calcul du total remboursé par membre
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Repayment r WHERE (r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId) AND r.status = 'PAID'")
    BigDecimal getTotalRepaidAmountByMember(@Param("memberId") Long memberId);

    // Calcul du montant en attente par membre
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Repayment r WHERE (r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId) AND r.status IN ('PENDING', 'OVERDUE')")
    BigDecimal getPendingAmountByMember(@Param("memberId") Long memberId);

    // Recherche des remboursements avec filtres multiples
    @Query("SELECT r FROM Repayment r WHERE " +
            "(:loanRequestId IS NULL OR r.loanRequest.id = :loanRequestId) AND " +
            "(:loanId IS NULL OR r.loan.id = :loanId) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:memberId IS NULL OR r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId) " +
            "ORDER BY r.dueDate DESC")
    List<Repayment> findWithFilters(@Param("loanRequestId") Long loanRequestId,
                                    @Param("loanId") Long loanId,
                                    @Param("status") String status,
                                    @Param("memberId") Long memberId);

    // Recherche des remboursements échus non payés
    @Query("SELECT r FROM Repayment r WHERE r.dueDate < :currentDate AND r.status IN ('PENDING', 'OVERDUE') ORDER BY r.dueDate ASC")
    List<Repayment> findOverdueRepayments(@Param("currentDate") Date currentDate);

    // Statistiques par période
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Repayment r WHERE r.status = 'PAID' AND r.repaymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRepaidAmountByPeriod(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Nombre de remboursements par statut
    @Query("SELECT COUNT(r) FROM Repayment r WHERE r.status = :status")
    Long countByStatus(@Param("status") String status);

    // Prochain remboursement dû pour un membre - CORRIGÉ (supprimé LIMIT 1)
    @Query("SELECT r FROM Repayment r WHERE (r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId) AND r.status IN ('PENDING', 'OVERDUE') ORDER BY r.dueDate ASC")
    List<Repayment> findNextDueRepaymentByMember(@Param("memberId") Long memberId);

    // Historique des remboursements avec pagination et tri
    @Query("SELECT r FROM Repayment r WHERE " +
            "(:memberId IS NULL OR r.loanRequest.member.id = :memberId OR r.loan.member.id = :memberId) AND " +
            "(:status IS NULL OR r.status = :status) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'dueDate' THEN r.dueDate END ASC, " +
            "CASE WHEN :sortBy = 'dueDateDesc' THEN r.dueDate END DESC, " +
            "CASE WHEN :sortBy = 'amount' THEN r.amount END ASC, " +
            "CASE WHEN :sortBy = 'amountDesc' THEN r.amount END DESC, " +
            "r.dueDate DESC")
    List<Repayment> findRepaymentHistory(@Param("memberId") Long memberId,
                                         @Param("status") String status,
                                         @Param("sortBy") String sortBy);

    // Recherche par référence de transaction
    List<Repayment> findByTransactionReference(String transactionReference);

    // Vérification si une référence de transaction existe déjà
    boolean existsByTransactionReference(String transactionReference);

    // Recherche par méthode de paiement
    List<Repayment> findByPaymentMethod(String paymentMethod);

    // Remboursements d'un prêt avec statut spécifique, triés par date d'échéance
    @Query("SELECT r FROM Repayment r WHERE r.loan.id = :loanId AND r.status = :status ORDER BY r.dueDate ASC")
    List<Repayment> findByLoanIdAndStatusOrdered(@Param("loanId") Long loanId, @Param("status") String status);

    // Remboursements d'une demande de prêt avec statut spécifique, triés par date d'échéance
    @Query("SELECT r FROM Repayment r WHERE r.loanRequest.id = :loanRequestId AND r.status = :status ORDER BY r.dueDate ASC")
    List<Repayment> findByLoanRequestIdAndStatusOrdered(@Param("loanRequestId") Long loanRequestId, @Param("status") String status);
}