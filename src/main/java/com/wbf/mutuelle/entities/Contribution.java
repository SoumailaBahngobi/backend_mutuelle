package com.wbf.mutuelle.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "contribution")
@Getter
@Setter
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type")
    private ContributionType contributionType;

    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date paymentDate;

    private BigDecimal amount;

    private String paymentMode;

    // private String paymentProofFileName;
    private String paymentProof;


    public String getPaymentProof() {
        return paymentProof;
    }

    public void setPaymentProof(String paymentProof) {
        this.paymentProof = paymentProof;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ContributionType getContributionType() {
        return contributionType;
    }

    public void setContributionType(ContributionType contributionType) {
        this.contributionType = contributionType;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public ContributionPeriod getContributionPeriod() {
        return contributionPeriod;
    }

    public void setContributionPeriod(ContributionPeriod contributionPeriod) {
        this.contributionPeriod = contributionPeriod;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    // Cas INDIVIDUELLE
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // Cas GROUPEE
    @ManyToMany
    @JoinTable(
            name = "contribution_members",
            joinColumns = @JoinColumn(name = "contribution_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private List<Member> members;

    // Une cotisation est toujours rattachée à une période
    @ManyToOne
    @JoinColumn(name = "contribution_period_id")
    private ContributionPeriod contributionPeriod;

    // Getter pour la balance (calculé dynamiquement)
    // Cette méthode devrait être implémentée dans le service
    // qui calculera la somme totale des cotisations
    // Méthode pour calculer la balance dynamiquement (ne pas persister)
   // @Transient
   // @JsonIgnore
    private BigDecimal balance;

    // Constructeurs
    public Contribution() {
    }

    public Contribution(ContributionType contributionType, BigDecimal amount, Date paymentDate) {
        this.contributionType = contributionType;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }
}