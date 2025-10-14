package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    List<Repayment> findByLoanRequestId(Long loanRequestId);

    List<Repayment> findByLoanRequestIdAndStatus(Long loanRequestId, String status);

    Optional<Repayment> findFirstByLoanRequestIdAndStatusOrderByDueDateAsc(Long loanRequestId, String status);

    // Loan-based queries
    List<Repayment> findByLoanId(Long loanId);

    Optional<Repayment> findFirstByLoanIdAndStatusOrderByDueDateAsc(Long loanId, String status);

    @Query("SELECT SUM(r.amount) FROM Repayment r WHERE r.loanRequest.id = :loanRequestId AND r.status = 'PAID'")
    BigDecimal getTotalRepaidAmount(@Param("loanRequestId") Long loanRequestId);

    List<Repayment> findByStatus(String status);
}