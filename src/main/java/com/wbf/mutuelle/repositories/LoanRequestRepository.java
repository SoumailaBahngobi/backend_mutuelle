package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

  //  List<LoanRequest> findByMemberId(Long memberId);

    @Query("SELECT lr FROM LoanRequest lr WHERE lr.member.email = :email")
    List<LoanRequest> findByMemberEmail(@Param("email") String email);

    @Query("SELECT lr FROM LoanRequest lr JOIN FETCH lr.member")
    List<LoanRequest> findAllWithMember();

    List<LoanRequest> findByStatus(String status);

    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = :status AND lr.isRepaid = :isRepaid")
    List<LoanRequest> findByStatusAndIsRepaid(@Param("status") String status, @Param("isRepaid") boolean isRepaid);

    List<LoanRequest> findByPresidentApproved(boolean approved);
    List<LoanRequest> findBySecretaryApproved(boolean approved);
    List<LoanRequest> findByTreasurerApproved(boolean approved);

    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = :status AND lr.member.id = :memberId")
    List<LoanRequest> findAllWithFilters(@Param("status") String status, @Param("memberId") Long memberId);

    // ✅ NOUVELLE MÉTHODE : Récupérer les demandes approuvées en attente d'accord par le trésorier
    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = 'APPROVED' AND lr.loanGranted = false")
    List<LoanRequest> findApprovedPendingGrant();

    @Query("SELECT lr FROM LoanRequest lr WHERE lr.member.id = :memberId")
    List<LoanRequest> findByMemberId(@Param("memberId") Long memberId);


    @Query("SELECT lr FROM LoanRequest lr " +
            "LEFT JOIN FETCH lr.member " +
            "WHERE lr.member.id = :memberId")
    List<LoanRequest> findByMemberIdWithDetails(@Param("memberId") Long memberId);


    @Query("SELECT DISTINCT lr FROM LoanRequest lr " +
            "LEFT JOIN FETCH lr.member " +
            "LEFT JOIN FETCH lr.repayments " +
            "ORDER BY lr.id DESC")
    List<LoanRequest> findAllWithAllDetails();

}