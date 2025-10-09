/*package com.wbf.mutuelle.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest,Long> {

  List<LoanRequest> findByMember(Member member);
    List<LoanRequest> findByMemberId(Long memberId);  
}
*/

package com.wbf.mutuelle.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest,Long> {
    List<LoanRequest> findByMember(Member member);
    List<LoanRequest> findByMemberId(Long memberId);
    List<LoanRequest> findByStatus(String status);


    List<LoanRequest> findByPresidentApproved(Boolean approved);
    List<LoanRequest> findBySecretaryApproved(Boolean approved);
    List<LoanRequest> findByTreasurerApproved(Boolean approved);

    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = 'PENDING' OR lr.status = 'IN_REVIEW'")
    List<LoanRequest> findActiveRequests();

    List<LoanRequest> findByMemberEmail(String email);


}
