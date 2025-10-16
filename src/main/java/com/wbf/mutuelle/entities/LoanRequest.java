package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "loan_request")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loan_request")
    private Long id;

    @Column(name = "request_amount", nullable = false)
    private BigDecimal requestAmount;

    @Column(nullable = false)
    private Integer duration;

    public BigDecimal getRequestAmount() {
        return requestAmount;
    }

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "is_repaid")
    private Boolean isRepaid = false;

    @Column(name = "accept_terms")
    private Boolean acceptTerms = false;

    @Temporal(TemporalType.DATE)
    @Column(name = "request_date", nullable = false)
    private Date requestDate = new Date();

    // Validations par les responsables
    @Column(name = "president_approved")
    private Boolean presidentApproved = false;

    @Column(name = "secretary_approved")
    private Boolean secretaryApproved = false;

    @Column(name = "treasurer_approved")
    private Boolean treasurerApproved = false;

    // Dates d'approbation
    @Column(name = "president_approval_date")
    private Date presidentApprovalDate;

    @Column(name = "secretary_approval_date")
    private Date secretaryApprovalDate;

    @Column(name = "treasurer_approval_date")
    private Date treasurerApprovalDate;

    // Commentaires des validateurs
    @Column(name = "president_comment")
    private String presidentComment;

    @Column(name = "secretary_comment")
    private String secretaryComment;

    @Column(name = "treasurer_comment")
    private String treasurerComment;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "interest_rate")
    private BigDecimal interestRate = new BigDecimal("0");

    // ✅ NOUVEAU CHAMP : Indique si le prêt a été créé automatiquement
    @Column(name = "loan_created")
    private Boolean loanCreated = false;

    // ✅ NOUVEAUX CHAMPS : Gestion par le trésorier
    @Column(name = "loan_granted")
    private Boolean loanGranted = false;

    @Column(name = "loan_granted_date")
    private Date loanGrantedDate;

    @Column(name = "treasurer_grant_comment")
    private String treasurerGrantComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Méthodes utilitaires
    public Boolean isFullyApproved() {
        return Boolean.TRUE.equals(presidentApproved) &&
                Boolean.TRUE.equals(secretaryApproved) &&
                Boolean.TRUE.equals(treasurerApproved);
    }

    public Boolean hasAnyApproval() {
        return Boolean.TRUE.equals(presidentApproved) ||
                Boolean.TRUE.equals(secretaryApproved) ||
                Boolean.TRUE.equals(treasurerApproved);
    }

    public String getApprovalProgress() {
        int approved = 0;
        if (Boolean.TRUE.equals(presidentApproved)) approved++;
        if (Boolean.TRUE.equals(secretaryApproved)) approved++;
        if (Boolean.TRUE.equals(treasurerApproved)) approved++;
        return approved + "/3";
    }

    public Boolean getIsRepaid() {
        return isRepaid;
    }

    public void setIsRepaid(Boolean isRepaid) {
        this.isRepaid = isRepaid;
    }

    @Transient
    private Map<String, Object> approvalProgress;

    public void setApprovalProgress(Map<String, Object> approvalProgress) {
        this.approvalProgress = approvalProgress;
    }

    @OneToMany(mappedBy = "loanRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Repayment> repayments;

    public List<Repayment> getRepayments() {
        return repayments;
    }

    // ✅ NOUVELLE MÉTHODE : Vérifier si le prêt peut être accordé
    public Boolean canBeGranted() {
        return "APPROVED".equals(this.status) &&
                Boolean.TRUE.equals(this.presidentApproved) &&
                Boolean.TRUE.equals(this.secretaryApproved) &&
                Boolean.TRUE.equals(this.treasurerApproved) &&
                !Boolean.TRUE.equals(this.loanGranted);
    }
}