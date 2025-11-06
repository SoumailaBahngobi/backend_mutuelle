package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Role;
import lombok.*;
import org.springframework.web.bind.annotation.RequestMapping;



@Setter
@RequestMapping("/mutuelle")
public class AuthRequest {

    private String name;
    private String firstName;

    public String getNpi() {
        return npi;
    }

    public void setNpi(String npi) {
        this.npi = npi;
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Enum<Role> getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    private String npi;
    private String phone;
    private Role role;
    private String email;

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    private String password;
}
