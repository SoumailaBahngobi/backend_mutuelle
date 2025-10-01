/*package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;           // montant emprunté
    private Integer duration;            // durée en mois
    private Date beginDate;              // date de début du prêt
    private BigDecimal repaymentAmount;// montant à rembourser
    private Date endDate;

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isRepaid() {
        return isRepaid;
    }

    public void setRepaid(boolean repaid) {
        isRepaid = repaid;
    }

    private boolean isRepaid;

    @ManyToOne // un membre peut avoir plusieurs prêts
    @JoinColumn(name = "member_id")
    private Member member;

    public void setId(Long id) {
        this.id = id;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public void setRepaymentAmount(BigDecimal repaymentAmount) {
        this.repaymentAmount = repaymentAmount;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getDuration() {
        return duration;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public BigDecimal getRepaymentAmount() {
        return repaymentAmount;
    }

    public Member getMember() {
        return member;
    }

    public Loan(Long id, BigDecimal amount, Integer duration, Date beginDate, BigDecimal repaymentAmount, Member member) {
        this.id = id;
        this.amount = amount;
        this.duration = duration;
        this.beginDate = beginDate;
        this.repaymentAmount = repaymentAmount;
        this.member = member;
    }

    public void setIsRepaid(boolean b) {
        this.isRepaid = isRepaid;
    }
}
*/

package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

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
    private BigDecimal interestRate = new BigDecimal("5.0");

    @Column(name = "is_repaid")
    private Boolean isRepaid = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id")
    private LoanRequest loanRequest;

    public Boolean getIsRepaid() {
        return isRepaid;
    }

    public void setIsRepaid(Boolean isRepaid) {
        this.isRepaid = isRepaid;
    }
}