package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private Integer duration;

    @Temporal(TemporalType.DATE)
    private Date beginDate;

    @Temporal(TemporalType.DATE)
    private Date endDate;

    private BigDecimal repaymentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "is_repaid")
    private Boolean isRepaid = false;

    private String status = "ACTIVE"; // ACTIVE, REPAID, OVERDUE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id")
    private LoanRequest loanRequest;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Repayment> repayments = new ArrayList<>();

    // Méthodes utilitaires pour les remboursements
    public BigDecimal calculateRemainingBalance() {
        if (repayments == null || repayments.isEmpty()) {
            return repaymentAmount;
        }

        BigDecimal totalPaid = repayments.stream()
                .filter(repayment -> "PAID".equals(repayment.getStatus()))
                .map(Repayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return repaymentAmount.subtract(totalPaid);
    }

    public boolean isFullyPaid() {
        return calculateRemainingBalance().compareTo(BigDecimal.ZERO) <= 0;
    }

    public void updateLoanStatus() {
        BigDecimal remaining = calculateRemainingBalance();
        this.remainingAmount = remaining;
        this.amountPaid = repaymentAmount.subtract(remaining);

        if (isFullyPaid()) {
            this.isRepaid = true;
            this.status = "REPAID";
        } else if (new Date().after(endDate)) {
            this.status = "OVERDUE";
        } else {
            this.status = "ACTIVE";
        }
    }

    public BigDecimal getMonthlyInstallment() {
        if (duration == null || duration == 0) {
            return repaymentAmount;
        }
    return repaymentAmount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
    }

    public Boolean getIsRepaid() {
        return isRepaid;
    }

    public void setIsRepaid(Boolean isRepaid) {
        this.isRepaid = isRepaid;
    }
}