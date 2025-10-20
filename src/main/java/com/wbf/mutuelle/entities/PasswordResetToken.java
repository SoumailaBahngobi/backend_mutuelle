package com.wbf.mutuelle.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = Member.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "member_id")
    private Member member;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // Constructeurs
    public PasswordResetToken() {}

    public PasswordResetToken(String token, Member member) {
        this.token = token;
        this.member = member;
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}