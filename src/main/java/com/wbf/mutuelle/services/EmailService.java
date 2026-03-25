package com.wbf.mutuelle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email envoyé avec succès à {}", to);
        } catch (Exception e) {
            log.error(" Erreur lors de l'envoi de l'email à {}: {}", to, e.getMessage());
            throw new RuntimeException("Erreur d'envoi d'email", e);
        }
    }

    // ==================== EMAILS TRANSACTIONNELS ====================

    /**
     * Email de confirmation de cotisation
     */
    public void sendContributionConfirmation(String to, String memberName, Double amount, String period, String date) {
        String subject = "✅ Confirmation de votre cotisation - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Nous vous confirmons le paiement de votre cotisation.
            
            Détails de la transaction :
            ────────────────────────────────
            Montant : %.0f FCFA
            Période : %s
            Date : %s
            ────────────────────────────────
            
            Votre cotisation a bien été enregistrée.
            
            Consultez votre historique : http://localhost:3000/mutuelle/contribution/individual/my-contributions
            
            Besoin d'aide ? Contactez-nous à support@mutuelle-wbf.com
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, period, date);

        sendSimpleEmail(to, subject, body);
    }

    /**
     * Email de confirmation de demande de prêt
     */
    public void sendLoanRequestConfirmation(String to, String memberName, Double amount, String reason, String requestId) {
        String subject = "Demande de prêt reçue - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Nous accusons réception de votre demande de prêt.
            
            Détails de votre demande :
            ────────────────────────────────
            Montant demandé : %.0f FCFA
            Motif : %s
            N° demande : %s
            Statut : En attente de traitement
            ────────────────────────────────
            
            Vous serez informé dès que votre demande sera traitée.
            
            Suivez l'évolution : http://localhost:3000/loans/requests
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, reason, requestId);

        sendSimpleEmail(to, subject, body);
    }

    /**
     * Email de notification d'approbation de prêt
     */
    public void sendLoanApprovalEmail(String to, String memberName, Double amount, String approvalDate) {
        String subject = " Votre demande de prêt a été approuvée ! - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Félicitations ! Votre demande de prêt a été approuvée.
            
            Détails du prêt :
            ────────────────────────────────
            Montant : %.0f FCFA
            Statut : Approuvé
            Date d'approbation : %s
            ────────────────────────────────
            
            Les fonds seront disponibles après les analyses.
            
            Consultez votre prêt : http://localhost:3000/loans
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, approvalDate);

        sendSimpleEmail(to, subject, body);
    }

    /**
     * Email de notification de rejet de prêt
     */
    public void sendLoanRejectionEmail(String to, String memberName, Double amount, String reason) {
        String subject = " Mise à jour sur votre demande de prêt - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Nous avons examiné votre demande de prêt.
            
            Détails :
            ────────────────────────────────
            Montant demandé : %.0f FCFA
            Statut : Non approuvé
            Motif : %s
            ────────────────────────────────
            
            Vous pouvez faire une nouvelle demande après régularisation de votre situation.
            
            Pour plus d'informations, contactez-nous à support@mutuelle-wbf.com
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, reason);

        sendSimpleEmail(to, subject, body);
    }

    /**
     * Email de confirmation de remboursement de prêt
     */
    public void sendRepaymentConfirmation(String to, String memberName, Double amount, Double remainingBalance, String loanId) {
        String subject = " Confirmation de remboursement - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Nous vous confirmons le remboursement effectué sur votre prêt.
            
            Détails du remboursement :
            ────────────────────────────────
            Montant remboursé : %.0f FCFA
            Solde restant : %.0f FCFA
            N° prêt : %s
            ────────────────────────────────
            
            Consultez votre historique de remboursements : http://localhost:3000/loans/repayment-history
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, remainingBalance, loanId);

        sendSimpleEmail(to, subject, body);
    }

    /**
     * Email de rappel de cotisation
     */
    public void sendContributionReminder(String to, String memberName, Double amount, String dueDate) {
        String subject = " Rappel : Cotisation à venir - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Ceci est un rappel pour votre prochaine cotisation.
            
             Détails :
            ────────────────────────────────
             Montant : %.0f FCFA
             Date limite : %s
            ────────────────────────────────
            
             Effectuez votre paiement dès maintenant : http://localhost:3000/mutuelle/contribution/individual
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, dueDate);

        sendSimpleEmail(to, subject, body);
    }

    /**
     * Email de confirmation de paiement via Kkiapay
     */
    public void sendPaymentConfirmation(String to, String memberName, Double amount, String transactionId, String paymentType) {
        String subject = " Confirmation de paiement - Mutuelle WBF";
        String body = String.format("""
            Bonjour %s,
            
            Nous vous confirmons le paiement effectué via Kkiapay.
            
             Détails de la transaction :
            ────────────────────────────────
             Montant : %.0f FCFA
             Transaction : %s
             Type : %s
             Statut : Confirmé
            ────────────────────────────────
            
            Consultez votre historique : http://localhost:3000/mutuelle/contribution/my-contributions
            
            Cordialement,
            L'équipe Mutuelle WBF
            """, memberName, amount, transactionId, paymentType);

        sendSimpleEmail(to, subject, body);
    }
}