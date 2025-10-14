package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "repayment")
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Temporal(TemporalType.DATE)
    private Date repaymentDate;

    @Temporal(TemporalType.DATE)
    private Date dueDate;

    private Integer installmentNumber;
    private Integer totalInstallments;

    private String status = "PENDING"; // PENDING, PAID, OVERDUE

    private String paymentMethod;
    private String transactionReference;
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id")
    private LoanRequest loanRequest;

    @PrePersist
    public void setDefaultDates() {
        if (repaymentDate == null) {
            repaymentDate = new Date();
        }
    }
}