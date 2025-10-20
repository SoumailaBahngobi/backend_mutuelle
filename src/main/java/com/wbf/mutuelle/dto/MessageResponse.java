package com.wbf.mutuelle.dto;

public class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    // Getter et Setter
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}