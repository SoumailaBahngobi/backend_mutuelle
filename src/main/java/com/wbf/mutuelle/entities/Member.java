package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "loanRequests", "loans"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;  // NOUVEAU - ID de l'utilisateur Keycloak
//Le nom et le prenom du membre
    private String name;
    private String firstName;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String password;  // Gardé pour compatibilité mais non utilisé

    private String npi;
    private String phone;
    private String profileImage;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Champs pour la gestion des prêts
   // @Column(name = "is_regular")
    private Boolean isRegular = false;

   // @Column(name = "has_previous_debt")
    private Boolean hasPreviousDebt = false;

    //@Column(name = "last_subscription_date")
    private LocalDate lastSubscriptionDate;

   // @Column(name = "subscription_status")
    private String subscriptionStatus = "PENDING";


    // Relations avec les prêts
    @JsonIgnore
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanRequest> loanRequests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Loan> loans = new ArrayList<>();

    // Constructeurs
    public Member() {}

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
        return role == Role.PRESIDENT;
    }

    public boolean isSecretary() {
        return role == Role.SECRETARY;
    }

    public boolean isTreasurer() {
        return role == Role.TREASURER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }
}