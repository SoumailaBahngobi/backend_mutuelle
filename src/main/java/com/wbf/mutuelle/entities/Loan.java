package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private BigDecimal interestRate; // Ajout du champ taux d'intérêt

    @Column(name = "is_repaid")
    private Boolean isRepaid = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id")
    private LoanRequest loanRequest;

    // Relation avec les remboursements
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Repayment> repayments;

    public Boolean getIsRepaid() {
        return isRepaid;
    }

    public void setIsRepaid(Boolean isRepaid) {
        this.isRepaid = isRepaid;
    }

    public BigDecimal calculateRemainingBalance() {
        // Si le prêt est marqué comme remboursé, retourner 0 directement
        if (Boolean.TRUE.equals(this.isRepaid)) {
            return BigDecimal.ZERO;
        }

        // Valider le montant du prêt
        if (this.amount == null || this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRepaid = BigDecimal.ZERO;

        // Calculer le total des remboursements effectués
        if (this.repayments != null && !this.repayments.isEmpty()) {
            totalRepaid = this.repayments.stream()
                .filter(repayment -> repayment != null)
                .filter(repayment -> repayment.getAmount() != null)
                .filter(repayment -> repayment.getStatus() != null)
                .filter(repayment -> "COMPLETED".equalsIgnoreCase(repayment.getStatus()) || 
                                    "PAID".equalsIgnoreCase(repayment.getStatus()))
                .map(Repayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calculer le montant total dû (capital + intérêts)
        BigDecimal totalDue = calculateTotalDue();

        // Calculer le solde restant
        BigDecimal remainingBalance = totalDue.subtract(totalRepaid);

        // S'assurer que le solde n'est pas négatif
        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.isRepaid = true; // Mettre à jour le statut si complètement remboursé
            return BigDecimal.ZERO;
        }

        return remainingBalance.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant total dû (capital + intérêts)
     */
    public BigDecimal calculateTotalDue() {
        if (this.amount == null) {
            return BigDecimal.ZERO;
        }

        // Si pas de taux d'intérêt, retourner juste le capital
        if (this.interestRate == null || this.interestRate.compareTo(BigDecimal.ZERO) == 0) {
            return this.amount;
        }

        // Calculer les intérêts
        BigDecimal interest = this.amount.multiply(this.interestRate)
                                       .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Retourner capital + intérêts
        return this.amount.add(interest);
    }

    /**
     * Calcule le montant de chaque mensualité
     */
    public BigDecimal calculateMonthlyPayment() {
        if (this.duration == null || this.duration <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDue = calculateTotalDue();
        return totalDue.divide(BigDecimal.valueOf(this.duration), 2, RoundingMode.HALF_UP);
    }

    /**
     * Vérifie si le prêt est en retard de paiement
     */
    public boolean isOverdue() {
        if (this.endDate == null || Boolean.TRUE.equals(this.isRepaid)) {
            return false;
        }

        Date today = new Date();
        return today.after(this.endDate) && calculateRemainingBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calcule le nombre de jours de retard
     */
    public long calculateDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }

        Date today = new Date();
        long diff = today.getTime() - this.endDate.getTime();
        return diff / (1000 * 60 * 60 * 24); // Conversion en jours
    }

    /**
     * Met à jour le statut isRepaid en fonction du solde
     */
    public void updateRepaymentStatus() {
        BigDecimal remainingBalance = calculateRemainingBalance();
        this.isRepaid = remainingBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Calcule le pourcentage de remboursement
     */
    public BigDecimal calculateRepaymentPercentage() {
        BigDecimal totalDue = calculateTotalDue();
        if (totalDue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRepaid = BigDecimal.ZERO;
        if (this.repayments != null && !this.repayments.isEmpty()) {
            totalRepaid = this.repayments.stream()
                .filter(repayment -> repayment != null && repayment.getAmount() != null)
                .filter(repayment -> repayment.getStatus() != null)
                .filter(repayment -> "COMPLETED".equalsIgnoreCase(repayment.getStatus()) || 
                                    "PAID".equalsIgnoreCase(repayment.getStatus()))
                .map(Repayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal percentage = totalRepaid.multiply(BigDecimal.valueOf(100))
                                         .divide(totalDue, 2, RoundingMode.HALF_UP);

        return percentage.compareTo(BigDecimal.valueOf(100)) > 0 ? 
               BigDecimal.valueOf(100) : percentage;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", amount=" + amount +
                ", duration=" + duration +
                ", beginDate=" + beginDate +
                ", endDate=" + endDate +
                ", isRepaid=" + isRepaid +
                ", interestRate=" + interestRate +
                '}';
    }

    // Méthode utilitaire pour formater le montant
    public String getFormattedAmount() {
        if (this.amount == null) {
            return "0.00";
        }
        return this.amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    // Méthode utilitaire pour formater le solde restant
    public String getFormattedRemainingBalance() {
        return calculateRemainingBalance().toString();
    }
}