
package com.wbf.mutuelle.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest,Long> {

    @Query("SELECT lr FROM LoanRequest lr LEFT JOIN FETCH lr.member ORDER BY lr.requestDate DESC")
    List<LoanRequest> findAllWithMember();

    List<LoanRequest> findByMember(Member member);
    List<LoanRequest> findByMemberId(Long memberId);
    List<LoanRequest> findByStatus(String status);

    List<LoanRequest> findByPresidentApproved(Boolean approved);
    List<LoanRequest> findBySecretaryApproved(Boolean approved);
    List<LoanRequest> findByTreasurerApproved(Boolean approved);

    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = 'PENDING' OR lr.status = 'IN_REVIEW'")
    List<LoanRequest> findActiveRequests();

    // CORRECTION : Implémenter la méthode findByMemberEmail
    @Query("SELECT lr FROM LoanRequest lr WHERE lr.member.email = :email")
    List<LoanRequest> findByMemberEmail(@Param("email") String email);

    List<LoanRequest> findByMemberIdAndStatusIn(Long memberId, List<String> statuses);

    @Query("SELECT lr FROM LoanRequest lr WHERE " +
            "(:status IS NULL OR lr.status = :status) AND " +
            "(:memberId IS NULL OR lr.member.id = :memberId) " +
            "ORDER BY lr.requestDate DESC")
    List<LoanRequest> findAllWithFilters(@Param("status") String status, @Param("memberId") Long memberId);

    @Query("SELECT lr FROM LoanRequest lr WHERE " +
            "(:role = 'PRESIDENT' AND lr.presidentApproved = false) OR " +
            "(:role = 'SECRETARY' AND lr.secretaryApproved = false) OR " +
            "(:role = 'TREASURER' AND lr.treasurerApproved = false)")
    List<LoanRequest> findPendingApprovalsByRole(@Param("role") String role);

    List<LoanRequest> findByStatusAndIsRepaid(String status, Boolean isRepaid);

    // ✅ NOUVELLE MÉTHODE : Trouver les demandes approuvées non accordées
    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = 'APPROVED' AND lr.loanGranted = false")
    List<LoanRequest> findApprovedPendingGrant();

    // ✅ NOUVELLE MÉTHODE : Trouver les demandes accordées
    @Query("SELECT lr FROM LoanRequest lr WHERE lr.loanGranted = true")
    List<LoanRequest> findGrantedLoanRequests();
}