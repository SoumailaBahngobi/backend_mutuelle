package com.wbf.mutuelle.dto;

public class ResetPasswordRequest {
    private String token;
    private String newPassword;

    // Constructeurs
    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    // Getters et Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}