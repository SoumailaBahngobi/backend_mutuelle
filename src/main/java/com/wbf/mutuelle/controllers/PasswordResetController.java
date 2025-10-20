package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mut/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/reset-all")
    public String resetAllPasswords() {
        passwordResetService.resetAllPasswordsToBCrypt();
        return "Tous les mots de passe ont été réinitialisés en BCrypt";
    }

    @PostMapping("/reset")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {
        passwordResetService.resetMemberPassword(email, newPassword);
        return "Mot de passe réinitialisé pour: " + email;
    }
}