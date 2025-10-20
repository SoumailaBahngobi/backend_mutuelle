package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.PasswordResetToken;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.repositories.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void resetAllPasswordsToBCrypt() {
        List<Member> members = memberRepository.findAll();

        for (Member member : members) {
            // Réencoder le mot de passe en BCrypt
            String rawPassword = "password123"; // Mot de passe par défaut
            String encodedPassword = passwordEncoder.encode(rawPassword);
            member.setPassword(encodedPassword);
            memberRepository.save(member);

            log.info("Mot de passe réinitialisé pour: {}", member.getEmail());
        }
    }

    @Transactional
    public void resetMemberPassword(String email, String newRawPassword) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        String encodedPassword = passwordEncoder.encode(newRawPassword);
        member.setPassword(encodedPassword);
        memberRepository.save(member);

        log.info("Mot de passe réinitialisé pour: {}", email);
    }

    @Transactional
    public void generateAndSendResetToken(Member member) {
        try {
            log.info("Génération du token pour le membre: {}", member.getEmail());

            // Supprimer les tokens existants pour ce membre
            passwordResetTokenRepository.deleteByMember(member);

            // Générer un nouveau token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setMember(member);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(24)); // 24 heures

            passwordResetTokenRepository.save(resetToken);
            log.info("Token généré avec succès pour: {}", member.getEmail());

            // Envoyer l'email (simulation pour le moment)
            sendPasswordResetEmail(member, token);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du token pour {}: {}", member.getEmail(), e.getMessage());
            throw new RuntimeException("Erreur lors de la génération du token de réinitialisation", e);
        }
    }

    @Transactional
    public boolean resetPasswordWithToken(String token, String newPassword) {
        try {
            log.info("Tentative de réinitialisation avec token: {}", token);

            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);

            if (resetToken == null) {
                log.error("Token non trouvé: {}", token);
                return false;
            }

            if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.error("Token expiré: {}", token);
                passwordResetTokenRepository.delete(resetToken);
                return false;
            }

            // Mettre à jour le mot de passe
            Member member = resetToken.getMember();
            member.setPassword(passwordEncoder.encode(newPassword));
            memberRepository.save(member);

            // Supprimer le token utilisé
            passwordResetTokenRepository.delete(resetToken);

            log.info("Mot de passe réinitialisé avec succès pour: {}", member.getEmail());
            return true;

        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du mot de passe: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateResetToken(String token) {
        try {
            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
            if (resetToken == null) {
                log.warn("Token non trouvé: {}", token);
                return false;
            }

            boolean isValid = resetToken.getExpiryDate().isAfter(LocalDateTime.now());
            if (!isValid) {
                log.warn("Token expiré: {}", token);
                // Supprimer le token expiré
                passwordResetTokenRepository.delete(resetToken);
            }

            log.info("Validation du token {}: {}", token, isValid ? "VALIDE" : "INVALIDE");
            return isValid;

        } catch (Exception e) {
            log.error("Erreur lors de la validation du token: {}", e.getMessage());
            return false;
        }
    }

    private void sendPasswordResetEmail(Member member, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            // Simulation d'envoi d'email - À remplacer par un vrai service d'email
            log.info("=== EMAIL DE RÉINITIALISATION ===");
            log.info("À: {}", member.getEmail());
            log.info("Sujet: Réinitialisation de votre mot de passe");
            log.info("Lien: {}", resetLink);
            log.info("Token (pour test): {}", token);
            log.info("===============================");

            // Affichage console pour le développement
            System.out.println("\n" + "=".repeat(50));
            System.out.println("EMAIL DE RÉINITIALISATION DE MOT DE PASSE");
            System.out.println("=".repeat(50));
            System.out.println("Destinataire: " + member.getEmail());
            System.out.println("Nom: " + member.getFirstName() + " " + member.getName());
            System.out.println("Lien de réinitialisation: " + resetLink);
            System.out.println("Token (pour test direct): " + token);
            System.out.println("Ce lien expire le: " + LocalDateTime.now().plusHours(24));
            System.out.println("=".repeat(50) + "\n");

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi simulé de l'email: {}", e.getMessage());
        }
    }

    // Méthode utilitaire pour obtenir un membre par email
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'email: " + email));
    }
}