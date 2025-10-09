package com.wbf.mutuelle.controllers;
import com.wbf.mutuelle.services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController


public class PasswordResetController {
   // @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/reset_all_passwords")
    public String resetAllPasswords() {
        passwordResetService.resetAllPasswordsToBCrypt();
        return "Tous les mots de passe ont été réinitialisés en BCrypt";
    }

    @PostMapping("/reset_password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {
        passwordResetService.resetMemberPassword(email, newPassword);
        return "Mot de passe réinitialisé pour: " + email;
    }
}
