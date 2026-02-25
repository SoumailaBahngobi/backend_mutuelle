package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Role;

public class AuthRequest {

    private String name;
    private String firstName;
    private String npi;
    private String phone;
    private Role role; // C'est bien de type Role, pas Enum<Role>
    private String email;
    private String password;

    // Getters et Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getNpi() {
        return npi;
    }

    public void setNpi(String npi) {
        this.npi = npi;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() { // Retourne Role directement
        return role;
    }

    public void setRole(Role role) { // Accepte Role directement
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}