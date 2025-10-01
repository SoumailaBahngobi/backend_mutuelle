/*package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository <Loan,Long> {
    boolean existsByMemberAndIsRepaidFalse(Member member);
}
*/

package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan,Long> {
    boolean existsByMemberAndIsRepaidFalse(Member member);
    List<Loan> findByMember(Member member);
    List<Loan> findByIsRepaidFalse();
}