package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Payment;
import com.wbf.mutuelle.PaymentStatus;
import com.wbf.mutuelle.PaymentType;
import com.wbf.mutuelle.entities.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // =============================================
    // RECHERCHES PAR ID ET TRANSACTION
    // =============================================

    Optional<Payment> findByTransactionId(String transactionId);
    boolean existsByTransactionId(String transactionId);


    // =============================================
    // RECHERCHES PAR MEMBRE
    // =============================================

    List<Payment> findByMemberId(Long memberId);
    List<Payment> findByMemberIdAndStatus(Long memberId, PaymentStatus status);
    List<Payment> findByMemberIdAndPaymentType(Long memberId, PaymentType paymentType);
    List<Payment> findByMemberIdAndPaymentTypeAndStatus(Long memberId, PaymentType paymentType, PaymentStatus status);
    List<Payment> findByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, PaymentStatus status);


    // =============================================
    // RECHERCHES PAR STATUT
    // =============================================

    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);


    // =============================================
    // RECHERCHES PAR TYPE DE PAIEMENT
    // =============================================

    List<Payment> findByPaymentType(PaymentType paymentType);
    List<Payment> findByPaymentTypeAndStatus(PaymentType paymentType, PaymentStatus status);
    List<Payment> findByPaymentTypeOrderByCreatedAtDesc(PaymentType paymentType);


    // =============================================
    // RECHERCHES PAR DATE
    // =============================================

    List<Payment> findByCreatedAtAfter(LocalDateTime date);
    List<Payment> findByCreatedAtBefore(LocalDateTime date);
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Payment> findByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime date);
    List<Payment> findByMemberIdAndCreatedAtBetween(Long memberId, LocalDateTime start, LocalDateTime end);
    List<Payment> findByStatusAndCreatedAtAfter(PaymentStatus status, LocalDateTime date);
    List<Payment> findByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
    List<Payment> findByPaymentTypeAndCreatedAtAfter(PaymentType paymentType, LocalDateTime date);
    List<Payment> findByPaymentTypeAndStatusAndCreatedAtAfter(PaymentType paymentType, PaymentStatus status, LocalDateTime date);


    // =============================================
    // RECHERCHES PAR NUMÉRO DE TÉLÉPHONE
    // =============================================

    List<Payment> findByPhoneNumber(String phoneNumber);
    List<Payment> findByPhoneNumberAndStatus(String phoneNumber, PaymentStatus status);


    // =============================================
    // STATISTIQUES ET AGRÉGATS
    // =============================================

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalSuccessfulPaymentsAmount();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.member.id = :memberId")
    BigDecimal getTotalSuccessfulPaymentsByMember(@Param("memberId") Long memberId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentType = :paymentType AND p.status = 'SUCCESS'")
    BigDecimal getTotalAmountByPaymentType(@Param("paymentType") PaymentType paymentType);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentType = :paymentType AND p.status = 'SUCCESS' AND p.member.id = :memberId")
    BigDecimal getTotalAmountByPaymentTypeAndMember(@Param("paymentType") PaymentType paymentType, @Param("memberId") Long memberId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS' AND p.member.id = :memberId")
    Long countSuccessfulPaymentsByMember(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentType = :paymentType")
    Long countByPaymentType(@Param("paymentType") PaymentType paymentType);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.createdAt BETWEEN :start AND :end AND p.status = 'SUCCESS'")
    BigDecimal getTotalAmountBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p.status, COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p GROUP BY p.status")
    List<Object[]> getPaymentStatisticsByStatus();

    @Query("SELECT p.paymentType, COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' GROUP BY p.paymentType")
    List<Object[]> getPaymentStatisticsByType();


    // =============================================
    // MÉTHODES PAR DÉFAUT (AVEC CALCUL DE DATES EN JAVA)
    // =============================================

    default List<Payment> findLast7DaysPayments() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return findByCreatedAtAfter(sevenDaysAgo);
    }

    default List<Payment> findLast30DaysPayments() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return findByCreatedAtAfter(thirtyDaysAgo);
    }

    default List<Payment> findSuccessfulPaymentsLast7Days() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return findByStatusAndCreatedAtAfter(PaymentStatus.SUCCESS, sevenDaysAgo);
    }

    default List<Payment> findMemberPaymentsLast30Days(Long memberId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return findByMemberIdAndCreatedAtAfter(memberId, thirtyDaysAgo);
    }

    default List<Payment> findPendingPaymentsOlderThan(int days) {
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        return findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, date);
    }

    default List<Payment> findRecentPaymentsByStatus(PaymentStatus status) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return findByStatusAndCreatedAtAfter(status, sevenDaysAgo);
    }

    default List<Payment> findRecentMemberPaymentsByStatus(Long memberId, PaymentStatus status) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return findByMemberIdAndStatusAndCreatedAtAfter(memberId, status, thirtyDaysAgo);
    }

    // =============================================
    // RELATION AVEC CONTRIBUTION
    // =============================================

    @Query("SELECT c FROM Contribution c WHERE c.payment.id = :paymentId")
    Optional<Contribution> findContributionByPaymentId(@Param("paymentId") Long paymentId);

    @Query("SELECT COUNT(c) > 0 FROM Contribution c WHERE c.payment.id = :paymentId")
    boolean hasAssociatedContribution(@Param("paymentId") Long paymentId);


    // =============================================
    // RECHERCHES COMPLEXES
    // =============================================

    @Query("SELECT p FROM Payment p WHERE " +
            "(:memberId IS NULL OR p.member.id = :memberId) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:paymentType IS NULL OR p.paymentType = :paymentType) AND " +
            "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR p.createdAt <= :endDate) " +
            "ORDER BY p.createdAt DESC")
    List<Payment> searchPayments(@Param("memberId") Long memberId,
                                 @Param("status") PaymentStatus status,
                                 @Param("paymentType") PaymentType paymentType,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.contribution WHERE p.member.id = :memberId ORDER BY p.createdAt DESC")
    List<Payment> findMemberPaymentsWithContributions(@Param("memberId") Long memberId);

    @Query("SELECT p FROM Payment p WHERE p.amount > :minAmount AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPaymentsAboveAmount(@Param("minAmount") BigDecimal minAmount,
                                                @Param("since") LocalDateTime since);

    // Méthode utilitaire pour findByStatusAndCreatedAtBefore (nécessaire pour les méthodes par défaut)
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime date);

    // Méthode utilitaire pour findByMemberIdAndStatusAndCreatedAtAfter
    List<Payment> findByMemberIdAndStatusAndCreatedAtAfter(Long memberId, PaymentStatus status, LocalDateTime date);
}