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

    public Long getId() {
        return id;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public Date getRepaymentDate() {
        return repaymentDate;
    }

    public String getStatus() {
        return status;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public LoanRequest getLoanRequest() {
        return loanRequest;
    }

    @Temporal(TemporalType.DATE)
    private Date repaymentDate;

    private String status; // PENDING, PAID, OVERDUE, CANCELLED

    private Integer installmentNumber; // Numéro de l'échéance (1, 2, 3...)

    @Temporal(TemporalType.DATE)
    private Date dueDate; // Date d'échéance

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id")
    private LoanRequest loanRequest;

    // Méthode utilitaire pour vérifier si le remboursement est en retard
    public boolean isOverdue() {
        return dueDate != null && new Date().after(dueDate) && !"PAID".equals(status);
    }
}