package com.wbf.mutuelle.dto;

public class ForgotPasswordRequest {
    private String email;

    // Constructeurs
    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    // Getters et Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}