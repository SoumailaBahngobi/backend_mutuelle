package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Contribution;
import com.wbf.mutuelle.entities.ContributionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    // Méthode de base pour trouver par type
    List<Contribution> findByContributionType(ContributionType contributionType);

    // Contributions individuelles d'un membre
    List<Contribution> findByContributionTypeAndMemberId(ContributionType contributionType, Long memberId);

    // Contributions groupées où le membre fait partie de la liste
    @Query("SELECT DISTINCT c FROM Contribution c WHERE c.contributionType = :contributionType AND EXISTS (SELECT m FROM c.members m WHERE m.id = :memberId)")
    List<Contribution> findGroupContributionsByMemberId(@Param("contributionType") ContributionType contributionType, @Param("memberId") Long memberId);

    // Toutes les contributions d'un membre (individuelles)
    List<Contribution> findByMemberId(Long memberId);

    // =============================================
    // NOUVELLES MÉTHODES POUR L'HISTORIQUE
    // =============================================

    // Récupérer toutes les cotisations d'un membre (individuelles ET groupées)
    @Query("SELECT c FROM Contribution c WHERE " +
           "c.member.id = :memberId OR " +
           ":memberId IN (SELECT m.id FROM c.members m)")
    List<Contribution> findByMemberIdOrMembersId(@Param("memberId") Long memberId);

    // Cotisations individuelles d'un membre (version améliorée)
    @Query("SELECT c FROM Contribution c WHERE c.member.id = :memberId AND c.contributionType = :contributionType")
    List<Contribution> findByMemberIdAndContributionType(@Param("memberId") Long memberId, 
                                                        @Param("contributionType") ContributionType contributionType);

    // Cotisations groupées où le membre est dans la liste des membres
    @Query("SELECT c FROM Contribution c WHERE :memberId IN (SELECT m.id FROM c.members m) AND c.contributionType = :contributionType")
    List<Contribution> findByMemberIdInMembersAndContributionType(@Param("memberId") Long memberId, 
                                                                 @Param("contributionType") ContributionType contributionType);

    // =============================================
    // STATISTIQUES - MÉTHODES EXISTANTES
    // =============================================

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c")
    BigDecimal calculateTotalBalance();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.member.id = :memberId")
    BigDecimal calculateBalanceByMemberId(@Param("memberId") Long memberId);

    // =============================================
    // NOUVELLES STATISTIQUES POUR L'HISTORIQUE
    // =============================================

    // Montant total de toutes les contributions (alias pour compatibilité)
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c")
    BigDecimal getTotalAmount();

    // Montant total des contributions d'un membre (individuelles ET groupées)
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE " +
           "c.member.id = :memberId OR :memberId IN (SELECT m.id FROM c.members m)")
    BigDecimal getTotalAmountByMember(@Param("memberId") Long memberId);

    // Montant total par type de contribution
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.contributionType = :contributionType")
    BigDecimal getTotalAmountByType(@Param("contributionType") ContributionType contributionType);

    // =============================================
    // MÉTHODES UTILITAIRES SUPPLÉMENTAIRES
    // =============================================

    // Trouver les contributions par période
    @Query("SELECT c FROM Contribution c WHERE c.contributionPeriod.id = :periodId")
    List<Contribution> findByContributionPeriodId(@Param("periodId") Long periodId);

    // Trouver les contributions d'un membre par période
    @Query("SELECT c FROM Contribution c WHERE (c.member.id = :memberId OR :memberId IN (SELECT m.id FROM c.members m)) AND c.contributionPeriod.id = :periodId")
    List<Contribution> findByMemberIdAndContributionPeriodId(@Param("memberId") Long memberId, 
                                                            @Param("periodId") Long periodId);

    // Dernières contributions d'un membre (avec limite)
    @Query("SELECT c FROM Contribution c WHERE c.member.id = :memberId OR :memberId IN (SELECT m.id FROM c.members m) ORDER BY c.paymentDate DESC")
    List<Contribution> findRecentContributionsByMemberId(@Param("memberId") Long memberId, 
                                                        org.springframework.data.domain.Pageable pageable);

    // Compter le nombre de contributions d'un membre
    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.member.id = :memberId OR :memberId IN (SELECT m.id FROM c.members m)")
    Long countContributionsByMemberId(@Param("memberId") Long memberId);
}