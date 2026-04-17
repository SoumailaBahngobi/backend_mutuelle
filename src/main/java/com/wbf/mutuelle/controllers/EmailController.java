package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.EmailRequest;
import com.wbf.mutuelle.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mutuelle/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        try {
            emailService.sendSimpleEmail(request.getTo(), request.getSubject(), request.getBody());
            return ResponseEntity.ok(" Email envoyé avec succès !");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(" Erreur : " + e.getMessage());
        }
    }

    @PostMapping("/welcome")
    public ResponseEntity<String> sendWelcome(@RequestParam String email, @RequestParam String firstName, @RequestParam String password) {
        try {
            String subject = " Bienvenue sur la Mutuelle WBF !";
            String body = String.format("""
                Bonjour %s,
                
                 Votre compte a été créé avec succès sur la plateforme Mutuelle WBF.
                
                 Vos informations de connexion :
                 Email : %s
                 Mot de passe : %s
                
                 Accédez à votre espace : http://localhost:3000/login
                
                Cordialement,
                L'équipe Mutuelle WBF
                """, firstName, email, password);

            emailService.sendSimpleEmail(email, subject, body);
            return ResponseEntity.ok(" Email de bienvenue envoyé !");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(" Erreur : " + e.getMessage());
        }
    }
}