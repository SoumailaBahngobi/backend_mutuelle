package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Setter
@Getter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String firstName;
    private String email;
    private String password;
    private String npi;
    private String phone;

    @Column(name = "profile_image")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Champs pour la gestion des prêts
    @Column(name = "is_regular")
    private Boolean isRegular = false;

    @Column(name = "has_previous_debt")
    private Boolean hasPreviousDebt = false;

    @Column(name = "last_subscription_date")
    private LocalDate lastSubscriptionDate;

    @Column(name = "subscription_status")
    private String subscriptionStatus = "PENDING";

    // RELATIONS AVEC LES PRÊTS - AJOUTEZ CES DEUX LIGNES
    @JsonIgnore
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanRequest> loanRequests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Loan> loans = new ArrayList<>();

    public Member() {
    }

    public Member(Long id, String name, String firstName, String email,
                  String password, String npi, String phone, Role role) {
        this.id = id;
        this.name = name;
        this.firstName = firstName;
        this.email = email;
        this.password = password;
        this.npi = npi;
        this.phone = phone;
        this.role = role;
        this.isRegular = false;
        this.hasPreviousDebt = false;
        this.loanRequests = new ArrayList<>();
        this.loans = new ArrayList<>();
    }

    public boolean canRequestLoan() {
        return isSubscriptionActive() &&
                !Boolean.TRUE.equals(hasPreviousDebt) &&
                hasNoActiveLoans();
    }

    private boolean hasNoActiveLoans() {
        if (loans == null || loans.isEmpty()) {
            return true;
        }
        return loans.stream()
                .noneMatch(loan -> loan != null && !loan.getIsRepaid());
    }

    public boolean isSubscriptionActive() {
        return "ACTIVE".equals(subscriptionStatus) && Boolean.TRUE.equals(isRegular);
    }

    public boolean isPresident() {
        return role != null && "PRESIDENT".equalsIgnoreCase(role.name());
    }

    public boolean isSecretary() {
        return role != null && "SECRETARY".equalsIgnoreCase(role.name());
    }

    public boolean isTreasurer() {
        return role != null && "TREASURER".equalsIgnoreCase(role.name());
    }

    public boolean isAdmin() {
        return role != null && "ADMIN".equalsIgnoreCase(role.name());
    }

    // Méthodes utilitaires pour éviter les NullPointerException
    public List<LoanRequest> getLoanRequests() {
        if (this.loanRequests == null) {
            this.loanRequests = new ArrayList<>();
        }
        return loanRequests;
    }

    public List<Loan> getLoans() {
        if (this.loans == null) {
            this.loans = new ArrayList<>();
        }
        return loans;
    }
}