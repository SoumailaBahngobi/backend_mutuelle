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
@Table(name = "loan_request")
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loan_request")
    private Long id;

    @Column(name = "request_amount", nullable = false)
    private BigDecimal requestAmount;

    @Column(nullable = false)
    private Integer duration;

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

    @Column(name = "interest_rate")
    private BigDecimal interestRate = new BigDecimal("5.0");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public Boolean isFullyApproved() {
        return Boolean.TRUE.equals(presidentApproved) &&
                Boolean.TRUE.equals(secretaryApproved) &&
                Boolean.TRUE.equals(treasurerApproved);
    }

    public Boolean getIsRepaid() {
        return isRepaid;
    }

    public void setIsRepaid(Boolean isRepaid) {
        this.isRepaid = isRepaid;
    }
}