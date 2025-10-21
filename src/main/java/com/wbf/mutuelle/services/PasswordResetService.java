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
    public void generateAndSendResetToken(Member member) {
        try {
            log.info("Génération du token pour le membre: {}", member.getEmail());

            // Supprimer les tokens existants
            PasswordResetToken existingToken = passwordResetTokenRepository.findByMember(member);
            if (existingToken != null) {
                passwordResetTokenRepository.delete(existingToken);
                passwordResetTokenRepository.flush();
            }

            // Générer un nouveau token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setMember(member);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(24));

            passwordResetTokenRepository.save(resetToken);
            log.info("Token généré avec succès pour: {}", member.getEmail());

            // Envoyer l'email
            sendPasswordResetEmail(member, token);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du token pour {}: {}", member.getEmail(), e.getMessage());
            throw new RuntimeException("Erreur lors de la génération du token de réinitialisation", e);
        }
    }

    private void sendPasswordResetEmail(Member member, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            // ✅ SOLUTION SIMPLE : Affichage clair + ouverture navigateur
            displayResetLink(member, resetLink, token);

            // ✅ OPTIONNEL : Essayer d'ouvrir dans le navigateur
            openInBrowser(resetLink);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email: {}", e.getMessage());
        }
    }

    private void displayResetLink(Member member, String resetLink, String token) {
        // Affichage très visible dans la console
        System.out.println("\n\n");
        System.out.println("🚀 " + "=".repeat(70));
        System.out.println("🚀 RÉINITIALISATION DE MOT DE PASSE - LIEN DISPONIBLE");
        System.out.println("🚀 " + "=".repeat(70));
        System.out.println("📧 Destinataire: " + member.getEmail());
        System.out.println("👤 Utilisateur: " + member.getFirstName() + " " + member.getName());
        System.out.println("🔗 LIEN DIRECT: " + resetLink);
        System.out.println("🔑 Token: " + token);
        System.out.println("⏰ Expire le: " + LocalDateTime.now().plusHours(24));
        System.out.println("🚀 " + "=".repeat(70));
        System.out.println("💡 Copiez le lien ci-dessus dans votre navigateur pour tester");
        System.out.println("🚀 " + "=".repeat(70));
        System.out.println("\n\n");

        // Logs standards
        log.info("Lien de réinitialisation généré pour {}: {}", member.getEmail(), resetLink);
    }

    private void openInBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();

            if (os.contains("win")) {
                // Windows
                rt.exec("cmd /c start " + url);
            } else if (os.contains("mac")) {
                // macOS
                rt.exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux/Unix
                rt.exec(new String[]{"xdg-open", url});
            }
            log.info("✅ Navigateur ouvert avec le lien de réinitialisation");
        } catch (Exception e) {
            log.info("ℹ️  Lien affiché dans la console - copiez-le manuellement");
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

            Member member = resetToken.getMember();
            member.setPassword(passwordEncoder.encode(newPassword));
            memberRepository.save(member);

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
                passwordResetTokenRepository.delete(resetToken);
            }

            log.info("Validation du token {}: {}", token, isValid ? "VALIDE" : "INVALIDE");
            return isValid;

        } catch (Exception e) {
            log.error("Erreur lors de la validation du token: {}", e.getMessage());
            return false;
        }
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'email: " + email));
    }

    // ✅ NOUVELLE MÉTHODE : Récupérer le membre par token
    public Member getMemberByResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null) {
            throw new RuntimeException("Token non trouvé");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Token expiré");
        }

        return resetToken.getMember();
    }

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
}